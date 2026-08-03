@echo off
chcp 65001 >nul
title Renovation Helper - Phone Access Server
echo Starting server... Open the printed http://192.168.x.x:8787 address on your phone.
echo (Phone must be on the same Wi-Fi as this computer)
echo.
node "%~dp0server.js"
pause
