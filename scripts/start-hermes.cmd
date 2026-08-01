@echo off
setlocal
set "HERMES_HOME=%LOCALAPPDATA%\hermes"
set "HERMES_BIN=%HERMES_HOME%\hermes-agent\venv\Scripts"
set "PATH=%HERMES_BIN%;%HERMES_HOME%\bin;%PATH%"

if not exist "%HERMES_BIN%\hermes.exe" (
  echo Hermes not installed. Run in PowerShell:
  echo   iex (irm https://hermes-agent.nousresearch.com/install.ps1)
  exit /b 1
)

if "%~1"=="" (
  hermes
) else (
  hermes %*
)
