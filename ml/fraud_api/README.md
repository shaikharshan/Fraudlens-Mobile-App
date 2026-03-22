# FraudLens fraud scoring API

Serves the trained sklearn bundle from `ml/model_training/artifacts/` (same preprocessing as training).

## Requirements

- Python 3.10+
- From repo root: `pip install -r ml/requirements.txt`
- Copy or ensure these exist next to the server working directory (default paths are under `ml/`):
  - `model_training/artifacts/<name>_model.pkl` (includes `model` + `beneficiary_encoder`)
  - Optional: `model_training/artifacts/best_model.txt` (picks which `*_model.pkl` to load if `FRAUD_MODEL_PATH` is unset)

## Run locally

From the **`ml`** directory (so `model_training` and `config` resolve):

```bash
cd ml
set FRAUD_MODEL_PATH=model_training/artifacts/logistic_regression_model.pkl
uvicorn fraud_api.main:app --host 0.0.0.0 --port 8000
```

On Windows PowerShell:

```powershell
cd ml
$env:FRAUD_MODEL_PATH = "model_training/artifacts/logistic_regression_model.pkl"
uvicorn fraud_api.main:app --host 0.0.0.0 --port 8000
```

- **Health:** `GET http://localhost:8000/health`
- **Docs:** `GET http://localhost:8000/docs` (OpenAPI / Swagger)

### Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `FRAUD_MODEL_PATH` | `best_model.txt` → `*_model.pkl`, else `logistic_regression_model.pkl` | Absolute or path relative to **current working directory** (`ml` when using examples above) |
| `CORS_ORIGINS` | `*` | Comma-separated origins for mobile/web dev; tighten in production |

## Request body (JSON)

`POST /predict` and items in `POST /predict_batch` body array:

| Field | Type | Notes |
|-------|------|--------|
| `txn_id` | string | Your correlation id |
| `AMOUNT` | number | |
| `TXN_TIMESTAMP` | string | Parseable by pandas (e.g. ISO `2025-03-06T23:39:48`) |
| `PAYER_VPA`, `BENEFICIARY_VPA` | string | |
| `PAYER_IFSC`, `BENEFICIARY_IFSC` | string | |
| `INITIATION_MODE` | string | Default `APP` (maps like training) |
| `TRANSACTION_TYPE` | string | `P2P` / `P2M` / `M2P` |
| `device_user_count` | int ≥ 1 | App-derived; clipped server-side to `MAX_DEVICE_USER_COUNT` |
| `txn_count_1h` | int ≥ 1 | App-derived; clipped to `MAX_TXN_COUNT_1H` |
| `failed_txn_count_24h` | int ≥ 0 | Prior 24h non-success count (no leakage from *current* response) |
| `consecutive_failures` | int ≥ 0 | Back-to-back failures before this attempt |

**Removed vs old API:** `TRN_STATUS`, `RESPONSE_CODE` (and derived `is_success`) are not used, to avoid leakage.

## Example: curl

```bash
curl -s -X POST "http://localhost:8000/predict" ^
  -H "Content-Type: application/json" ^
  -d "{\"txn_id\":\"t1\",\"AMOUNT\":500.0,\"TXN_TIMESTAMP\":\"2025-03-06T23:39:48\",\"PAYER_VPA\":\"user@pthdfc\",\"BENEFICIARY_VPA\":\"shop@okaxis\",\"PAYER_IFSC\":\"SBIN0001234\",\"BENEFICIARY_IFSC\":\"HDFC0000001\",\"INITIATION_MODE\":\"APP\",\"TRANSACTION_TYPE\":\"P2P\",\"device_user_count\":2,\"txn_count_1h\":5,\"failed_txn_count_24h\":1,\"consecutive_failures\":0}"
```

## Mobile app integration

1. **Base URL** — Use your deployed host, e.g. `https://api.yourdomain.com` (or `http://10.0.2.2:8000` for Android emulator to host machine).
2. **TLS** — Use HTTPS in production; pin or trust certificate as per your security policy.
3. **Endpoints** — `POST /predict` (single) or `POST /predict_batch` (array of same shape).
4. **Headers** — `Content-Type: application/json`.
5. **Compute before call** — On device, maintain rolling windows to fill `device_user_count`, `txn_count_1h`, `failed_txn_count_24h`, `consecutive_failures` from local history (see `ml/TRAINING_NOTES.txt`).
6. **Response** — `fraud_probability` (0–1), `is_fraud` (threshold 0.5 from sklearn), `risk_level` (`HIGH` / `MEDIUM` / `LOW`). Tune thresholds in the app if needed.

### Android (Retrofit / OkHttp)

```kotlin
// POST to "${baseUrl}/predict" with JSON body matching TransactionData fields
// Use Gson or kotlinx.serialization for the request DTO
```

### Flutter / Dart

```dart
final uri = Uri.parse('$baseUrl/predict');
final response = await http.post(
  uri,
  headers: {'Content-Type': 'application/json'},
  body: jsonEncode({
    'txn_id': id,
    'AMOUNT': amount,
    'TXN_TIMESTAMP': timestampIso,
    // ... all required fields
  }),
);
```

## Deploy (outline)

- **Container:** Docker image with `python`, copy `ml/` (or `ml/fraud_api` + `ml/model_training/artifacts` + `ml/model_training/features.py` + `ml/config.py`), `WORKDIR ml`, `CMD ["uvicorn", "fraud_api.main:app", "--host", "0.0.0.0", "--port", "8000"]`.
- **Cloud:** Any service that runs a Python process (Cloud Run, Fly.io, ECS, etc.); set `FRAUD_MODEL_PATH` to the artifact path inside the image.
- **Scaling:** Model is in-memory; use one process per vCPU or load-balance with sticky sessions not required (stateless).
