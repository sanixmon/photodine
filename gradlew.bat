@echo off
rem Minimal Gradle wrapper bootstrap for Photopia (Windows).
setlocal
set "APP_HOME=%~dp0"
for /f "tokens=1,* delims==" %%a in ('findstr /b "distributionUrl" "%APP_HOME%gradle\wrapper\gradle-wrapper.properties"') do set "DIST_URL=%%b"
set "DIST_URL=%DIST_URL:https\://=https://%"
for %%i in ("%DIST_URL%") do set "DIST_FILE=%%~ni"
set "DIST_DIR=%USERPROFILE%\.gradle\wrapper\dists\%DIST_FILE%"
set "GRADLE_BIN=%DIST_DIR%\%DIST_FILE%\bin\gradle.bat"
if not exist "%GRADLE_BIN%" (
  echo Downloading %DIST_URL% ...
  powershell -NoProfile -Command "New-Item -ItemType Directory -Force -Path '%DIST_DIR%' | Out-Null; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%DIST_DIR%\gradle.zip'; Expand-Archive -Path '%DIST_DIR%\gradle.zip' -DestinationPath '%DIST_DIR%' -Force; Remove-Item '%DIST_DIR%\gradle.zip'"
)
call "%GRADLE_BIN%" %*
