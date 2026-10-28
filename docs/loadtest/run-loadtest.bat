@echo off
setlocal

if "%~2"=="" (
  echo Usage: docs\loadtest\run-loadtest.bat "C:\tools\apache-jmeter-5.6.3" EC2_PUBLIC_IP [USERS]
  echo Example: docs\loadtest\run-loadtest.bat "C:\tools\apache-jmeter-5.6.3" 13.54.224.116 30
  exit /b 1
)

set "JMETER=%~1"
set "HOST=%~2"
set "USERS=%~3"
if "%USERS%"=="" set "USERS=30"

if not exist "%JMETER%\bin\jmeter.bat" (
  echo Could not find "%JMETER%\bin\jmeter.bat". Check the JMeter folder path.
  exit /b 1
)

set "OUT=%~dp0results\users-%USERS%"
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo.
echo Running the load test: %USERS% users against http://%HOST%
echo This takes about a minute.
echo.

call "%JMETER%\bin\jmeter.bat" -n -t "%~dp0Hostel-Mess-LoadTest.jmx" -l "%OUT%\results.jtl" -e -o "%OUT%\report" -Jhost=%HOST% -Jport=80 -Jusers=%USERS% -Jramp=10 -Jloops=5
if errorlevel 1 (
  echo.
  echo JMeter reported a problem. Read the messages above.
  exit /b 1
)

echo.
echo Done. Open this file in your browser to see the report:
echo   %OUT%\report\index.html
start "" "%OUT%\report\index.html"
exit /b 0
