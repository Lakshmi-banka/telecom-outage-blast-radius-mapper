import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import ErrorBox from '../components/ErrorBox'
import TopologyGraph from '../components/TopologyGraph'

/**
 * POST /api/simulations/outage
 * body: { incidentId, failedComponentIds: [...codes], traversal: "bfs"|"dfs" }
 *
 * This is a what-if: it does NOT write to the database. It asks the engine
 * what would break if the selected components failed together.
 */
export default function Simulation() {
  const [incidents, setIncidents] = useState([])
  const [components, setComponents] = useState([])
  const [deps, setDeps] = useState([])
  const [incidentId, setIncidentId] = useState('')
  const [selected, setSelected] = useState([])
  const [traversal, setTraversal] = useState('bfs')
  const [result, setResult] = useState(null)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([Api.incidents(), Api.components(), Api.dependencies()])
      .then(([inc, comp, dep]) => {
        if (inc.status === 'fulfilled') {
          const list = inc.value.data || []
          setIncidents(list)
          if (list.length) setIncidentId(String(list[0].id))
        }
        if (comp.status === 'fulfilled') setComponents(comp.value.data || [])
        if (dep.status === 'fulfilled') setDeps(dep.value.data || [])
        setLoading(false)
      })
  }, [])

  function toggle(code) {
    setSelected((prev) => prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code])
  }

  async function run() {
    if (!incidentId || selected.length === 0) {
      setError('Pick an incident and at least one component to fail.')
      return
    }
    setRunning(true)
    setError('')
    setResult(null)
    try {
      const { data } = await Api.simulate(Number(incidentId), selected, traversal)
      setResult(data)
    } catch (e) {
      setError(errorText(e))
    } finally {
      setRunning(false)
    }
  }

  const edges = deps
    .map((d) => ({ source: d.source?.componentCode, dependent: d.dependent?.componentCode }))
    .filter((e) => e.source && e.dependent)

  if (loading) return <Spinner />

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Outage simulation</h1>
          <p className="muted">
            What-if analysis. Nothing is written to the database.
          </p>
        </div>
      </div>

      <ErrorBox message={error} />

      <section className="card">
        <h2>Configure</h2>
        <div className="form-grid">
          <label>Incident context
            <select value={incidentId} onChange={(e) => setIncidentId(e.target.value)}>
              {incidents.map((i) => (
                <option key={i.id} value={i.id}>{i.incidentCode} — {i.title}</option>
              ))}
            </select>
          </label>
          <label>Traversal
            <select value={traversal} onChange={(e) => setTraversal(e.target.value)}>
              <option value="bfs">bfs</option>
              <option value="dfs">dfs</option>
            </select>
          </label>
        </div>

        <h3>Failed components ({selected.length} selected)</h3>
        <div className="chip-picker">
          {components.map((c) => (
            <button key={c.id} type="button"
                    className={selected.includes(c.componentCode) ? 'chip chip-root' : 'chip'}
                    onClick={() => toggle(c.componentCode)}>
              {c.componentCode}
            </button>
          ))}
        </div>

        <div className="head-actions" style={{ marginTop: '1rem' }}>
          <button className="btn btn-primary" disabled={running} onClick={run}>
            {running ? 'Simulating…' : 'Run simulation'}
          </button>
          <button className="btn btn-ghost" onClick={() => { setSelected([]); setResult(null) }}>
            Clear
          </button>
        </div>
      </section>

      {running && <Spinner label="Running the what-if…" />}

      {result && (
        <section className="card">
          <h2>Simulation result</h2>
          <div className="stat-grid stat-grid-4">
            <div className="stat stat-bad">
              <span className="stat-value">{result.combined_affected_component_count ?? 0}</span>
              <span className="stat-label">Components affected</span>
            </div>
            <div className="stat stat-warn">
              <span className="stat-value">{result.combined_affected_service_count ?? 0}</span>
              <span className="stat-label">Services affected</span>
            </div>
            <div className="stat stat-bad">
              <span className="stat-value">
                {(result.estimated_affected_customers ?? 0).toLocaleString()}
              </span>
              <span className="stat-label">Estimated customers</span>
            </div>
            <div className="stat">
              <span className="stat-value">{result.traversal}</span>
              <span className="stat-label">Traversal</span>
            </div>
          </div>

          <p className="note">
            Each affected service is counted once, but subscribers who use several
            affected services are still counted more than once.
          </p>

          <h3>Impact on the topology</h3>
          <TopologyGraph
            edges={edges}
            rootId={selected[0]}
            affected={result.combined_affected_component_ids || []}
          />

          <h3>Affected component IDs</h3>
          <div className="chip-row">
            {(result.combined_affected_component_ids || []).map((c) => (
              <span key={c} className="chip chip-affected">{c}</span>
            ))}
          </div>

          {(result.combined_affected_services || []).length > 0 && (
            <>
              <h3>Affected services</h3>
              <div className="table-wrap">
                <table className="table">
                  <thead><tr><th>Service</th><th>Name</th><th>Customers</th><th>Via components</th></tr></thead>
                  <tbody>
                    {result.combined_affected_services.map((s) => (
                      <tr key={s.service_id}>
                        <td><code>{s.service_id}</code></td>
                        <td>{s.service_name}</td>
                        <td>{Number(s.estimated_customer_count || 0).toLocaleString()}</td>
                        <td className="muted">{(s.affected_component_ids || []).join(', ')}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}

          <details className="raw">
            <summary>Raw JSON response</summary>
            <pre>{JSON.stringify(result, null, 2)}</pre>
          </details>
        </section>
      )}
    </>
  )
}
