from __future__ import annotations

import json
import asyncio
from typing import Any

from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from pydantic import ValidationError

from app.config import settings
from app.gemini.realtime_ws_bridge import GeminiRealtimeBridge
from app.models import WsAudioMessage, WsDisconnectMessage, WsTextMessage

router = APIRouter(prefix="/vishing", tags=["vishing"])


@router.websocket("/ws")
async def vishing_websocket(websocket: WebSocket) -> None:
    await websocket.accept()
    await websocket.send_json({"event": "status", "message": "Connecting to Gemini..."})

    try:
        settings.ensure_api_key()
    except RuntimeError as exc:
        await websocket.send_json({"event": "error", "message": str(exc)})
        await websocket.close(code=1011)
        return

    bridge = GeminiRealtimeBridge(api_key=settings.gemini_api_key)

    try:
        await bridge.connect()
    except Exception as exc:
        await websocket.send_json({"event": "error", "message": f"Upstream connect failed: {exc}"})
        await websocket.close(code=1011)
        return

    async def pump_upstream() -> None:
        async def emit(event: dict[str, Any]) -> None:
            await websocket.send_json(event)

        try:
            await bridge.start_read_loop(emit)
        except Exception as exc:
            await websocket.send_json({"event": "error", "message": f"Upstream stream error: {exc}"})

    upstream_task = asyncio.create_task(pump_upstream())

    try:
        while True:
            raw = await websocket.receive_text()
            payload = json.loads(raw)
            message_type = payload.get("type")

            if message_type == "disconnect":
                _ = WsDisconnectMessage(**payload)
                await websocket.close(code=1000)
                break

            if message_type == "audio":
                audio_msg = WsAudioMessage(**payload)
                await bridge.send_audio(audio_msg.data_b64, audio_msg.mime_type)
                continue

            if message_type == "text":
                text_msg = WsTextMessage(**payload)
                await bridge.send_text(text_msg.text)
                continue

            await websocket.send_json({"event": "error", "message": "Unsupported message type"})

    except WebSocketDisconnect:
        pass
    except (json.JSONDecodeError, ValidationError) as exc:
        await websocket.send_json({"event": "error", "message": f"Invalid client message: {exc}"})
    finally:
        upstream_task.cancel()
        await bridge.close()
