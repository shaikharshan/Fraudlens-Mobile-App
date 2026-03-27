# FraudLens fraud scoring API


- **Health:** `GET http://localhost:8000/health`
- **Docs:** `GET http://localhost:8000/docs` (OpenAPI / Swagger)



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


Syntheetic Data Generation using SDV:
Production fraud scoring must not use RESPONSE_CODE or TRN_STATUS for the *current* transaction when those values are only known after the bank responds (label/temporal leakage).

The SDV pipeline no longer includes TRN_STATUS or RESPONSE_CODE. Instead, the seed and synthetic data use app-computable rolling features:

  failed_txn_count_24h — injected with label-conditional distributions so SDV learns correlation with IS_FRAUD (same idea as device_user_count injection).

  consecutive_failures — same.

At inference, the mobile app should compute these from history and pass them to the model.

Stratified conditional sampling (IS_FRAUD x is_night) optionally biases the joint distribution; timestamps are adjusted so TXN_TIMESTAMP matches is_night.

With fraud_correlated device mode (default), seed rows use overlapping label-conditional distributions (see fraud_feature_distributions.py): primary signal device_user_count, secondary txn_count_1h (capped at MAX_TXN_COUNT_1H=15), then failed_txn_count_24h / consecutive_failures. generate_synthetic.py reapplies the same device/txn + rolling injections after sampling unless --no-enforce-threshold-separation.

VPAs: before device_user_count is computed, payer/beneficiary strings are replaced with Indian PSP handles and label-conditioned lengths — legit ~13–18 chars both sides; fraud payer ~14–22 (often bot-style), fraud beneficiary ~24–35 (impersonator/long). generate_synthetic reapplies the same via rewrite_vpa_columns → inject_vpa_by_fraud_label.

Full 50k regeneration (from repo root, venv on):
  python ml/generate_synthetic.py --rows 50000 --refit --stratified-sampling --stratified-empirical --night-fraud-boost 1.25 --seed 42 --device-features fraud_correlated --synthesizer copula
