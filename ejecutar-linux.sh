#!/usr/bin/env bash
# Calculadora de Integrales | Java + SWI-Prolog
# Programación Declarativa 02/2026 - UCA

echo "========================================================"
echo "  Calculadora de Integrales  -  Java + SWI-Prolog"
echo "  Programación Declarativa 02/2026 - UCA"
echo "========================================================"
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] No se encontró Java en el sistema."
    echo "Por favor instala OpenJDK 11 o superior."
    exit 1
fi

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
cd "$DIR"

if [ -f "calc-integrales-prog-declarativa.jar" ]; then
    echo "Iniciando aplicación..."
    java -jar "calc-integrales-prog-declarativa.jar"
elif [ -f "src/IntegralApp.java" ]; then
    echo "[AVISO] Compilando desde el código fuente..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin src/*.java
    java -cp bin IntegralApp
else
    echo "[ERROR] No se encontraron los archivos necesarios."
    exit 1
fi
