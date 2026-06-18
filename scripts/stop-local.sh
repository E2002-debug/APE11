#!/bin/bash
# ============================================
# Script para detener los 5 nodos
# ============================================

cd "$(dirname "$0")/.."

echo "🛑 Deteniendo los nodos..."

if [ -f .local_pids ]; then
  while read pid; do
    # Mata el proceso si existe, mandando el error a la nada para que no moleste
    kill $pid 2>/dev/null
  done < .local_pids
  
  rm .local_pids
  echo "✅ Nodos detenidos usando archivo de PIDs."
else
  echo "⚠️ No se encontró el archivo de PIDs. Intentando matar procesos java 'bully-algorithm'..."
  pkill -f bully-algorithm
  echo "✅ Nodos detenidos por nombre de proceso."
fi
