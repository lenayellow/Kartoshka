@echo off
setlocal

REM === Kartoshka dev launcher ===
REM   1. Kills old backend on :8080
REM   2. Builds Go backend
REM   3. Sets adb reverse tcp:8080 tcp:8080 (phone sees localhost)
REM   4. Starts backend in this window. Close window or Ctrl+C to stop.

cd /d "%~dp0"

echo === Stopping old process on :8080 ===
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do (
    echo Stopping PID %%a
    taskkill /F /PID %%a >nul 2>nul
)

echo.
echo === Building backend ===
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
    echo WARNING: adb not found. Skipping reverse tunnel.
    echo If testing on phone -- install Android Studio or add adb to PATH.
) else (
    "%ADB%" reverse tcp:8080 tcp:8080
    "%ADB%" reverse --list
)

echo.
echo === Starting backend on :8080 ===
echo Close this window or press Ctrl+C to stop.
echo.
cd backend
api.exe
