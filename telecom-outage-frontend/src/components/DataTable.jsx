// Renders any array of flat objects. Backend entity endpoints return nested
// objects for lazy relations, so anything non-primitive is summarised rather
// than dumped as [object Object].
function cellText(value) {
  if (value === null || value === undefined) return '—'
  if (typeof value === 'object') {
    if (Array.isArray(value)) return `${value.length} item(s)`
    return value.componentCode || value.serviceCode || value.incidentCode
      || value.email || value.roleName || value.name || `#${value.id ?? '?'}`
  }
  if (typeof value === 'boolean') return value ? 'yes' : 'no'
  return String(value)
}

export default function DataTable({ rows, columns, empty = 'No records found.', renderCell }) {
  if (!rows || rows.length === 0) return <p className="muted">{empty}</p>

  const cols = columns && columns.length
    ? columns
    : Object.keys(rows[0]).map((key) => ({ key, label: key }))

  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>{cols.map((c) => <th key={c.key}>{c.label}</th>)}</tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={row.id ?? i}>
              {cols.map((c) => (
                <td key={c.key}>
                  {renderCell
                    ? (renderCell(c.key, row) ?? cellText(row[c.key]))
                    : cellText(row[c.key])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
