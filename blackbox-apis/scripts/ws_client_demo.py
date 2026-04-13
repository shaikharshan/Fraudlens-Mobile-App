import argparse
import asyncio
import base64
import json
import os

import websockets


async def main() -> None:
    parser = argparse.ArgumentParser(description="FraudLens vishing websocket demo client")
    parser.add_argument("--url", default="ws://127.0.0.1:8000/vishing/ws", help="WebSocket URL")
    parser.add_argument("--pcm-file", default="", help="Path to pcm16 mono 16kHz raw audio")
    args = parser.parse_args()

    async with websockets.connect(args.url) as ws:
        print("Connected:", args.url)

        async def reader() -> None:
            while True:
                message = await ws.recv()
                print("<<", message)

        read_task = asyncio.create_task(reader())

        if args.pcm_file and os.path.exists(args.pcm_file):
            with open(args.pcm_file, "rb") as f:
                b64 = base64.b64encode(f.read()).decode("utf-8")
            await ws.send(
                json.dumps({"type": "audio", "data_b64": b64, "mime_type": "audio/pcm;rate=16000"})
            )
            print(">> sent audio chunk from", args.pcm_file)
        else:
            # Optional text trigger for fraud analysis, mirrors app-side helper behavior.
            await ws.send(json.dumps({"type": "text", "text": "Hello, please share OTP to unblock account"}))
            print(">> sent text snippet")

        await asyncio.sleep(10)
        await ws.send(json.dumps({"type": "disconnect"}))
        await asyncio.sleep(1)
        read_task.cancel()


if __name__ == "__main__":
    asyncio.run(main())
