import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'
import { useAuth } from '../auth/AuthContext'

const BLANK = {
  incidentId: '', componentId: '', actionDescription: '',
  previousStatus: 'DOWN', newStatus: 'UP', restorationStatus: 'VERIFICATION_PENDING',
}

export default function Restorations() {
  const { hasRole } = useAuth()
  const [rows, setRows] = useState([])
  const [incidents, setIncidents] = useState([])
  const [components, setComponents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [form, setForm] = useState(BLANK)
  const [showForm, setShowForm] = useState(false)
  const [saving, setSaving] = useState(false)
  const [verifyingId, setVerifyingId] = useState(null)

  const canCreate = hasRole('ADMIN', 'NOC_MANAGER', 'NETWORK_ENGINEER')
  const canVerify = hasRole('ADMIN', 'NOC_MANAGER')

  async function load() {
    setLoading(true)
    const [r, i, c] = await Promise.allSettled([
      Api.restorations(), Api.incidents(), Api.components()
    ])
    if (r.status === 'fulfilled') {
      setRows(Array.isArray(r.value.data) ? r.value.data : [])
      setError('')
    } else setError(errorText(r.reason))
    if (i.status === 'fulfilled') setIncidents(i.value.data || [])
    if (c.status === 'fulfilled') setComponents(c.value.data || [])
    setLoading(false)
  }

  useEffect(() => { load() }, [])

  async function create(e) {
    e.preventDefault()
    setSaving(true)
    setError('')
    try {
      await Api.createRestoration({
        ...form,
        incidentId: Number(form.incidentId),
        componentId: form.componentId === '' ? null : Number(form.componentId),
        restorationStatus: 'VERIFICATION_PENDING',
      })
      setForm(BLANK)
      setShowForm(false)
      await load()
    } catch (err) {
      setError(errorText(err))
    } finally {
      setSaving(false)
    }
  }

  async function verify(row, approved) {
    const notes = window.prompt(
      approved ? 'Enter verification notes:' : 'Enter rejection reason:'
    )
    if (notes === null) return
    if (!notes.trim()) {
      setError('Please enter verification notes or a rejection reason.')
      return
    }
    setVerifyingId(row.id)
    setError('')
    try {
      await Api.verifyRestoration(row.id, approved, notes.trim())
      await load()
    } catch (e) {
      setError(errorText(e))
    } finally {
      setVerifyingId(null)
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Restoration management</h1>
          <p className="muted">Engineers submit completed repair work for verification. Authorized reviewers approve or reject it.</p>
        </div>
        {canCreate && (
          <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancel' : 'Submit restoration'}
          </button>
        )}
      </div>

      <ErrorBox message={error} onRetry={load} />

      {showForm && canCreate && (
        <section className="card">
          <h2>Submit restoration for verification</h2>
          <form className="form-grid" onSubmit={create}>
            <label>Incident
              <select required value={form.incidentId}
                onChange={(e) => setForm({ ...form, incidentId: e.target.value })}>
                <option value="">Select incident…</option>
                {incidents.map((i) => <option key={i.id} value={i.id}>{i.incidentCode} — {i.title}</option>)}
              </select>
            </label>
            <label>Component
              <select required value={form.componentId}
                onChange={(e) => setForm({ ...form, componentId: e.target.value })}>
                <option value="">Select component…</option>
                {components.map((c) => <option key={c.id} value={c.id}>{c.componentCode} — {c.componentName}</option>)}
              </select>
            </label>
            <label>Previous status
              <select value={form.previousStatus}
                onChange={(e) => setForm({ ...form, previousStatus: e.target.value })}>
                {['DOWN', 'DEGRADED', 'MAINTENANCE', 'UP'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label>Expected status after approval
              <select value={form.newStatus}
                onChange={(e) => setForm({ ...form, newStatus: e.target.value })}>
                {['UP', 'DEGRADED'].map((s) => <option key={s}>{s}</option>)}
              </select>
            </label>
            <label className="span-2">Repair action and verification details
              <textarea required rows="3" value={form.actionDescription}
                placeholder="Describe the repair performed and checks completed."
                onChange={(e) => setForm({ ...form, actionDescription: e.target.value })} />
            </label>
            <div className="span-2">
              <p className="note">Submitting this form does not change the component status. It remains pending until an authorized reviewer verifies the repair.</p>
              <button className="btn btn-primary" disabled={saving || !form.incidentId || !form.componentId}>
                {saving ? 'Submitting…' : 'Submit for verification'}
              </button>
            </div>
          </form>
        </section>
      )}

      <section className="card">
        <div className="card-head">
          <h2>Restoration requests</h2>
          <button className="btn btn-ghost btn-sm" onClick={load} disabled={loading}>↻ Refresh</button>
        </div>
        {loading ? <Spinner /> : rows.length === 0 ? (
          <p className="muted">No restoration requests found.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Incident</th><th>Component</th><th>Repair action</th>
                  <th>Transition</th><th>Status</th><th>Submitted</th>
                  {canVerify && <th>Verification</th>}
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => {
                  const status = String(r.restorationStatus || '').toUpperCase()
                  const pending = status === 'VERIFICATION_PENDING'
                  return (
                    <tr key={r.id}>
                      <td><code>{r.incident?.incidentCode || `#${r.incident?.id ?? '—'}`}</code></td>
                      <td>{r.component?.componentCode || '—'}</td>
                      <td>{r.actionDescription}</td>
                      <td className="muted small">{r.previousStatus || '—'} → {r.newStatus || '—'}</td>
                      <td><Badge value={status || 'UNKNOWN'} /></td>
                      <td className="muted small">{r.performedAt ? String(r.performedAt).replace('T', ' ').slice(0, 16) : '—'}</td>
                      {canVerify && (
                        <td>
                          {pending ? (
                            <div className="head-actions">
                              <button className="btn btn-primary btn-sm" disabled={verifyingId === r.id}
                                onClick={() => verify(r, true)}>Approve</button>
                              <button className="btn btn-sm" disabled={verifyingId === r.id}
                                onClick={() => verify(r, false)}>Reject</button>
                            </div>
                          ) : (
                            <span className="muted small">
                              {r.verificationNotes || r.verificationNotes === '' ? r.verificationNotes : r.verifiedAt ? `Verified ${String(r.verifiedAt).slice(0, 16)}` : '—'}
                            </span>
                          )}
                        </td>
                      )}
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
