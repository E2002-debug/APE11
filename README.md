# Algoritmo Bully - Eleccion de Coordinador - Bizantino

Practica 11 - Sistemas Distribuidos 
- FEIRNNR - Carrera de Computacion

Sistema distribuido real con 4 nodos que implementa el Algoritmo Bully para eleccion dinamica de coordinador.

## [+] Tecnologias

- Backend: Java 17 + Spring Boot 3.2
- Frontend: React 18 + Vite
- Comunicacion: REST HTTP entre nodos
- Red: Ethernet (switch/router) para baja latencia

## [+] Como se usa la aplicacion (Guia Rapida)

Para probar el algoritmo en las 4 PCs, debes seguir estos pasos:

### 1. Compilar el proyecto (en una sola PC)

Primero, debes compilar todo el codigo. En la terminal de la computadora principal ejecuta:
```bash
# Dar permisos a los scripts
chmod +x scripts/*.sh

# Compilar todo (frontend + backend)
./scripts/build.sh
```

### 2. Distribuir a las 4 PCs

Una vez compilado, copia todo el proyecto a las otras 3 computadoras (o al menos los archivos necesarios):
- La carpeta `target/` (que contiene el archivo `.jar`).
- La carpeta `scripts/`.
- El archivo `src/main/resources/application.properties`.

### 3. Configurar cada nodo

En CADA UNA de las 4 computadoras, abre una terminal en la carpeta del proyecto y ejecuta:
```bash
./scripts/configure-node.sh
```

El script te hara dos preguntas importantes:
1. El ID del nodo actual (Ingresa 1, 2, 3 o 4 dependiendo de la PC).
2. Las direcciones IP de las 4 computadoras de la red.

### 4. Iniciar los nodos

Una vez configurado, en cada computadora ejecuta el script de inicio que se genero para ese nodo:
```bash
# Ejemplo si estas en el nodo 1:
./scripts/start-node1.sh
```

(En la computadora 2 sera start-node2.sh, etc.)

### 5. Interfaz Grafica

Abre tu navegador web en cualquiera de las PCs e ingresa a:
http://localhost:8080

Desde esta interfaz podras ver el estado del cluster, simular fallas (para ver como otro nodo asume como coordinador) y probar el sistema de Consenso Bizantino.

## [+] Flujo de Prueba de la Practica

1. Iniciar los 4 nodos: Todos estaran activos y P4 sera el coordinador.
2. En P4: Presiona "Simular Falla" en la interfaz. El nodo se apagara logicamente.
3. Esperar (aprox 5s): Otro nodo detectara la falta del coordinador por el ping.
4. Observar mensajes: Veras que se envian mensajes de ELECTION y OK.
5. Verificar resultado: P3 deberia declararse como el nuevo coordinador.
6. En P4: Presiona "Recuperar". P4 volvera a estar activo, iniciara otra eleccion y volvera a ser coordinador.

## [+] Tolerancia a Fallos Bizantinos (BFT)

Puedes marcar un nodo como "Bizantino" desde la interfaz para probar la tolerancia a fallos.
- Cuando un nodo es normal, respondera de forma honesta a las peticiones de consenso.
- Cuando un nodo es bizantino, enviara respuestas de consenso contradictorias (Voto "SI" a unos, Voto "NO" a otros) para intentar confundir la decision global.
