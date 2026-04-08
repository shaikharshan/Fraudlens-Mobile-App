from __future__ import annotations

from typing import Literal, Optional

from pydantic import BaseModel, Field


class SmsAnalyzeRequest(BaseModel):
    sender_id: str = Field(..., min_length=1)
    message: str = Field(..., min_length=1)


class ScamAnalysisResponse(BaseModel):
    is_scam: bool
    confidence_score: float
    reasoning: str
    recommendation: str


class ErrorEvent(BaseModel):
    event: Literal["error"] = "error"
    message: str


class StatusEvent(BaseModel):
    event: Literal["status"] = "status"
    message: str


class TranscriptEvent(BaseModel):
    event: Literal["transcript"] = "transcript"
    text: str
    is_user: bool


class FraudAnalysisEvent(BaseModel):
    event: Literal["fraud_analysis"] = "fraud_analysis"
    analysis: ScamAnalysisResponse


class WsAudioMessage(BaseModel):
    type: Literal["audio"]
    data_b64: str = Field(..., min_length=1)
    mime_type: str = "audio/pcm"


class WsDisconnectMessage(BaseModel):
    type: Literal["disconnect"]


class WsTextMessage(BaseModel):
    type: Literal["text"]
    text: str = Field(..., min_length=1)


class GeminiInputTranscription(BaseModel):
    text: Optional[str] = None
