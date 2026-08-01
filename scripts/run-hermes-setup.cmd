@echo off
setlocal
set "HERMES_HOME=%LOCALAPPDATA%\hermes"
set "HERMES_BIN=%HERMES_HOME%\hermes-agent\venv\Scripts"
set "PATH=%HERMES_BIN%;%HERMES_HOME%\bin;%PATH%"

if not exist "%HERMES_BIN%\hermes.exe" (
  echo Hermes not found. Install first:
  echo   iex (irm https://hermes-agent.nousresearch.com/install.ps1)
  pause
  exit /b 1
)

echo Starting Hermes setup wizard...
echo Home: %HERMES_HOME%
echo.
hermes setup
echo.
pause
