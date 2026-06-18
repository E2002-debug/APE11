/**
 * ClusterView - Visualización de los 5 nodos del sistema distribuido.
 * Muestra cada nodo como una tarjeta con su estado, rol e información.
 */
function ClusterView({ nodes, localNodeId, coordinatorId }) {
  if (!nodes || nodes.length === 0) {
    return (
      <div className="cluster-section">
        <div className="section-title">🖥️ Cluster de Procesos</div>
        <div className="empty-state">
          <div className="empty-icon">🔄</div>
          <p>Cargando nodos del cluster...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="cluster-section">
      <div className="section-title">🖥️ Cluster de Procesos</div>
      <div className="cluster-grid">
        {nodes.map((node) => {
          const isCoordinator = node.nodeId === coordinatorId;
          const isLocal = node.nodeId === localNodeId;
          const isActive = node.active;
          const isInElection = node.inElection;

          let cardClass = 'node-card';
          if (isActive) cardClass += ' active';
          if (!isActive) cardClass += ' inactive';
          if (isCoordinator && isActive) cardClass += ' coordinator';
          if (isInElection) cardClass += ' in-election';
          if (isLocal) cardClass += ' local';

          // Determinar el rol
          let roleText, roleClass;
          if (!isActive) {
            roleText = '● INACTIVO';
            roleClass = 'inactive';
          } else if (isInElection) {
            roleText = '⚡ EN ELECCIÓN';
            roleClass = 'electing';
          } else if (isCoordinator) {
            roleText = '👑 COORDINADOR';
            roleClass = 'coordinator';
          } else {
            roleText = '● PROCESO';
            roleClass = 'process';
          }

          // Icono del nodo
          let icon;
          if (!isActive) icon = '💀';
          else if (node.byzantine) icon = '😈';
          else if (isCoordinator) icon = '👑';
          else if (isInElection) icon = '⚡';
          else icon = '🖥️';

          return (
            <div key={node.nodeId} className={cardClass}>
              <span className="node-icon">{icon}</span>
              <div className="node-name">P{node.nodeId}</div>
              <span className={`node-role ${roleClass}`}>{roleText}</span>
              {node.byzantine && isActive && (
                <div style={{ marginTop: '4px', fontSize: '0.75rem', fontWeight: 'bold', color: '#ff4d4d' }}>
                  [ BIZANTINO ]
                </div>
              )}
              <div style={{ marginTop: '6px' }}>
                <span className={`node-status-dot ${isActive ? 'active' : 'inactive'}`}></span>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                  {isActive ? 'En línea' : 'Fuera de línea'}
                </span>
              </div>
              {isActive && node.coordinatorId > 0 && (
                <div style={{
                  fontSize: '0.72rem',
                  color: 'var(--text-muted)',
                  marginTop: '4px'
                }}>
                  Coord. conocido: P{node.coordinatorId}
                </div>
              )}
              {isLocal && (
                <div className="node-local-tag">◆ Este nodo</div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default ClusterView
