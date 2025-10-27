@echo off
echo ========================================
echo DETENIENDO MODO DESARROLLO
echo ========================================
echo.

docker-compose -f dev/docker-compose.dev.yml down

echo.
echo ========================================
echo SERVICIOS DETENIDOS
echo ========================================
echo.
pause