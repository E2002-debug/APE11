/**
 * StatsPanel - Estadísticas del algoritmo Bully.
 * Muestra conteos de mensajes por tipo, coordinador actual y tiempo de convergencia.
 */
function StatsPanel({ stats }) {
  if (!stats) {
    return (
      <div className="stats-panel">
        <div className="section-title"> Estadísticas</div>
        <div className="empty-state">
          <div className="empty-icon"></div>
          <p>Cargando estadísticas...</p>
        </div>
      </div>
    );
  }

  // Obtener el último tiempo de convergencia
  const lastElection = stats.elections && stats.elections.length > 0
    ? stats.elections[stats.elections.length - 1]
    : null;
  const convergenceTime = lastElection
    ? `${lastElection.convergenceTimeMs}ms`
    : '';

  return (
    <div className="stats-panel">
      <div className="section-title"> Estadísticas</div>
      <div className="stats-grid">
        <div className="stat-item">
          <div className="stat-value total">{stats.totalMessages || 0}</div>
          <div className="stat-label">Total Mensajes</div>
        </div>
        <div className="stat-item">
          <div className="stat-value election">{stats.electionMessages || 0}</div>
          <div className="stat-label">Election</div>
        </div>
        <div className="stat-item">
          <div className="stat-value ok">{stats.okMessages || 0}</div>
          <div className="stat-label">OK</div>
        </div>
        <div className="stat-item">
          <div className="stat-value coordinator-stat">{stats.coordinatorMessages || 0}</div>
          <div className="stat-label">Coordinator</div>
        </div>
        <div className="stat-item">
          <div className="stat-value failed">{stats.failedMessages || 0}</div>
          <div className="stat-label">Fallidos</div>
        </div>
        <div className="stat-item">
          <div className="stat-value time">{convergenceTime}</div>
          <div className="stat-label">Convergencia</div>
        </div>
      </div>

      {/* Historial de elecciones */}
      {stats.elections && stats.elections.length > 0 && (
        <div style={{ marginTop: '14px' }}>
          <div style={{
            fontSize: '0.72rem',
            color: 'var(--text-secondary)',
            textTransform: 'uppercase',
            letterSpacing: '0.08em',
            fontWeight: 600,
            marginBottom: '8px'
          }}>
            Historial de Elecciones
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {stats.elections.map((election, idx) => (
              <div key={idx} style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '6px 10px',
                background: 'rgba(255,255,255,0.02)',
                borderRadius: '6px',
                fontSize: '0.78rem'
              }}>
                <span style={{ color: 'var(--text-muted)' }}>
                  #{idx + 1}  {election.timestamp}
                </span>
                <span>
                  <span style={{ color: 'var(--gold)', fontWeight: 700 }}>
                    P{election.coordinatorId}
                  </span>
                  <span style={{ color: 'var(--text-muted)', marginLeft: '8px' }}>
                    {election.convergenceTimeMs}ms
                  </span>
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default StatsPanel
