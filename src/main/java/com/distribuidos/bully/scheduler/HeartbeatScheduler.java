package com.distribuidos.bully.scheduler;

import com.distribuidos.bully.config.NodeConfig;
import com.distribuidos.bully.service.BullyService;
import com.distribuidos.bully.service.MessageLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de heartbeat que verifica periódicamente si el coordinador
 * está activo. Si el coordinador no responde, inicia automáticamente
 * una elección usando el Algoritmo Bully.
 * 
 * Intervalo configurable via bully.heartbeat-interval (default: 5000ms)
 */
@Component
public class HeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatScheduler.class);

    private final BullyService bullyService;
    private final MessageLogger messageLogger;
    private final NodeConfig nodeConfig;

    // Contador de fallos consecutivos antes de iniciar elección
    private int consecutiveFailures = 0;
    private static final int FAILURE_THRESHOLD = 1;

    public HeartbeatScheduler(BullyService bullyService, MessageLogger messageLogger,
                              NodeConfig nodeConfig) {
        this.bullyService = bullyService;
        this.messageLogger = messageLogger;
        this.nodeConfig = nodeConfig;
    }

    /**
     * Tarea programada que hace ping al coordinador cada X milisegundos.
     * Si el coordinador no responde, inicia una elección.
     */
    @Scheduled(fixedDelayString = "${bully.heartbeat-interval:5000}")
    public void checkCoordinator() {
        // No hacer nada si el nodo está inactivo
        if (!bullyService.isActive()) {
            consecutiveFailures = 0;
            return;
        }

        // No hacer nada si somos el coordinador
        if (bullyService.getCoordinatorId() == nodeConfig.getNodeId()) {
            consecutiveFailures = 0;
            return;
        }

        // No hacer nada si ya estamos en elección
        if (bullyService.isInElection()) {
            return;
        }

        // Hacer ping al coordinador
        boolean coordinatorAlive = bullyService.pingCoordinator();

        if (coordinatorAlive) {
            consecutiveFailures = 0;
            log.debug("Heartbeat OK - Coordinador P{} activo",
                    bullyService.getCoordinatorId());
        } else {
            consecutiveFailures++;
            log.warn("Heartbeat FALLO #{} - Coordinador P{} no responde",
                    consecutiveFailures, bullyService.getCoordinatorId());

            if (consecutiveFailures >= FAILURE_THRESHOLD) {
                log.warn("Coordinador P{} detectado como INACTIVO  iniciando elección",
                        bullyService.getCoordinatorId());
                messageLogger.logEvent("Nodo P" + nodeConfig.getNodeId() +
                        " detecta falla del coordinador P" +
                        bullyService.getCoordinatorId());
                consecutiveFailures = 0;
                bullyService.startElection();
            }
        }
    }
}
