import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'
import TopologyGraph from '../components/TopologyGraph'

/**
 * The core screen: runs the blast-radius analysis for one incident.
 *
 * POST /api/incidents/{id}/blast-radius      -> BFS
 * POST /api/incidents/{id}/blast-radius/dfs  -> DFS
 *
 * Spring's response shape (see BlastRadiusService.persistBlastRadiusResult):
 *   { incidentId, rootComponent, traversal, affectedComponentCount,
 *     affectedServiceCount, estimatedAffectedCustomers,
 *     components: [{ componentCode, componentName, criticality, rootCause }],
 *     services:   [{ serviceCode, serviceName, estimatedCustomers }],
 *     visitedOrder: [...] }
 */
export default function IncidentDetail() {
  const { id } = useParams()
  const [incident, setIncident] = useState(null)
  const [edges, setEdges] = useState([])
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [running, setRunning] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let alive = true
    Promise.allSettled([Api.incident(id), Api.dependencies()])
      .then(([inc, dep]) => {
        if (!alive) return
        if (inc.status === 'fulfilled') setIncident(inc.value.data)
        else setError(errorText(inc.reason))
        if (dep.status === 'fulfilled' && Array.isArray(dep.value.data)) {
          setEdges(dep.value.data.map((d) => ({
            source: d.source?.componentCode,
            dependent: d.dependent?.componentCode,
          })).filter((e) => e.source && e.dependent))
        }
        setLoading(false)
      })
    return () => { alive = false }
  }, [id])

  async function run(mode) {
    setRunning(true)
    setError('')
    setResult(null)
    try {
      const { data } = mode === 'dfs' ? await Api.blastRadiusDfs(id) : await Api.blastRadiusBfs(id)
      setResult(data)
    } catch (e) {
      setError(errorText(e))
    } finally {
      setRunning(false)
    }
  }

  if (loading) return <Spinner label="Loading incident…" />

  const affectedCodes = (result?.components || [])
    .filter((c) => !c.rootCause)
    .map((c) => c.componentCode)
  const rootCode = result?.rootComponent || incident?.rootComponent?.componentCode

  return (
    <>
      <div className="page-head">
        <div>
          <Link to="/incidents" className="back-link">← Incidents</Link>
          <h1>{incident?.title || `Incident #${id}`}</h1>
          <p className="muted">
            <code>{incident?.incidentCode}</code>
            {incident?.rootComponent?.componentCode && <> · root <code>{incident.rootComponent.componentCode}</code></>}
          </p>
        </div>
        <div className="head-actions">
          <Badge value={incident?.severity} />
          <Badge value={incident?.incidentStatus} />
        </div>
      </div>

      <section className="card">
        <div className="card-head">
          <h2>Blast-radius analysis</h2>
          <div className="head-actions">
            <button className="btn btn-primary" disabled={running} onClick={() => run('bfs')}>
              {running ? 'Running…' : 'Run BFS'}
            </button>
            <button className="btn" disabled={running} onClick={() => run('dfs')}>
              {running ? 'Running…' : 'Run DFS'}
            </button>
          </div>
        </div>

        <p className="muted small">
          Spring pulls the topology from PostgreSQL, sends it to the FastAPI engine,
          persists the result, and returns it here.
        </p>

        <ErrorBox message={error} />

        {running && <Spinner label="Traversing the dependency graph…" />}

        {result && (
          <>
            <div className="stat-grid stat-grid-4">
              <div className="stat stat-bad">
                <span className="stat-value">{result.affectedComponentCount ?? 0}</span>
                <span className="stat-label">Affected components</span>
              </div>
              <div className="stat stat-warn">
                <span className="stat-value">{result.affectedServiceCount ?? 0}</span>
                <span className="stat-label">Affected services</span>
              </div>
              <div className="stat stat-bad">
                <span className="stat-value">
                  {(result.estimatedAffectedCustomers ?? 0).toLocaleString()}
                </span>
                <span className="stat-label">Estimated customers</span>
              </div>
              <div className="stat">
                <span className="stat-value">{result.traversal}</span>
                <span className="stat-label">Traversal used</span>
              </div>
            </div>

            <p className="note">
              Estimated customers sums each affected service's estimate. People who use
              more than one affected service are counted once per service, so this is an
              upper bound, not a unique subscriber count.
            </p>

            <h3>Dependency graph</h3>
            <TopologyGraph edges={edges} rootId={rootCode} affected={affectedCodes} />

            {result.visitedOrder?.length > 0 && (
              <>
                <h3>Traversal order</h3>
                <div className="chip-row">
                  <span className="chip chip-root">{rootCode}</span>
                  {result.visitedOrder.map((c, i) => (
                    <span key={`${c}-${i}`} className="chip chip-affected">{c}</span>
                  ))}
                </div>
              </>
            )}

            <div className="split">
              <div>
                <h3>Affected components</h3>
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Code</th><th>Name</th><th>Criticality</th><th>Role</th></tr></thead>
                    <tbody>
                      {(result.components || []).map((c) => (
                        <tr key={c.componentCode}>
                          <td><code>{c.componentCode}</code></td>
                          <td>{c.componentName}</td>
                          <td><Badge value={c.criticality} /></td>
                          <td>{c.rootCause
                            ? <span className="badge badge-bad">Root cause</span>
                            : <span className="badge badge-warn">Downstream</span>}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div>
                <h3>Affected services</h3>
                {(result.services || []).length === 0 ? (
                  <p className="muted">No services affected.</p>
                ) : (
                  <div className="table-wrap">
                    <table className="table">
                      <thead><tr><th>Code</th><th>Service</th><th>Customers</th></tr></thead>
                      <tbody>
                        {result.services.map((s) => (
                          <tr key={s.serviceCode}>
                            <td><code>{s.serviceCode}</code></td>
                            <td>{s.serviceName}</td>
                            <td>{Number(s.estimatedCustomers || 0).toLocaleString()}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>

            <details className="raw">
              <summary>Raw JSON response</summary>
              <pre>{JSON.stringify(result, null, 2)}</pre>
            </details>
          </>
        )}

        {!result && !running && !error && (
          <p className="muted">Run BFS or DFS to calculate the blast radius for this incident.</p>
        )}
      </section>
    </>
  )
}
