"""
Fit an SDV single-table synthesizer on prepared transactions and sample synthetic rows.

Usage (from repo root):
    pip install -r ml/requirements.txt
    python ml/generate_synthetic.py --rows 10000 --refit
    python ml/generate_synthetic.py --stratified-sampling --refit
    python ml/generate_synthetic.py --stratified-sampling empirical --night-fraud-boost 1.3 --refit

Changing --device-features requires --refit or a different --prepared path so the cache matches.

VPAs are normalized into valid UPI-like shapes, but are not label-conditioned. This avoids
training a model that over-relies on VPA length (which can drift at inference).
"""

from __future__ import annotations

import argparse
import sys
import uuid
from pathlib import Path

import numpy as np
import pandas as pd
from sdv.single_table import CTGANSynthesizer, GaussianCopulaSynthesizer

_ML_DIR = Path(__file__).resolve().parent
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))

from config import (
    DEFAULT_CTGAN_EPOCHS,
    DEFAULT_DEVICE_FEATURE_MODE,
    DEFAULT_INPUT_CSV,
    DEFAULT_METADATA_PATH,
    DEFAULT_NUM_SYNTHETIC_ROWS,
    DEFAULT_PREPARED_PATH,
    DEFAULT_RANDOM_SEED,
    DEFAULT_STRATIFIED_QUADRANTS,
    DEFAULT_SYNTHESIZER,
    DEFAULT_SYNTHETIC_PATH,
    MAX_DEVICE_USER_COUNT,
    MAX_FAILED_TXN_COUNT_24H,
    MAX_CONSECUTIVE_FAILURES,
    MAX_TXN_COUNT_1H,
    MAX_AMOUNT_SUM_1H,
    NIGHT_FRAUD_BOOST,
)
from prepare_data import (
    apply_amount_sum_by_is_fraud,
    apply_rolling_features_by_is_fraud,
    apply_threshold_separation_by_is_fraud,
    dataframe_for_sdv,
    load_and_prepare,
)
from sample_utils import reconcile_timestamp_with_is_night, sample_conditional_stratified
from sdv_constraints import build_constraints
from sdv_metadata import build_metadata
from vpa_indian import rewrite_vpa_columns


def _make_synthesizer(metadata, synthesizer_name: str, train_rows: int, ctgan_epochs: int):
    name = (synthesizer_name or DEFAULT_SYNTHESIZER).lower().strip()
    constraints = build_constraints()
    if name == "copula":
        synth = GaussianCopulaSynthesizer(metadata)
    elif name == "ctgan":
        batch = min(500, max(2, train_rows))
        synth = CTGANSynthesizer(
            metadata,
            enforce_rounding=False,
            epochs=ctgan_epochs,
            batch_size=batch,
            verbose=False,
        )
    else:
        raise ValueError(f"Unknown synthesizer {synthesizer_name!r}. Use 'copula' or 'ctgan'.")
    synth.add_constraints(constraints=constraints)
    return synth


def _clean_ifsc(series: pd.Series) -> pd.Series:
    s = series.fillna("UNKN0000000").astype(str).str.strip()
    s = s.replace({"nan": "UNKN0000000", "None": "UNKN0000000", "<NA>": "UNKN0000000"})
    return s.mask(s == "", "UNKN0000000")


def _sanitize_synthetic(df: pd.DataFrame) -> pd.DataFrame:
    """Fill occasional nulls from the sampler so API-style preprocessing never sees NaNs."""
    out = df.copy()
    out["PAYER_VPA"] = out["PAYER_VPA"].fillna("unknown@unknown").astype(str)
    out["BENEFICIARY_VPA"] = out["BENEFICIARY_VPA"].fillna("unknown@unknown").astype(str)
    out["PAYER_IFSC"] = _clean_ifsc(out["PAYER_IFSC"])
    out["BENEFICIARY_IFSC"] = _clean_ifsc(out["BENEFICIARY_IFSC"])
    out["AMOUNT"] = pd.to_numeric(out["AMOUNT"], errors="coerce").fillna(1.0).clip(lower=0.01)
    if "amount_sum_1h" in out.columns:
        out["amount_sum_1h"] = (
            pd.to_numeric(out["amount_sum_1h"], errors="coerce")
            .fillna(0.0)
            .clip(lower=0.0, upper=MAX_AMOUNT_SUM_1H)
            .astype(float)
        )
    out["device_user_count"] = (
        pd.to_numeric(out["device_user_count"], errors="coerce")
        .fillna(1)
        .clip(lower=1, upper=MAX_DEVICE_USER_COUNT)
        .astype(int)
    )
    out["txn_count_1h"] = (
        pd.to_numeric(out["txn_count_1h"], errors="coerce")
        .fillna(1)
        .clip(lower=1, upper=MAX_TXN_COUNT_1H)
        .astype(int)
    )
    if "failed_txn_count_24h" in out.columns:
        out["failed_txn_count_24h"] = (
            pd.to_numeric(out["failed_txn_count_24h"], errors="coerce")
            .fillna(0)
            .clip(lower=0, upper=MAX_FAILED_TXN_COUNT_24H)
            .astype(int)
        )
    if "consecutive_failures" in out.columns:
        out["consecutive_failures"] = (
            pd.to_numeric(out["consecutive_failures"], errors="coerce")
            .fillna(0)
            .clip(lower=0, upper=MAX_CONSECUTIVE_FAILURES)
            .astype(int)
        )
    out["IS_FRAUD"] = pd.to_numeric(out["IS_FRAUD"], errors="coerce").fillna(0).clip(0, 1).astype(int)
    return out


def _strip_sdv_and_time_derivatives(df: pd.DataFrame) -> pd.DataFrame:
    """API-facing columns only; hour / is_night are derivable from TXN_TIMESTAMP via preprocess()."""
    drop_cols = ["hour", "is_weekend", "is_night", "_sdv_strict_positive", "_sdv_device_cap"]
    return df.drop(columns=[c for c in drop_cols if c in df.columns], errors="ignore")


def _ensure_prepared(
    input_csv: Path,
    prepared_path: Path,
    device_feature_mode: str,
    random_seed: int | None,
) -> pd.DataFrame:
    prepared_path.parent.mkdir(parents=True, exist_ok=True)
    if prepared_path.exists():
        return pd.read_parquet(prepared_path)
    prepared = load_and_prepare(
        input_csv,
        device_feature_mode=device_feature_mode,
        random_seed=random_seed,
    )
    prepared.to_parquet(prepared_path, index=False)
    return prepared


def generate(
    input_csv: Path,
    prepared_path: Path,
    output_path: Path,
    metadata_path: Path,
    num_rows: int,
    refit: bool,
    synthesizer_name: str,
    ctgan_epochs: int,
    device_feature_mode: str,
    random_seed: int | None,
    stratified_sampling: bool,
    stratified_empirical: bool,
    night_fraud_boost: float,
    stratified_quadrants: tuple[float, float, float, float],
    enforce_threshold_separation: bool,
) -> pd.DataFrame:
    prepared_path.parent.mkdir(parents=True, exist_ok=True)
    if refit:
        prepared = load_and_prepare(
            input_csv,
            device_feature_mode=device_feature_mode,
            random_seed=random_seed,
        )
        prepared.to_parquet(prepared_path, index=False)
    else:
        prepared = _ensure_prepared(input_csv, prepared_path, device_feature_mode, random_seed)

    train = dataframe_for_sdv(prepared)
    metadata = build_metadata(train)
    metadata.validate()
    metadata.save_to_json(str(metadata_path), mode="overwrite")

    synthesizer = _make_synthesizer(metadata, synthesizer_name, len(train), ctgan_epochs)
    synthesizer.fit(train)

    rng = np.random.default_rng(random_seed)

    if stratified_sampling:
        synthetic = sample_conditional_stratified(
            synthesizer,
            num_rows,
            prepared,
            empirical=stratified_empirical,
            default_quadrants=stratified_quadrants,
            night_fraud_boost=night_fraud_boost,
            rng=rng,
        )
    else:
        synthetic = synthesizer.sample(num_rows=num_rows)

    synthetic = _sanitize_synthetic(synthetic)
    if enforce_threshold_separation and "IS_FRAUD" in synthetic.columns:
        apply_threshold_separation_by_is_fraud(synthetic, rng)
    if "IS_FRAUD" in synthetic.columns:
        apply_rolling_features_by_is_fraud(synthetic, rng)
        apply_amount_sum_by_is_fraud(synthetic, rng)
    if all(c in synthetic.columns for c in ("TXN_TIMESTAMP", "is_night")):
        reconcile_timestamp_with_is_night(synthetic, rng)

    rewrite_vpa_columns(synthetic, rng, label_conditioned=False)

    synthetic.insert(0, "txn_id", [uuid.uuid4().hex[:24] for _ in range(len(synthetic))])
    synthetic = _strip_sdv_and_time_derivatives(synthetic)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    synthetic.to_parquet(output_path, index=False)
    synthetic.to_csv(output_path.with_suffix(".csv"), index=False)
    return synthetic


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate synthetic FraudLens transactions with SDV.")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT_CSV, help="Raw anonymized CSV")
    parser.add_argument(
        "--prepared",
        type=Path,
        default=DEFAULT_PREPARED_PATH,
        help="Cached prepared parquet (created if missing)",
    )
    parser.add_argument("--output", type=Path, default=DEFAULT_SYNTHETIC_PATH, help="Output parquet + csv")
    parser.add_argument("--metadata", type=Path, default=DEFAULT_METADATA_PATH, help="Written SDV metadata JSON")
    parser.add_argument("--rows", type=int, default=DEFAULT_NUM_SYNTHETIC_ROWS, help="Number of synthetic rows")
    parser.add_argument("--refit", action="store_true", help="Rebuild prepared parquet from raw CSV")
    parser.add_argument(
        "--synthesizer",
        choices=("copula", "ctgan"),
        default=DEFAULT_SYNTHESIZER,
        help="GaussianCopula (fast, default) or CTGAN (heavier, can fit mixed cat/num better on some data).",
    )
    parser.add_argument(
        "--ctgan-epochs",
        type=int,
        default=DEFAULT_CTGAN_EPOCHS,
        help="Training epochs when --synthesizer ctgan",
    )
    parser.add_argument(
        "--device-features",
        choices=("historical", "fraud_correlated", "blend"),
        default=DEFAULT_DEVICE_FEATURE_MODE,
        help="Device feature strategy; use --refit or a new --prepared path when changing this.",
    )
    parser.add_argument("--seed", type=int, default=DEFAULT_RANDOM_SEED, help="RNG seed")
    parser.add_argument(
        "--stratified-sampling",
        action="store_true",
        help="Sample in (IS_FRAUD × is_night) quadrants, then align TXN_TIMESTAMP to is_night.",
    )
    parser.add_argument(
        "--stratified-empirical",
        action="store_true",
        help="With --stratified-sampling, match quadrant weights to the seed data (with --night-fraud-boost on fraud+night).",
    )
    parser.add_argument(
        "--night-fraud-boost",
        type=float,
        default=NIGHT_FRAUD_BOOST,
        help="When --stratified-empirical, multiply empirical P(fraud, night) by this factor before renormalizing.",
    )
    parser.add_argument(
        "--no-enforce-threshold-separation",
        action="store_true",
        help="Skip post-SDV overwrite of device/txn features. Default reapplies overlapping label-conditional distributions (see fraud_feature_distributions).",
    )
    args = parser.parse_args()

    out = generate(
        input_csv=args.input,
        prepared_path=args.prepared,
        output_path=args.output,
        metadata_path=args.metadata,
        num_rows=args.rows,
        refit=args.refit,
        synthesizer_name=args.synthesizer,
        ctgan_epochs=args.ctgan_epochs,
        device_feature_mode=args.device_features,
        random_seed=args.seed,
        stratified_sampling=args.stratified_sampling,
        stratified_empirical=args.stratified_empirical,
        night_fraud_boost=args.night_fraud_boost,
        stratified_quadrants=DEFAULT_STRATIFIED_QUADRANTS,
        enforce_threshold_separation=not args.no_enforce_threshold_separation,
    )
    print(f"Wrote {len(out)} synthetic rows to {args.output} and {args.output.with_suffix('.csv')}")


if __name__ == "__main__":
    main()
