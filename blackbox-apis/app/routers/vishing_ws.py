from __future__ import annotations

import asyncio
import contextlib
import json
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

    await websocket.send_json({"event": "status", "message": "Connected to Gemini"})

    # Serialize outbound events through one task to avoid concurrent send issues.
    event_queue: asyncio.Queue[dict[str, Any]] = asyncio.Queue(maxsize=500)

    async def pump_upstream() -> None:
        async def emit(event: dict[str, Any]) -> None:
            await event_queue.put(event)

        try:
            await bridge.start_read_loop(emit)
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            await event_queue.put({"event": "error", "message": f"Upstream stream error: {exc}"})

    upstream_task = asyncio.create_task(pump_upstream())

    async def _cancel_task(t: asyncio.Task[Any]) -> None:
        t.cancel()
        with contextlib.suppress(asyncio.CancelledError):
            await t

    try:
        while True:
            recv_task = asyncio.create_task(websocket.receive())
            queue_task = asyncio.create_task(event_queue.get())
            done, pending = await asyncio.wait(
                {recv_task, queue_task},
                return_when=asyncio.FIRST_COMPLETED,
            )
            for t in pending:
                await _cancel_task(t)

            if queue_task in done:
                event = queue_task.result()
                await websocket.send_json(event)

            if recv_task in done:
                msg = recv_task.result()
                if msg["type"] == "websocket.disconnect":
                    break
                if msg["type"] != "websocket.receive":
                    continue
                raw = msg.get("text")
                if raw is None and msg.get("bytes") is not None:
                    raw = msg["bytes"].decode("utf-8")
                if raw is None:
                    continue
                try:
                    payload = json.loads(raw)
                except json.JSONDecodeError as exc:
                    await websocket.send_json(
                        {"event": "error", "message": f"Invalid client message JSON: {exc}"}
                    )
                    continue

                message_type = payload.get("type")

                try:
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
                except ValidationError as exc:
                    await websocket.send_json(
                        {"event": "error", "message": f"Invalid client message: {exc}"}
                    )
                    continue

                await websocket.send_json({"event": "error", "message": "Unsupported message type"})

    except WebSocketDisconnect:
        pass
    finally:
        await _cancel_task(upstream_task)
        await bridge.close()
