@echo off
echo ========================================
echo INICIANDO MODO DESARROLLO
echo ========================================
echo.

echo 1. Deteniendo contenedores existentes (si hay)...
docker-compose -f dev/docker-compose.dev.yml down 2>nul

echo.
echo 2. Iniciando servicios Docker (MySQL + phpMyAdmin)...
docker-compose -f dev/docker-compose.dev.yml up -d

echo.
echo 3. Esperando a que MySQL este listo...
ping 127.0.0.1 -n 11 > nul

echo.
echo ========================================
echo SERVICIOS INICIADOS
echo ========================================
echo.
echo MySQL:      localhost:33061
echo phpMyAdmin: http://localhost:9090
echo   Usuario:  inventario_user
echo   Password: inventario_pass
echo.
echo ========================================
echo.
echo Para ejecutar la aplicacion:
echo   cd ..
echo   mvn clean compile
echo   mvn exec:java
echo.
echo Para detener los servicios:
echo   dev\dev-stop.bat
echo.
pause