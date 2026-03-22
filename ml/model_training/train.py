"""
Train and compare fraud classifiers on synthetic_transactions.csv; save .pkl artifacts.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import HistGradientBoostingClassifier, RandomForestClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    average_precision_score,
    confusion_matrix,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

_ML_DIR = Path(__file__).resolve().parents[1]
if str(_ML_DIR) not in sys.path:
    sys.path.insert(0, str(_ML_DIR))

from model_training.features import (  # noqa: E402
    FEATURE_COLUMNS,
    build_feature_frame,
    fit_beneficiary_encoder,
)


def _build_models(random_state: int) -> dict[str, object]:
    return {
        "logistic_regression": Pipeline(
            [
                ("scaler", StandardScaler()),
                (
                    "clf",
                    LogisticRegression(
                        class_weight="balanced",
                        max_iter=2000,
                        random_state=random_state,
                        solver="lbfgs",
                    ),
                ),
            ]
        ),
        "random_forest": RandomForestClassifier(
            class_weight="balanced",
            n_estimators=200,
            max_depth=None,
            random_state=random_state,
            n_jobs=-1,
        ),
        "hist_gradient_boosting": HistGradientBoostingClassifier(
            class_weight="balanced",
            max_depth=8,
            max_iter=200,
            random_state=random_state,
        ),
    }


def _metrics_dict(y_true: np.ndarray, y_pred: np.ndarray, y_proba: np.ndarray) -> dict:
    tn, fp, fn, tp = confusion_matrix(y_true, y_pred).ravel()
    return {
        "accuracy": float(accuracy_score(y_true, y_pred)),
        "roc_auc": float(roc_auc_score(y_true, y_proba)),
        "pr_auc": float(average_precision_score(y_true, y_proba)),
        "confusion_matrix": {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)},
    }


def main() -> None:
    p = argparse.ArgumentParser(description="Train fraud models on synthetic CSV.")
    p.add_argument(
        "--csv",
        type=Path,
        default=_ML_DIR / "data" / "synthetic_transactions.csv",
        help="Path to synthetic_transactions.csv",
    )
    p.add_argument(
        "--out",
        type=Path,
        default=_ML_DIR / "model_training" / "artifacts",
        help="Output directory for .pkl and metrics",
    )
    p.add_argument("--test-size", type=float, default=0.2)
    p.add_argument("--seed", type=int, default=42)
    args = p.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)

    df = pd.read_csv(args.csv)
    if "IS_FRAUD" not in df.columns:
        raise SystemExit("CSV must contain IS_FRAUD column")

    y = df["IS_FRAUD"].astype(int).values
    train_idx, test_idx = train_test_split(
        np.arange(len(df)),
        test_size=args.test_size,
        random_state=args.seed,
        stratify=y,
    )
    train_df = df.iloc[train_idx].reset_index(drop=True)
    test_df = df.iloc[test_idx].reset_index(drop=True)

    bene_enc = fit_beneficiary_encoder(train_df)
    X_train, _ = build_feature_frame(train_df, beneficiary_encoder=bene_enc)
    X_test, _ = build_feature_frame(test_df, beneficiary_encoder=bene_enc)
    y_train = train_df["IS_FRAUD"].astype(int).values
    y_test = test_df["IS_FRAUD"].astype(int).values

    models = _build_models(args.seed)
    results: dict[str, dict] = {}

    for name, est in models.items():
        est.fit(X_train, y_train)
        y_pred = est.predict(X_test)
        y_proba = est.predict_proba(X_test)[:, 1]
        results[name] = _metrics_dict(y_test, y_pred, y_proba)

        artifact = {
            "model": est,
            "beneficiary_encoder": bene_enc,
            "feature_columns": FEATURE_COLUMNS,
        }
        path = args.out / f"{name}_model.pkl"
        joblib.dump(artifact, path)

    # Shared encoder + feature list (for inference that loads only encoder)
    joblib.dump(bene_enc, args.out / "beneficiary_encoder.pkl")

    with open(args.out / "feature_columns.json", "w", encoding="utf-8") as f:
        json.dump({"feature_columns": FEATURE_COLUMNS}, f, indent=2)

    best_by_roc = max(results.keys(), key=lambda k: results[k]["roc_auc"])
    with open(args.out / "best_model.txt", "w", encoding="utf-8") as f:
        f.write(best_by_roc + "\n")

    summary = {
        "test_size": args.test_size,
        "random_seed": args.seed,
        "n_train": int(len(train_df)),
        "n_test": int(len(test_df)),
        "models": results,
        "best_model_roc_auc": best_by_roc,
    }
    with open(args.out / "metrics.json", "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
