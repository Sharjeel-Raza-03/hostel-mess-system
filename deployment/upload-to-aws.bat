@echo off
setlocal

if "%~2"=="" (
  echo Usage: deployment\upload-to-aws.bat "C:\path\to\hostel-key.pem" EC2_PUBLIC_IP
  exit /b 1
)

set "KEY=%~1"
set "HOST=%~2"
set "SSHOPT=-o StrictHostKeyChecking=accept-new"

pushd "%~dp0.."
set "ROOT=%CD%"
popd

echo.
echo === 1/3 Building the backend JAR ===
pushd "%ROOT%\backend"
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
  popd
  echo Build failed. Fix the errors above and run this again.
  exit /b 1
)
popd

set "JAR="
for %%f in ("%ROOT%\backend\target\mess-*.jar") do set "JAR=%%~ff"
if not defined JAR (
  echo Could not find the JAR in backend\target.
  exit /b 1
)
echo Using %JAR%

echo.
echo === 2/3 Preparing the server ===
ssh -i "%KEY%" %SSHOPT% ubuntu@%HOST% "rm -rf ~/upload && mkdir -p ~/upload"
if errorlevel 1 (
  echo Could not connect to the server. Check the IP address, the key path and the SSH rule in the security group.
  exit /b 1
)

echo.
echo === 3/3 Uploading ===
scp -i "%KEY%" %SSHOPT% "%JAR%" ubuntu@%HOST%:~/upload/hostel-mess.jar
if errorlevel 1 goto :fail
scp -i "%KEY%" %SSHOPT% -r "%ROOT%\frontend" ubuntu@%HOST%:~/upload/
if errorlevel 1 goto :fail
scp -i "%KEY%" %SSHOPT% -r "%ROOT%\deployment" ubuntu@%HOST%:~/upload/
if errorlevel 1 goto :fail
scp -i "%KEY%" %SSHOPT% -r "%ROOT%\database" ubuntu@%HOST%:~/upload/
if errorlevel 1 goto :fail

ssh -i "%KEY%" %SSHOPT% ubuntu@%HOST% "find ~/upload -type f \( -name '*.sh' -o -name '*.service' -o -name '*.conf' -o -name '*.sql' \) -exec sed -i 's/\r$//' {} +"

echo.
echo Upload finished. Now connect to the server and run the next script:
echo   ssh -i "%KEY%" ubuntu@%HOST%
echo   bash ~/upload/deployment/deploy-app.sh
exit /b 0

:fail
echo Upload failed. Check the messages above.
exit /b 1
