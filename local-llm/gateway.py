import json
import os
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = os.environ.get("WAYFARER_LLM_HOST", "0.0.0.0")
PORT = int(os.environ.get("WAYFARER_LLM_PORT", "11435"))
TOKEN = os.environ.get("WAYFARER_LLM_TOKEN", "")
OLLAMA = os.environ.get("OLLAMA_URL", "http://127.0.0.1:11434")

PROFILES = {
    "gm": "qwen3.5:9b",
    "fast": "wayfarer-gm-fast:latest",
    "coder": "local-coder:latest",
}

class Handler(BaseHTTPRequestHandler):
    server_version = "WayfarerLocalLLM/0.1"

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
            return self._json(200, {"ok": True, "profiles": PROFILES})
        return self._json(404, {"error": "not found"})

    def do_POST(self):
        if self.path != "/v1/chat":
            return self._json(404, {"error": "not found"})
        if not TOKEN or self.headers.get("Authorization") != f"Bearer {TOKEN}":
            return self._json(401, {"error": "unauthorized"})
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
                "messages": [{"role": "user", "content": prompt}],
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
            return self._json(200, {"text": text, "model": model, "profile": profile})
        except Exception as exc:
            print("gateway error:", repr(exc))
            return self._json(502, {"error": type(exc).__name__, "message": str(exc)[:300]})

if __name__ == "__main__":
    if not TOKEN:
        raise SystemExit("WAYFARER_LLM_TOKEN is required")
    print(f"Wayfarer local LLM gateway listening on {HOST}:{PORT}")
    print("Profiles:", PROFILES)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
