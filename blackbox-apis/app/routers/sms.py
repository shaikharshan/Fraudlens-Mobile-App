from __future__ import annotations

from fastapi import APIRouter, HTTPException

from app.config import settings
from app.gemini.rest_sms_client import GeminiSmsClient
from app.models import ScamAnalysisResponse, SmsAnalyzeRequest

router = APIRouter(prefix="/sms", tags=["sms"])
client = GeminiSmsClient()


@router.post("/analyze", response_model=ScamAnalysisResponse)
async def analyze_sms(request: SmsAnalyzeRequest) -> ScamAnalysisResponse:
    try:
        settings.ensure_api_key()
        return await client.analyze_sms(
            api_key=settings.gemini_api_key,
            sender_id=request.sender_id,
            message=request.message,
        )
    except RuntimeError as exc:
        raise HTTPException(status_code=502, detail=str(exc)) from exc
