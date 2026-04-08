from __future__ import annotations

import json
import re
from typing import Any, Awaitable, Callable

import websockets
from websockets.client import WebSocketClientProtocol

from app.config import settings
from app.models import ScamAnalysisResponse
from app.prompts import VISHING_PROMPT

EventCallback = Callable[[dict[str, Any]], Awaitable[None]]


class GeminiRealtimeBridge:
    def __init__(self, api_key: str) -> None:
        self.api_key = api_key
        self._upstream: WebSocketClientProtocol | None = None

    async def connect(self) -> None:
        ws_url = (
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta."
            f"GenerativeService.BidiGenerateContent?key={self.api_key}"
        )
        self._upstream = await websockets.connect(ws_url, max_size=8_000_000)
        await self._send_json(self._setup_payload())

    async def close(self) -> None:
        if self._upstream is not None:
            await self._upstream.close()
            self._upstream = None

    async def send_audio(self, data_b64: str, mime_type: str = "audio/pcm") -> None:
        await self._send_json(
            {
                "realtimeInput": {
                    "audio": {
                        "data": data_b64,
                        "mimeType": mime_type,
                    }
                }
            }
        )

    async def send_text(self, text: str) -> None:
        await self._send_json(
            {
                "clientContent": {
                    "turns": [{"parts": [{"text": f'Analyze this conversation snippet for fraud: "{text}"'}]}],
                    "turnComplete": True,
                }
            }
        )

    async def start_read_loop(self, on_event: EventCallback) -> None:
        if self._upstream is None:
            raise RuntimeError("Gemini realtime bridge not connected")

        while True:
            raw = await self._upstream.recv()
            if not isinstance(raw, str):
                continue

            parsed = self._map_upstream_to_events(raw)
            for event in parsed:
                await on_event(event)

    async def _send_json(self, payload: dict[str, Any]) -> None:
        if self._upstream is None:
            raise RuntimeError("Gemini realtime bridge not connected")
        await self._upstream.send(json.dumps(payload))

    def _setup_payload(self) -> dict[str, Any]:
        return {
            "setup": {
                "model": "models/gemini-2.0-flash-exp",
                "generationConfig": {
                    "temperature": 0.3,
                    "maxOutputTokens": 8192,
                    "responseModalities": ["TEXT"],
                },
                "systemInstruction": {"parts": [{"text": VISHING_PROMPT}]},
                "realtimeInputConfig": {
                    "automaticActivityDetection": {
                        "disabled": False,
                        "startOfSpeechSensitivity": "START_SENSITIVITY_HIGH",
                        "endOfSpeechSensitivity": "END_SENSITIVITY_HIGH",
                        "prefixPaddingMs": 300,
                        "silenceDurationMs": 1000,
                    },
                    "activityHandling": "START_OF_ACTIVITY_INTERRUPTS",
                    "turnCoverage": "TURN_INCLUDES_ONLY_ACTIVITY",
                },
                "inputAudioTranscription": {},
                "outputAudioTranscription": {},
            }
        }

    def _map_upstream_to_events(self, text: str) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = []
        payload = json.loads(text)

        if payload.get("setupComplete") is not None:
            events.append({"event": "status", "message": "Connected to Gemini"})
            return events

        server_content = payload.get("serverContent") or {}

        input_tx = server_content.get("inputTranscription") or {}
        input_text = input_tx.get("text")
        if input_text:
            events.append({"event": "transcript", "text": input_text, "is_user": True})

        model_turn = server_content.get("modelTurn") or {}
        for part in model_turn.get("parts") or []:
            response_text = part.get("text")
            if not response_text:
                continue
            events.append({"event": "transcript", "text": response_text, "is_user": False})

            analysis = self._try_parse_analysis(response_text)
            if analysis and analysis.is_scam and analysis.confidence_score > settings.fraud_confidence_threshold:
                events.append({"event": "fraud_analysis", "analysis": analysis.model_dump()})

        return events

    @staticmethod
    def _try_parse_analysis(model_text: str) -> ScamAnalysisResponse | None:
        match = re.search(r"\{[\s\S]*\}", model_text)
        if not match:
            return None
        try:
            payload = json.loads(match.group(0))
            return ScamAnalysisResponse(**payload)
        except Exception:
            return None
