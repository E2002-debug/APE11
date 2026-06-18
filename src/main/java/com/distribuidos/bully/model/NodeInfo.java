package com.distribuidos.bully.model;

/**
 * Información de conexión de un nodo (proceso) en el sistema distribuido.
 * Almacena el ID único, dirección IP y puerto del nodo.
 */
public class NodeInfo {

    private int id;
    private String host;
    private int port;

    public NodeInfo() {
    }

    public NodeInfo(int id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    /**
     * Construye la URL base HTTP para comunicarse con este nodo.
     * Ejemplo: "http://192.168.1.101:8080"
     */
    public String getUrl() {
        return "http://" + host + ":" + port;
    }

    // ==================== Getters y Setters ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "NodeInfo{id=" + id + ", host='" + host + "', port=" + port + "}";
    }
}
