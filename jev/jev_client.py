import json
import os
import random
import time
import urllib.error
import urllib.request

GATEWAY_URL = "https://ai-gateway.vercel.sh/v1/evaluate"
MODEL = "typesafe-ai/jev"


class JevError(RuntimeError):
    def __init__(self, message, status=None, error_type=None):
        super().__init__(message)
        self.status = status
        self.error_type = error_type


def _api_key():
    key = os.getenv("AI_GATEWAY_API_KEY") or os.getenv("JEV_API_KEY")
    if not key:
        raise JevError("AI Gateway key is not configured.")
    return key


def evaluate(state, questions, timeout=12, retries=4):
    payload = {
        "model": MODEL,
        "state": state,
        "questions": questions,
        "providerOptions": {
            "gateway": {"only": ["typesafe-ai"]}
        },
    }
    body = json.dumps(payload).encode("utf-8")
    start = time.perf_counter()

    for attempt in range(retries + 1):
        request = urllib.request.Request(
            GATEWAY_URL,
            data=body,
            method="POST",
            headers={
                "Authorization": "Bearer " + _api_key(),
                "Content-Type": "application/json",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                raw = response.read().decode("utf-8")
                elapsed_ms = round((time.perf_counter() - start) * 1000)
                data = json.loads(raw)
                data["_elapsedMs"] = elapsed_ms
                data["_attempts"] = attempt + 1
                return data
        except urllib.error.HTTPError as error:
            raw = error.read().decode("utf-8", errors="replace")
            try:
                detail = json.loads(raw).get("error", {})
                message = detail.get("message") or raw
                error_type = detail.get("type")
            except Exception:
                message = raw or str(error)
                error_type = None

            if error.code in (429, 500, 502, 503, 504) and attempt < retries:
                time.sleep((0.55 * (2 ** attempt)) + random.uniform(0.05, 0.20))
                continue
            raise JevError(message, error.code, error_type) from error
        except urllib.error.URLError as error:
            if attempt < retries:
                time.sleep((0.55 * (2 ** attempt)) + random.uniform(0.05, 0.20))
                continue
            raise JevError("Jev network request failed: " + str(error)) from error

    raise JevError("Jev request failed after retries.")
