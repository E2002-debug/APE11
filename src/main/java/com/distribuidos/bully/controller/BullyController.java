package com.distribuidos.bully.controller;

import com.distribuidos.bully.config.NodeConfig;
import com.distribuidos.bully.model.NodeInfo;
import com.distribuidos.bully.model.NodeStatus;
import com.distribuidos.bully.service.BullyService;
import com.distribuidos.bully.service.MessageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST que expone todos los endpoints para:
 * - Comunicación entre nodos (ELECTION, OK, COORDINATOR, PING)
 * - Control del nodo desde el frontend (iniciar elección, simular falla, recuperar)
 * - Consultas de estado (status, cluster, mensajes, estadísticas)
 */
@RestController
@RequestMapping("/api")
public class BullyController {

    private static final Logger log = LoggerFactory.getLogger(BullyController.class);

    private final BullyService bullyService;
    private final MessageLogger messageLogger;
    private final NodeConfig nodeConfig;

    public BullyController(BullyService bullyService, MessageLogger messageLogger,
                           NodeConfig nodeConfig) {
        this.bullyService = bullyService;
        this.messageLogger = messageLogger;
        this.nodeConfig = nodeConfig;
    }

    // ================================================================
    //  ENDPOINTS DE COMUNICACIÓN ENTRE NODOS
    // ================================================================

    /**
     * Recibe un mensaje ELECTION de otro nodo.
     * Si este nodo está activo, responde OK (HTTP 200) e inicia su propia elección.
     */
    @PostMapping("/election")
    public ResponseEntity<Map<String, Object>> receiveElection(
            @RequestBody Map<String, Integer> body) {

        int senderId = body.get("senderId");
        log.info("Recibido POST /api/election de P{}", senderId);

        boolean accepted = bullyService.handleElectionMessage(senderId);

        if (accepted) {
            return ResponseEntity.ok(Map.of(
                    "type", "OK",
                    "senderId", nodeConfig.getNodeId(),
                    "message", "Nodo P" + nodeConfig.getNodeId() + " responde OK"
            ));
        } else {
            // Nodo inactivo, no responde
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Nodo P" + nodeConfig.getNodeId() + " está inactivo"
            ));
        }
    }

    /**
     * Recibe un mensaje COORDINATOR de otro nodo.
     * Actualiza el coordinador conocido.
     */
    @PostMapping("/coordinator")
    public ResponseEntity<Map<String, Object>> receiveCoordinator(
            @RequestBody Map<String, Integer> body) {

        int newCoordinatorId = body.get("coordinatorId");
        log.info("Recibido POST /api/coordinator: P{} es coordinador", newCoordinatorId);

        if (!bullyService.isActive()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Nodo inactivo"
            ));
        }

        bullyService.handleCoordinatorMessage(newCoordinatorId);

        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "Coordinador actualizado a P" + newCoordinatorId
        ));
    }

    /**
     * Endpoint de ping/heartbeat.
     * Verifica si el nodo está activo.
     */
    @PostMapping("/ping")
    public ResponseEntity<Map<String, Object>> receivePing(
            @RequestBody Map<String, Integer> body) {

        if (!bullyService.isActive()) {
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Nodo inactivo"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "alive",
                "nodeId", nodeConfig.getNodeId(),
                "coordinatorId", bullyService.getCoordinatorId()
        ));
    }

    // ================================================================
    //  ENDPOINTS DE CONTROL (Frontend)
    // ================================================================

    /**
     * Inicia manualmente una elección desde el frontend.
     */
    @PostMapping("/start-election")
    public ResponseEntity<Map<String, Object>> startElection() {
        log.info("Solicitud manual de elección desde frontend");

        if (!bullyService.isActive()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "No se puede iniciar elección: nodo inactivo"
            ));
        }

        CompletableFuture.runAsync(bullyService::startElection);

        return ResponseEntity.ok(Map.of(
                "status", "election_started",
                "message", "Elección iniciada por P" + nodeConfig.getNodeId()
        ));
    }

    /**
     * Simula una falla del nodo (lo desactiva).
     */
    @PostMapping("/fail")
    public ResponseEntity<Map<String, Object>> simulateFailure() {
        log.info("Simulando falla del nodo P{}", nodeConfig.getNodeId());
        bullyService.simulateFailure();

        return ResponseEntity.ok(Map.of(
                "status", "failed",
                "message", "Nodo P" + nodeConfig.getNodeId() + " desactivado"
        ));
    }

    /**
     * Recupera el nodo e inicia elección automáticamente.
     */
    @PostMapping("/recover")
    public ResponseEntity<Map<String, Object>> recover() {
        log.info("Recuperando nodo P{}", nodeConfig.getNodeId());

        CompletableFuture.runAsync(bullyService::recover);

        return ResponseEntity.ok(Map.of(
                "status", "recovering",
                "message", "Nodo P" + nodeConfig.getNodeId() +
                        " recuperándose e iniciando elección"
        ));
    }

    // ================================================================
    //  ENDPOINTS DE CONSULTA
    // ================================================================

    /**
     * Devuelve el estado actual de este nodo.
     */
    @GetMapping("/status")
    public ResponseEntity<NodeStatus> getStatus() {
        return ResponseEntity.ok(bullyService.getStatus());
    }

    /**
     * Devuelve el estado del cluster completo (consulta a todos los peers en paralelo).
     */
    @GetMapping("/cluster")
    public ResponseEntity<Map<String, Object>> getClusterStatus() {
        List<CompletableFuture<NodeStatus>> futures = nodeConfig.getPeers().stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> queryPeerStatus(peer)))
                .toList();

        List<NodeStatus> cluster = futures.stream()
                .map(f -> {
                    try {
                        return f.get(2, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(NodeStatus::getNodeId))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", cluster);
        result.put("localNodeId", nodeConfig.getNodeId());
        result.put("coordinatorId", bullyService.getCoordinatorId());
        result.put("localActive", bullyService.isActive());
        result.put("inElection", bullyService.isInElection());

        return ResponseEntity.ok(result);
    }

    /**
     * Consulta el estado de un peer individual.
     */
    private NodeStatus queryPeerStatus(NodeInfo peer) {
        if (peer.getId() == nodeConfig.getNodeId()) {
            return bullyService.getStatus();
        }

        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(1500));
            factory.setReadTimeout(Duration.ofMillis(1500));
            RestTemplate rt = new RestTemplate(factory);

            ResponseEntity<NodeStatus> response = rt.getForEntity(
                    peer.getUrl() + "/api/status", NodeStatus.class);
            return response.getBody();
        } catch (Exception e) {
            // Nodo no disponible
            NodeStatus offlineNode = new NodeStatus();
            offlineNode.setNodeId(peer.getId());
            offlineNode.setActive(false);
            offlineNode.setCoordinatorId(-1);
            offlineNode.setInElection(false);
            return offlineNode;
        }
    }

    /**
     * Devuelve el log completo de mensajes.
     */
    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getMessages() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("messages", messageLogger.getMessages());
        result.put("events", messageLogger.getEvents());
        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve estadísticas del algoritmo.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = messageLogger.getStats();
        stats.put("nodeId", nodeConfig.getNodeId());
        stats.put("coordinatorId", bullyService.getCoordinatorId());
        stats.put("active", bullyService.isActive());
        return ResponseEntity.ok(stats);
    }

    /**
     * Limpia todos los registros de mensajes y estadísticas.
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearMessages() {
        messageLogger.clearAll();
        return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "message", "Registros limpiados"
        ));
    }

    // ================================================================
    //  ENDPOINTS BFT (Consenso)
    // ================================================================

    @PostMapping("/byzantine")
    public ResponseEntity<Map<String, Object>> toggleByzantine() {
        boolean currentState = bullyService.isByzantine();
        bullyService.setByzantine(!currentState);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "byzantine", bullyService.isByzantine(),
                "message", "Nodo P" + nodeConfig.getNodeId() + (bullyService.isByzantine() ? " es BIZANTINO" : " es HONESTO")
        ));
    }

    @PostMapping("/consensus/start")
    public ResponseEntity<Map<String, Object>> startConsensus(@RequestBody(required = false) Map<String, String> body) {
        if (!bullyService.isActive()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nodo inactivo"));
        }
        
        String transactionId = (body != null && body.containsKey("transactionId")) 
                ? body.get("transactionId") 
                : "TX-" + System.currentTimeMillis();

        CompletableFuture.runAsync(() -> bullyService.startConsensus(transactionId));

        return ResponseEntity.ok(Map.of(
                "status", "consensus_started",
                "transactionId", transactionId,
                "message", "Consenso iniciado por P" + nodeConfig.getNodeId()
        ));
    }

    @PostMapping("/consensus/prepare")
    public ResponseEntity<Map<String, Object>> receiveConsensusPrepare(@RequestBody Map<String, Object> body) {
        int senderId = (int) body.get("senderId");
        String transactionId = (String) body.get("transactionId");

        if (!bullyService.isActive()) {
            return ResponseEntity.status(503).body(Map.of("error", "Nodo inactivo"));
        }

        bullyService.handleConsensusPrepare(senderId, transactionId);

        return ResponseEntity.ok(Map.of("status", "preparing"));
    }

    @PostMapping("/consensus/vote")
    public ResponseEntity<Map<String, Object>> receiveConsensusVote(@RequestBody Map<String, Object> body) {
        int senderId = (int) body.get("senderId");
        String transactionId = (String) body.get("transactionId");
        boolean vote = (boolean) body.get("vote");

        if (!bullyService.isActive()) {
            return ResponseEntity.status(503).body(Map.of("error", "Nodo inactivo"));
        }

        bullyService.handleConsensusVote(senderId, transactionId, vote);

        return ResponseEntity.ok(Map.of("status", "vote_recorded"));
    }

    @GetMapping("/consensus/results")
    public ResponseEntity<Map<String, Object>> getConsensusResults(@RequestParam(required = false) String transactionId) {
        String txId = transactionId != null ? transactionId : bullyService.getLastTransactionId();
        if (txId == null) {
            return ResponseEntity.ok(Map.of("transactionId", null, "votes", Map.of()));
        }

        Map<Integer, Boolean> votes = bullyService.getConsensusResults(txId);
        
        int yesVotes = 0;
        int noVotes = 0;
        for (Boolean v : votes.values()) {
            if (v) yesVotes++;
            else noVotes++;
        }
        
        int totalNodes = nodeConfig.getPeers().size();
        
        String decision = "Pendiente";
        if (yesVotes + noVotes >= totalNodes - 1) { // Almost all voted
            if (yesVotes > noVotes) decision = "SI (Aprobar)";
            else decision = "NO (Rechazar)";
        }

        return ResponseEntity.ok(Map.of(
                "transactionId", txId,
                "votes", votes,
                "decision", decision,
                "yesVotes", yesVotes,
                "noVotes", noVotes,
                "totalNodes", totalNodes
        ));
    }
}
