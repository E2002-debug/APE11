package com.distribuidos.bully.service;

import com.distribuidos.bully.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio de registro de mensajes del Algoritmo Bully.
 * Registra todos los mensajes ELECTION, OK y COORDINATOR intercambiados,
 * así como eventos del sistema y estadísticas de las elecciones.
 * 
 * Thread-safe: usa Collections.synchronizedList para acceso concurrente.
 */
@Service
public class MessageLogger {

    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> events = Collections.synchronizedList(new ArrayList<>());
    private final List<Map<String, Object>> electionRecords = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong electionStartTime = new AtomicLong(0);

    /**
     * Registra un mensaje enviado exitosamente.
     */
    public void logSent(Message.Type type, int senderId, int receiverId) {
        Message msg = new Message(type, senderId, receiverId, Message.Direction.SENT, true);
        messages.add(msg);
    }

    /**
     * Registra un mensaje recibido.
     */
    public void logReceived(Message.Type type, int senderId, int receiverId) {
        Message msg = new Message(type, senderId, receiverId, Message.Direction.RECEIVED, true);
        messages.add(msg);
    }

    /**
     * Registra un mensaje que falló (nodo destino no disponible).
     */
    public void logFailed(Message.Type type, int senderId, int receiverId) {
        Message msg = new Message(type, senderId, receiverId, Message.Direction.SENT, false);
        messages.add(msg);
    }

    /**
     * Registra un evento del sistema (texto descriptivo).
     */
    public void logEvent(String description) {
        Map<String, Object> event = new HashMap<>();
        event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
        event.put("description", description);
        events.add(event);
    }

    /**
     * Marca el inicio de un proceso de elección (para calcular tiempo de convergencia).
     */
    public void startElectionTimer() {
        electionStartTime.set(System.currentTimeMillis());
    }

    /**
     * Registra la finalización de una elección con el nuevo coordinador.
     */
    public void recordElection(int coordinatorId) {
        long startTime = electionStartTime.get();
        long elapsed = startTime > 0 ? System.currentTimeMillis() - startTime : 0;

        Map<String, Object> record = new HashMap<>();
        record.put("coordinatorId", coordinatorId);
        record.put("convergenceTimeMs", elapsed);
        record.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
        record.put("totalMessagesInElection", messages.size());
        electionRecords.add(record);

        electionStartTime.set(0);
    }

    /**
     * Obtiene todos los mensajes registrados.
     */
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    /**
     * Obtiene todos los eventos del sistema.
     */
    public List<Map<String, Object>> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Calcula y devuelve estadísticas completas.
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalMessages = messages.size();
        long electionMsgs = messages.stream().filter(m -> m.getType() == Message.Type.ELECTION).count();
        long okMsgs = messages.stream().filter(m -> m.getType() == Message.Type.OK).count();
        long coordinatorMsgs = messages.stream().filter(m -> m.getType() == Message.Type.COORDINATOR).count();
        long failedMsgs = messages.stream().filter(m -> !m.isSuccess()).count();
        long successMsgs = messages.stream().filter(Message::isSuccess).count();

        stats.put("totalMessages", totalMessages);
        stats.put("electionMessages", electionMsgs);
        stats.put("okMessages", okMsgs);
        stats.put("coordinatorMessages", coordinatorMsgs);
        stats.put("failedMessages", failedMsgs);
        stats.put("successMessages", successMsgs);
        stats.put("elections", new ArrayList<>(electionRecords));
        stats.put("totalElections", electionRecords.size());

        return stats;
    }

    /**
     * Limpia todos los registros.
     */
    public void clearAll() {
        messages.clear();
        events.clear();
        electionRecords.clear();
        electionStartTime.set(0);
    }
}
