"""
Preprocessing + features aligned with MANIT_hackathon.ipynb.

Training feature list (RandomForest + RobustScaler + SMOTE in notebook):
  TRN_STATUS, RESPONSE_CODE, INITIATION_MODE, TRANSACTION_TYPE,
  is_night, txn_count_1h, spending_spike, device_user_count, new_device_flag

Order: drop cols -> label-encode 4 categoricals -> frequency encodings -> time/velocity/device.
hour / day_of_week are computed for is_night only; not model inputs.
"""

from __future__ import annotations

import pandas as pd

DROP_COLS = ["CARD_NUMBER", "PAYMENT_INSTRUMENT", "PAYER_CODE", "UPI_LITE_LRN", "BENEFICIARY_CODE"]
LABEL_ENCODE_COLS = ["TRN_STATUS", "RESPONSE_CODE", "INITIATION_MODE", "TRANSACTION_TYPE"]

FREQUENCY_ENCODE_COLS = [
    "AMOUNT",
    "PAYER_VPA",
    "PAYER_IFSC",
    "PAYER_ACCOUNT",
    "BENEFICIARY_IFSC",
    "BENEFICIARY_ACCOUNT",
    "LONGITUDE",
    "LATITUDE",
    "DEVICE_ID",
]

MANIT_MODEL_FEATURE_COL = [
    "TRN_STATUS",
    "RESPONSE_CODE",
    "INITIATION_MODE",
    "TRANSACTION_TYPE",
    "is_night",
    "txn_count_1h",
    "spending_spike",
    "device_user_count",
    "new_device_flag",
]

# Columns matching FastAPI TransactionData / model input dict (strings for categoricals).
MODEL_API_INPUT_COLS = [
    "AMOUNT",
    "TXN_TIMESTAMP",
    "PAYER_VPA",
    "BENEFICIARY_VPA",
    "PAYER_IFSC",
    "BENEFICIARY_IFSC",
    "TRN_STATUS",
    "RESPONSE_CODE",
    "INITIATION_MODE",
    "TRANSACTION_TYPE",
    "device_user_count",
    "txn_count_1h",
]


def build_api_input_table(df: pd.DataFrame) -> pd.DataFrame:
    """
    From raw anonymized CSV: drop unused cols, compute txn_count_1h + device_user_count
    (needs DEVICE_ID, PAYER_VPA, TXN_TIMESTAMP, AMOUNT), keep categoricals as strings.
    Returns MODEL_API_INPUT_COLS plus IS_FRAUD when present.
    """
    out = drop_unused_columns(df.copy())
    required = {"PAYER_VPA", "TXN_TIMESTAMP", "AMOUNT", "DEVICE_ID"}
    missing = required - set(out.columns)
    if missing:
        raise ValueError(f"build_api_input_table missing columns: {missing}")

    out["AMOUNT"] = pd.to_numeric(out["AMOUNT"], errors="coerce")
    for col in ("TRN_STATUS", "RESPONSE_CODE", "INITIATION_MODE", "TRANSACTION_TYPE"):
        if col in out.columns:
            out[col] = out[col].astype(str)

    out = engineer_velocity_and_device(out)
    out["device_user_count"] = out["device_user_count"].fillna(1).astype(int)

    cols = [c for c in MODEL_API_INPUT_COLS if c in out.columns]
    if "IS_FRAUD" in out.columns:
        out["IS_FRAUD"] = pd.to_numeric(out["IS_FRAUD"], errors="coerce").fillna(0).astype(int)
        cols = cols + ["IS_FRAUD"]
    return out[cols].copy()


def drop_unused_columns(df: pd.DataFrame) -> pd.DataFrame:
    return df.drop(columns=[c for c in DROP_COLS if c in df.columns], errors="ignore")


def label_encode_like_manit(df: pd.DataFrame) -> pd.DataFrame:
    from sklearn.preprocessing import LabelEncoder

    out = df.copy()
    for col in LABEL_ENCODE_COLS:
        if col not in out.columns:
            continue
        le = LabelEncoder()
        out[col] = le.fit_transform(out[col].astype(str))
    return out


def add_frequency_encodings(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    for col in FREQUENCY_ENCODE_COLS:
        if col not in out.columns:
            continue
        freq_map = out[col].value_counts().to_dict()
        out[f"{col}_freq"] = out[col].map(freq_map)
    return out


def engineer_velocity_and_device(df: pd.DataFrame) -> pd.DataFrame:
    out = df.copy()
    out["TXN_TIMESTAMP"] = pd.to_datetime(out["TXN_TIMESTAMP"], format="%d/%m/%Y %H:%M", errors="coerce")

    out["hour"] = out["TXN_TIMESTAMP"].dt.hour
    out["day_of_week"] = out["TXN_TIMESTAMP"].dt.dayofweek
    out["is_night"] = out["hour"].apply(lambda x: 1 if x < 6 or x > 22 else 0)

    out = out.sort_values(by=["PAYER_VPA", "TXN_TIMESTAMP"])
    for payer in out["PAYER_VPA"].unique():
        user_df = out[out["PAYER_VPA"] == payer]
        timestamps = user_df["TXN_TIMESTAMP"].tolist()
        counts = []
        for i in range(len(timestamps)):
            t1 = timestamps[i]
            window_start = t1 - pd.Timedelta(hours=1)
            count = sum((t >= window_start) and (t < t1) for t in timestamps[:i])
            counts.append(count)
        out.loc[out["PAYER_VPA"] == payer, "txn_count_1h"] = counts

    out["txn_count_1h"] = out["txn_count_1h"].fillna(0).astype(int)

    out["avg_amount_user"] = out.groupby("PAYER_VPA")["AMOUNT"].transform("mean")
    out["spending_spike"] = (out["AMOUNT"] > 3 * out["avg_amount_user"]).astype(int)

    device_user_counts = out.groupby("DEVICE_ID")["PAYER_VPA"].nunique()
    out["device_user_count"] = out["DEVICE_ID"].map(device_user_counts)

    last_device = out.groupby("PAYER_VPA")["DEVICE_ID"].shift()
    out["new_device_flag"] = (out["DEVICE_ID"] != last_device).astype(int)

    return out


def preprocess_then_features(df: pd.DataFrame) -> pd.DataFrame:
    out = drop_unused_columns(df)
    out = label_encode_like_manit(out)
    out = add_frequency_encodings(out)
    out = engineer_velocity_and_device(out)
    return out


def model_feature_subset(df: pd.DataFrame) -> pd.DataFrame:
    cols = list(MANIT_MODEL_FEATURE_COL)
    if "IS_FRAUD" in df.columns:
        cols.append("IS_FRAUD")
    return df[cols].copy()
