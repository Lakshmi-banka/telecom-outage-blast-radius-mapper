import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'

export default function Alarms() {
  const [alarms, setAlarms] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('ALL')

  const [windowMinutes, setWindowMinutes] = useState(10)
  const [groupBy, setGroupBy] = useState('component')
  const [correlation, setCorrelation] = useState(null)
  const [correlating, setCorrelating] = useState(false)

  function load() {
    setLoading(true)
    const call = status === 'ALL' ? Api.alarms() : Api.alarmsByStatus(status)
    call
      .then((r) => { setAlarms(r.data || []); setError('') })
      .catch((e) => setError(errorText(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [status])

  async function correlate() {
    setCorrelating(true)
    setError('')
    setCorrelation(null)
    try {
      const { data } = await Api.correlateAlarms(Number(windowMinutes), groupBy)
      setCorrelation(data)
    } catch (e) {
      setError(errorText(e))
    } finally {
      setCorrelating(false)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Alarms</h1>
          <p className="muted">Raw alarms, and time-window correlation via the Python engine.</p>
        </div>
      </div>

      <ErrorBox message={error} onRetry={load} />

      <section className="card">
        <div className="card-head">
          <h2>Alarm correlation</h2>
          <div className="head-actions">
            <label className="inline-field">
              Window (min)
              <input type="number" min="1" max="1440" value={windowMinutes}
                     onChange={(e) => setWindowMinutes(e.target.value)} />
            </label>
            <label className="inline-field">
              Group by
              <select value={groupBy} onChange={(e) => setGroupBy(e.target.value)}>
                <option value="component">component</option>
                <option value="component_and_type">component_and_type</option>
              </select>
            </label>
            <button className="btn btn-primary" disabled={correlating} onClick={correlate}>
              {correlating ? 'Correlating…' : 'Correlate'}
            </button>
          </div>
        </div>

        <p className="muted small">
          Groups alarms whose consecutive timestamps fall inside the window. A deterministic
          rule-based baseline — it suggests a likely first alarm, it does not prove root cause.
        </p>

        {correlating && <Spinner label="Grouping alarms…" />}

        {correlation && (
          <>
            <div className="stat-grid stat-grid-4">
              <div className="stat"><span className="stat-value">{correlation.input_alarm_count ?? 0}</span><span className="stat-label">Alarms in</span></div>
              <div className="stat stat-warn"><span className="stat-value">{correlation.correlated_group_count ?? 0}</span><span className="stat-label">Groups found</span></div>
              <div className="stat"><span className="stat-value">{correlation.correlation_window_minutes ?? 0}m</span><span className="stat-label">Window</span></div>
              <div className="stat"><span className="stat-value small-val">{correlation.group_by}</span><span className="stat-label">Grouped by</span></div>
            </div>

            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr><th>Key</th><th>Component</th><th>Alarms</th><th>Codes</th>
                      <th>Highest severity</th><th>Open</th><th>Suggested first</th></tr>
                </thead>
                <tbody>
                  {(correlation.groups || []).map((g, i) => (
                    <tr key={i}>
                      <td><code>{g.correlation_key}</code></td>
                      <td>{g.component_id}</td>
                      <td>{g.alarm_count}</td>
                      <td className="muted">{(g.alarm_codes || []).join(', ')}</td>
                      <td><Badge value={g.highest_severity} /></td>
                      <td>{g.open_alarm_count}</td>
                      <td className="muted">{g.suggested_root_alarm_id}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </section>

      <section className="card">
        <div className="card-head">
          <h2>Alarm list</h2>
          <select className="inline-select" value={status} onChange={(e) => setStatus(e.target.value)}>
            {['ALL', 'OPEN', 'ACKNOWLEDGED', 'CLEARED'].map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        {loading ? <Spinner /> : alarms.length === 0 ? (
          <p className="muted">No alarms for this filter.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Code</th><th>Component</th><th>Type</th><th>Severity</th>
                    <th>Status</th><th>Occurred</th></tr>
              </thead>
              <tbody>
                {alarms.map((a) => (
                  <tr key={a.id}>
                    <td><code>{a.alarmCode}</code></td>
                    <td>{a.component?.componentCode || '—'}</td>
                    <td className="muted">{a.alarmType || '—'}</td>
                    <td><Badge value={a.severity} /></td>
                    <td><Badge value={a.alarmStatus} /></td>
                    <td className="muted small">
                      {a.occurredAt ? String(a.occurredAt).replace('T', ' ').slice(0, 16) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
