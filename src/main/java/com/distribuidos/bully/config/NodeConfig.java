package com.distribuidos.bully.config;

import com.distribuidos.bully.model.NodeInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuración del nodo actual y sus peers en el sistema distribuido.
 * Lee las propiedades del archivo application.properties:
 * - node.id: ID único del nodo (1-5)
 * - node.peers: Lista de nodos en formato "id,ip,puerto;id,ip,puerto;..."
 * - bully.election-timeout: Timeout para mensajes de elección (ms)
 * - bully.heartbeat-interval: Intervalo de heartbeat (ms)
 */
@Component
public class NodeConfig {

    private static final Logger log = LoggerFactory.getLogger(NodeConfig.class);

    @Value("${node.id}")
    private int nodeId;

    @Value("${node.peers}")
    private String peersString;

    @Value("${bully.election-timeout:2000}")
    private int electionTimeout;

    @Value("${bully.heartbeat-interval:5000}")
    private int heartbeatInterval;

    private List<NodeInfo> peers;

    /**
     * Inicializa la lista de peers parseando la cadena de configuración.
     * Formato: "id,host,port;id,host,port;..."
     */
    @PostConstruct
    public void init() {
        peers = parsePeers(peersString);
        log.info("========================================");
        log.info("  NODO P{} CONFIGURADO", nodeId);
        log.info("  Peers: {}", peers.size());
        peers.forEach(p -> log.info("    P{} -> {}:{}", p.getId(), p.getHost(), p.getPort()));
        log.info("  Election Timeout: {}ms", electionTimeout);
        log.info("  Heartbeat Interval: {}ms", heartbeatInterval);
        log.info("========================================");
    }

    /**
     * Parsea la cadena de peers en una lista de NodeInfo.
     */
    private List<NodeInfo> parsePeers(String peersStr) {
        List<NodeInfo> result = new ArrayList<>();
        if (peersStr == null || peersStr.isBlank()) {
            return result;
        }

        String[] peerEntries = peersStr.split(";");
        for (String entry : peerEntries) {
            String[] parts = entry.trim().split(",");
            if (parts.length == 3) {
                int id = Integer.parseInt(parts[0].trim());
                String host = parts[1].trim();
                int port = Integer.parseInt(parts[2].trim());
                result.add(new NodeInfo(id, host, port));
            }
        }
        return result;
    }

    // ==================== Getters ====================

    public int getNodeId() {
        return nodeId;
    }

    public List<NodeInfo> getPeers() {
        return peers;
    }

    public int getElectionTimeout() {
        return electionTimeout;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * Busca un peer por su ID.
     */
    public NodeInfo getPeerById(int peerId) {
        return peers.stream()
                .filter(p -> p.getId() == peerId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Obtiene el ID máximo entre todos los peers (será el coordinador inicial).
     */
    public int getMaxPeerId() {
        return peers.stream()
                .mapToInt(NodeInfo::getId)
                .max()
                .orElse(nodeId);
    }
}
