@echo off
REM 실행: healthcheck.bat > m5.log

setlocal enabledelayedexpansion
for /L %%i in (1,1,150) do (
    <nul set /p "=!time! "
    curl.exe -o NUL -s --max-time 10 -w "%%{http_code} %%{time_total}\n" https://booktalker.co.kr/api/actuator/health
    timeout /t 2 /nobreak >nul
)
