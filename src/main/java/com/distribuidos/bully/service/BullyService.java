package com.distribuidos.bully.service;

import com.distribuidos.bully.config.NodeConfig;
import com.distribuidos.bully.model.Message;
import com.distribuidos.bully.model.NodeInfo;
import com.distribuidos.bully.model.NodeStatus;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Servicio principal que implementa el Algoritmo Bully para elección de coordinador.
 * 
 * ALGORITMO BULLY:
 * 1. Cuando un proceso P detecta que el coordinador falló:
 *    - P envía ELECTION a todos los procesos con ID mayor
 *    - Si ninguno responde (timeout)  P se declara COORDINADOR
 *    - Si alguno responde OK  P espera anuncio COORDINATOR
 * 
 * 2. Cuando un proceso Q recibe ELECTION de un proceso con ID menor:
 *    - Q responde OK al emisor
 *    - Q inicia su propia elección (envía ELECTION a procesos con ID mayor que Q)
 * 
 * 3. Cuando un proceso recibe COORDINATOR:
 *    - Actualiza su referencia al coordinador actual
 * 
 * 4. Cuando un proceso se recupera de una falla:
 *    - Inicia una elección automáticamente
 */
@Service
public class BullyService {

    private static final Logger log = LoggerFactory.getLogger(BullyService.class);

    private final NodeConfig nodeConfig;
    private final MessageLogger messageLogger;

    // Estado del nodo (volatile para visibilidad entre hilos)
    private volatile boolean active = true;
    private volatile int coordinatorId;
    private volatile boolean inElection = false;

    private volatile boolean byzantine = false;

    // Lock para operaciones de elección
    private final Object electionLock = new Object();

    // Consenso BFT: Transaction ID -> (Sender Node ID -> Vote)
    private final Map<String, Map<Integer, Boolean>> consensusVotes = new ConcurrentHashMap<>();
    private volatile String lastTransactionId = null;

    // Executor para tareas asíncronas (timeout de espera de coordinador)
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> coordinatorWaitTask;

    public BullyService(NodeConfig nodeConfig, MessageLogger messageLogger) {
        this.nodeConfig = nodeConfig;
        this.messageLogger = messageLogger;
    }

    /**
     * Inicialización: establece P5 (o el ID máximo) como coordinador inicial.
     */
    @PostConstruct
    public void init() {
        coordinatorId = nodeConfig.getMaxPeerId();

        if (nodeConfig.getNodeId() == coordinatorId) {
            log.info("*** Nodo P{} es el COORDINADOR INICIAL ***", nodeConfig.getNodeId());
            messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() + " inicia como COORDINADOR");
        } else {
            log.info("Nodo P{} iniciado. Coordinador actual: P{}",
                    nodeConfig.getNodeId(), coordinatorId);
            messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                    " iniciado (Coordinador: P" + coordinatorId + ")");
        }
    }

    // ================================================================
    //  ALGORITMO BULLY - LÓGICA PRINCIPAL
    // ================================================================

    /**
     * PASO 1: Inicia el proceso de elección.
     * Envía mensajes ELECTION a todos los procesos con ID superior.
     */
    public void startElection() {
        synchronized (electionLock) {
            if (!active) {
                log.info("Nodo P{} está inactivo, no puede iniciar elección",
                        nodeConfig.getNodeId());
                return;
            }
            if (inElection) {
                log.info("Nodo P{} ya está en proceso de elección",
                        nodeConfig.getNodeId());
                return;
            }
            inElection = true;
        }

        log.info("==========================================");
        log.info("  NODO P{} INICIA ELECCIÓN", nodeConfig.getNodeId());
        log.info("==========================================");

        messageLogger.startElectionTimer();
        messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                " inicia proceso de elección");

        // Obtener procesos con ID mayor
        List<NodeInfo> higherPeers = nodeConfig.getPeers().stream()
                .filter(p -> p.getId() > nodeConfig.getNodeId())
                .toList();

        // Si no hay procesos con ID mayor  soy el coordinador
        if (higherPeers.isEmpty()) {
            log.info("Nodo P{} tiene el ID más alto  se declara coordinador",
                    nodeConfig.getNodeId());
            declareCoordinator();
            return;
        }

        // Enviar ELECTION a todos los procesos con ID mayor
        boolean anyResponded = false;
        for (NodeInfo peer : higherPeers) {
            boolean responded = sendElectionMessage(peer);
            if (responded) {
                anyResponded = true;
            }
        }

        if (!anyResponded) {
            // Ningún proceso superior respondió  soy el coordinador
            log.info("Ningún nodo superior respondió  P{} se declara coordinador",
                    nodeConfig.getNodeId());
            declareCoordinator();
        } else {
            // Alguno respondió OK  esperar anuncio COORDINATOR con timeout
            log.info("Nodo P{} recibió OK, esperando anuncio COORDINATOR...",
                    nodeConfig.getNodeId());
            scheduleCoordinatorWaitTimeout();
        }
    }

    /**
     * PASO 2: Maneja la recepción de un mensaje ELECTION de otro nodo.
     * Responde OK (implícito en HTTP 200) e inicia su propia elección.
     * 
     * @param senderId ID del nodo que envió el mensaje ELECTION
     * @return true si este nodo está activo y puede responder
     */
    public boolean handleElectionMessage(int senderId) {
        if (!active) {
            log.info("Nodo P{} está inactivo, ignora ELECTION de P{}",
                    nodeConfig.getNodeId(), senderId);
            return false;
        }

        log.info("Recibido ELECTION de P{}  respondiendo OK", senderId);
        messageLogger.logReceived(Message.Type.ELECTION, senderId, nodeConfig.getNodeId());
        messageLogger.logSent(Message.Type.OK, nodeConfig.getNodeId(), senderId);

        // Iniciar propia elección asíncronamente
        CompletableFuture.runAsync(() -> {
            try {
                // Pequeña pausa para evitar condición de carrera
                Thread.sleep(100);
                startElection();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        return true;
    }

    /**
     * PASO 3: Maneja la recepción de un mensaje COORDINATOR.
     * Actualiza el coordinador conocido y finaliza la elección.
     * 
     * @param newCoordinatorId ID del nuevo coordinador
     */
    public void handleCoordinatorMessage(int newCoordinatorId) {
        if (!active) return;

        log.info("Recibido COORDINATOR: P{} es el nuevo coordinador", newCoordinatorId);
        messageLogger.logReceived(Message.Type.COORDINATOR, newCoordinatorId,
                nodeConfig.getNodeId());

        synchronized (electionLock) {
            coordinatorId = newCoordinatorId;
            inElection = false;
            if (coordinatorWaitTask != null) {
                coordinatorWaitTask.cancel(false);
                coordinatorWaitTask = null;
            }
        }

        messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                " reconoce a P" + newCoordinatorId + " como COORDINADOR");
    }

    // ================================================================
    //  MÉTODOS AUXILIARES DEL ALGORITMO
    // ================================================================

    /**
     * Envía un mensaje ELECTION a un peer específico.
     * @return true si el peer respondió OK
     */
    private boolean sendElectionMessage(NodeInfo peer) {
        try {
            RestTemplate rt = createRestTemplate();
            Map<String, Object> body = Map.of("senderId", nodeConfig.getNodeId());

            log.info("Enviando ELECTION: P{}  P{}", nodeConfig.getNodeId(), peer.getId());
            messageLogger.logSent(Message.Type.ELECTION, nodeConfig.getNodeId(), peer.getId());

            ResponseEntity<Map> response = rt.postForEntity(
                    peer.getUrl() + "/api/election", body, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Recibido OK de P{}", peer.getId());
                messageLogger.logReceived(Message.Type.OK, peer.getId(),
                        nodeConfig.getNodeId());
                return true;
            }
        } catch (Exception e) {
            log.warn("P{} no responde: {}", peer.getId(), e.getMessage());
            messageLogger.logFailed(Message.Type.ELECTION, nodeConfig.getNodeId(),
                    peer.getId());
        }
        return false;
    }

    /**
     * Se declara coordinador y envía COORDINATOR a todos los demás nodos.
     */
    private void declareCoordinator() {
        synchronized (electionLock) {
            coordinatorId = nodeConfig.getNodeId();
            inElection = false;
            if (coordinatorWaitTask != null) {
                coordinatorWaitTask.cancel(false);
                coordinatorWaitTask = null;
            }
        }

        log.info("***************************************");
        log.info("  NODO P{} ES EL NUEVO COORDINADOR", nodeConfig.getNodeId());
        log.info("***************************************");

        messageLogger.logEvent("¡P" + nodeConfig.getNodeId() +
                " se declara COORDINADOR!");
        messageLogger.recordElection(nodeConfig.getNodeId());

        // Enviar COORDINATOR a TODOS los demás nodos
        for (NodeInfo peer : nodeConfig.getPeers()) {
            if (peer.getId() != nodeConfig.getNodeId()) {
                sendCoordinatorMessage(peer);
            }
        }
    }

    /**
     * Envía mensaje COORDINATOR a un peer específico.
     */
    private void sendCoordinatorMessage(NodeInfo peer) {
        try {
            RestTemplate rt = createRestTemplate();
            Map<String, Object> body = Map.of("coordinatorId", nodeConfig.getNodeId());

            log.info("Enviando COORDINATOR: P{}  P{}", nodeConfig.getNodeId(), peer.getId());
            messageLogger.logSent(Message.Type.COORDINATOR, nodeConfig.getNodeId(),
                    peer.getId());

            rt.postForEntity(peer.getUrl() + "/api/coordinator", body, String.class);
            log.info("COORDINATOR enviado a P{}", peer.getId());
        } catch (Exception e) {
            log.warn("No se pudo enviar COORDINATOR a P{}: {}", peer.getId(),
                    e.getMessage());
            messageLogger.logFailed(Message.Type.COORDINATOR, nodeConfig.getNodeId(),
                    peer.getId());
        }
    }

    /**
     * Programa un timeout para esperar el anuncio COORDINATOR.
     * Si no llega en el tiempo esperado, reinicia la elección.
     */
    private void scheduleCoordinatorWaitTimeout() {
        if (coordinatorWaitTask != null) {
            coordinatorWaitTask.cancel(false);
        }

        long timeout = nodeConfig.getElectionTimeout() * 3L;
        coordinatorWaitTask = scheduler.schedule(() -> {
            synchronized (electionLock) {
                if (inElection) {
                    log.warn("Timeout esperando COORDINATOR  reiniciando elección");
                    messageLogger.logEvent("Timeout esperando COORDINATOR en P" +
                            nodeConfig.getNodeId());
                    inElection = false;
                }
            }
            startElection();
        }, timeout, TimeUnit.MILLISECONDS);
    }

    // ================================================================
    //  CONTROL DEL NODO (simular falla / recuperar)
    // ================================================================

    /**
     * Simula una falla del nodo: lo desactiva para que no responda.
     */
    public void simulateFailure() {
        synchronized (electionLock) {
            active = false;
            inElection = false;
            if (coordinatorWaitTask != null) {
                coordinatorWaitTask.cancel(false);
                coordinatorWaitTask = null;
            }
        }

        log.info("NODO P{} HA FALLADO (simulado) ", nodeConfig.getNodeId());
        messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                " ha fallado (simulado)");
    }

    /**
     * Recupera el nodo de una falla e inicia automáticamente una elección.
     */
    public void recover() {
        synchronized (electionLock) {
            active = true;
        }

        log.info("Nodo P{} SE HA RECUPERADO", nodeConfig.getNodeId());
        messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                " se ha recuperado  inicia elección");

        // Al recuperarse, el nodo inicia una elección automáticamente
        startElection();
    }

    // ================================================================
    //  HEARTBEAT / PING
    // ================================================================

    /**
     * Hace ping al coordinador actual para verificar que está activo.
     * @return true si el coordinador responde
     */
    public boolean pingCoordinator() {
        if (!active) return true;
        if (coordinatorId == nodeConfig.getNodeId()) return true;

        NodeInfo coordinator = nodeConfig.getPeerById(coordinatorId);
        if (coordinator == null) return false;

        try {
            RestTemplate rt = createRestTemplate();
            ResponseEntity<Map> response = rt.postForEntity(
                    coordinator.getUrl() + "/api/ping",
                    Map.of("senderId", nodeConfig.getNodeId()),
                    Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Coordinador P{} no responde al ping", coordinatorId);
            return false;
        }
    }

    /**
     * Verifica si algún nodo con ID superior está activo.
     * Útil para recuperar el sistema de un "split-brain" o partición de red.
     */
    public boolean checkHigherNodesAlive() {
        if (!active) return false;
        for (NodeInfo peer : nodeConfig.getPeers()) {
            if (peer.getId() > nodeConfig.getNodeId()) {
                try {
                    RestTemplate rt = createRestTemplate();
                    ResponseEntity<Map> response = rt.postForEntity(
                            peer.getUrl() + "/api/ping",
                            Map.of("senderId", nodeConfig.getNodeId()),
                            Map.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return true;
                    }
                } catch (Exception e) {
                    // Ignorar nodo caído
                }
            }
        }
        return false;
    }

    /**
     * Responde a un ping (health check) de otro nodo.
     */
    public boolean handlePing(int senderId) {
        return active;
    }

    // ================================================================
    //  CONSULTAS DE ESTADO
    // ================================================================

    /**
     * Devuelve el estado actual de este nodo.
     */
    public NodeStatus getStatus() {
        return new NodeStatus(
                nodeConfig.getNodeId(),
                active,
                coordinatorId,
                inElection,
                byzantine
        );
    }

    public boolean isActive() {
        return active;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public boolean isInElection() {
        return inElection;
    }

    public boolean isByzantine() {
        return byzantine;
    }

    public void setByzantine(boolean byzantine) {
        this.byzantine = byzantine;
        if (byzantine) {
            log.info("NODO P{} AHORA ES BIZANTINO", nodeConfig.getNodeId());
            messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() + " configurado como BIZANTINO");
        } else {
            log.info("NODO P{} AHORA ES HONESTO", nodeConfig.getNodeId());
            messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() + " configurado como HONESTO");
        }
    }

    // ================================================================
    //  CONSENSO BFT
    // ================================================================

    public void startConsensus(String transactionId) {
        if (!active) return;
        
        log.info("Iniciando consenso para transacción: {}", transactionId);
        messageLogger.logEvent("P" + nodeConfig.getNodeId() + " inicia consenso para: " + transactionId);
        
        lastTransactionId = transactionId;
        consensusVotes.put(transactionId, new ConcurrentHashMap<>());

        // El coordinador manda un mensaje PREPARE a todos los nodos
        for (NodeInfo peer : nodeConfig.getPeers()) {
            sendConsensusPrepare(peer, transactionId);
        }
    }

    private void sendConsensusPrepare(NodeInfo peer, String transactionId) {
        try {
            RestTemplate rt = createRestTemplate();
            Map<String, Object> body = Map.of(
                    "senderId", nodeConfig.getNodeId(),
                    "transactionId", transactionId
            );

            log.info("Enviando CONSENSUS_PREPARE: P{}  P{}", nodeConfig.getNodeId(), peer.getId());
            messageLogger.logSent(Message.Type.CONSENSUS_PREPARE, nodeConfig.getNodeId(), peer.getId());

            rt.postForEntity(peer.getUrl() + "/api/consensus/prepare", body, Map.class);
        } catch (Exception e) {
            log.warn("P{} no responde al PREPARE: {}", peer.getId(), e.getMessage());
            messageLogger.logFailed(Message.Type.CONSENSUS_PREPARE, nodeConfig.getNodeId(), peer.getId());
        }
    }

    public void handleConsensusPrepare(int senderId, String transactionId) {
        if (!active) return;
        
        log.info("Recibido CONSENSUS_PREPARE de P{} para {}", senderId, transactionId);
        messageLogger.logReceived(Message.Type.CONSENSUS_PREPARE, senderId, nodeConfig.getNodeId());

        lastTransactionId = transactionId;
        consensusVotes.putIfAbsent(transactionId, new ConcurrentHashMap<>());

        // Inicia el broadcast de su voto a todos asíncronamente
        CompletableFuture.runAsync(() -> broadcastConsensusVote(transactionId));
    }

    private void broadcastConsensusVote(String transactionId) {
        for (NodeInfo peer : nodeConfig.getPeers()) {
            boolean myVote = true; // El voto honesto es SI (true)
            
            // Comportamiento bizantino: enviar SI a algunos, NO a otros
            if (byzantine) {
                // Enviar SI a los nodos pares y NO a los impares
                myVote = (peer.getId() % 2 == 0);
            }

            sendConsensusVote(peer, transactionId, myVote);
        }
    }

    private void sendConsensusVote(NodeInfo peer, String transactionId, boolean vote) {
        try {
            RestTemplate rt = createRestTemplate();
            Map<String, Object> body = Map.of(
                    "senderId", nodeConfig.getNodeId(),
                    "transactionId", transactionId,
                    "vote", vote
            );

            log.info("Enviando CONSENSUS_VOTE ({}) a P{}", vote ? "SI" : "NO", peer.getId());
            messageLogger.logSent(Message.Type.CONSENSUS_VOTE, nodeConfig.getNodeId(), peer.getId());

            rt.postForEntity(peer.getUrl() + "/api/consensus/vote", body, Map.class);
        } catch (Exception e) {
            log.warn("P{} no responde al VOTE: {}", peer.getId(), e.getMessage());
        }
    }

    public void handleConsensusVote(int senderId, String transactionId, boolean vote) {
        if (!active) return;
        
        log.info("Recibido CONSENSUS_VOTE ({}) de P{} para {}", vote ? "SI" : "NO", senderId, transactionId);
        messageLogger.logReceived(Message.Type.CONSENSUS_VOTE, senderId, nodeConfig.getNodeId());

        consensusVotes.putIfAbsent(transactionId, new ConcurrentHashMap<>());
        consensusVotes.get(transactionId).put(senderId, vote);
    }
    
    public Map<Integer, Boolean> getConsensusResults(String transactionId) {
        if (transactionId == null || !consensusVotes.containsKey(transactionId)) {
            return Map.of();
        }
        return consensusVotes.get(transactionId);
    }
    
    public String getLastTransactionId() {
        return lastTransactionId;
    }

    /**
     * Crea un RestTemplate con timeouts configurados para la red local.
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(nodeConfig.getElectionTimeout()));
        factory.setReadTimeout(Duration.ofMillis(nodeConfig.getElectionTimeout()));
        return new RestTemplate(factory);
    }
}
