import { useState, useEffect, useRef, useCallback } from 'react'
import ClusterView from './components/ClusterView'
import ControlPanel from './components/ControlPanel'
import StatsPanel from './components/StatsPanel'
import MessageLog from './components/MessageLog'
import ConsensusPanel from './components/ConsensusPanel'

const API_BASE = '/api';
const POLL_INTERVAL = 2000;

function App() {
  const [cluster, setCluster] = useState(null);
  const [messages, setMessages] = useState([]);
  const [events, setEvents] = useState([]);
  const [stats, setStats] = useState(null);
  const [consensusData, setConsensusData] = useState(null);
  const [connected, setConnected] = useState(true);
  const [loading, setLoading] = useState(true);

  // Polling: consultar estado del cluster, mensajes y stats
  const fetchData = useCallback(async () => {
    try {
      const [clusterRes, messagesRes, statsRes, consensusRes] = await Promise.all([
        fetch(`${API_BASE}/cluster`),
        fetch(`${API_BASE}/messages`),
        fetch(`${API_BASE}/stats`),
        fetch(`${API_BASE}/consensus/results`)
      ]);

      if (clusterRes.ok) {
        const clusterData = await clusterRes.json();
        setCluster(clusterData);
      }

      if (messagesRes.ok) {
        const msgData = await messagesRes.json();
        setMessages(msgData.messages || []);
        setEvents(msgData.events || []);
      }

      if (statsRes.ok) {
        const statsData = await statsRes.json();
        setStats(statsData);
      }

      if (consensusRes.ok) {
        const cData = await consensusRes.json();
        if (cData.transactionId) {
            setConsensusData(cData);
        }
      }

      setConnected(true);
      setLoading(false);
    } catch (error) {
      console.error('Error fetching data:', error);
      setConnected(false);
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchData]);

  // Acciones del panel de control
  const handleAction = async (action) => {
    try {
      const response = await fetch(`${API_BASE}/${action}`, { method: 'POST' });
      if (response.ok) {
        // Refrescar datos inmediatamente después de la acción
        setTimeout(fetchData, 500);
        setTimeout(fetchData, 1500);
        setTimeout(fetchData, 3000);
      }
    } catch (error) {
      console.error(`Error en acción ${action}:`, error);
    }
  };

  const localNodeId = cluster?.localNodeId || '?';
  const isActive = cluster?.localActive !== false;

  if (loading) {
    return (
      <div className="app">
        <div className="loading">
          <div className="loading-spinner"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <h1>⚡ Algoritmo Bully — Elección de Coordinador</h1>
        <p className="header-subtitle">
          Sistemas Distribuidos • Simulación en tiempo real
        </p>
        <div className={`header-node-badge ${isActive ? '' : 'inactive'}`}>
          <span className="pulse-dot"></span>
          Nodo P{localNodeId} • {isActive ? 'Activo' : 'Inactivo'}
          {cluster?.coordinatorId === localNodeId && ' • 👑 Coordinador'}
        </div>
      </header>

      {/* Error de conexión */}
      {!connected && (
        <div className="connection-error">
          ⚠️ No se puede conectar con el backend. Verifica que el servidor esté ejecutándose.
        </div>
      )}

      {/* Vista del Cluster (5 nodos) */}
      <ClusterView
        nodes={cluster?.nodes || []}
        localNodeId={localNodeId}
        coordinatorId={cluster?.coordinatorId}
      />

      {/* Panel de Control + Estadísticas */}
      <div className="controls-stats-row">
        <ControlPanel
          onAction={handleAction}
          isActive={isActive}
          isInElection={cluster?.inElection}
        />
        <StatsPanel stats={stats} />
      </div>

      <ConsensusPanel consensusData={consensusData} />

      {/* Log de Mensajes */}
      <MessageLog messages={messages} />

      {/* Eventos del Sistema */}
      {events.length > 0 && (
        <div className="events-section">
          <div className="section-title">📋 Eventos del Sistema</div>
          <div className="event-list">
            {[...events].reverse().map((event, index) => (
              <div key={index} className="event-item">
                <span className="event-time">{event.timestamp}</span>
                <span className="event-description">{event.description}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default App
