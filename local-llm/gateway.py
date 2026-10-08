import json
import hmac
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


SECRET_KEY = re.compile(r"(?i).*(authorization|credential|api[_-]?key|password|token|secret|private[_-]?key).*")
SAFE_ID = re.compile(r"^[a-zA-Z0-9-]{1,128}$")
MAX_BODY = 16 * 1024 * 1024


def redact(value):
    if isinstance(value, dict):
        return {
            key: "[redacted]" if re.search(
                r"(?i)authorization|credential|password|secret|token|api[_-]?key|private[_-]?key", key
            ) else redact(item)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [redact(item) for item in value]
    if not isinstance(value, str):
        return value
    if TOKEN:
        value = value.replace(TOKEN, "[redacted]")
    value = re.sub(r'(?i)Bearer\s+[^\s"<>]+', "Bearer [redacted]", value)
    value = re.sub(
        r"""(?i)(["']?(?:token|api[_-]?key|password|secret|authorization|credential)["']?\s*[:=]\s*)(?:"[^"]*"|'[^']*'|[^\s,;}&]+)""",
        r"\1[redacted]", value,
    )
    value = re.sub(r"(?i)https?://[^/\s:@]+:[^/\s@]+@", "https://[redacted]@", value)
    value = re.sub(r"-----BEGIN [^-]*PRIVATE KEY-----[\s\S]*?-----END [^-]*PRIVATE KEY-----", "[redacted]", value)
    return re.sub(r"(?:AIza[0-9A-Za-z_-]{20,}|sk-(?:proj-)?[0-9A-Za-z_-]{20,}|eyJ[0-9A-Za-z_-]+\.[0-9A-Za-z_-]+\.[0-9A-Za-z_-]+)", "[redacted]", value)


def valid_id(value):
    return isinstance(value, str) and re.fullmatch(r"[a-zA-Z0-9-]{1,80}", value) is not None


def ingest_transcript(record):
    """Session journals with durable event-ID deduplication; callers ACK only after fsync."""
    if not isinstance(record, dict) or record.get("schemaVersion") != 1:
        raise ValueError("invalid transcript schema")
    for field in ("sessionId", "requestId", "eventId", "correlationId"):
        if not valid_id(record.get(field)):
            raise ValueError("invalid transcript ID")
    safe = redact(record)
    target = TRANSCRIPT_DIR / "app" / (record["sessionId"] + ".jsonl")
    with TRANSCRIPT_LOCK:
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            with target.open(encoding="utf-8") as handle:
                for line in handle:
                    try:
                        if json.loads(line).get("eventId") == record["eventId"]:
                            return False
                    except (ValueError, AttributeError):
                        continue
        with target.open("a", encoding="utf-8") as handle:
            handle.write("\n" + json.dumps(safe, ensure_ascii=False) + "\n")
            handle.flush()
            os.fsync(handle.fileno())
    return True


def write_transcript(record):
    try:
        TRANSCRIPT_DIR.mkdir(parents=True, exist_ok=True)
        session = record.get("sessionId", record["requestId"])
        if not valid_id(session):
            session = record["requestId"]
        target = TRANSCRIPT_DIR / f"{session}.jsonl"
        line = json.dumps(redact(record), ensure_ascii=False)
        with TRANSCRIPT_LOCK:
            with target.open("a", encoding="utf-8") as handle:
                handle.write("\n" + line + "\n")
                handle.flush()
                os.fsync(handle.fileno())
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
        if self.path not in ("/v1/chat", "/v1/transcripts"):
            return self._json(404, {"error": "not found"})
        if not TOKEN or not hmac.compare_digest(self.headers.get("Authorization", ""), f"Bearer {TOKEN}") :
            return self._json(401, {"error": "unauthorized"})

        if self.path == "/v1/transcripts":
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if length <= 0 or length > MAX_BODY:
                    return self._json(413, {"error": "invalid transcript size"})
                record = json.loads(self.rfile.read(length))
                inserted = ingest_transcript(record)
                return self._json(200, {"ok": True, "eventId": record["eventId"], "inserted": inserted})
            except (ValueError, KeyError, TypeError):
                return self._json(400, {"error": "invalid transcript"})
            except OSError:
                return self._json(503, {"error": "transcript storage unavailable"})

        request_id = str(uuid.uuid4())
        started = time.perf_counter()
        received_at = utc_now()
        session_id = "gateway-" + request_id
        correlation_id = request_id
        prompt = ""
        data = {}
        profile = "gm"
        model = PROFILES["gm"]

        try:
            length = int(self.headers.get("Content-Length", "0"))
            if length <= 0 or length > MAX_BODY:
                return self._json(413, {"error": "request body size"})
            data = json.loads(self.rfile.read(length) or b"{}")
            client_request_id = str(data.get("requestId", ""))
            if SAFE_ID.fullmatch(client_request_id):
                request_id = client_request_id
            correlation_id = str(data.get("correlationId", ""))
            if not SAFE_ID.fullmatch(correlation_id):
                correlation_id = request_id
            session_id = str(data.get("sessionId", ""))
            if not SAFE_ID.fullmatch(session_id):
                session_id = "gateway-" + request_id
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
                    "sessionId": session_id,
                    "correlationId": correlation_id,
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
                    "sessionId": session_id,
                    "correlationId": correlation_id,
                },
            )
        except Exception as exc:
            duration_ms = int((time.perf_counter() - started) * 1000)
            write_transcript(
                {
                    "schemaVersion": 1,
                    "requestId": request_id,
                    "sessionId": session_id,
                    "correlationId": correlation_id,
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
            print("gateway error:", type(exc).__name__)
            return self._json(
                502,
                {
                    "error": type(exc).__name__,
                    "message": redact(str(exc))[:300],
                    "requestId": request_id,
                    "sessionId": session_id,
                    "correlationId": correlation_id,
                },
            )


if __name__ == "__main__":
    if not TOKEN:
        raise SystemExit("LOREWISE_LLM_TOKEN (or legacy WAYFARER_LLM_TOKEN) is required")
    print(f"LoreWise local LLM gateway listening on {HOST}:{PORT}")
    print("Profiles:", PROFILES)
    print("Transcript mirror:", TRANSCRIPT_DIR)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
