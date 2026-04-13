from __future__ import annotations

import os

from dotenv import load_dotenv

load_dotenv()


class Settings:
    gemini_api_key: str
    fraud_confidence_threshold: float = 0.6
    # Live / BidiGenerateContent model (Gemini Developer API). Default: Gemini 2.5 Flash Live Preview.
    # See: https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash-native-audio-preview-12-2025
    gemini_live_model: str
    gemini_sms_fallback_models: list[str]
    gemini_live_fallback_models: list[str]

    def __init__(self) -> None:
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "").strip()
        self.gemini_live_model = os.getenv(
            "GEMINI_LIVE_MODEL",
            "gemini-2.5-flash-native-audio-preview-12-2025",
        ).strip()
        self.gemini_sms_fallback_models = self._parse_fallback_models(
            os.getenv("GEMINI_SMS_FALLBACK_MODELS", ""),
            defaults=[
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.5-pro",
                "gemini-2.5-flash-preview-09-2025",
                "gemini-2.0-flash-001",
                "gemini-2.0-flash-lite-001",
            ],
        )
        self.gemini_live_fallback_models = self._parse_fallback_models(
            os.getenv("GEMINI_LIVE_FALLBACK_MODELS", ""),
            defaults=[
                self.gemini_live_model,
                "gemini-3.1-flash-live-preview",
            ],
        )
        self.vishing_upstream_debug = os.getenv("FRAUDLENS_VISHING_DEBUG", "").strip() in (
            "1",
            "true",
            "yes",
        )

    def ensure_api_key(self) -> None:
        if not self.gemini_api_key:
            raise RuntimeError("GEMINI_API_KEY is not set")

    @staticmethod
    def _parse_fallback_models(raw: str, defaults: list[str]) -> list[str]:
        values = [item.strip() for item in raw.split(",") if item.strip()]
        if not values:
            values = defaults
        deduped: list[str] = []
        for model in values:
            if model not in deduped:
                deduped.append(model)
        return deduped


settings = Settings()
