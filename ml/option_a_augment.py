#!/usr/bin/env python3
"""
Option A: synthesize wide transaction rows with SDV, then run the MANIT preprocessing
pipeline (incl. frequency encodings) and export the 9 model features + IS_FRAUD.

Usage:
  cd ml && ../.venv-ml/bin/python option_a_augment.py \\
    --csv ../anonymized_sample_fraud_txn.csv --extra-fraud 200 --outdir ../out_augment

Requires: pip install -r requirements-ml-augment.txt (sdv, pandas, scikit-learn, numpy)
"""

from __future__ import annotations

import argparse
import hashlib
import secrets
from pathlib import Path

import pandas as pd

from fraud_feature_pipeline import MANIT_MODEL_FEATURE_COL, preprocess_then_features


def _random_id(nbytes: int = 10) -> str:
    return secrets.token_hex(nbytes)


def load_csv(path: Path) -> pd.DataFrame:
    return pd.read_csv(path, low_memory=False)


def prepare_sdv_frame(df: pd.DataFrame) -> pd.DataFrame:
    """Match MANIT drops; remove row-unique IDs from synthesis (re-added after)."""
    from fraud_feature_pipeline import DROP_COLS

    out = df.drop(columns=[c for c in DROP_COLS if c in df.columns], errors="ignore")
    out = out.drop(columns=[c for c in ("TRANSACTION_ID", "RRN") if c in out.columns])
    out["TXN_TIMESTAMP"] = pd.to_datetime(out["TXN_TIMESTAMP"], format="%d/%m/%Y %H:%M", errors="coerce")
    out["IS_FRAUD"] = pd.to_numeric(out["IS_FRAUD"], errors="coerce").fillna(0).astype(int)
    return out


def synthesize_blocks(
    df_sdv: pd.DataFrame,
    extra_fraud: int,
    extra_legit: int,
) -> pd.DataFrame:
    from sdv.metadata import Metadata
    from sdv.sampling import Condition
    from sdv.single_table import GaussianCopulaSynthesizer

    metadata = Metadata.detect_from_dataframe(df_sdv)
    synthesizer = GaussianCopulaSynthesizer(metadata)
    synthesizer.fit(df_sdv)

    parts = []
    if extra_fraud > 0:
        parts.append(
            synthesizer.sample_from_conditions(
                [Condition(column_values={"IS_FRAUD": 1}, num_rows=extra_fraud)]
            )
        )
    if extra_legit > 0:
        parts.append(
            synthesizer.sample_from_conditions(
                [Condition(column_values={"IS_FRAUD": 0}, num_rows=extra_legit)]
            )
        )
    if not parts:
        return pd.DataFrame()
    syn = pd.concat(parts, ignore_index=True)
    return syn


def stitch_ids_and_timestamp(syn: pd.DataFrame) -> pd.DataFrame:
    """Restore ID columns and stringify timestamp like the source CSV."""
    out = syn.copy()
    n = len(out)
    out["TRANSACTION_ID"] = [_random_id(10) for _ in range(n)]
    out["RRN"] = [hashlib.sha256(bytes(_random_id(12), "utf-8")).hexdigest()[:20] for _ in range(n)]
    out["TXN_TIMESTAMP"] = pd.to_datetime(out["TXN_TIMESTAMP"], errors="coerce")
    out["TXN_TIMESTAMP"] = out["TXN_TIMESTAMP"].dt.strftime("%d/%m/%Y %H:%M")
    return out


def main() -> None:
    p = argparse.ArgumentParser(description="Option A: SDV + MANIT feature pipeline")
    p.add_argument("--csv", type=Path, required=True, help="Path to anonymized_sample_fraud_txn.csv")
    p.add_argument("--extra-fraud", type=int, default=300, help="Synthetic fraud rows to add")
    p.add_argument("--extra-legit", type=int, default=0, help="Synthetic non-fraud rows to add")
    p.add_argument("--outdir", type=Path, default=Path("out_augment"), help="Output directory")
    args = p.parse_args()

    args.outdir.mkdir(parents=True, exist_ok=True)

    raw = load_csv(args.csv)
    raw["__SOURCE__"] = "real"

    df_sdv = prepare_sdv_frame(raw.drop(columns=["__SOURCE__"], errors="ignore"))
    print(f"SDV training frame: {df_sdv.shape[1]} columns, {len(df_sdv)} rows")
    syn = synthesize_blocks(df_sdv, args.extra_fraud, args.extra_legit)
    if syn.empty:
        print("No synthetic rows requested; exiting.")
        return

    syn = stitch_ids_and_timestamp(syn)
    for c in raw.columns:
        if c == "__SOURCE__":
            continue
        if c not in syn.columns:
            syn[c] = None
    syn["__SOURCE__"] = "synthetic"

    combined_wide = pd.concat([raw, syn[raw.columns]], ignore_index=True)

    features = preprocess_then_features(combined_wide.drop(columns=["__SOURCE__"], errors="ignore"))
    features["__SOURCE__"] = combined_wide["__SOURCE__"].values

    out_wide = args.outdir / "wide_real_plus_synthetic.csv"
    out_feat = args.outdir / "features_manit_9_with_label.csv"
    combined_wide.to_csv(out_wide, index=False)
    features[MANIT_MODEL_FEATURE_COL + ["IS_FRAUD", "__SOURCE__"]].to_csv(out_feat, index=False)

    print(f"Wrote {out_wide} ({len(combined_wide)} rows)")
    print(f"Wrote {out_feat} (columns: {MANIT_MODEL_FEATURE_COL})")
    print(features["__SOURCE__"].value_counts())


if __name__ == "__main__":
    main()
