$ErrorActionPreference = 'Stop'
$tokenPath = 'C:\Wayfarer\local-llm\gateway.token'
if (-not (Test-Path $tokenPath)) { throw 'Missing gateway.token' }
$env:LOREWISE_LLM_TOKEN = (Get-Content $tokenPath -Raw).Trim()
$env:LOREWISE_LLM_HOST = '0.0.0.0'
$env:LOREWISE_LLM_PORT = '11435'
python 'C:\Wayfarer\local-llm\gateway.py'
