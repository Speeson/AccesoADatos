@echo off
echo ========================================
echo COMPILANDO Y EJECUTANDO APLICACION
echo ========================================
echo.

echo 1. Compilando proyecto...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR EN COMPILACION
    echo ========================================
    pause
    exit /b 1
)

echo.
echo 2. Ejecutando aplicacion...
echo.
call mvn exec:java

pause