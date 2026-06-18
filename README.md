# 🗳️ Algoritmo Bully - Elección de Coordinador

**Práctica 10 - Sistemas Distribuidos**  
**FEIRNNR - Carrera de Computación**

Sistema distribuido real con 5 nodos que implementa el Algoritmo Bully para elección dinámica de coordinador.

## 🏗️ Tecnologías

- **Backend**: Java 17 + Spring Boot 3.2
- **Frontend**: React 18 + Vite
- **Comunicación**: REST HTTP entre nodos
- **Red**: Ethernet (switch/router) para baja latencia

## 📁 Estructura del Proyecto

```
Practica_Bully/
├── pom.xml                          # Configuración Maven
├── scripts/
│   ├── configure-node.sh            # Configurar nodo (IPs + ID)
│   └── build.sh                     # Compilar todo
├── src/main/java/.../bully/
│   ├── BullyAlgorithmApplication.java
│   ├── config/
│   │   ├── NodeConfig.java          # Configuración del nodo
│   │   └── WebConfig.java           # CORS
│   ├── model/
│   │   ├── Message.java             # Modelo de mensaje
│   │   ├── NodeInfo.java            # Info de un nodo
│   │   └── NodeStatus.java          # Estado de un nodo
│   ├── service/
│   │   ├── BullyService.java        # ★ Lógica del algoritmo
│   │   └── MessageLogger.java       # Registro de mensajes
│   ├── controller/
│   │   └── BullyController.java     # Endpoints REST
│   └── scheduler/
│       └── HeartbeatScheduler.java   # Detección de fallas
└── frontend/
    └── src/
        ├── App.jsx                   # Componente principal
        └── components/
            ├── ClusterView.jsx       # Visualización de nodos
            ├── ControlPanel.jsx      # Botones de control
            ├── MessageLog.jsx        # Log de mensajes
            └── StatsPanel.jsx        # Estadísticas
```

## 🚀 Guía Rápida (5 minutos)

### 1. Compilar (en una sola PC)

```bash
# Dar permisos a los scripts
chmod +x scripts/*.sh

# Compilar todo (frontend + backend)
./scripts/build.sh
```

### 2. Copiar a las 5 PCs

Copiar **todo el proyecto** o solo estos archivos a cada PC:
- `target/bully-algorithm-1.0.0.jar` (el JAR compilado)
- `scripts/` (carpeta de scripts)
- `src/main/resources/application.properties`

### 3. Configurar cada PC

En **cada computadora**, ejecutar:
```bash
./scripts/configure-node.sh
```

Esto preguntará:
- El ID del nodo (1, 2, 3, 4 o 5)
- Las IPs de las 5 computadoras

### 4. Iniciar los nodos

En **cada computadora**:
```bash
./scripts/start-nodeX.sh   # X = número del nodo
```

O manualmente:
```bash
java -jar target/bully-algorithm-1.0.0.jar --node.id=X --node.peers="1,IP1,8080;2,IP2,8080;3,IP3,8080;4,IP4,8080;5,IP5,8080"
```

### 5. Abrir el navegador

En cada PC, abrir: **http://localhost:8080**

## 🧪 Flujo de la Práctica

| Paso | Acción | Resultado |
|------|--------|-----------|
| 1 | Iniciar los 5 nodos | Todos activos, P5 es coordinador |
| 2 | En P5: "Simular Falla" | P5 se desactiva |
| 3 | Esperar ~5s (heartbeat) | Un nodo detecta la falla e inicia elección |
| 4 | Observar mensajes | ELECTION → OK → COORDINATOR |
| 5 | Verificar resultado | P4 es el nuevo coordinador |
| 6 | En P5: "Recuperar" | P5 vuelve, inicia elección, se convierte en coordinador |

## 📡 Endpoints REST

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | /api/election | Recibir ELECTION |
| POST | /api/coordinator | Recibir COORDINATOR |
| POST | /api/ping | Heartbeat |
| GET | /api/status | Estado del nodo |
| GET | /api/cluster | Estado del cluster |
| POST | /api/start-election | Iniciar elección manual |
| POST | /api/fail | Simular falla |
| POST | /api/recover | Recuperar nodo |
| GET | /api/messages | Log de mensajes |
| GET | /api/stats | Estadísticas |

## ⚙️ Configuración

En `src/main/resources/application.properties`:

```properties
node.id=1                    # ID del nodo (1-5)
server.port=8080             # Puerto HTTP
node.peers=1,IP,8080;...     # Lista de nodos
bully.election-timeout=2000  # Timeout de elección (ms)
bully.heartbeat-interval=5000 # Intervalo de heartbeat (ms)
```

## 📚 Algoritmo Bully

1. **Detección de falla**: Heartbeat periódico al coordinador
2. **Elección**: Enviar ELECTION a procesos con ID mayor
3. **Respuesta OK**: Procesos superiores responden y hacen su propia elección
4. **Coordinador**: El proceso activo con mayor ID gana y anuncia COORDINATOR
5. **Recuperación**: Un nodo que se recupera inicia elección automática
