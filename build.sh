#!/bin/bash
set -e

echo "=========================================="
echo "  WGF Firework Autofarm - Build Tool"
echo "=========================================="
echo ""

# Verifica Java 17
if ! java -version 2>&1 | grep -q "17"; then
    echo "[ERRORE] Java 17 non trovato!"
    echo "Installa Java 17 da: https://adoptium.net/"
    exit 1
fi
echo "[OK] Java 17 trovato."

# Scarica Gradle se non esiste
if [ ! -f "gradle-8.5/bin/gradle" ]; then
    echo ""
    echo "[INFO] Download Gradle 8.5..."
    curl -L "https://services.gradle.org/distributions/gradle-8.5-bin.zip" -o gradle.zip
    echo "[OK] Estrazione..."
    unzip -q gradle.zip
    rm gradle.zip
    echo "[OK] Gradle pronto."
fi

echo ""
echo "[INFO] Avvio build..."
echo ""
./gradle-8.5/bin/gradle build

echo ""
echo "=========================================="
echo "  BUILD COMPLETATA CON SUCCESSO!"
echo "=========================================="
echo ""
echo "Il tuo JAR si trova in:"
echo "  build/libs/wgf-firework-autofarm-1.0.0.jar"
echo ""
echo "Copia quel file in:"
echo "  ~/.minecraft/mods/"
