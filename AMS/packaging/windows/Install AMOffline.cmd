@echo off
rem Installs AMOffline for this Windows user (no administrator needed). Java is included.
title Installing AMOffline
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup\install.ps1"
if errorlevel 1 (
    echo.
    echo Installation did not finish. See the message above.
    pause
    exit /b 1
)
rem leave the message on screen for a few seconds
ping -n 5 127.0.0.1 >nul
exit /b 0
