package com.distribuidos.bully.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un mensaje intercambiado durante el Algoritmo Bully.
 * 
 * Tipos de mensaje:
 * - ELECTION: Enviado por un proceso a procesos con ID mayor para iniciar elección.
 * - OK: Respuesta de un proceso con ID mayor indicando que tomará el control.
 * - COORDINATOR: Anuncio del nuevo coordinador a todos los procesos.
 */
public class Message {

    /**
     * Tipos de mensaje del Algoritmo Bully.
     */
    public enum Type {
        ELECTION,
        OK,
        COORDINATOR,
        CONSENSUS_PREPARE,
        CONSENSUS_VOTE
    }

    /**
     * Dirección del mensaje respecto al nodo local.
     */
    public enum Direction {
        SENT,
        RECEIVED
    }

    private Type type;
    private int senderId;
    private int receiverId;
    private Direction direction;
    private boolean success;
    private String timestamp;
    private String description;

    public Message() {
    }

    public Message(Type type, int senderId, int receiverId, Direction direction, boolean success) {
        this.type = type;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.direction = direction;
        this.success = success;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        this.description = buildDescription();
    }

    private String buildDescription() {
        String arrow = direction == Direction.SENT ? "" : "";
        String status = success ? "" : "";
        return String.format("[%s] P%d %s P%d %s", type, senderId, arrow, receiverId, status);
    }

    // ==================== Getters y Setters ====================

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
