"""No live model, token files, or deployment required."""
import importlib.util
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path

spec = importlib.util.spec_from_file_location("gateway", Path(__file__).with_name("gateway.py"))
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)


class TranscriptIngestTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.old_dir, self.old_token = gateway.TRANSCRIPT_DIR, gateway.TOKEN
        gateway.TRANSCRIPT_DIR = Path(self.temp.name)
        gateway.TOKEN = "fixture-desktop-credential"
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), gateway.Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = f"http://127.0.0.1:{self.server.server_port}/v1/transcripts"

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        gateway.TRANSCRIPT_DIR, gateway.TOKEN = self.old_dir, self.old_token
        self.temp.cleanup()

    def record(self, event="event-1"):
        return dict(schemaVersion=1, sessionId="session-1", requestId="request-1",
                    eventId=event, correlationId="request-1", provider="firebase",
                    response="Bearer response-secret",
                    contextCategories=[{"content": "password='two words' fixture-desktop-credential",
                                        "nested": {"accessToken": "never-on-disk"}}])

    def post(self, record, token="fixture-desktop-credential"):
        request = urllib.request.Request(self.url, json.dumps(record).encode(),
                                         {"Content-Type": "application/json",
                                          "Authorization": f"Bearer {token}"}, method="POST")
        with urllib.request.urlopen(request, timeout=2) as response:
            return json.loads(response.read())

    def test_cloud_transcript_ingest_is_authenticated_and_independent_of_ollama(self):
        with self.assertRaises(urllib.error.HTTPError) as caught:
            self.post(self.record(), token="wrong")
        self.assertEqual(401, caught.exception.code)
        self.assertFalse(list(gateway.TRANSCRIPT_DIR.rglob("*.jsonl")))
        self.assertTrue(self.post(self.record())["ok"])
        raw = (gateway.TRANSCRIPT_DIR / "app/session-1.jsonl").read_text()
        for secret in ("response-secret", "two words", "fixture-desktop-credential", "never-on-disk"):
            self.assertNotIn(secret, raw)
        self.assertIn("firebase", raw)

    def test_retry_after_lost_ack_does_not_duplicate_entry(self):
        self.assertTrue(self.post(self.record())["inserted"])
        self.assertFalse(self.post(self.record())["inserted"])
        self.post(self.record("event-2"))
        rows = [json.loads(line) for line in (gateway.TRANSCRIPT_DIR / "app/session-1.jsonl").read_text().splitlines() if line]
        self.assertEqual(["event-1", "event-2"], [row["eventId"] for row in rows])

    def test_path_traversal_and_invalid_schema_are_rejected_without_write(self):
        for changes in ({"sessionId": "../escape"}, {"schemaVersion": 2}, {"eventId": ""}):
            record = self.record()
            record.update(changes)
            with self.assertRaises(urllib.error.HTTPError) as caught:
                self.post(record)
            self.assertEqual(400, caught.exception.code)
        self.assertFalse(list(gateway.TRANSCRIPT_DIR.rglob("*.jsonl")))

    def test_storage_error_does_not_ack(self):
        gateway.TRANSCRIPT_DIR = Path(self.temp.name) / "not-a-directory"
        gateway.TRANSCRIPT_DIR.write_text("fixture")
        with self.assertRaises(urllib.error.HTTPError) as caught:
            self.post(self.record())
        self.assertEqual(503, caught.exception.code)

    def test_gateway_own_transcripts_use_recursive_redaction(self):
        record = self.record()
        gateway.write_transcript(record)
        raw = (gateway.TRANSCRIPT_DIR / "session-1.jsonl").read_text()
        self.assertNotIn("never-on-disk", raw)
        self.assertNotIn("response-secret", raw)


if __name__ == "__main__":
    unittest.main()
