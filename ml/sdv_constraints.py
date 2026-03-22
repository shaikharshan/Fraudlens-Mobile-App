"""
Constraint-Augmented Generation (CAG) for FraudLens single-table synthesis.

Rules encoded here are *business-valid* for the prepared CSV (SDV requires 100%
compliance in the real data used to fit).

TRN_STATUS / RESPONSE_CODE are not modeled (avoid inference-time leakage).
"""

from __future__ import annotations

from sdv.cag import Inequality


def build_constraints():
    """
    Return SDV constraint objects.

    - Inequality(_sdv_strict_positive, AMOUNT): amounts are strictly positive.
    - Inequality(_sdv_strict_positive, txn_count_1h): counts are strictly positive.
    - Inequality(_sdv_strict_positive, device_user_count): at least one user on device.
    - Inequality(device_user_count, _sdv_device_cap): device_user_count < 5 (realistic UPI wallet cap).
    """
    return [
        Inequality(low_column_name="_sdv_strict_positive", high_column_name="AMOUNT"),
        Inequality(low_column_name="_sdv_strict_positive", high_column_name="txn_count_1h"),
        Inequality(low_column_name="_sdv_strict_positive", high_column_name="device_user_count"),
        Inequality(low_column_name="device_user_count", high_column_name="_sdv_device_cap"),
    ]
