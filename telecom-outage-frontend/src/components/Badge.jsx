// Colour-codes the status / severity / criticality strings the backend uses.
const MAP = {
  UP: 'ok', ACTIVE: 'ok', RESOLVED: 'ok', CLEARED: 'ok', COMPLETED: 'ok', LOW: 'ok',
  DEGRADED: 'warn', MEDIUM: 'warn', IN_PROGRESS: 'warn', OPEN: 'warn',
  ACKNOWLEDGED: 'warn', INVESTIGATING: 'warn', ESTIMATED: 'warn', PARTIAL: 'warn',
  DOWN: 'bad', CRITICAL: 'bad', HIGH: 'bad', MAJOR: 'bad', FAILED: 'bad',
  MAINTENANCE: 'info', UNKNOWN: 'info', CONFIRMED: 'info',
}

export default function Badge({ value }) {
  if (value === null || value === undefined || value === '') return <span className="muted">—</span>
  const text = String(value)
  const tone = MAP[text.toUpperCase()] || 'info'
  return <span className={`badge badge-${tone}`}>{text}</span>
}
