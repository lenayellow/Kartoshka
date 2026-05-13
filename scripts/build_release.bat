@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0.."

echo ===========================================================
echo  Kartoshka - Release Bundle Builder
echo ===========================================================
echo.

REM -- Java ---------------------------------------------------
if "%JAVA_HOME%"=="" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo [ERROR] Java not found at: %JAVA_HOME%
    echo Set JAVA_HOME to your JDK or Android Studio JBR path.
    exit /b 1
)

REM -- keystore.properties ------------------------------------
if not exist "keystore.properties" (
    echo [ERROR] keystore.properties not found.
    echo Copy keystore.properties.example to keystore.properties and fill in your values.
    exit /b 1
)

REM -- Git checks (warnings only) -----------------------------
set WARN=0

git status --porcelain 2>nul | findstr /r "." >nul && set WARN=1

set GIT_TAG=
for /f "delims=" %%t in ('git describe --exact-match --match "v*" HEAD 2^>nul') do set GIT_TAG=%%t
if "!GIT_TAG!"=="" set WARN=1

if "!WARN!"=="1" (
    echo [WARNING] One or more checks failed:
    if "!GIT_TAG!"=="" (
        echo   - HEAD has no git tag matching v*
        echo     versionName will fall back to VERSION file or "0.1.0-dev"
    )
    git status --porcelain 2>nul | findstr /r "." >nul && (
        echo   - Working tree has uncommitted changes
    )
    echo.
    set ANSWER=
    set /p ANSWER=Continue anyway? (y/N):
    if /i not "!ANSWER!"=="y" (
        echo Aborted.
        exit /b 0
    )
    echo.
)

REM -- Version info -------------------------------------------
set VERSION_NAME=
set VERSION_CODE=
for /f "delims=" %%v in ('git describe --tags --abbrev=0 --match "v*" 2^>nul') do set RAW_TAG=%%v
for /f "delims=" %%c in ('git rev-list --count HEAD 2^>nul') do set VERSION_CODE=%%c
if defined RAW_TAG (
    set VERSION_NAME=!RAW_TAG:~1!
) else (
    for /f "usebackq delims=" %%f in ("VERSION") do set VERSION_NAME=%%f
)
if not defined VERSION_NAME set VERSION_NAME=0.1.0-dev
if not defined VERSION_CODE set VERSION_CODE=1

echo  versionName : !VERSION_NAME!
echo  versionCode : !VERSION_CODE!
echo.

REM -- Build --------------------------------------------------
echo Building (clean + bundleRelease --no-daemon)...
echo.
call gradlew.bat clean :app:bundleRelease --no-daemon
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] BUILD FAILED - see Gradle output above.
    exit /b 1
)

REM -- Output info --------------------------------------------
set "AAB=app\build\outputs\bundle\release\app-release.aab"
if not exist "!AAB!" (
    echo [ERROR] AAB not found: !AAB!
    exit /b 1
)

for %%A in ("!AAB!") do set FILE_BYTES=%%~zA
set /a FILE_MB=FILE_BYTES / 1048576
set /a FILE_KB=FILE_BYTES / 1024

echo.
echo ===========================================================
echo  BUILD SUCCESSFUL
echo ===========================================================
echo  AAB     : !AAB!
echo  Size    : !FILE_MB! MB  (!FILE_KB! KB)
echo  Version : !VERSION_NAME! (code !VERSION_CODE!)
echo.
echo  SHA-256 :
certutil -hashfile "!AAB!" SHA256 2>nul | findstr /v /i "certutil hash"
echo ===========================================================
echo.

explorer /select,"!AAB!"
endlocal
