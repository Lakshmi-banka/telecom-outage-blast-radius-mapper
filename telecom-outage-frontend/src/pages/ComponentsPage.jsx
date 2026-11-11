import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'
import { useAuth } from '../auth/AuthContext'

const BLANK = {
  componentCode: '', componentName: '', componentType: 'ROUTER',
  location: '', latitude: '', longitude: '', status: 'UP', criticality: 'MEDIUM',
}

export default function ComponentsPage() {
  const { hasRole } = useAuth()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [filter, setFilter] = useState('')

  const canEdit = hasRole('ADMIN', 'NETWORK_ENGINEER')

  function load() {
    setLoading(true)
    Api.components()
      .then((r) => { setRows(r.data || []); setError('') })
      .catch((e) => setError(errorText(e)))
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function create(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await Api.createComponent({
        ...form,
        latitude: form.latitude === '' ? null : Number(form.latitude),
        longitude: form.longitude === '' ? null : Number(form.longitude),
      })
      setForm(BLANK)
      setShowForm(false)
      load()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  async function setStatus(id, status) {
    setError('')
    try { await Api.updateComponentStatus(id, status); load() }
    catch (e) { setError(errorText(e)) }
  }

  const visible = rows.filter((c) => {
    if (!filter) return true
    const q = filter.toLowerCase()
    return [c.componentCode, c.componentName, c.componentType, c.location]
      .some((v) => String(v || '').toLowerCase().includes(q))
  })

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Network components</h1>
          <p className="muted">{rows.length} component(s) in the inventory.</p>
        </div>
        {canEdit && (
          <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancel' : 'Add component'}
          </button>
        )}
      </div>

      <ErrorBox message={error} onRetry={load} />

      {showForm && canEdit && (
        <section className="card">
          <h2>New component</h2>
          <form className="form-grid" onSubmit={create}>
            <label>Code
              <input required value={form.componentCode} placeholder="BTS-006"
                     onChange={(e) => setForm({ ...form, componentCode: e.target.value })} />
            </label>
            <label>Name
              <input required value={form.componentName} placeholder="Base Tower 006"
                     onChange={(e) => setForm({ ...form, componentName: e.target.value })} />
            </label>
            <label>Type
              <select value={form.componentType}
                      onChange={(e) => setForm({ ...form, componentType: e.target.value })}>
                {['ROUTER', 'SWITCH', 'BTS', 'FIBER', 'AGGREGATOR', 'OTHER'].map((t) => <option key={t}>{t}</option>)}
              </select>
            </label>
            <label>Location
              <input value={form.location} placeholder="Hyderabad"
                     onChange={(e) => setForm({ ...form, location: e.target.value })} />
            </label>
            <label>Latitude
              <input type="number" step="any" value={form.latitude}
                     onChange={(e) => setForm({ ...form, latitude: e.target.value })} />
            </label>
            <label>Longitude
              <input type="number" step="any" value={form.longitude}
                     onChange={(e) => setForm({ ...form, longitude: e.target.value })} />
            </label>
            <label>Status
              <select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                {['UP', 'DOWN', 'DEGRADED', 'MAINTENANCE'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label>Criticality
              <select value={form.criticality} onChange={(e) => setForm({ ...form, criticality: e.target.value })}>
                {['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <div className="span-2">
              <button className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Create component'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="card">
        <div className="card-head">
          <h2>Inventory</h2>
          <input className="search" placeholder="Filter…" value={filter}
                 onChange={(e) => setFilter(e.target.value)} />
        </div>
        {loading ? <Spinner /> : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th>Code</th><th>Name</th><th>Type</th><th>Location</th><th>Criticality</th><th>Status</th></tr>
              </thead>
              <tbody>
                {visible.map((c) => (
                  <tr key={c.id}>
                    <td><code>{c.componentCode}</code></td>
                    <td>{c.componentName}</td>
                    <td className="muted">{c.componentType}</td>
                    <td className="muted">{c.location || '—'}</td>
                    <td><Badge value={c.criticality} /></td>
                    <td>
                      {canEdit ? (
                        <select className="inline-select" value={c.status}
                                onChange={(e) => setStatus(c.id, e.target.value)}>
                          {['UP', 'DOWN', 'DEGRADED', 'MAINTENANCE'].map((s) => <option key={s}>{s}</option>)}
                        </select>
                      ) : <Badge value={c.status} />}
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
