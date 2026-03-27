"""
Overlapping, label-conditional distributions for device / velocity / rolling-failure features.

Priority (strongest → weakest fraud signal in training data):
  1. device_user_count — primary; fraud skewed toward 3–4, legit toward 1–2, with overlap.
  2. txn_count_1h — secondary; fraud skewed high, legit skewed low; both use 1..MAX_TXN_COUNT_1H.
  3. failed_txn_count_24h, consecutive_failures — tertiary; broad overlap so legit rows are not
     almost always zero.

Used by prepare_data (seed for SDV) and post-SDV enforcement in generate_synthetic.
"""

from __future__ import annotations

import numpy as np

from config import (
    MAX_AMOUNT_SUM_1H,
    MAX_CONSECUTIVE_FAILURES,
    MAX_DEVICE_USER_COUNT,
    MAX_FAILED_TXN_COUNT_24H,
    MAX_TXN_COUNT_1H,
)


def sample_device_user_count(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Label-conditional categorical distributions over 1..MAX_DEVICE_USER_COUNT.

    Tuned to make device_user_count a strong fraud signal (higher recall).
    """
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(1, MAX_DEVICE_USER_COUNT + 1, dtype=np.int64)
    # Legit: overwhelmingly 1–2; Fraud: overwhelmingly 3–4
    p_legit = np.array([0.74, 0.22, 0.03, 0.01], dtype=float)
    p_fraud = np.array([0.01, 0.04, 0.25, 0.70], dtype=float)
    fi = np.flatnonzero(fraud_mask)
    li = np.flatnonzero(~fraud_mask)
    if len(li):
        out[li] = rng.choice(vals, size=len(li), p=p_legit)
    if len(fi):
        out[fi] = rng.choice(vals, size=len(fi), p=p_fraud)
    return out


def sample_txn_count_1h(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Overlapping 1..MAX_TXN_COUNT_1H with fraud sensitivity at moderate values."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(1, MAX_TXN_COUNT_1H + 1, dtype=np.int64)
    # Legit: heavy mass near 1–3; Fraud: heavy mass near ~6–12
    p_legit = np.exp(-0.7 * (vals - 1))
    p_fraud = np.exp(-0.35 * (MAX_TXN_COUNT_1H - vals))
    p_legit /= p_legit.sum()
    p_fraud /= p_fraud.sum()
    fi = np.flatnonzero(fraud_mask)
    li = np.flatnonzero(~fraud_mask)
    if len(li):
        out[li] = rng.choice(vals, size=len(li), p=p_legit)
    if len(fi):
        out[fi] = rng.choice(vals, size=len(fi), p=p_fraud)
    return out


def sample_device_and_txn_overlap(
    n_rows: int, fraud_mask: np.ndarray, rng: np.random.Generator
) -> tuple[np.ndarray, np.ndarray]:
    """device_user_count (priority 1) and txn_count_1h (priority 2), overlapping by class."""
    dev = sample_device_user_count(fraud_mask, rng)
    txn = sample_txn_count_1h(fraud_mask, rng)
    # Weak coupling: high device count slightly bumps txn (bounded); reinforces primary signal.
    bump = rng.random(n_rows) < 0.25
    hi_dev = dev >= 3
    txn = np.where(bump & hi_dev & fraud_mask, np.minimum(txn + rng.integers(0, 2, size=n_rows), MAX_TXN_COUNT_1H), txn)
    txn = np.where(bump & (dev <= 2) & ~fraud_mask, np.maximum(txn - rng.integers(0, 2, size=n_rows), 1), txn)
    return dev, txn


def sample_failed_txn_count_24h(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Label-conditional counts 0..MAX_FAILED_TXN_COUNT_24H with fraud lift at 3+."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(0, MAX_FAILED_TXN_COUNT_24H + 1, dtype=np.int64)
    p_legit = np.exp(-0.65 * vals)  # mostly 0–2
    p_fraud = np.exp(-0.25 * (MAX_FAILED_TXN_COUNT_24H - vals))  # skew toward higher failures
    p_legit /= p_legit.sum()
    p_fraud /= p_fraud.sum()
    fi = np.flatnonzero(fraud_mask)
    li = np.flatnonzero(~fraud_mask)
    if len(li):
        out[li] = rng.choice(vals, size=len(li), p=p_legit)
    if len(fi):
        out[fi] = rng.choice(vals, size=len(fi), p=p_fraud)
    return out


def sample_consecutive_failures(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Label-conditional 0..MAX_CONSECUTIVE_FAILURES with fraud lift at 2+."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(0, MAX_CONSECUTIVE_FAILURES + 1, dtype=np.int64)
    p_legit = np.exp(-0.9 * vals)  # mostly 0–1
    p_fraud = np.exp(-0.35 * (MAX_CONSECUTIVE_FAILURES - vals))  # skew high
    p_legit /= p_legit.sum()
    p_fraud /= p_fraud.sum()
    fi = np.flatnonzero(fraud_mask)
    li = np.flatnonzero(~fraud_mask)
    if len(li):
        out[li] = rng.choice(vals, size=len(li), p=p_legit)
    if len(fi):
        out[fi] = rng.choice(vals, size=len(fi), p=p_fraud)
    return out


def inject_rolling_features_by_is_fraud(
    n_rows: int, fraud_mask: np.ndarray, rng: np.random.Generator
) -> tuple[np.ndarray, np.ndarray]:
    """failed_txn_count_24h and consecutive_failures with overlapping class distributions."""
    failed = sample_failed_txn_count_24h(fraud_mask, rng)
    conv = sample_consecutive_failures(fraud_mask, rng)
    return failed, conv


def sample_amount_sum_1h_by_is_fraud(
    txn_count_1h: np.ndarray,
    fraud_mask: np.ndarray,
    rng: np.random.Generator,
) -> np.ndarray:
    """
    Value-velocity feature: total outgoing amount in 1h.

    Fraud rows get a higher per-txn average and larger variance, while still depending on
    txn_count_1h so this feature stays coherent with velocity.
    """
    txn = np.clip(txn_count_1h.astype(np.float64), 1.0, float(MAX_TXN_COUNT_1H))
    n = len(txn)

    per_txn_legit = rng.lognormal(mean=np.log(650.0), sigma=0.55, size=n)
    per_txn_fraud = rng.lognormal(mean=np.log(1500.0), sigma=0.75, size=n)
    per_txn = np.where(fraud_mask, per_txn_fraud, per_txn_legit)

    raw_sum = per_txn * txn
    return np.clip(raw_sum, 0.0, float(MAX_AMOUNT_SUM_1H))
