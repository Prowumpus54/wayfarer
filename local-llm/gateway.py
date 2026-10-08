import json
import os
import re
import threading
import time
import urllib.request
import uuid
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HOST = os.environ.get("LOREWISE_LLM_HOST", os.environ.get("WAYFARER_LLM_HOST", "0.0.0.0"))
PORT = int(os.environ.get("LOREWISE_LLM_PORT", os.environ.get("WAYFARER_LLM_PORT", "11435")))
TOKEN = os.environ.get("LOREWISE_LLM_TOKEN", os.environ.get("WAYFARER_LLM_TOKEN", ""))
OLLAMA = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434")
TRANSCRIPT_DIR = Path(
    os.environ.get(
        "LOREWISE_TRANSCRIPT_DIR",
        str(Path(__file__).resolve().parent / "transcripts"),
    )
)
TRANSCRIPT_LOCK = threading.Lock()

PROFILES = {
    "gm": "gemma4:e2b-it-qat",
    "fast": "wayfarer-gm-fast:latest",
    "coder": "local-coder:latest",
}


def gm_messages(prompt, system_prompt=""):
    """Separate optional hard authority from user/scene data; preserve legacy callers."""
    messages = [{"role": "system", "content": str(system_prompt)}] if system_prompt else []
    return messages + [{"role": "user", "content": prompt}]


def utc_now():
    return datetime.now(timezone.utc).isoformat()


def redact(value):
    if not isinstance(value, str):
        return value
    value = re.sub(r"(?i)Bearer\s+\S+", "Bearer [redacted]", value)
    value = re.sub(
        r"(?i)(token|api[_-]?key|password|secret)\s*[:=]\s*[^\s,;]+",
        r"\1=[redacted]",
        value,
    )
    return value


def write_transcript(record):
    try:
        TRANSCRIPT_DIR.mkdir(parents=True, exist_ok=True)
        day = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        target = TRANSCRIPT_DIR / f"{day}.jsonl"
        line = json.dumps(record, ensure_ascii=False)
        with TRANSCRIPT_LOCK:
            with target.open("a", encoding="utf-8") as handle:
                handle.write(line + "\n")
    except Exception as exc:
        print("transcript write error:", type(exc).__name__)


class Handler(BaseHTTPRequestHandler):
    server_version = "LoreWiseLocalLLM/0.3"

    def _json(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        print(f"[{self.address_string()}] {fmt % args}")

    def do_GET(self):
        if self.path == "/health":
            return self._json(
                200,
                {
                    "ok": True,
                    "profiles": PROFILES,
                    "transcriptMirror": True,
                },
            )
        return self._json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/v1/chat":
            return self._json(404, {"error": "not found"})
        if not TOKEN or self.headers.get("Authorization") != f"Bearer {TOKEN}":
            return self._json(401, {"error": "unauthorized"})

        request_id = str(uuid.uuid4())
        started = time.perf_counter()
        received_at = utc_now()
        prompt = ""
        data = {}
        profile = "gm"
        model = PROFILES["gm"]

        try:
            length = int(self.headers.get("Content-Length", "0"))
            data = json.loads(self.rfile.read(length) or b"{}")
            prompt = str(data.get("prompt", "")).strip()
            profile = str(data.get("profile", "gm")).lower()
            model = PROFILES.get(profile, PROFILES["gm"])
            if not prompt:
                return self._json(400, {"error": "prompt required"})

            payload = {
                "model": model,
                "stream": False,
                "think": False,
                "messages": gm_messages(prompt, data.get("systemPrompt", "")),
                "options": {"temperature": 0.55, "num_ctx": 4096},
            }
            req = urllib.request.Request(
                OLLAMA + "/api/chat",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=180) as resp:
                result = json.loads(resp.read().decode("utf-8"))
            text = result.get("message", {}).get("content", "")
            duration_ms = int((time.perf_counter() - started) * 1000)
            write_transcript(
                {
                    "schemaVersion": 1,
                    "requestId": request_id,
                    "receivedAt": received_at,
                    "completedAt": utc_now(),
                    "client": self.client_address[0],
                    "profile": profile,
                    "model": model,
                    "durationMs": duration_ms,
                    "status": "ok",
                    "prompt": redact(prompt),
                    "systemPrompt": redact(str(data.get("systemPrompt", ""))),
                    "response": redact(text),
                }
            )
            return self._json(
                200,
                {
                    "text": text,
                    "model": model,
                    "profile": profile,
                    "requestId": request_id,
                },
            )
        except Exception as exc:
            duration_ms = int((time.perf_counter() - started) * 1000)
            write_transcript(
                {
                    "schemaVersion": 1,
                    "requestId": request_id,
                    "receivedAt": received_at,
                    "completedAt": utc_now(),
                    "client": self.client_address[0],
                    "profile": profile,
                    "model": model,
                    "durationMs": duration_ms,
                    "status": "error",
                    "prompt": redact(prompt),
                    "systemPrompt": redact(str(data.get("systemPrompt", ""))),
                    "errorType": type(exc).__name__,
                    "error": redact(str(exc)[:500]),
                }
            )
            print("gateway error:", repr(exc))
            return self._json(
                502,
                {
                    "error": type(exc).__name__,
                    "message": str(exc)[:300],
                    "requestId": request_id,
                },
            )


if __name__ == "__main__":
    if not TOKEN:
        raise SystemExit("LOREWISE_LLM_TOKEN (or legacy WAYFARER_LLM_TOKEN) is required")
    print(f"LoreWise local LLM gateway listening on {HOST}:{PORT}")
    print("Profiles:", PROFILES)
    print("Transcript mirror:", TRANSCRIPT_DIR)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
