/**
 * ControlPanel - Botones para controlar el nodo local.
 * Permite iniciar elecciones, simular fallas y recuperar el nodo.
 */
function ControlPanel({ onAction, isActive, isInElection }) {
  return (
    <div className="control-panel">
      <div className="section-title">🎮 Panel de Control</div>
      <div className="control-buttons">
        <button
          className="btn btn-election"
          onClick={() => onAction('start-election')}
          disabled={!isActive || isInElection}
          title={!isActive ? 'Nodo inactivo' : isInElection ? 'Elección en curso' : 'Iniciar elección'}
        >
          🗳️ Iniciar Elección
        </button>

        <button
          className="btn btn-fail"
          onClick={() => onAction('fail')}
          disabled={!isActive}
          title={!isActive ? 'Ya está inactivo' : 'Simular falla del nodo'}
        >
          💀 Simular Falla
        </button>

        <button
          className="btn btn-recover"
          onClick={() => onAction('recover')}
          disabled={isActive}
          title={isActive ? 'Ya está activo' : 'Recuperar nodo'}
        >
          🔄 Recuperar Nodo
        </button>

        <button
          className="btn btn-clear"
          onClick={() => onAction('clear')}
          title="Limpiar registros de mensajes"
        >
          🗑️ Limpiar Logs
        </button>

        <button
          className="btn"
          style={{ backgroundColor: '#444', borderColor: '#ff4d4d', color: '#ff4d4d' }}
          onClick={() => onAction('byzantine')}
          title="Alternar comportamiento Bizantino"
        >
          😈 Alternar Bizantino
        </button>

        <button
          className="btn"
          style={{ backgroundColor: '#2d3748', borderColor: '#4299e1', color: '#63b3ed' }}
          onClick={() => onAction('consensus/start')}
          disabled={!isActive}
          title={!isActive ? 'Nodo inactivo' : 'Iniciar Consenso BFT'}
        >
          ⚖️ Iniciar Consenso
        </button>
      </div>
    </div>
  );
}

export default ControlPanel
