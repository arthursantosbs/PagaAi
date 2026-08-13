@echo off
REM Sobe o Paga ai em http://localhost:8080
REM Usa o JAVA_HOME se estiver configurado; senao, o JDK 17 que ja esta na maquina.

setlocal
if "%JAVA_HOME%"=="" set "JAVA_HOME=%USERPROFILE%\.jdks\ms-17.0.20"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Nao encontrei um JDK em "%JAVA_HOME%".
  echo Instale o JDK 17+ ^(winget install Microsoft.OpenJDK.17^) ou ajuste JAVA_HOME.
  exit /b 1
)

cd /d "%~dp0"

if not exist "target\paga-ai-0.1.0.jar" (
  echo Jar nao encontrado. Rode o build antes: mvn -DskipTests package
  exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -jar "target\paga-ai-0.1.0.jar" %*
endlocal
