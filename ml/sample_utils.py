"""Stratified conditional sampling and timestamp alignment for SDV output."""

from __future__ import annotations

import numpy as np
import pandas as pd
from sdv.sampling import Condition

_NIGHT_HOURS = np.array([22, 23, 0, 1, 2, 3, 4, 5, 6])


def compute_quadrant_weights(
    prepared: pd.DataFrame,
    empirical: bool,
    default_quadrants: tuple[float, float, float, float],
    night_fraud_boost: float,
    rng: np.random.Generator,
) -> np.ndarray:
    """Return normalized weights for (fraud+night, fraud+day, legit+night, legit+day)."""
    if empirical:
        fn = int(((prepared["IS_FRAUD"] == 1) & (prepared["is_night"] == 1)).sum())
        fd = int(((prepared["IS_FRAUD"] == 1) & (prepared["is_night"] == 0)).sum())
        ln = int(((prepared["IS_FRAUD"] == 0) & (prepared["is_night"] == 1)).sum())
        ld = int(((prepared["IS_FRAUD"] == 0) & (prepared["is_night"] == 0)).sum())
        w = np.array([fn, fd, ln, ld], dtype=float)
        if w.sum() <= 0:
            w = np.array(default_quadrants, dtype=float)
        else:
            w[0] *= night_fraud_boost
            w = w / w.sum()
    else:
        w = np.array(default_quadrants, dtype=float)
        w = w / w.sum()
    return w


def multinomial_counts(num_rows: int, p: np.ndarray, rng: np.random.Generator) -> list[int]:
    return rng.multinomial(num_rows, p).tolist()


def build_conditions(counts: list[int]) -> list:
    """(IS_FRAUD, is_night) quadrants: (1,1), (1,0), (0,1), (0,0)."""
    specs = [
        {"IS_FRAUD": 1, "is_night": 1},
        {"IS_FRAUD": 1, "is_night": 0},
        {"IS_FRAUD": 0, "is_night": 1},
        {"IS_FRAUD": 0, "is_night": 0},
    ]
    out = []
    for spec, n in zip(specs, counts):
        if n > 0:
            out.append(Condition(column_values=spec, num_rows=n))
    return out


def sample_conditional_stratified(synthesizer, num_rows: int, prepared: pd.DataFrame, empirical: bool, default_quadrants, night_fraud_boost: float, rng: np.random.Generator, max_tries: int = 100):
    w = compute_quadrant_weights(prepared, empirical, default_quadrants, night_fraud_boost, rng)
    counts = multinomial_counts(num_rows, w, rng)
    conditions = build_conditions(counts)
    if not conditions:
        return synthesizer.sample(num_rows=num_rows, max_tries_per_batch=max_tries)
    try:
        return synthesizer.sample_from_conditions(conditions, max_tries_per_batch=max_tries)
    except Exception:
        return synthesizer.sample(num_rows=num_rows, max_tries_per_batch=max_tries)


def reconcile_timestamp_with_is_night(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """Force clock time to match ``is_night`` (conditional sample can disagree with SDV timestamps)."""
    ts = pd.to_datetime(df["TXN_TIMESTAMP"])
    night = df["is_night"].astype(int).to_numpy() == 1
    n = len(df)
    mins = rng.integers(0, 60, size=n)
    secs = rng.integers(0, 60, size=n)
    hours = np.zeros(n, dtype=int)
    if night.sum():
        hours[night] = rng.choice(_NIGHT_HOURS, size=int(night.sum()))
    if (~night).sum():
        hours[~night] = rng.integers(7, 22, size=int((~night).sum()))
    new_ts = []
    for i in range(n):
        t = ts.iloc[i]
        new_ts.append(
            t.replace(hour=int(hours[i]), minute=int(mins[i]), second=int(secs[i]))
        )
    df["TXN_TIMESTAMP"] = pd.to_datetime(new_ts)
