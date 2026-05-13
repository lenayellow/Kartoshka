@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================================
echo  Kartoshka — Release Bundle Build
echo ============================================================
echo.

REM ── Java ─────────────────────────────────────────────────────
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java not found at %JAVA_HOME%
    echo Set JAVA_HOME to your JDK or Android Studio JBR location.
    exit /b 1
)

REM ── Keystore ─────────────────────────────────────────────────
if not exist "keystore.properties" (
    echo ERROR: keystore.properties not found.
    echo Copy keystore.properties.example to keystore.properties and fill in your values.
    exit /b 1
)

REM ── Version info ─────────────────────────────────────────────
for /f "delims=" %%v in ('git describe --tags --always 2^>nul') do set VERSION_NAME=%%v
for /f "delims=" %%c in ('git rev-list --count HEAD 2^>nul') do set VERSION_CODE=%%c
if "%VERSION_NAME%"=="" set VERSION_NAME=unknown
if "%VERSION_CODE%"=="" set VERSION_CODE=0

echo  versionName : %VERSION_NAME%
echo  versionCode : %VERSION_CODE%
echo.

REM ── Build ────────────────────────────────────────────────────
echo Building...
call gradlew.bat :app:bundleRelease
if %ERRORLEVEL% neq 0 (
    echo.
    echo BUILD FAILED — see errors above.
    exit /b 1
)

REM ── Output ───────────────────────────────────────────────────
set AAB=app\build\outputs\bundle\release\app-release.aab
echo.
echo ============================================================
echo  BUILD SUCCESSFUL
echo  %AAB%
echo ============================================================
echo.

REM Open the output folder in Explorer
explorer /select,"%~dp0%AAB%"
endlocal
