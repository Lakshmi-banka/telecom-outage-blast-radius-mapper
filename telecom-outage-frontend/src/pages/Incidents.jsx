import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'
import { useAuth } from '../auth/AuthContext'

const BLANK = { incidentCode: '', title: '', severity: 'HIGH', rootComponentId: '', description: '' }

export default function Incidents() {
  const { hasRole } = useAuth()
  const [incidents, setIncidents] = useState([])
  const [components, setComponents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)

  const canCreate = hasRole('ADMIN', 'NOC_MANAGER', 'NOC_ANALYST')
  const canSetStatus = hasRole('ADMIN', 'NOC_MANAGER')

  function load() {
    setLoading(true)
    setError('')
    Promise.allSettled([Api.incidents(), Api.components()]).then(([inc, comp]) => {
      if (inc.status === 'fulfilled') setIncidents(inc.value.data || [])
      else setError(errorText(inc.reason))
      if (comp.status === 'fulfilled') setComponents(comp.value.data || [])
      setLoading(false)
    })
  }

  useEffect(load, [])

  async function create(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await Api.createIncident({ ...form, rootComponentId: Number(form.rootComponentId) })
      setForm(BLANK)
      setShowForm(false)
      load()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  async function changeStatus(id, status) {
    setError('')
    try {
      await Api.updateIncidentStatus(id, status)
      load()
    } catch (err) {
      setError(errorText(err))
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Incidents</h1>
          <p className="muted">Create an incident, then run its blast-radius analysis.</p>
        </div>
        {canCreate && (
          <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancel' : 'New incident'}
          </button>
        )}
      </div>

      <ErrorBox message={error} onRetry={load} />

      {showForm && canCreate && (
        <section className="card">
          <h2>New incident</h2>
          <form className="form-grid" onSubmit={create}>
            <label>Incident code
              <input required value={form.incidentCode} placeholder="INC-2026-004"
                     onChange={(e) => setForm({ ...form, incidentCode: e.target.value })} />
            </label>
            <label>Title
              <input required value={form.title} placeholder="Core router failure"
                     onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </label>
            <label>Severity
              <select value={form.severity} onChange={(e) => setForm({ ...form, severity: e.target.value })}>
                {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label>Root component
              <select required value={form.rootComponentId}
                      onChange={(e) => setForm({ ...form, rootComponentId: e.target.value })}>
                <option value="">Select…</option>
                {components.map((c) => (
                  <option key={c.id} value={c.id}>{c.componentCode} — {c.componentName}</option>
                ))}
              </select>
            </label>
            <label className="span-2">Description
              <textarea rows="3" value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
            <div className="span-2">
              <button className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Create incident'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="card">
        {loading ? <Spinner /> : incidents.length === 0 ? (
          <p className="muted">No incidents yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th><th>Title</th><th>Severity</th><th>Status</th>
                  <th>Root component</th><th>Started</th><th />
                </tr>
              </thead>
              <tbody>
                {incidents.map((i) => (
                  <tr key={i.id}>
                    <td><code>{i.incidentCode}</code></td>
                    <td>{i.title}</td>
                    <td><Badge value={i.severity} /></td>
                    <td>
                      {canSetStatus ? (
                        <select className="inline-select" value={i.incidentStatus}
                                onChange={(e) => changeStatus(i.id, e.target.value)}>
                          {['OPEN', 'INVESTIGATING', 'IN_PROGRESS', 'RESOLVED'].map((s) => <option key={s}>{s}</option>)}
                        </select>
                      ) : <Badge value={i.incidentStatus} />}
                    </td>
                    <td>{i.rootComponent?.componentCode || '—'}</td>
                    <td className="muted small">{i.startedAt ? String(i.startedAt).replace('T', ' ').slice(0, 16) : '—'}</td>
                    <td><Link className="btn btn-sm btn-primary" to={`/incidents/${i.id}`}>Analyse</Link></td>
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
