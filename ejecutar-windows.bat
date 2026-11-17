@echo off
chcp 65001 >nul
title Calculadora de Integrales ^| Java + SWI-Prolog
echo ========================================================
echo   Calculadora de Integrales  -  Java + SWI-Prolog
echo   Programación Declarativa 02/2026 - UCA
echo ========================================================
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] No se encontró Java instalado o no está en el PATH.
    echo Por favor instala Java JDK/JRE 11 o superior.
    echo.
    pause
    exit /b 1
)

if exist "calc-integrales-prog-declarativa.jar" (
    echo Iniciando aplicación...
    start "" javaw -jar "calc-integrales-prog-declarativa.jar" 2>nul || java -jar "calc-integrales-prog-declarativa.jar"
) else if exist "src\IntegralApp.java" (
    echo [AVISO] Compilando desde el código fuente...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src\*.java
    start "" javaw -cp bin IntegralApp 2>nul || java -cp bin IntegralApp
) else (
    echo [ERROR] No se encontró 'calc-integrales-prog-declarativa.jar' ni 'src\IntegralApp.java'.
    pause
)
