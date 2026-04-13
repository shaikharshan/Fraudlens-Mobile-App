from __future__ import annotations

import json
import re
from typing import Any

import httpx

from app.config import settings
from app.models import ScamAnalysisResponse
from app.prompts import SMS_PROMPT


class GeminiSmsClient:
    _base_url = "https://generativelanguage.googleapis.com/v1beta/models"

    async def analyze_sms(self, api_key: str, sender_id: str, message: str) -> ScamAnalysisResponse:
        combined_text = f"Sender: {sender_id.strip()}\nMessage: {message.strip()}"
        request_body: dict[str, Any] = {
            "contents": [
                {"role": "user", "parts": [{"text": SMS_PROMPT}]},
                {"role": "model", "parts": [{"text": "OK, I am ready. Please provide the message for analysis."}]},
                {"role": "user", "parts": [{"text": combined_text}]},
            ],
            "generationConfig": {
                "temperature": 0.2,
                "topP": 0.8,
                "topK": 40,
                "maxOutputTokens": 1024,
                "response_mime_type": "application/json",
            },
        }

        model_errors: list[str] = []
        async with httpx.AsyncClient(timeout=35.0) as client:
            for model_id in settings.gemini_sms_fallback_models:
                response = await client.post(
                    f"{self._base_url}/{model_id}:generateContent?key={api_key}",
                    json=request_body,
                    headers={"Content-Type": "application/json"},
                )
                if response.status_code >= 400:
                    model_errors.append(f"{model_id}: {response.status_code}")
                    if self._should_try_next_model(response.status_code):
                        continue
                    raise RuntimeError(
                        f"Gemini API error on model '{model_id}': {response.status_code} - {response.text}"
                    )
                payload = response.json()
                text = self._extract_first_text(payload)
                if not text:
                    model_errors.append(f"{model_id}: incomplete response")
                    continue
                try:
                    normalized = self._extract_json_blob(text)
                    data = json.loads(normalized)
                    return ScamAnalysisResponse(**data)
                except Exception as exc:  # noqa: BLE001
                    model_errors.append(f"{model_id}: invalid payload ({exc})")
                    continue

        raise RuntimeError(f"All Gemini SMS models failed: {', '.join(model_errors)}")

    @staticmethod
    def _extract_first_text(payload: dict[str, Any]) -> str:
        candidates = payload.get("candidates") or []
        if not candidates:
            return ""
        parts = ((candidates[0].get("content") or {}).get("parts")) or []
        if not parts:
            return ""
        return str(parts[0].get("text") or "").strip()

    @staticmethod
    def _extract_json_blob(text: str) -> str:
        stripped = text.strip()
        if stripped.startswith("{") and stripped.endswith("}"):
            return stripped

        match = re.search(r"\{[\s\S]*\}", text)
        if not match:
            raise RuntimeError("No JSON object found in Gemini response text")
        return match.group(0)

    @staticmethod
    def _should_try_next_model(status_code: int) -> bool:
        # Retry another model on overload/transient or model-availability type errors.
        return status_code in (404, 408, 409, 429, 500, 502, 503, 504)
