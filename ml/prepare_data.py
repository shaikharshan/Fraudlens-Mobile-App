"""
Load raw anonymized CSV and build a single-table frame aligned with the FastAPI
`TransactionData` + `preprocess()` contract (plus `IS_FRAUD` and device-derived fields).
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np
import pandas as pd

_ML_DIR = Path(__file__).resolve().parent
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))

from config import (
    DEFAULT_DEVICE_FEATURE_MODE,
    DEFAULT_INPUT_CSV,
    DEFAULT_PREPARED_PATH,
    DEFAULT_RANDOM_SEED,
    MAX_AMOUNT_SUM_1H,
    MAX_DEVICE_USER_COUNT,
    MAX_TXN_COUNT_1H,
    SDV_DEVICE_CAP_VALUE,
)
from fraud_feature_distributions import (
    inject_rolling_features_by_is_fraud,
    sample_amount_sum_1h_by_is_fraud,
    sample_device_and_txn_overlap,
)
from vpa_indian import rewrite_vpa_columns


def _map_initiation_mode(raw: str) -> str:
    """Map CSV initiation labels to FastAPI `INITIATION_MODE` enum."""
    if not isinstance(raw, str):
        return "UNKNOWN"
    key = raw.strip()
    mapping = {
        "Default": "APP",
        "QR Code": "APP",
        "Intent": "APP",
        "Mandate": "APP",
        "Secure QR Code": "APP",
        "Online Static QR Code": "WEB",
        "Dynamic Qr Code": "APP",
        "BHARAT QR Code": "APP",
        "SDK (Software development Kit)": "APP",
        "Secure Intent": "APP",
    }
    return mapping.get(key, "APP")


def _fill_missing_ifsc(series: pd.Series) -> pd.Series:
    """Replace missing IFSC with a stable placeholder so SDV + str ops never see NaN."""
    return series.fillna("UNKN0000000")


def _add_txn_count_1h(df: pd.DataFrame) -> pd.Series:
    """
    For each row, count transactions on the same DEVICE_ID in [t-1h, t] inclusive.
    Two-pointer pass per device (linear in segment size vs. naive O(n²)).
    """
    df = df.sort_values(["DEVICE_ID", "TXN_TIMESTAMP"])
    counts = []
    for _, g in df.groupby("DEVICE_ID", sort=False):
        g = g.sort_values("TXN_TIMESTAMP")
        ts = g["TXN_TIMESTAMP"].values.astype("datetime64[ns]")
        n = len(ts)
        c = np.zeros(n, dtype=np.int64)
        j = 0
        one_hour = np.timedelta64(1, "h")
        for i in range(n):
            t_i = ts[i]
            win_start = t_i - one_hour
            while j <= i and ts[j] < win_start:
                j += 1
            c[i] = i - j + 1
        counts.append(pd.Series(c, index=g.index))
    return pd.concat(counts).sort_index()


def apply_rolling_features_by_is_fraud(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """Re-apply the same label-conditional rolling distributions (e.g. after SDV sampling)."""
    mask = df["IS_FRAUD"].astype(int).to_numpy() == 1
    failed, conv = inject_rolling_features_by_is_fraud(len(df), mask, rng)
    df["failed_txn_count_24h"] = failed
    df["consecutive_failures"] = conv


def apply_amount_sum_by_is_fraud(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """Re-apply amount_sum_1h from IS_FRAUD + txn_count_1h (e.g. after SDV sampling)."""
    mask = df["IS_FRAUD"].astype(int).to_numpy() == 1
    txn = pd.to_numeric(df["txn_count_1h"], errors="coerce").fillna(1).clip(1, MAX_TXN_COUNT_1H).astype(int).to_numpy()
    amt = sample_amount_sum_1h_by_is_fraud(txn, mask, rng)
    df["amount_sum_1h"] = np.clip(amt, 0.0, float(MAX_AMOUNT_SUM_1H))


def apply_threshold_separation_by_is_fraud(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """
    Overwrite ``device_user_count`` and ``txn_count_1h`` from ``IS_FRAUD`` using the same
    overlapping distributions as ``fraud_correlated`` (primary signal: device count). Use after
    SDV sampling when you want label-consistent device/velocity features.
    """
    n = len(df)
    fraud = df["IS_FRAUD"].astype(int).to_numpy() == 1
    dev, txn = sample_device_and_txn_overlap(n, fraud, rng)
    df["device_user_count"] = dev
    df["txn_count_1h"] = txn


def _apply_time_features(df: pd.DataFrame) -> None:
    """Match FastAPI preprocess: hour, is_weekend, is_night."""
    ts = df["TXN_TIMESTAMP"]
    df["hour"] = ts.dt.hour.astype(int)
    dow = ts.dt.dayofweek.astype(int)
    df["is_weekend"] = (dow >= 5).astype(int)
    df["is_night"] = ((df["hour"] >= 22) | (df["hour"] <= 6)).astype(int)


def sync_derived_time_columns(df: pd.DataFrame) -> None:
    """Recompute hour / is_weekend / is_night from TXN_TIMESTAMP (call after adjusting timestamps)."""
    _apply_time_features(df)


def prepare_dataframe(
    df: pd.DataFrame,
    device_feature_mode: str = DEFAULT_DEVICE_FEATURE_MODE,
    random_seed: int | None = DEFAULT_RANDOM_SEED,
) -> pd.DataFrame:
    """Return a cleaned frame with API-aligned columns + device/rolling features + label.

    TRN_STATUS / RESPONSE_CODE are omitted (inference leakage). Instead,
    ``failed_txn_count_24h`` and ``consecutive_failures`` are injected with
    label-conditional distributions for SDV to learn realistic correlations.

    VPAs are normalized to valid UPI-like shapes, but are NOT overwritten with
    label-conditioned lengths. This keeps the model less sensitive to VPA length
    and better aligned with raw/mobile VPAs at inference.

    device_feature_mode:
        historical — derive counts from DEVICE_ID + timestamps only.
        fraud_correlated — overlapping distributions (device strongest, then txn/h, then rolling failures).
        blend — legit uses historical; fraud rows use max(historical, overlap synthetic draws).
    """
    out = pd.DataFrame()
    out["TXN_TIMESTAMP"] = pd.to_datetime(df["TXN_TIMESTAMP"], dayfirst=True, errors="coerce")
    out["txn_id"] = df["TRANSACTION_ID"].astype(str)
    out["AMOUNT"] = pd.to_numeric(df["AMOUNT"], errors="coerce").clip(lower=0.01)

    out["PAYER_VPA"] = df["PAYER_VPA"].fillna("unknown@unknown").astype(str)
    out["BENEFICIARY_VPA"] = df["BENEFICIARY_VPA"].fillna("unknown@unknown").astype(str)
    out["PAYER_IFSC"] = _fill_missing_ifsc(df["PAYER_IFSC"].astype(str))
    out["BENEFICIARY_IFSC"] = _fill_missing_ifsc(df["BENEFICIARY_IFSC"].astype(str))

    out["INITIATION_MODE"] = df["INITIATION_MODE"].map(_map_initiation_mode).fillna("APP")
    out["TRANSACTION_TYPE"] = df["TRANSACTION_TYPE"].astype(str)
    out["IS_FRAUD"] = pd.to_numeric(df["IS_FRAUD"], errors="coerce").fillna(0).astype(int)

    out["DEVICE_ID"] = df["DEVICE_ID"].astype(str)

    rng = np.random.default_rng(random_seed)
    rewrite_vpa_columns(out, rng, label_conditioned=False)

    nunique_payer = out.groupby("DEVICE_ID")["PAYER_VPA"].transform("nunique")
    hist_device = nunique_payer.clip(lower=1).astype(np.int64)
    hist_txn = _add_txn_count_1h(out).astype(np.int64)
    fraud_mask = out["IS_FRAUD"].to_numpy(dtype=bool)

    mode = (device_feature_mode or DEFAULT_DEVICE_FEATURE_MODE).lower().strip()
    if mode == "historical":
        out["device_user_count"] = hist_device.astype(int)
        out["txn_count_1h"] = hist_txn.astype(int)
    elif mode == "fraud_correlated":
        dev, txn = sample_device_and_txn_overlap(len(out), fraud_mask, rng)
        out["device_user_count"] = dev
        out["txn_count_1h"] = txn
    elif mode == "blend":
        dev_syn, txn_syn = sample_device_and_txn_overlap(len(out), fraud_mask, rng)
        hd = hist_device.to_numpy()
        ht = hist_txn.to_numpy()
        out["device_user_count"] = np.where(fraud_mask, np.maximum(hd, dev_syn), hd).astype(int)
        out["txn_count_1h"] = np.minimum(
            np.where(fraud_mask, np.maximum(ht, txn_syn), ht).astype(int),
            MAX_TXN_COUNT_1H,
        )
    else:
        raise ValueError(
            f"Unknown device_feature_mode={device_feature_mode!r}. "
            "Use 'historical', 'fraud_correlated' (overlapping label-conditional features), or 'blend'."
        )

    out["device_user_count"] = out["device_user_count"].clip(lower=1, upper=MAX_DEVICE_USER_COUNT).astype(int)
    out["txn_count_1h"] = out["txn_count_1h"].clip(lower=1, upper=MAX_TXN_COUNT_1H).astype(int)
    out["amount_sum_1h"] = sample_amount_sum_1h_by_is_fraud(
        out["txn_count_1h"].to_numpy(dtype=np.int64),
        fraud_mask,
        rng,
    )
    _apply_time_features(out)

    # SDV CAG helpers
    out["_sdv_strict_positive"] = 0.0
    out["_sdv_device_cap"] = float(SDV_DEVICE_CAP_VALUE)

    out = out.dropna(subset=["TXN_TIMESTAMP"])
    out = out.sort_values("TXN_TIMESTAMP").reset_index(drop=True)

    fraud_final = out["IS_FRAUD"].to_numpy(dtype=bool)
    ft, cf = inject_rolling_features_by_is_fraud(len(out), fraud_final, rng)
    out["failed_txn_count_24h"] = ft
    out["consecutive_failures"] = cf

    return out


def load_and_prepare(
    csv_path: Path,
    device_feature_mode: str = DEFAULT_DEVICE_FEATURE_MODE,
    random_seed: int | None = DEFAULT_RANDOM_SEED,
) -> pd.DataFrame:
    raw = pd.read_csv(csv_path)
    return prepare_dataframe(raw, device_feature_mode=device_feature_mode, random_seed=random_seed)


def dataframe_for_sdv(df: pd.DataFrame) -> pd.DataFrame:
    """Columns passed to SDV (identifiers for txn_id generated at sample time)."""
    cols = [
        "TXN_TIMESTAMP",
        "AMOUNT",
        "PAYER_VPA",
        "BENEFICIARY_VPA",
        "PAYER_IFSC",
        "BENEFICIARY_IFSC",
        "INITIATION_MODE",
        "TRANSACTION_TYPE",
        "failed_txn_count_24h",
        "consecutive_failures",
        "device_user_count",
        "txn_count_1h",
        "amount_sum_1h",
        "hour",
        "is_weekend",
        "is_night",
        "IS_FRAUD",
        "_sdv_strict_positive",
        "_sdv_device_cap",
    ]
    return df[cols].copy()


def main() -> None:
    parser = argparse.ArgumentParser(description="Prepare transaction CSV for SDV / training.")
    parser.add_argument("--input", type=Path, default=DEFAULT_INPUT_CSV)
    parser.add_argument("--output", type=Path, default=DEFAULT_PREPARED_PATH)
    parser.add_argument(
        "--device-features",
        choices=("historical", "fraud_correlated", "blend"),
        default=DEFAULT_DEVICE_FEATURE_MODE,
        help="How to set device_user_count / txn_count_1h (see prepare_dataframe docstring).",
    )
    parser.add_argument("--seed", type=int, default=DEFAULT_RANDOM_SEED, help="RNG seed for fraud_correlated / blend")
    args = parser.parse_args()

    args.output.parent.mkdir(parents=True, exist_ok=True)
    prepared = load_and_prepare(args.input, device_feature_mode=args.device_features, random_seed=args.seed)
    prepared.to_parquet(args.output, index=False)
    print(f"Wrote {len(prepared)} rows to {args.output}")


if __name__ == "__main__":
    main()
