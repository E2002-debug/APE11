import React from 'react';
import { ShieldCheck } from 'lucide-react';

function ConsensusPanel({ consensusData }) {
  if (!consensusData || !consensusData.transactionId) {
    return null;
  }

  const { transactionId, votes, decision, yesVotes, noVotes, totalNodes } = consensusData;

  return (
    <div className="consensus-panel" style={{
        marginTop: '20px',
        background: 'var(--bg-secondary)',
        borderRadius: '8px',
        padding: '16px',
        border: '1px solid var(--border-color)',
        boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)'
    }}>
      <div className="section-title" style={{ marginBottom: '12px', borderBottom: '1px solid var(--border-color)', paddingBottom: '8px' }}>
        <ShieldCheck size={18} style={{marginRight: '6px', verticalAlign: 'middle'}}/>Resultados de Consenso BFT
      </div>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div>
          <strong>Transacción:</strong> <span style={{ fontFamily: 'monospace', background: 'rgba(0,0,0,0.2)', padding: '2px 6px', borderRadius: '4px' }}>{transactionId}</span>
        </div>
        <div>
          <strong>Decisión Final:</strong> {' '}
          <span style={{ 
              fontWeight: 'bold', 
              color: decision.startsWith('SI') ? '#48bb78' : decision.startsWith('NO') ? '#f56565' : '#ecc94b',
              background: 'rgba(0,0,0,0.2)', padding: '2px 6px', borderRadius: '4px'
          }}>
            {decision}
          </span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: '12px', marginBottom: '16px' }}>
        <div style={{ background: 'rgba(0,0,0,0.2)', padding: '10px', borderRadius: '6px', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#48bb78' }}>{yesVotes}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Votos SI</div>
        </div>
        <div style={{ background: 'rgba(0,0,0,0.2)', padding: '10px', borderRadius: '6px', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#f56565' }}>{noVotes}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Votos NO</div>
        </div>
        <div style={{ background: 'rgba(0,0,0,0.2)', padding: '10px', borderRadius: '6px', textAlign: 'center' }}>
          <div style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#a0aec0' }}>{totalNodes - (yesVotes + noVotes)}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Pendientes</div>
        </div>
      </div>

      <div>
        <strong style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>Votos Recibidos (Vista local):</strong>
        <div style={{ 
            display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px'
        }}>
          {Object.entries(votes).map(([nodeId, vote]) => (
            <div key={nodeId} style={{ 
                background: vote ? 'rgba(72, 187, 120, 0.2)' : 'rgba(245, 101, 101, 0.2)',
                border: `1px solid ${vote ? '#48bb78' : '#f56565'}`,
                padding: '4px 8px',
                borderRadius: '4px',
                fontSize: '0.85rem'
            }}>
              P{nodeId}: <strong>{vote ? 'SI' : 'NO'}</strong>
            </div>
          ))}
          {Object.keys(votes).length === 0 && (
            <span style={{ color: 'var(--text-muted)', fontStyle: 'italic', fontSize: '0.85rem' }}>No se han recibido votos aún.</span>
          )}
        </div>
      </div>
    </div>
  );
}

export default ConsensusPanel;
