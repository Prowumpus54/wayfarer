import json, urllib.request
from pathlib import Path

token=Path(r'C:\Wayfarer\local-llm\gateway.token').read_text().strip()
body=json.dumps({'profile':'fast','prompt':'Reply with exactly: OK'}).encode()
req=urllib.request.Request('http://127.0.0.1:11435/v1/chat',data=body,headers={'Content-Type':'application/json','Authorization':'Bearer '+token},method='POST')
with urllib.request.urlopen(req,timeout=60) as r:
    print(r.read().decode())
