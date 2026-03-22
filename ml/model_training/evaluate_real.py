"""
Evaluate trained fraud models on real-world CSV with increasing difficulty:
  1 easy   — full sample + honest rolling features (history-only; no label leakage).
  2 medium — device/txn velocity overlap band (more class overlap).
  3 hard   — overlap band plus low prior-failure counts (smallest, most ambiguous).

Writes ml/model_training/artifacts/real_world_metrics.json
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    confusion_matrix,
    roc_auc_score,
)

_ML_DIR = Path(__file__).resolve().parents[1]
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))

from model_training.features import build_feature_frame  # noqa: E402
from model_training.real_world_prep import load_real_world_csv  # noqa: E402


def _safe_roc_auc(y_true: np.ndarray, y_score: np.ndarray) -> float | None:
    if len(np.unique(y_true)) < 2:
        return None
    return float(roc_auc_score(y_true, y_score))


def _safe_pr_auc(y_true: np.ndarray, y_score: np.ndarray) -> float | None:
    if len(np.unique(y_true)) < 2:
        return None
    return float(average_precision_score(y_true, y_score))


def _metrics_block(y_true: np.ndarray, y_pred: np.ndarray, y_proba: np.ndarray) -> dict:
    out: dict = {
        "n": int(len(y_true)),
        "fraud_rate": float(np.mean(y_true)),
        "accuracy": float(accuracy_score(y_true, y_pred)),
    }
    ra = _safe_roc_auc(y_true, y_proba)
    pa = _safe_pr_auc(y_true, y_proba)
    out["roc_auc"] = ra
    out["pr_auc"] = pa
    if len(np.unique(y_true)) >= 2:
        tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
        out["confusion_matrix"] = {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)}
    else:
        out["confusion_matrix"] = None
    return out


def _tier_masks(df: pd.DataFrame) -> dict[str, pd.Series]:
    """
    easy → hard by making the prediction problem more constrained / ambiguous:

    easy — full sample with honest history features (most stable metrics).
    medium — device velocity overlap band (classes less separable by volume alone).
    hard — overlap band AND low prior-failure counts (smallest, most ambiguous slice).
    """
    easy = pd.Series(True, index=df.index)
    overlap = df["device_user_count"].between(2, 3) & df["txn_count_1h"].between(4, 12, inclusive="both")
    low_fail = (df["failed_txn_count_24h"] <= 1) & (df["consecutive_failures"] <= 1)
    hard = overlap & low_fail
    return {
        "1_easy_full_sample_honest": easy,
        "2_medium_device_velocity_overlap": overlap,
        "3_hard_overlap_low_failure_signal": hard,
    }


def main() -> None:
    p = argparse.ArgumentParser(description="Evaluate fraud model on real anonymized CSV.")
    p.add_argument(
        "--csv",
        type=Path,
        default=_ML_DIR.parent / "anonymized_sample_fraud_txn.csv",
        help="Raw anonymized transaction CSV",
    )
    p.add_argument(
        "--artifact-dir",
        type=Path,
        default=_ML_DIR / "model_training" / "artifacts",
        help="Directory containing *_model.pkl and best_model.txt",
    )
    p.add_argument(
        "--model",
        type=str,
        default=None,
        help="Model name key (e.g. logistic_regression). Default: read best_model.txt",
    )
    p.add_argument("--out", type=Path, default=None, help="Output JSON path")
    args = p.parse_args()

    artifact_dir = args.artifact_dir
    if args.model:
        name = args.model
    else:
        best_path = artifact_dir / "best_model.txt"
        if not best_path.is_file():
            raise SystemExit(f"Missing {best_path}; pass --model explicitly")
        name = best_path.read_text(encoding="utf-8").strip()

    pkl_path = artifact_dir / f"{name}_model.pkl"
    bundle = joblib.load(pkl_path)
    model = bundle["model"]
    bene_enc = bundle["beneficiary_encoder"]

    df = load_real_world_csv(args.csv)
    X, _ = build_feature_frame(df, beneficiary_encoder=bene_enc)
    y = df["IS_FRAUD"].astype(int).values

    y_proba_full = model.predict_proba(X)[:, 1]
    y_pred_full = model.predict(X)

    tiers = _tier_masks(df)
    report: dict = {
        "csv": str(args.csv.resolve()),
        "model": name,
        "n_rows_total": len(df),
        "note": "Tiers 2–3 use small slices; ROC-AUC/PR-AUC can be noisy. Compare to tier 1 for stable OOD signal.",
        "global_metrics": _metrics_block(y, y_pred_full, y_proba_full),
        "tiers": {},
    }

    for tier_name, mask in tiers.items():
        m = mask.to_numpy()
        if m.sum() < 10:
            report["tiers"][tier_name] = {
                "skipped": True,
                "reason": "too_few_rows",
                "n": int(m.sum()),
            }
            continue
        yt = y[m]
        yp = model.predict(X.loc[mask])
        ypr = model.predict_proba(X.loc[mask])[:, 1]
        report["tiers"][tier_name] = {
            "skipped": False,
            "description": {
                "1_easy_full_sample_honest": "All rows; honest rolling features from history only (no label leakage).",
                "2_medium_device_velocity_overlap": "device_user_count in [2,3] and txn_count_1h in [4,12].",
                "3_hard_overlap_low_failure_signal": "Same overlap band plus failed_txn_count_24h<=1 and consecutive_failures<=1.",
            }[tier_name],
            **_metrics_block(yt, yp, ypr),
        }

    out_path = args.out or (artifact_dir / "real_world_metrics.json")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=2)

    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
