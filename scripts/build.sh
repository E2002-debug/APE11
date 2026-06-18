#!/bin/bash
# ============================================
# Script de compilación
# Algoritmo Bully - Sistemas Distribuidos
# ============================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR/.."

cd "$PROJECT_DIR"

echo "╔══════════════════════════════════════════╗"
echo "║  Compilando Algoritmo Bully               ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# Verificar Java
echo "🔍 Verificando Java..."
java -version 2>&1 | head -1
echo ""

# Verificar Node.js
echo "🔍 Verificando Node.js..."
if command -v node &> /dev/null; then
    echo "Node.js: $(node --version)"
    echo "npm: $(npm --version)"
else
    echo "❌ Node.js no encontrado. Instalalo con:"
    echo "   sudo apt install nodejs npm"
    exit 1
fi
echo ""

# 1. Compilar Frontend React
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 Paso 1: Compilando Frontend React..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
cd frontend
npm install
npm run build
cd ..
echo ""
echo "✅ Frontend compilado → src/main/resources/static/"
echo ""

# 2. Compilar Backend Spring Boot
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 Paso 2: Compilando Backend Spring Boot..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Usar Maven wrapper si existe, sino mvn global
if [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    ./mvnw clean package -DskipTests
elif command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
else
    echo "❌ Maven no encontrado. Instalalo con:"
    echo "   sudo apt install maven"
    exit 1
fi
echo ""

# Verificar que el JAR se creó
JAR_FILE="target/bully-algorithm-1.0.0.jar"
if [ -f "$JAR_FILE" ]; then
    JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
    echo "╔══════════════════════════════════════════╗"
    echo "║  ✅ COMPILACIÓN EXITOSA                  ║"
    echo "╠══════════════════════════════════════════╣"
    echo "║  JAR: $JAR_FILE"
    echo "║  Tamaño: $JAR_SIZE"
    echo "╠══════════════════════════════════════════╣"
    echo "║  Pasos siguientes:                       ║"
    echo "║  1. Copiar el JAR a las 5 PCs            ║"
    echo "║  2. En cada PC: ./scripts/configure-node.sh"
    echo "║  3. En cada PC: ./scripts/start-nodeX.sh ║"
    echo "║  4. Abrir http://localhost:8080           ║"
    echo "╚══════════════════════════════════════════╝"
else
    echo "❌ Error: No se generó el archivo JAR"
    exit 1
fi
