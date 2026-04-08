from __future__ import annotations

from fastapi import FastAPI
from fastapi.responses import JSONResponse

from app.routers.sms import router as sms_router
from app.routers.vishing_ws import router as vishing_router

app = FastAPI(title="FraudLens Blackbox APIs", version="1.0.0")
app.include_router(sms_router)
app.include_router(vishing_router)


@app.get("/health")
async def health() -> JSONResponse:
    return JSONResponse({"status": "ok"})
