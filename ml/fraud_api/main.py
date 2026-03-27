"""
Fraud scoring API aligned with ml/model_training (no TRN_STATUS/RESPONSE_CODE leakage).
"""

from __future__ import annotations

import os
import sys
import warnings
from pathlib import Path

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List

warnings.filterwarnings("ignore")

_ML_DIR = Path(__file__).resolve().parents[1]
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))

from fraud_api.config import (  # noqa: E402
    MAX_AMOUNT_SUM_1H,
    MAX_CONSECUTIVE_FAILURES,
    MAX_DEVICE_USER_COUNT,
    MAX_FAILED_TXN_COUNT_24H,
    MAX_TXN_COUNT_1H,
)
from fraud_api.features import build_feature_frame  # noqa: E402

# Easiest local workflow:
# - Copy your trained bundle (.pkl containing {"model", "beneficiary_encoder", ...})
#   into this same folder: `fraud_api/`
# - Set this filename to match.
LOCAL_MODEL_FILENAME = "random_forest_model.pkl"


def _resolve_model_path() -> Path:

    # 2) Local/dev: load model placed in ml/fraud_api/
    fraud_api_dir = Path(__file__).resolve().parent
    if LOCAL_MODEL_FILENAME:
        p = fraud_api_dir / LOCAL_MODEL_FILENAME
        if p.is_file():
            return p.resolve()

 


MODEL_PATH = _resolve_model_path()

app = FastAPI(title="FraudLens Fraud Detection API", version="2.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=os.environ.get("CORS_ORIGINS", "*").split(","),
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

_model_bundle = None


def get_bundle():
    global _model_bundle
    if _model_bundle is None:
        if not MODEL_PATH.is_file():
            raise FileNotFoundError(f"Model bundle not found: {MODEL_PATH}")
        _model_bundle = joblib.load(MODEL_PATH)
    return _model_bundle


@app.on_event("startup")
def startup_load_model():
    try:
        get_bundle()
    except FileNotFoundError as e:
        print(str(e))


class TransactionData(BaseModel):
    txn_id: str
    AMOUNT: float = Field(..., description="Transaction amount")
    amount_sum_1h: float = Field(
        ...,
        ge=0,
        description="Total sent amount from payer VPA in last 1h (app/server computed)",
    )
    TXN_TIMESTAMP: str = Field(..., description="ISO or parseable datetime string")
    PAYER_VPA: str
    BENEFICIARY_VPA: str
    PAYER_IFSC: str
    BENEFICIARY_IFSC: str
    INITIATION_MODE: str = "APP"
    TRANSACTION_TYPE: str = "P2P"
    device_user_count: int = Field(..., ge=1, description="Distinct payers on device (app-computed)")
    txn_count_1h: int = Field(..., ge=1, description="Txns in last 1h on device (clipped to training cap server-side)")
    failed_txn_count_24h: int = Field(
        0, ge=0, description="Non-success responses in prior 24h for payer/device (app-computed)"
    )
    consecutive_failures: int = Field(
        0, ge=0, description="Back-to-back failures before this attempt (app-computed)"
    )


def _rows_to_df(transactions: List[TransactionData]) -> pd.DataFrame:
    rows = []
    for t in transactions:
        rows.append(
            {
                "TXN_TIMESTAMP": t.TXN_TIMESTAMP,
                "AMOUNT": t.AMOUNT,
                "amount_sum_1h": t.amount_sum_1h,
                "PAYER_VPA": t.PAYER_VPA,
                "BENEFICIARY_VPA": t.BENEFICIARY_VPA,
                "PAYER_IFSC": t.PAYER_IFSC,
                "BENEFICIARY_IFSC": t.BENEFICIARY_IFSC,
                "INITIATION_MODE": t.INITIATION_MODE,
                "TRANSACTION_TYPE": t.TRANSACTION_TYPE,
                "device_user_count": t.device_user_count,
                "txn_count_1h": t.txn_count_1h,
                "failed_txn_count_24h": t.failed_txn_count_24h,
                "consecutive_failures": t.consecutive_failures,
            }
        )
    df = pd.DataFrame(rows)
    df["device_user_count"] = df["device_user_count"].clip(lower=1, upper=MAX_DEVICE_USER_COUNT).astype(int)
    df["txn_count_1h"] = df["txn_count_1h"].clip(lower=1, upper=MAX_TXN_COUNT_1H).astype(int)
    df["amount_sum_1h"] = df["amount_sum_1h"].clip(lower=0.0, upper=MAX_AMOUNT_SUM_1H).astype(float)
    df["failed_txn_count_24h"] = df["failed_txn_count_24h"].clip(lower=0, upper=MAX_FAILED_TXN_COUNT_24H).astype(int)
    df["consecutive_failures"] = df["consecutive_failures"].clip(lower=0, upper=MAX_CONSECUTIVE_FAILURES).astype(int)
    return df


def preprocess_batch(transactions: List[TransactionData]):
    bundle = get_bundle()
    model = bundle["model"]
    bene_enc = bundle["beneficiary_encoder"]
    df = _rows_to_df(transactions)
    features, _ = build_feature_frame(df, beneficiary_encoder=bene_enc)
    return model, features


def _risk_level(probability: float) -> str:
    if probability >= 0.6:
        return "HIGH"
    if probability > 0.3:
        return "MEDIUM"
    return "LOW"


@app.post("/predict")
async def predict_fraud(transaction: TransactionData):
    try:
        model, features = preprocess_batch([transaction])
        prediction = int(model.predict(features)[0])
        probability = float(model.predict_proba(features)[0][1])
        return {
            "txn_id": transaction.txn_id,
            "is_fraud": bool(prediction),
            "fraud_probability": probability,
            "risk_level": _risk_level(probability),
        }
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e)) from e


@app.post("/predict_batch")
async def predict_batch(transactions: List[TransactionData]):
    try:
        model, features = preprocess_batch(transactions)
        preds = model.predict(features)
        probas = model.predict_proba(features)[:, 1]
        results = []
        for i, t in enumerate(transactions):
            results.append(
                {
                    "txn_id": t.txn_id,
                    "is_fraud": bool(preds[i]),
                    "fraud_probability": float(probas[i]),
                    "risk_level": _risk_level(float(probas[i])),
                }
            )
        return {"results": results}
    except FileNotFoundError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e)) from e


@app.get("/health")
async def health_check():
    bundle_ok = MODEL_PATH.is_file()
    return {
        "status": "healthy" if bundle_ok else "degraded",
        "model_loaded": bundle_ok,
        "model_path": str(MODEL_PATH.resolve()),
    }
