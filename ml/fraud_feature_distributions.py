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

from config import MAX_DEVICE_USER_COUNT, MAX_TXN_COUNT_1H


def sample_device_user_count(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Overlapping categorical distributions over 1..MAX_DEVICE_USER_COUNT."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(1, MAX_DEVICE_USER_COUNT + 1, dtype=np.int64)
    # Legit: mostly 1–2; fraud: mostly 3–4; overlap on 2–3
    p_legit = np.array([0.42, 0.36, 0.14, 0.08], dtype=float)
    p_fraud = np.array([0.07, 0.11, 0.32, 0.50], dtype=float)
    fi = np.flatnonzero(fraud_mask)
    li = np.flatnonzero(~fraud_mask)
    if len(li):
        out[li] = rng.choice(vals, size=len(li), p=p_legit)
    if len(fi):
        out[fi] = rng.choice(vals, size=len(fi), p=p_fraud)
    return out


def sample_txn_count_1h(fraud_mask: np.ndarray, rng: np.random.Generator) -> np.ndarray:
    """Overlapping 1..MAX_TXN_COUNT_1H; fraud mass shifted toward higher counts."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(1, MAX_TXN_COUNT_1H + 1, dtype=np.int64)
    # Skew legit low, fraud high; both cover full range
    p_legit = np.array(
        [0.12, 0.11, 0.10, 0.09, 0.08, 0.08, 0.07, 0.06, 0.06, 0.05, 0.05, 0.04, 0.04, 0.03, 0.02],
        dtype=float,
    )
    p_fraud = np.array(
        [0.02, 0.03, 0.04, 0.05, 0.06, 0.08, 0.10, 0.11, 0.11, 0.10, 0.09, 0.08, 0.07, 0.06, 0.06],
        dtype=float,
    )
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
    """Overlapping counts 0..10; legit has substantial non-zero mass."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(0, 11, dtype=np.int64)
    p_legit = np.array([18, 20, 16, 14, 12, 8, 5, 4, 2, 1, 1], dtype=float)
    p_fraud = np.array([5, 8, 10, 12, 14, 15, 12, 10, 8, 4, 2], dtype=float)
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
    """Overlapping 0..6; legit often 0–1 but not ~90% at 0."""
    n = len(fraud_mask)
    out = np.empty(n, dtype=np.int64)
    vals = np.arange(0, 7, dtype=np.int64)
    p_legit = np.array([38, 28, 16, 9, 5, 2, 2], dtype=float)
    p_fraud = np.array([8, 12, 18, 20, 18, 14, 10], dtype=float)
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
