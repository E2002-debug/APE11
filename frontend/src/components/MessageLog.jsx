import { useState, useEffect, useRef } from 'react'

/**
 * MessageLog - Log en tiempo real de mensajes del algoritmo Bully.
 * Muestra mensajes ELECTION, OK y COORDINATOR con color-coding,
 * dirección, timestamps y estado de éxito/fallo.
 * Incluye filtros por tipo de mensaje.
 */
function MessageLog({ messages }) {
  const [filter, setFilter] = useState('ALL');
  const listRef = useRef(null);
  const prevLengthRef = useRef(0);

  // Auto-scroll al final cuando llegan nuevos mensajes
  useEffect(() => {
    if (messages.length > prevLengthRef.current && listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
    prevLengthRef.current = messages.length;
  }, [messages]);

  const filteredMessages = filter === 'ALL'
    ? messages
    : messages.filter(m => m.type === filter);

  const filters = [
    { key: 'ALL', label: 'Todos' },
    { key: 'ELECTION', label: 'Election' },
    { key: 'OK', label: 'OK' },
    { key: 'COORDINATOR', label: 'Coordinator' },
  ];

  return (
    <div className="message-log-section">
      <div className="message-log-header">
        <div className="section-title" style={{ marginBottom: 0 }}>
          💬 Mensajes Intercambiados
          <span style={{
            marginLeft: '8px',
            fontSize: '0.75rem',
            color: 'var(--accent-primary)',
            fontFamily: 'var(--font-mono)'
          }}>
            ({messages.length})
          </span>
        </div>
        <div className="message-filters">
          {filters.map(f => (
            <button
              key={f.key}
              className={`filter-btn ${filter === f.key ? 'active' : ''}`}
              onClick={() => setFilter(f.key)}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {filteredMessages.length === 0 ? (
        <div className="empty-state">
          <div className="empty-icon">💬</div>
          <p>No hay mensajes registrados aún.</p>
          <p style={{ fontSize: '0.78rem', marginTop: '4px', color: 'var(--text-muted)' }}>
            Inicia una elección o espera la detección automática de fallas.
          </p>
        </div>
      ) : (
        <div className="message-list" ref={listRef}>
          {filteredMessages.map((msg, index) => (
            <div key={index} className="message-item">
              <span className="msg-time">{msg.timestamp}</span>
              <span className={`msg-type ${msg.type}`}>{msg.type}</span>
              <span className="msg-flow">
                <span>P{msg.senderId}</span>
                {' '}
                {msg.direction === 'SENT' ? '→' : '←'}
                {' '}
                <span>P{msg.receiverId}</span>
              </span>
              <span className={`msg-direction ${msg.direction}`}>
                {msg.direction === 'SENT' ? '📤 Enviado' : '📥 Recibido'}
              </span>
              <span className={`msg-status ${msg.success ? 'success' : 'failed'}`}>
                {msg.success ? '✓' : '✗'}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default MessageLog
