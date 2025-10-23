@echo off
echo ========================================
echo INICIANDO MODO DESARROLLO
echo ========================================
echo.

echo 1. Iniciando servicios Docker (MySQL + phpMyAdmin)...
docker-compose -f docker-compose.dev.yml up -d

echo.
echo Esperando a que MySQL este listo...
timeout /t 10 /nobreak > nul

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
echo   mvn clean compile
echo   mvn exec:java
echo.
echo Para detener los servicios:
echo   docker-compose -f docker-compose.dev.yml down
echo.
pause