#!/usr/bin/env python3
"""
SDV augmentation for the exact API / model input columns (12 features + IS_FRAUD).

Builds rows from anonymized_sample_fraud_txn.csv by computing txn_count_1h and
device_user_count the same way as fraud_feature_pipeline (needs DEVICE_ID in CSV).

Example (50k synthetic rows, fraud mix ~ same as training CSV):
  cd ml && ../.venv-ml/bin/python augment_model_inputs.py \\
    --csv ../anonymized_sample_fraud_txn.csv --num-rows 50000 --out ../synthetic_api_inputs_50k.csv

Requires: pip install -r requirements-ml-augment.txt
"""

from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd

from fraud_feature_pipeline import MODEL_API_INPUT_COLS, build_api_input_table


def _coerce_synthetic_dtypes(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["TXN_TIMESTAMP"] = pd.to_datetime(out["TXN_TIMESTAMP"], errors="coerce")
    out["TXN_TIMESTAMP"] = out["TXN_TIMESTAMP"].dt.strftime("%d/%m/%Y %H:%M")
    out["AMOUNT"] = pd.to_numeric(out["AMOUNT"], errors="coerce").clip(lower=0).round(2)
    out["device_user_count"] = pd.to_numeric(out["device_user_count"], errors="coerce").fillna(1).astype(int)
    out["txn_count_1h"] = pd.to_numeric(out["txn_count_1h"], errors="coerce").fillna(0).astype(int)
    out["IS_FRAUD"] = pd.to_numeric(out["IS_FRAUD"], errors="coerce").fillna(0).astype(int)
    for col in ("TRN_STATUS", "RESPONSE_CODE", "INITIATION_MODE", "TRANSACTION_TYPE"):
        if col in out.columns:
            s = out[col].astype(str).replace({"nan": "", "NaT": ""})
            s = s.str.replace(r"\.0$", "", regex=True)
            out[col] = s
    for col in ("PAYER_VPA", "BENEFICIARY_VPA", "PAYER_IFSC", "BENEFICIARY_IFSC"):
        if col in out.columns:
            out[col] = out[col].astype(str)
    return out


def sample_sdv(
    train: pd.DataFrame,
    num_rows: int,
    fraud_fraction: float | None,
    seed: int,
) -> pd.DataFrame:
    from sdv.metadata import Metadata
    from sdv.sampling import Condition
    from sdv.single_table import GaussianCopulaSynthesizer

    train_fit = train.copy()
    train_fit["TXN_TIMESTAMP"] = pd.to_datetime(train_fit["TXN_TIMESTAMP"], errors="coerce")

    metadata = Metadata.detect_from_dataframe(train_fit)
    synthesizer = GaussianCopulaSynthesizer(metadata)
    synthesizer.fit(train_fit)

    if fraud_fraction is None:
        fraud_fraction = float(train["IS_FRAUD"].mean())
    fraud_fraction = min(max(fraud_fraction, 0.0), 1.0)
    n_fraud = int(round(num_rows * fraud_fraction))
    n_legit = num_rows - n_fraud
    if n_fraud < 0:
        n_fraud = 0
    if n_legit < 0:
        n_legit = 0

    rng = np.random.default_rng(seed)
    parts = []
    if n_fraud > 0:
        parts.append(
            synthesizer.sample_from_conditions(
                [Condition(column_values={"IS_FRAUD": 1}, num_rows=n_fraud)]
            )
        )
    if n_legit > 0:
        parts.append(
            synthesizer.sample_from_conditions(
                [Condition(column_values={"IS_FRAUD": 0}, num_rows=n_legit)]
            )
        )
    if not parts:
        raise ValueError("num_rows must be > 0")

    syn = pd.concat(parts, ignore_index=True)
    syn = syn.sample(frac=1.0, random_state=int(rng.integers(0, 2**31 - 1))).reset_index(drop=True)
    return syn


def main() -> None:
    p = argparse.ArgumentParser(description="SDV augment API model input columns")
    p.add_argument("--csv", type=Path, required=True)
    p.add_argument("--num-rows", type=int, default=50_000, help="Total synthetic rows (default 50000)")
    p.add_argument(
        "--fraud-fraction",
        type=float,
        default=None,
        help="Fraction labeled fraud (default: match training CSV)",
    )
    p.add_argument("--out", type=Path, default=Path("synthetic_api_inputs.csv"))
    p.add_argument("--seed", type=int, default=42)
    args = p.parse_args()

    raw = pd.read_csv(args.csv, low_memory=False)
    train = build_api_input_table(raw)
    if "IS_FRAUD" not in train.columns:
        raise ValueError("CSV must include IS_FRAUD for conditional SDV sampling")

    print(f"Training SDV on {len(train)} rows, columns: {list(train.columns)}")
    syn = sample_sdv(train, args.num_rows, args.fraud_fraction, args.seed)
    syn = _coerce_synthetic_dtypes(syn)

    # Column order for downstream training / inspection
    out_cols = list(MODEL_API_INPUT_COLS) + ["IS_FRAUD"]
    syn = syn[[c for c in out_cols if c in syn.columns]]

    args.out = Path(args.out)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    syn.to_csv(args.out, index=False)

    print(f"Wrote {args.out} ({len(syn)} rows)")
    print(syn["IS_FRAUD"].value_counts())


if __name__ == "__main__":
    main()
