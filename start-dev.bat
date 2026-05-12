@echo off
setlocal

REM === Супер Списки — dev launcher ===
REM   1. Останавливает старый бэкенд на :8080
REM   2. Собирает Go-бэкенд
REM   3. Ставит adb reverse tcp:8080 tcp:8080 (чтобы телефон видел localhost)
REM   4. Запускает бэкенд в этом окне. Закрой окно или Ctrl+C — бэкенд остановится.

cd /d "%~dp0"

echo === Останавливаю старый процесс на :8080 ===
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    echo Stopping PID %%a
    taskkill /F /PID %%a >nul 2>nul
)

echo.
echo === Сборка бэкенда ===
pushd backend
go build -o api.exe ./cmd/api
if errorlevel 1 (
    echo Build failed.
    popd
    pause
    exit /b 1
)
popd

echo.
echo === adb reverse tcp:8080 tcp:8080 ===
set "ADB="
where adb >nul 2>nul && set "ADB=adb"
if not defined ADB (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
)
if not defined ADB (
    echo WARNING: adb не найден. Пропускаю reverse-туннель.
    echo Если тестируешь на телефоне — установи Android Studio или добавь adb в PATH.
) else (
    "%ADB%" reverse tcp:8080 tcp:8080
    "%ADB%" reverse --list
)

echo.
echo === Запускаю бэкенд на :8080 ===
echo Чтобы остановить — закрой это окно или нажми Ctrl+C.
echo.
cd backend
api.exe
