from __future__ import annotations

import os

from dotenv import load_dotenv

load_dotenv()


class Settings:
    gemini_api_key: str
    fraud_confidence_threshold: float = 0.6

    def __init__(self) -> None:
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "").strip()

    def ensure_api_key(self) -> None:
        if not self.gemini_api_key:
            raise RuntimeError("GEMINI_API_KEY is not set")


settings = Settings()
