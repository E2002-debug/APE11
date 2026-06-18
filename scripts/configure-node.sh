#!/bin/bash
# ============================================
# Script de configuración de nodo
# Algoritmo Bully - Sistemas Distribuidos
# ============================================

set -e

echo "╔══════════════════════════════════════════╗"
echo "║  Configuración del Nodo - Algoritmo Bully ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# Obtener la IP local automáticamente
LOCAL_IP=$(hostname -I | awk '{print $1}')
echo "📡 Tu IP detectada: $LOCAL_IP"
echo ""

# Preguntar el ID del nodo
read -p "🔢 Ingresa el ID de este nodo (1-5): " NODE_ID

if [[ ! "$NODE_ID" =~ ^[1-5]$ ]]; then
    echo "❌ Error: El ID debe ser un número entre 1 y 5"
    exit 1
fi

echo ""
echo "Ingresa las IPs de las 5 computadoras."
echo "(Presiona Enter para usar la IP detectada en este nodo)"
echo ""

declare -a IPS

for i in 1 2 3 4 5; do
    if [ "$i" -eq "$NODE_ID" ]; then
        read -p "  PC $i (este nodo) [$LOCAL_IP]: " IP
        IP=${IP:-$LOCAL_IP}
    else
        read -p "  PC $i: " IP
        if [ -z "$IP" ]; then
            echo "❌ Error: Debes ingresar la IP de la PC $i"
            exit 1
        fi
    fi
    IPS[$i]=$IP
done

# Construir la cadena de peers
PEERS=""
for i in 1 2 3 4 5; do
    if [ -n "$PEERS" ]; then
        PEERS="$PEERS;"
    fi
    PEERS="${PEERS}${i},${IPS[$i]},8080"
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 Configuración:"
echo "   Nodo ID:  P$NODE_ID"
echo "   Peers:    $PEERS"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Buscar el archivo application.properties
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROPERTIES_FILE="$SCRIPT_DIR/../src/main/resources/application.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
    echo "❌ No se encontró application.properties en: $PROPERTIES_FILE"
    exit 1
fi

# Actualizar application.properties
sed -i "s/^node.id=.*/node.id=$NODE_ID/" "$PROPERTIES_FILE"
sed -i "s|^node.peers=.*|node.peers=$PEERS|" "$PROPERTIES_FILE"

echo ""
echo "✅ Archivo application.properties actualizado correctamente"
echo ""

# Crear script de inicio específico para este nodo
START_SCRIPT="$SCRIPT_DIR/start-node${NODE_ID}.sh"
cat > "$START_SCRIPT" << EOF
#!/bin/bash
# Inicio del Nodo P${NODE_ID}
cd "\$(dirname "\$0")/.."
echo "🚀 Iniciando Nodo P${NODE_ID}..."
echo "   IP: ${IPS[$NODE_ID]}"
echo "   Puerto: 8080"
echo "   Abrir en navegador: http://localhost:8080"
echo ""
java -jar target/bully-algorithm-1.0.0.jar \\
  --node.id=${NODE_ID} \\
  --node.peers="${PEERS}"
EOF
chmod +x "$START_SCRIPT"

echo "📝 Script de inicio creado: start-node${NODE_ID}.sh"
echo ""
echo "═══════════════════════════════════════════"
echo "  Para iniciar este nodo ejecuta:"
echo "  ./scripts/start-node${NODE_ID}.sh"
echo "═══════════════════════════════════════════"
