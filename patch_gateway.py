from pathlib import Path
p=Path(r'C:\Wayfarer\local-llm\gateway.py')
s=p.read_text(encoding='utf-8')
s=s.replace('            payload = {\n                "model": model,\n                "stream": False,\n                "messages": [{"role": "user", "content": prompt}],\n                "options": {"temperature": 0.55, "num_ctx": 8192},\n            }','            payload = {\n                "model": model,\n                "stream": False,\n                "think": False,\n                "messages": [{"role": "user", "content": prompt}],\n                "options": {"temperature": 0.55, "num_ctx": 4096},\n            }')
p.write_text(s,encoding='utf-8')
print('gateway patched')
