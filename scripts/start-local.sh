#!/bin/bash
# ============================================
# Script para iniciar los 5 nodos en LOCAL
# Algoritmo Bully - Sistemas Distribuidos
# ============================================

cd "$(dirname "$0")/.."

# Lista de peers usando localhost y puertos diferentes
PEERS="1,127.0.0.1,8081;2,127.0.0.1,8082;3,127.0.0.1,8083;4,127.0.0.1,8084;5,127.0.0.1,8085"

# Crear carpeta de logs si no existe
mkdir -p logs

echo "╔══════════════════════════════════════════╗"
echo "║  Iniciando Cluster Local (5 Nodos)       ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# Limpiar archivo de PIDs si existe
rm -f .local_pids

for i in {1..5}; do
  PORT=$((8080 + i))
  echo "🚀 Iniciando Nodo P$i en el puerto $PORT..."
  
  # Ejecutar en segundo plano
  java -jar target/bully-algorithm-1.0.0.jar \
    --server.port=$PORT \
    --node.id=$i \
    --node.peers="$PEERS" > "logs/node$i.log" 2>&1 &
    
  # Guardar el PID (Process ID) para poder cerrarlo luego
  echo $! >> .local_pids
  
  # Pequeña pausa para que no inicien todos exactamente al mismo milisegundo
  sleep 1
done

echo ""
echo "✅ ¡Los 5 nodos están corriendo en segundo plano!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🌐 Abre en tu navegador:"
echo "   http://localhost:8081  (Para ver y controlar el Nodo 1)"
echo "   http://localhost:8082  (Para ver y controlar el Nodo 2)"
echo "   (y así hasta el 8085)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🛑 Para detenerlos todos usa: ./scripts/stop-local.sh"
echo ""
