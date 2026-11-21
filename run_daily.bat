@echo off
setlocal
cd /d "%~dp0"
set "AUTO_GIT_PUSH=True"
python daily_engine.py
endlocal
