@echo off
setlocal

REM === reset-tunnel: только ставит adb reverse tcp:8080 tcp:8080. ===
REM Удобно, когда бэкенд уже запущен, а телефон переподключили —
REM тогда сам бэкенд перезапускать не нужно, достаточно поднять туннель.

set "ADB="
where adb >nul 2>nul && set "ADB=adb"
if not defined ADB (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
if not defined ADB (
    echo adb не найден. Установи Android Studio или добавь adb в PATH.
    pause
    exit /b 1
)

"%ADB%" reverse tcp:8080 tcp:8080
"%ADB%" reverse --list
echo.
echo Готово. Если телефон подключён — связь с :8080 должна работать.
echo.
pause
