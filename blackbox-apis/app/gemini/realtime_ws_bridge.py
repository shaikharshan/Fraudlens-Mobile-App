from __future__ import annotations

import json
import logging
import re
from typing import Any, Awaitable, Callable

import websockets
from websockets.client import WebSocketClientProtocol

from app.config import settings
from app.models import ScamAnalysisResponse
from app.prompts import VISHING_PROMPT

logger = logging.getLogger(__name__)

EventCallback = Callable[[dict[str, Any]], Awaitable[None]]


class GeminiRealtimeBridge:
    """Bridges client WebSocket to Gemini BidiGenerateContent (Live API)."""

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
        # Wait for setupComplete before accepting client audio/text (avoids dropped / no-op turns).
        first = await self._recv_text()
        payload = self._safe_json(first)
        if payload is None:
            raise RuntimeError(f"Upstream sent non-JSON first frame: {first[:200]!r}")
        if payload.get("setupComplete") is None:
            raise RuntimeError(
                "Expected setupComplete from Gemini after setup; got keys: "
                f"{list(payload.keys())}"
            )

    async def close(self) -> None:
        if self._upstream is not None:
            await self._upstream.close()
            self._upstream = None

    async def send_audio(self, data_b64: str, mime_type: str = "audio/pcm") -> None:
        if mime_type in ("audio/pcm", "audio/pcm;rate=16000"):
            mime_type = "audio/pcm;rate=16000"
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
        # Live API text streaming uses realtimeInput.text (not clientContent turns).
        prompt = f'Analyze this conversation snippet for fraud: "{text}"'
        await self._send_json({"realtimeInput": {"text": prompt}})

    async def start_read_loop(self, on_event: EventCallback) -> None:
        if self._upstream is None:
            raise RuntimeError("Gemini realtime bridge not connected")

        while True:
            raw = await self._recv_text()
            payload = self._safe_json(raw)
            if payload is None:
                await on_event(
                    {
                        "event": "error",
                        "message": f"Non-JSON upstream frame: {raw[:300]!r}",
                    }
                )
                continue

            try:
                events = self._map_upstream_to_events(payload)
            except Exception as exc:  # noqa: BLE001
                logger.exception("map upstream message")
                await on_event(
                    {
                        "event": "error",
                        "message": f"Failed to parse upstream message: {exc}",
                    }
                )
                continue

            for event in events:
                await on_event(event)

    async def _recv_text(self) -> str:
        if self._upstream is None:
            raise RuntimeError("Gemini realtime bridge not connected")
        raw = await self._upstream.recv()
        if isinstance(raw, bytes):
            return raw.decode("utf-8")
        if isinstance(raw, str):
            return raw
        raise RuntimeError(f"Unexpected upstream frame type: {type(raw)!r}")

    @staticmethod
    def _safe_json(text: str) -> dict[str, Any] | None:
        try:
            out = json.loads(text)
        except json.JSONDecodeError:
            return None
        return out if isinstance(out, dict) else None

    async def _send_json(self, payload: dict[str, Any]) -> None:
        if self._upstream is None:
            raise RuntimeError("Gemini realtime bridge not connected")
        await self._upstream.send(json.dumps(payload))

    def _setup_payload(self) -> dict[str, Any]:
        model_id = settings.gemini_live_model
        if not model_id.startswith("models/"):
            model_id = f"models/{model_id}"
        return {
            "setup": {
                "model": model_id,
                "generationConfig": {
                    "temperature": 0.3,
                    "maxOutputTokens": 8192,
                    "responseModalities": ["AUDIO"],
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

    def _map_upstream_to_events(self, payload: dict[str, Any]) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = []

        if payload.get("setupComplete") is not None:
            events.append({"event": "status", "message": "Connected to Gemini"})
            return events

        # usage-only ticks are normal; no user-visible event
        non_usage_keys = {k for k in payload if k not in ("usageMetadata", "usage_metadata")}

        # Some gateways may surface transcriptions at top level instead of under serverContent.
        top_in = payload.get("inputTranscription") or payload.get("input_transcription")
        if isinstance(top_in, dict) and top_in.get("text"):
            events.append({"event": "transcript", "text": top_in["text"], "is_user": True})
        top_out = payload.get("outputTranscription") or payload.get("output_transcription")
        if isinstance(top_out, dict) and top_out.get("text"):
            events.extend(self._events_from_model_text(top_out["text"]))

        server_content = (
            payload.get("serverContent")
            or payload.get("server_content")
            or {}
        )

        # inputTranscription / outputTranscription live under serverContent in the spec.
        input_tx = server_content.get("inputTranscription") or server_content.get(
            "input_transcription"
        ) or {}
        input_text = input_tx.get("text")
        if input_text:
            events.append({"event": "transcript", "text": input_text, "is_user": True})

        output_tx = server_content.get("outputTranscription") or server_content.get(
            "output_transcription"
        ) or {}
        output_text = output_tx.get("text")
        if output_text:
            events.extend(self._events_from_model_text(output_text))

        model_turn = server_content.get("modelTurn") or server_content.get("model_turn") or {}
        for part in model_turn.get("parts") or []:
            response_text = part.get("text")
            if not response_text:
                continue
            events.extend(self._events_from_model_text(response_text))

        # Optional: help debug "tokens increase but no events" (can be noisy; off by default).
        if (
            settings.vishing_upstream_debug
            and not events
            and non_usage_keys
            - {"inputTranscription", "input_transcription", "outputTranscription", "output_transcription"}
        ):
            events.append(
                {
                    "event": "upstream_debug",
                    "keys": sorted(non_usage_keys),
                }
            )

        return events

    def _events_from_model_text(self, response_text: str) -> list[dict[str, Any]]:
        events: list[dict[str, Any]] = [
            {"event": "transcript", "text": response_text, "is_user": False},
        ]
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
            data = json.loads(match.group(0))
            return ScamAnalysisResponse(**data)
        except Exception:
            return None
