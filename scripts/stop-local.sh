#!/bin/bash
# ============================================
# Script para detener los 4 nodos
# ============================================

cd "$(dirname "$0")/.."

echo "[STOP] Deteniendo los nodos..."

if [ -f .local_pids ]; then
  while read pid; do
    # Mata el proceso si existe, mandando el error a la nada para que no moleste
    kill $pid 2>/dev/null
  done < .local_pids
  
  rm .local_pids
  echo "[OK] Nodos detenidos usando archivo de PIDs."
else
  echo "[WARN] No se encontró el archivo de PIDs. Intentando matar procesos java 'bully-algorithm'..."
  pkill -f bully-algorithm
  echo "[OK] Nodos detenidos por nombre de proceso."
fi
