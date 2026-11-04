@echo off
setlocal
set "GAME_ADB=%USERPROFILE%\.codex\android-deckball-tools\platform-tools\adb.exe"
set "GAME_SCREEN=%USERPROFILE%\.codex\android-screen\scrcpy-win64-v5.0.1\scrcpy.exe"
set "ANDROID_AVD_HOME=C:\Users\Public\CodexTowerQA"
set "ANDROID_SDK_ROOT=C:\Users\Public\CodexTowerSdk"
if not exist "%~dp0BlockGarden.apk" goto missing
if not exist "%GAME_ADB%" goto missing
if not exist "%GAME_SCREEN%" goto missing
if not exist "%ANDROID_SDK_ROOT%\emulator\emulator.exe" goto missing
"%GAME_ADB%" -s emulator-5580 get-state >nul 2>&1
if not errorlevel 1 goto waitboot
echo Starting Android emulator. Please wait...
powershell.exe -NoProfile -Command "Start-Process -FilePath 'C:\Users\Public\CodexTowerSdk\emulator\emulator.exe' -ArgumentList '-avd','TowerFront','-port','5580','-no-window','-no-audio','-no-snapshot','-no-boot-anim','-gpu','swiftshader_indirect' -WindowStyle Hidden"
:waitboot
set /a GAME_TRIES=0
:poll
set "GAME_BOOT="
for /f "delims=" %%B in ('"%GAME_ADB%" -s emulator-5580 shell getprop sys.boot_completed 2^>nul') do set "GAME_BOOT=%%B"
if "%GAME_BOOT%"=="1" goto launch
set /a GAME_TRIES+=1
if %GAME_TRIES% GEQ 90 goto failed
timeout /t 2 /nobreak >nul
goto poll
:launch
"%GAME_ADB%" -s emulator-5580 install -r "%~dp0BlockGarden.apk"
if errorlevel 1 goto failed
"%GAME_ADB%" -s emulator-5580 shell am start -n com.blockgarden/.MainActivity
if errorlevel 1 goto failed
start "BlockGarden" "%GAME_SCREEN%" -s emulator-5580 --no-audio --window-title=BlockGarden --window-height=850
exit /b 0
:missing
echo Android emulator or screen viewer files are missing.
goto errorend
:failed
echo Android could not start. Please try again or request help.
:errorend
pause
exit /b 1
