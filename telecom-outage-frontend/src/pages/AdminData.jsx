import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import DataTable from '../components/DataTable'
import ErrorBox from '../components/ErrorBox'
import { useAuth } from '../auth/AuthContext'

// Mirrors the allow-list in AdminCrudController.TABLES
const TABLES = [
  'roles', 'users', 'components', 'component_dependencies', 'services',
  'service_components', 'alarms', 'incidents', 'incident_affected_components',
  'incident_affected_services', 'restoration_history', 'audit_logs',
]

export default function AdminData() {
  const { hasRole } = useAuth()
  const [table, setTable] = useState('components')
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  function load() {
    setLoading(true)
    setError('')
    Api.adminList(table)
      .then((r) => setRows(r.data || []))
      .catch((e) => { setRows([]); setError(errorText(e)) })
      .finally(() => setLoading(false))
  }

  useEffect(load, [table])

  if (!hasRole('ADMIN')) {
    return (
      <div className="card">
        <h1>Admin data</h1>
        <p className="muted">This section requires the ADMIN role.</p>
      </div>
    )
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Admin data browser</h1>
        </div>
        <select className="inline-select" value={table} onChange={(e) => setTable(e.target.value)}>
          {TABLES.map((t) => <option key={t}>{t}</option>)}
        </select>
      </div>

      <ErrorBox message={error} onRetry={load} />

      <section className="card">
        <div className="card-head">
          <h2>{table}</h2>
          <span className="muted small">{rows.length} row(s)</span>
        </div>
        {loading ? <Spinner /> : <DataTable rows={rows} />}
      </section>
    </>
  )
}
