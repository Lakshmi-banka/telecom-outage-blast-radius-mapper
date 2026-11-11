import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import ErrorBox from '../components/ErrorBox'
import TopologyGraph from '../components/TopologyGraph'
import Badge from '../components/Badge'

export default function Network() {
  const [deps, setDeps] = useState([])
  const [services, setServices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  function load() {
    setLoading(true)
    Promise.allSettled([Api.dependencies(), Api.services()]).then(([d, s]) => {
      if (d.status === 'fulfilled') { setDeps(d.value.data || []); setError('') }
      else setError(errorText(d.reason))
      if (s.status === 'fulfilled') setServices(s.value.data || [])
      setLoading(false)
    })
  }

  useEffect(load, [])

  const edges = deps
    .map((d) => ({ source: d.source?.componentCode, dependent: d.dependent?.componentCode }))
    .filter((e) => e.source && e.dependent)

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Network topology</h1>
          <p className="muted">
            Active dependencies, drawn upstream → downstream. Traversal follows these arrows.
          </p>
        </div>
      </div>

      <ErrorBox message={error} onRetry={load} />

      {loading ? <Spinner /> : (
        <>
          <section className="card">
            <h2>Dependency graph ({edges.length} active edges)</h2>
            <TopologyGraph edges={edges} />
          </section>

          <div className="split">
            <section className="card">
              <h2>Dependencies</h2>
              {deps.length === 0 ? <p className="muted">No active dependencies.</p> : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Source</th><th>Depends on it</th><th>Type</th><th>Active</th></tr></thead>
                    <tbody>
                      {deps.map((d) => (
                        <tr key={d.id}>
                          <td><code>{d.source?.componentCode || '—'}</code></td>
                          <td><code>{d.dependent?.componentCode || '—'}</code></td>
                          <td className="muted">{d.dependencyType}</td>
                          <td><Badge value={d.active ? 'ACTIVE' : 'DOWN'} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="card">
              <h2>Services</h2>
              {services.length === 0 ? <p className="muted">No services.</p> : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>Code</th><th>Name</th><th>Customers</th><th>Status</th></tr></thead>
                    <tbody>
                      {services.map((s) => (
                        <tr key={s.id}>
                          <td><code>{s.serviceCode}</code></td>
                          <td>{s.serviceName}</td>
                          <td>{Number(s.estimatedCustomerCount || 0).toLocaleString()}</td>
                          <td><Badge value={s.status} /></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </div>
        </>
      )}
    </>
  )
}
