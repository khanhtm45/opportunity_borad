# Load Hermes Agent into the current PowerShell session (Windows).
# Usage (dot-source required so PATH sticks in THIS terminal):
#   . .\scripts\hermes-env.ps1

$HermesHome = Join-Path $env:LOCALAPPDATA "hermes"
$HermesBin = Join-Path $HermesHome "hermes-agent\venv\Scripts"
$UvBin = Join-Path $HermesHome "bin"
$HermesExe = Join-Path $HermesBin "hermes.exe"

if (-not (Test-Path $HermesExe)) {
    Write-Error "Hermes not found at $HermesExe`nInstall: iex (irm https://hermes-agent.nousresearch.com/install.ps1)"
    return
}

# Refresh from User/Machine registry (fixes stale Cursor terminals)
$machine = [Environment]::GetEnvironmentVariable("Path", "Machine")
$user = [Environment]::GetEnvironmentVariable("Path", "User")
$env:Path = "$HermesBin;$UvBin;$machine;$user"

$env:HERMES_HOME = $HermesHome
$bash = [Environment]::GetEnvironmentVariable("HERMES_GIT_BASH_PATH", "User")
if ($bash) { $env:HERMES_GIT_BASH_PATH = $bash }

$ver = & $HermesExe version 2>$null | Select-Object -First 1
Write-Host "OK  $ver"
Write-Host "    $HermesExe"
Write-Host "Next: hermes setup"
