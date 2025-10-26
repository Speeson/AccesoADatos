@echo off
echo Probando importacion de movimientos CSV...
echo.
echo Selecciona opcion 8 para importar
echo Presiona Enter para usar archivo por defecto
echo Confirma con S
echo.
pause
mvn exec:java -Dexec.mainClass="com.inventario.Main"
