"""
Build `SingleTableMetadata` for FraudLens transaction rows.

VPAs use ``email`` + ``pii=True`` so SDV treats them as anonymized identifiers
(helpful for `payer_vpa_length`-style features). IFSCs stay ``id``.
"""

from __future__ import annotations

import sys
from pathlib import Path

import pandas as pd
from sdv.metadata import SingleTableMetadata

_ML_DIR = Path(__file__).resolve().parent
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))


def build_metadata(df: pd.DataFrame) -> SingleTableMetadata:
    metadata = SingleTableMetadata()
    metadata.detect_from_dataframe(df)

    metadata.update_column("TXN_TIMESTAMP", sdtype="datetime", datetime_format="%Y-%m-%d %H:%M:%S")
    metadata.update_column("AMOUNT", sdtype="numerical")
    # UPI-style `user@psp` strings: email + PII lets SDV anonymize while preserving length-like behaviour
    metadata.update_column("PAYER_VPA", sdtype="email", pii=True)
    metadata.update_column("BENEFICIARY_VPA", sdtype="email", pii=True)
    metadata.update_column("PAYER_IFSC", sdtype="id")
    metadata.update_column("BENEFICIARY_IFSC", sdtype="id")
    metadata.update_column("INITIATION_MODE", sdtype="categorical")
    metadata.update_column("TRANSACTION_TYPE", sdtype="categorical")
    metadata.update_column("failed_txn_count_24h", sdtype="numerical")
    metadata.update_column("consecutive_failures", sdtype="numerical")
    metadata.update_column("device_user_count", sdtype="numerical")
    metadata.update_column("txn_count_1h", sdtype="numerical")
    metadata.update_column("hour", sdtype="numerical")
    metadata.update_column("is_weekend", sdtype="categorical")
    metadata.update_column("is_night", sdtype="categorical")
    metadata.update_column("IS_FRAUD", sdtype="categorical")
    metadata.update_column("_sdv_strict_positive", sdtype="numerical")
    metadata.update_column("_sdv_device_cap", sdtype="numerical")

    return metadata
