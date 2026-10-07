# Run this script as Administrator once.
$ErrorActionPreference = 'Stop'
$rule = 'LoreWise Local LLM Gateway'
$legacyRule = 'Wayfarer Local LLM Gateway'
Get-NetFirewallRule -DisplayName $legacyRule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
Get-NetFirewallRule -DisplayName $rule -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $rule -Direction Inbound -Action Allow -Protocol TCP -LocalPort 11435 -Profile Private
Write-Host 'LoreWise Local LLM firewall rule enabled on TCP 11435 (Private networks).'
