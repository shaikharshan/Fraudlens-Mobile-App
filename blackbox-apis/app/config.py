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

    def __init__(self) -> None:
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "").strip()
        self.gemini_live_model = os.getenv(
            "GEMINI_LIVE_MODEL",
            "gemini-2.5-flash-native-audio-preview-12-2025",
        ).strip()
        self.vishing_upstream_debug = os.getenv("FRAUDLENS_VISHING_DEBUG", "").strip() in (
            "1",
            "true",
            "yes",
        )

    def ensure_api_key(self) -> None:
        if not self.gemini_api_key:
            raise RuntimeError("GEMINI_API_KEY is not set")


settings = Settings()
