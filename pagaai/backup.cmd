@echo off
REM ===========================================================================
REM  Baixa uma copia do banco de producao para a sua maquina.
REM
REM  Rode isto de tempos em tempos (uma vez por semana ja resolve). O arquivo
REM  gerado e um SQL completo: com ele da para reconstruir o sistema inteiro
REM  do zero, em qualquer servidor.
REM
REM  COMO USAR
REM    1. Instale o cliente do PostgreSQL, que traz o pg_dump:
REM         winget install PostgreSQL.PostgreSQL.16
REM       (ou instale o pgAdmin, que ja vem com ele)
REM    2. Pegue a connection string do Neon (a que comeca com postgresql://)
REM    3. Rode:
REM         backup.cmd "postgresql://usuario:senha@ep-xxx.neon.tech/neondb?sslmode=require"
REM
REM  O arquivo cai em backups\ com a data no nome.
REM  GUARDE UMA COPIA FORA DO SEU COMPUTADOR TAMBEM (Drive, HD externo, email).
REM  Backup que so existe num lugar nao e backup.
REM ===========================================================================

setlocal

if "%~1"=="" (
  echo.
  echo  Falta a connection string do banco.
  echo.
  echo  Uso: backup.cmd "postgresql://usuario:senha@host/base?sslmode=require"
  echo.
  echo  Pegue essa string no painel do Neon, em Connection Details.
  echo  Use a que comeca com postgresql:// ^(nao a JDBC^).
  echo.
  exit /b 1
)

where pg_dump >nul 2>&1
if errorlevel 1 (
  echo.
  echo  Nao encontrei o pg_dump nesta maquina.
  echo  Instale o cliente do PostgreSQL:
  echo.
  echo      winget install PostgreSQL.PostgreSQL.16
  echo.
  echo  Depois abra um terminal NOVO ^(para o PATH atualizar^) e rode de novo.
  echo.
  exit /b 1
)

cd /d "%~dp0"
if not exist "backups" mkdir "backups"

REM Data no formato AAAA-MM-DD, independente do formato regional do Windows.
for /f %%d in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd-HHmm"') do set STAMP=%%d
set ARQUIVO=backups\pagaai-%STAMP%.sql

echo.
echo  Baixando o banco para %ARQUIVO% ...

pg_dump --no-owner --no-privileges --file="%ARQUIVO%" "%~1"

if errorlevel 1 (
  echo.
  echo  FALHOU. Confira a connection string e se a maquina tem internet.
  if exist "%ARQUIVO%" del "%ARQUIVO%"
  exit /b 1
)

for %%A in ("%ARQUIVO%") do set TAMANHO=%%~zA
echo.
echo  Pronto: %ARQUIVO%  ^(%TAMANHO% bytes^)
echo.
echo  Agora copie esse arquivo para fora deste computador.
echo  Se o HD morrer, o backup morre junto com ele.
echo.

endlocal
