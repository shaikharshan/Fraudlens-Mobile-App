# FraudLens Blackbox APIs

Separate FastAPI service for Gemini-backed scam detection.
This folder does not modify or depend on in-place edits to existing Android app code.

## Features

- `POST /sms/analyze`:
  - Input: `sender_id` + `message`
  - Output: exactly `{is_scam, confidence_score, reasoning, recommendation}`
- `WS /vishing/ws`:
  - Input stream: base64 PCM audio chunks (`audio/pcm`)
  - Output events: status, transcript, fraud_analysis, error

## Setup

1. Create and activate a Python virtual environment.
2. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Copy `.env.example` to `.env` and set `GEMINI_API_KEY`.

## Run

```bash
uvicorn app.main:app --reload
```

Health:

```bash
curl http://127.0.0.1:8000/health
```

## SMS API example

```bash
curl -X POST http://127.0.0.1:8000/sms/analyze \
  -H "Content-Type: application/json" \
  -d "{\"sender_id\":\"VM-HDFCBK\",\"message\":\"Your account blocked. Click http://bit.ly/x to verify.\"}"
```

Expected response shape:

```json
{
  "is_scam": true,
  "confidence_score": 0.93,
  "reasoning": "...",
  "recommendation": "..."
}
```

## Vishing WebSocket protocol

Connect:

- `ws://127.0.0.1:8000/vishing/ws`

Client -> server messages:

- `{"type":"audio","data_b64":"...","mime_type":"audio/pcm"}`
- `{"type":"text","text":"Analyze this phrase..."}` (optional helper)
- `{"type":"disconnect"}`

Server -> client events:

- `{"event":"status","message":"Connecting to Gemini..."}`
- `{"event":"status","message":"Connected to Gemini"}`
- `{"event":"transcript","text":"...","is_user":true}`
- `{"event":"transcript","text":"...","is_user":false}`
- `{"event":"fraud_analysis","analysis":{"is_scam":true,"confidence_score":0.8,"reasoning":"...","recommendation":"..."}}`
- `{"event":"error","message":"..."}`

## Demo WebSocket client

```bash
python scripts/ws_client_demo.py
```

With a raw PCM file:

```bash
python scripts/ws_client_demo.py --pcm-file path/to/audio.pcm
```
