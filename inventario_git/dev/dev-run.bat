@echo off
echo ========================================
echo COMPILANDO Y EJECUTANDO APLICACION
echo ========================================
echo.

REM Guardar el directorio actual y cambiar al directorio del script
pushd %~dp0

REM Si estamos en dev/, subir al directorio raíz del proyecto
if exist "pom.xml" (
    REM Ya estamos en la raíz
    echo Ejecutando desde raiz del proyecto...
) else (
    REM Estamos en dev/, subir un nivel
    cd ..
    if not exist "pom.xml" (
        echo ERROR: No se encuentra pom.xml
        popd
        pause
        exit /b 1
    )
)

echo 1. Compilando proyecto...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ========================================
    echo ERROR EN COMPILACION
    echo ========================================
    popd
    pause
    exit /b 1
)

echo.
echo 2. Ejecutando aplicacion...
echo.
call mvn exec:java

REM Restaurar el directorio original
popd
pause