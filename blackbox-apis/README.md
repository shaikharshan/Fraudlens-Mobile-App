# FraudLens Blackbox APIs

Separate FastAPI service for Gemini-backed scam detection.
This folder does not modify or depend on in-place edits to existing Android app code.

## Features

- **`POST /sms/analyze`**: REST — `sender_id` + `message` → `{is_scam, confidence_score, reasoning, recommendation}`
- **`WebSocket /vishing/ws`**: Live detection — base64 PCM audio and/or text; server streams status, transcript, `fraud_analysis`, errors
- **`GET /health`**: Liveness check

---

## API reference (paths)

| Purpose | Method | Path |
|--------|--------|------|
| Health | `GET` | `/health` |
| SMS analysis | `POST` | `/sms/analyze` |
| Live (vishing) | WebSocket | `/vishing/ws` |

Interactive OpenAPI (when the server is running):

- Swagger UI: `{BASE_URL}/docs`
- OpenAPI JSON: `{BASE_URL}/openapi.json`

Replace `{BASE_URL}` with your local or Cloud Run origin (no trailing slash).

---

## Deployed service (Cloud Run)

**Example production base URL** (yours may differ if you redeploy or change region):

```text
https://fraudlens-blackbox-875422601666.us-central1.run.app
```

Set a shell variable for copy-paste examples:

```bash
export BASE_URL="https://fraudlens-blackbox-875422601666.us-central1.run.app"
```

### Production: health

```bash
curl -s "${BASE_URL}/health"
```

Expected:

```json
{"status":"ok"}
```

### Production: SMS analyze

**Request**

- **URL:** `{BASE_URL}/sms/analyze`
- **Method:** `POST`
- **Headers:** `Content-Type: application/json`
- **Body:**

| Field | Type | Required |
|-------|------|----------|
| `sender_id` | string | yes (non-empty) |
| `message` | string | yes (non-empty) |

**curl**

```bash
curl -s -X POST "${BASE_URL}/sms/analyze" \
  -H "Content-Type: application/json" \
  -d '{"sender_id":"VM-HDFCBK","message":"Your account blocked. Click http://bit.ly/x to verify."}'
```

**PowerShell**

```powershell
$BASE_URL = "https://fraudlens-blackbox-875422601666.us-central1.run.app"
$body = @{ sender_id = "VM-HDFCBK"; message = "Your account blocked. Click http://bit.ly/x to verify." } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BASE_URL/sms/analyze" -ContentType "application/json" -Body $body
```

**Response (200)**

```json
{
  "is_scam": true,
  "confidence_score": 0.93,
  "reasoning": "...",
  "recommendation": "..."
}
```

### Production: live detection (WebSocket)

Use **`wss://`** (TLS), same host and path:

```text
wss://fraudlens-blackbox-875422601666.us-central1.run.app/vishing/ws
```

**Client → server** (send each message as one WebSocket **text** frame containing JSON):

| `type` | Fields | Purpose |
|--------|--------|---------|
| `audio` | `data_b64` (required), `mime_type` (optional, default `audio/pcm;rate=16000`) | Raw PCM16 mono LE @ 16 kHz, base64 |
| `text` | `text` (required) | Text for the model |
| `disconnect` | — | End session |

Examples:

```json
{"type":"audio","data_b64":"<base64>","mime_type":"audio/pcm;rate=16000"}
```

```json
{"type":"text","text":"Hello, please share OTP to unblock account"}
```

```json
{"type":"disconnect"}
```

**Server → client** (JSON objects):

| `event` | Extra | Meaning |
|---------|--------|---------|
| `status` | `message` | Status line |
| `transcript` | `text`, `is_user` | Transcript |
| `fraud_analysis` | `analysis` (same fields as SMS response) | Fraud result |
| `error` | `message` | Error |

**Demo client (this repo)**

```bash
pip install -r requirements.txt
python scripts/ws_client_demo.py --url "wss://fraudlens-blackbox-875422601666.us-central1.run.app/vishing/ws"
```

With raw PCM (16-bit mono, 16 kHz):

```bash
python scripts/ws_client_demo.py --url "wss://fraudlens-blackbox-875422601666.us-central1.run.app/vishing/ws" --pcm-file path/to/audio.pcm
```

**wscat (Node.js)**

```bash
npx wscat -c "wss://fraudlens-blackbox-875422601666.us-central1.run.app/vishing/ws"
```

After connect, paste e.g. `{"type":"text","text":"Hello, please share OTP to unblock account"}` then `{"type":"disconnect"}`.

---

## Local development

### Setup

1. Create and activate a Python virtual environment.
2. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Set `GEMINI_API_KEY` in your environment or create a `.env` file in this directory (`python-dotenv` loads it on startup).

4. **Live / vishing WebSocket** uses Gemini [2.5 Flash Live Preview](https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash-native-audio-preview-12-2025) by default (`gemini-2.5-flash-native-audio-preview-12-2025`). The old `gemini-2.0-flash-exp` model is not valid for `BidiGenerateContent` anymore. To override the Live model, set optional env var `GEMINI_LIVE_MODEL` (with or without the `models/` prefix). Another documented Live model is `gemini-3.1-flash-live-preview` if your API key has access.

### Run

```bash
uvicorn app.main:app --reload
```

**Local base URL:** `http://127.0.0.1:8000`

### Local: health

```bash
curl http://127.0.0.1:8000/health
```

### Local: SMS

```bash
curl -X POST http://127.0.0.1:8000/sms/analyze \
  -H "Content-Type: application/json" \
  -d "{\"sender_id\":\"VM-HDFCBK\",\"message\":\"Your account blocked. Click http://bit.ly/x to verify.\"}"
```

### Local: WebSocket

- URL: `ws://127.0.0.1:8000/vishing/ws`

```bash
python scripts/ws_client_demo.py
```

With PCM file:

```bash
python scripts/ws_client_demo.py --pcm-file path/to/audio.pcm
```

### Vishing WebSocket protocol (reference)

**Client → server**

- `{"type":"audio","data_b64":"...","mime_type":"audio/pcm;rate=16000"}` (plain `audio/pcm` is normalized to `audio/pcm;rate=16000` upstream)
- `{"type":"text","text":"Analyze this phrase..."}`
- `{"type":"disconnect"}`

**Server → client**

- `{"event":"status","message":"Connecting to Gemini..."}`
- `{"event":"status","message":"Connected to Gemini"}` — wait for this before sending `audio` / `text` (the server buffers Gemini setup until setup completes).
- `{"event":"transcript","text":"...","is_user":true}`
- `{"event":"transcript","text":"...","is_user":false}`
- `{"event":"fraud_analysis","analysis":{"is_scam":true,"confidence_score":0.8,"reasoning":"...","recommendation":"..."}}`
- `{"event":"error","message":"..."}`
- `{"event":"upstream_debug","keys":["..."]}` — only when env `FRAUDLENS_VISHING_DEBUG=1` and a frame was not mapped to transcript/analysis.

---

## Docker / Cloud Run

Build and run locally:

```bash
docker build -t fraudlens-blackbox .
docker run --rm -e PORT=8080 -e GEMINI_API_KEY=your_key -p 8080:8080 fraudlens-blackbox
# Optional: -e GEMINI_LIVE_MODEL=gemini-2.5-flash-native-audio-preview-12-2025
```

Deploy uses the same image; set `GEMINI_API_KEY` via Secret Manager on Cloud Run (see project root deployment notes if any).
