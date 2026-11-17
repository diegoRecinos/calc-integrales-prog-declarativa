@echo off
:: ============================================================
:: ejecutar.bat  -  Lanzador de la Calculadora de Integrales
:: Requiere: Java 11+ y SWI-Prolog (swipl en el PATH)
:: ============================================================
setlocal

:: Nombre del .jar (ajusta si lo renombras)
set JAR=calc-integrales-prog-declarativa.jar

:: Buscar el .jar primero en la carpeta del script, luego en out/artifacts
set SCRIPT_DIR=%~dp0
set JAR_LOCAL=%SCRIPT_DIR%%JAR%
set JAR_OUT=%SCRIPT_DIR%out\artifacts\calc_integrales_prog_declarativa_jar\%JAR%

if exist "%JAR_LOCAL%" (
    set JAR_PATH=%JAR_LOCAL%
) else if exist "%JAR_OUT%" (
    set JAR_PATH=%JAR_OUT%
) else (
    echo.
    echo  [ERROR] No se encontro el archivo %JAR%
    echo  Coloca el .jar en la misma carpeta que este script, o compila primero.
    echo.
    pause
    exit /b 1
)

:: Verificar que Java esta disponible
where java >nul 2>&1
if errorlevel 1 (
    echo.
    echo  [ERROR] Java no encontrado en el PATH.
    echo  Instala Java 11 o superior y agrega java.exe al PATH.
    echo.
    pause
    exit /b 1
)

:: Verificar que SWI-Prolog esta disponible
where swipl >nul 2>&1
if errorlevel 1 (
    echo.
    echo  [ADVERTENCIA] swipl no encontrado en el PATH.
    echo  La app abre igual, pero deberas indicar la ruta completa a swipl.exe
    echo  en el campo "Ejecutable swipl" dentro de la aplicacion.
    echo.
)

echo.
echo  Iniciando Calculadora de Integrales...
echo  JAR: %JAR_PATH%
echo.

:: Ejecutar el .jar pasando la carpeta del script como directorio de trabajo
:: para que src/prolog/integrales.pl se resuelva correctamente
java -jar "%JAR_PATH%" %*

if errorlevel 1 (
    echo.
    echo  [ERROR] La aplicacion termino con un error (codigo %errorlevel%).
    pause
)

endlocal
