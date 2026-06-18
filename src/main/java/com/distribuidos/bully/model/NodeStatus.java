package com.distribuidos.bully.model;

/**
 * Representa el estado actual de un nodo en el sistema distribuido.
 * Se utiliza como respuesta del endpoint /api/status y para
 * construir la vista del cluster completo.
 */
public class NodeStatus {

    private int nodeId;
    private boolean active;
    private int coordinatorId;
    private boolean inElection;
    private boolean byzantine;

    public NodeStatus() {
    }

    public NodeStatus(int nodeId, boolean active, int coordinatorId, boolean inElection, boolean byzantine) {
        this.nodeId = nodeId;
        this.active = active;
        this.coordinatorId = coordinatorId;
        this.inElection = inElection;
        this.byzantine = byzantine;
    }

    // ==================== Getters y Setters ====================

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public void setCoordinatorId(int coordinatorId) {
        this.coordinatorId = coordinatorId;
    }

    public boolean isInElection() {
        return inElection;
    }

    public void setInElection(boolean inElection) {
        this.inElection = inElection;
    }

    public boolean isByzantine() {
        return byzantine;
    }

    public void setByzantine(boolean byzantine) {
        this.byzantine = byzantine;
    }

    @Override
    public String toString() {
        return "NodeStatus{" +
                "nodeId=" + nodeId +
                ", active=" + active +
                ", coordinatorId=" + coordinatorId +
                ", inElection=" + inElection +
                ", byzantine=" + byzantine +
                '}';
    }
}
