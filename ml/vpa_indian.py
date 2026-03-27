"""
Indian UPI VPA shapes: PSP handles + label-conditioned lengths for payer_vpa_length / beneficiary_vpa_length.

Profiles:
- Legit: shorter VPAs (~13–18 chars) on both sides.
- Fraud: payer medium (compromised-looking, ~14–22); beneficiary very long (impersonator/bot, ~24–35).
"""

from __future__ import annotations

import re
import string

import numpy as np
import pandas as pd

INDIAN_UPI_PSP_HANDLES: tuple[str, ...] = (
    "okhdfcbank",
    "okicici",
    "oksbi",
    "ybl",
    "paytm",
    "ibl",
    "okaxis",
    "axisbank",
    "axl",
    "ptyes",
    "ptaxis",
    "pthdfc",
    "ptybl",
    "centralbank",
    "sbin",
    "cbin",
)

_PSP_SET = frozenset(INDIAN_UPI_PSP_HANDLES)
_LOCAL_OK = re.compile(r"^[a-zA-Z0-9._-]{3,64}$")


def _prefix_alnum(plen: int, rng: np.random.Generator) -> str:
    pool = string.ascii_lowercase + string.digits
    return "".join(rng.choice(list(pool), size=max(plen, 1)))


def _prefix_phone_style(plen: int, rng: np.random.Generator) -> str:
    """e.g. 9876543210@ybl style local part."""
    if plen <= 0:
        return "x"
    nd = min(10, plen)
    digits = "".join(str(int(rng.integers(0, 10))) for _ in range(nd))
    if plen > nd:
        digits += _prefix_alnum(plen - nd, rng)
    return digits[:plen]


def _prefix_medium_bot(plen: int, rng: np.random.Generator) -> str:
    """Random alnum cluster + dots (20–26 char 'bot' feel) for fraud payer."""
    pool = string.ascii_lowercase + string.digits + "."
    raw = "".join(rng.choice(list(pool), size=max(plen, 1)))
    return raw.strip(".")[:plen].ljust(plen, "a")[:plen]


def _prefix_impersonator(plen: int, rng: np.random.Generator) -> str:
    """Long official-looking strings with dots (customer.support.refund style)."""
    if plen <= 0:
        return "x"
    pool = string.ascii_lowercase + string.digits + "."
    return "".join(rng.choice(list(pool), size=plen))


def build_vpa_exact_length(
    target_length: int,
    rng: np.random.Generator,
    *,
    impersonator_long: bool = False,
    medium_bot: bool = False,
    normal_short: bool = True,
) -> str:
    """Build ``local@psp`` with total length ``target_length`` (ASCII)."""
    psp = str(rng.choice(INDIAN_UPI_PSP_HANDLES))
    suffix = "@" + psp
    plen = target_length - len(suffix)
    if plen < 3:
        psp = "ybl"
        suffix = "@" + psp
        plen = target_length - len(suffix)
        if plen < 3:
            plen = 3
    if impersonator_long:
        prefix = _prefix_impersonator(plen, rng)
    elif medium_bot:
        prefix = _prefix_medium_bot(plen, rng)
    elif normal_short and plen >= 12 and rng.random() < 0.45:
        prefix = _prefix_phone_style(plen, rng)
    else:
        prefix = _prefix_alnum(plen, rng)
    out = prefix + suffix
    if len(out) < target_length:
        out = out + "x" * (target_length - len(out))
    return out[:target_length]


def inject_vpa_by_fraud_label(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """
    Overwrite PAYER_VPA / BENEFICIARY_VPA with label-conditioned lengths so SDV learns
    payer_vpa_length / beneficiary_vpa_length vs IS_FRAUD. Call before DEVICE_ID → nunique(payer).
    """
    n = len(df)
    fraud = df["IS_FRAUD"].astype(int).to_numpy() == 1
    payers: list[str] = []
    benes: list[str] = []
    for i in range(n):
        if fraud[i]:
            lp = int(rng.integers(14, 23, endpoint=False))
            lb = int(rng.integers(24, 36, endpoint=False))
            payers.append(
                build_vpa_exact_length(lp, rng, normal_short=False, medium_bot=rng.random() < 0.55, impersonator_long=False)
            )
            benes.append(build_vpa_exact_length(lb, rng, impersonator_long=True))
        else:
            lp = int(rng.integers(13, 19, endpoint=False))
            lb = int(rng.integers(13, 19, endpoint=False))
            payers.append(build_vpa_exact_length(lp, rng, normal_short=True, impersonator_long=False))
            benes.append(build_vpa_exact_length(lb, rng, normal_short=True, impersonator_long=False))
    df["PAYER_VPA"] = payers
    df["BENEFICIARY_VPA"] = benes


def apply_vpa_by_fraud_label(df: pd.DataFrame, rng: np.random.Generator) -> None:
    """Same as ``inject_vpa_by_fraud_label`` (alias for post-SDV enforcement)."""
    inject_vpa_by_fraud_label(df, rng)


# --- legacy helpers used by older generate path (kept for compatibility) ---

def rewrite_vpa_indian(vpa: str, rng: np.random.Generator) -> str:
    raw = (vpa or "").strip()
    if "@" not in raw:
        return build_vpa_exact_length(int(rng.integers(16, 24)), rng, normal_short=True)
    local, _, domain = raw.partition("@")
    local, domain = local.strip(), domain.strip().lower()
    if domain in _PSP_SET and local and _LOCAL_OK.match(local):
        return f"{local}@{domain}"
    psp = str(rng.choice(INDIAN_UPI_PSP_HANDLES))
    target = max(len(local) + 1 + len(psp), 16)
    return build_vpa_exact_length(target, rng, normal_short=True)


def rewrite_vpa_columns(
    df: pd.DataFrame,
    rng: np.random.Generator,
    cols: tuple[str, ...] = ("PAYER_VPA", "BENEFICIARY_VPA"),
    *,
    label_conditioned: bool = True,
) -> pd.DataFrame:
    """
    Rewrite VPA strings into valid Indian UPI-like shapes.

    By default, this preserves the legacy behavior: if ``label_conditioned`` is True and
    ``IS_FRAUD`` is present, it will overwrite VPA lengths based on the label so SDV learns a
    strong VPA-length correlation.

    For production-like training (raw VPAs at inference), set ``label_conditioned=False`` so
    VPA shapes are normalized without leaking label-conditioned length patterns into the model.
    """
    if label_conditioned and "IS_FRAUD" in df.columns:
        inject_vpa_by_fraud_label(df, rng)
        return df
    for c in cols:
        if c in df.columns:
            df[c] = [rewrite_vpa_indian(x, rng) for x in df[c].astype(str).tolist()]
    return df
