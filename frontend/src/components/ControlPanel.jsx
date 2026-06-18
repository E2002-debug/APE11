import { CheckSquare, XCircle, RefreshCw, Trash2, Cpu, Scale, Settings } from 'lucide-react';

/**
 * ControlPanel - Botones para controlar el nodo local.
 * Permite iniciar elecciones, simular fallas y recuperar el nodo.
 */
function ControlPanel({ onAction, isActive, isInElection }) {
  return (
    <div className="control-panel">
      <div className="section-title"><Settings size={18} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Panel de Control</div>
      <div className="control-buttons">
        <button
          className="btn btn-election"
          onClick={() => onAction('start-election')}
          disabled={!isActive || isInElection}
          title={!isActive ? 'Nodo inactivo' : isInElection ? 'Elección en curso' : 'Iniciar elección'}
        >
          <CheckSquare size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Iniciar Elección
        </button>

        <button
          className="btn btn-fail"
          onClick={() => onAction('fail')}
          disabled={!isActive}
          title={!isActive ? 'Ya está inactivo' : 'Simular falla del nodo'}
        >
          <XCircle size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Simular Falla
        </button>

        <button
          className="btn btn-recover"
          onClick={() => onAction('recover')}
          disabled={isActive}
          title={isActive ? 'Ya está activo' : 'Recuperar nodo'}
        >
          <RefreshCw size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Recuperar Nodo
        </button>

        <button
          className="btn btn-clear"
          onClick={() => onAction('clear')}
          title="Limpiar registros de mensajes"
        >
          <Trash2 size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Limpiar Logs
        </button>

        <button
          className="btn"
          style={{ backgroundColor: '#444', borderColor: '#ff4d4d', color: '#ff4d4d' }}
          onClick={() => onAction('byzantine')}
          title="Alternar comportamiento Bizantino"
        >
          <Cpu size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Alternar Bizantino
        </button>

        <button
          className="btn"
          style={{ backgroundColor: '#2d3748', borderColor: '#4299e1', color: '#63b3ed' }}
          onClick={() => onAction('consensus/start')}
          disabled={!isActive}
          title={!isActive ? 'Nodo inactivo' : 'Iniciar Consenso BFT'}
        >
          <Scale size={16} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Iniciar Consenso
        </button>
      </div>
    </div>
  );
}

export default ControlPanel
