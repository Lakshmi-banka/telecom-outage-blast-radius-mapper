
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  CartesianGrid,
  XAxis,
  YAxis,
  LineChart,
  Line,
  BarChart,
  Bar,
} from 'recharts'

import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'

const COLORS = {
  blue: '#3b82f6',
  green: '#22c55e',
  red: '#ef4444',
  amber: '#f59e0b',
  purple: '#8b5cf6',
  muted: '#8b98a8',
  grid: '#26303d',
}

const SEVERITY_COLORS = {
  CRITICAL: COLORS.red,
  HIGH: '#f97316',
  MEDIUM: COLORS.amber,
  LOW: COLORS.green,
}

function Stat({ label, value, to, tone }) {
  const body = (
    <div className={`stat ${tone ? `stat-${tone}` : ''}`}>
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  )

  return to ? <Link to={to} className="stat-link">{body}</Link> : body
}

function ChartCard({ title, subtitle, children, className = '' }) {
  return (
    <section className={`card chart-card ${className}`}>
      <div className="chart-card-head">
        <div>
          <h2>{title}</h2>
          <p className="muted chart-subtitle">{subtitle}</p>
        </div>
      </div>
      {children}
    </section>
  )
}

function EmptyChart({ message = 'No data available for this chart yet.' }) {
  return (
    <div className="chart-empty">
      <span className="chart-empty-icon">▥</span>
      <p>{message}</p>
    </div>
  )
}

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null

  return (
    <div className="chart-tooltip">
      {label && <p className="chart-tooltip-label">{label}</p>}
      {payload.map((entry, index) => (
        <div className="chart-tooltip-row" key={`${entry.name}-${index}`}>
          <span
            className="chart-tooltip-dot"
            style={{ background: entry.color || entry.fill }}
          />
          <span>{entry.name || entry.dataKey}</span>
          <strong>{entry.value}</strong>
        </div>
      ))}
    </div>
  )
}

function normalize(value) {
  return String(value || 'UNKNOWN').trim().toUpperCase()
}

function Dashboard() {
  const [state, setState] = useState({ loading: true })
  const [lastUpdated, setLastUpdated] = useState(null)
  const [refreshing, setRefreshing] = useState(false)

  async function loadDashboard(isRefresh = false) {
    if (isRefresh) setRefreshing(true)

    const results = await Promise.allSettled([
      Api.incidents(),
      Api.components(),
      Api.alarms(),
      Api.services(),
      Api.dependencies(),
      Api.pythonHealth(),
    ])

    const [inc, comp, alarm, svc, dep, health] = results

    const pick = (result) =>
      result.status === 'fulfilled' && Array.isArray(result.value.data)
        ? result.value.data
        : null

    setState({
      loading: false,
      incidents: pick(inc),
      components: pick(comp),
      alarms: pick(alarm),
      services: pick(svc),
      dependencies: pick(dep),
      engine: health.status === 'fulfilled' ? health.value.data : null,
      errors: results
        .filter((result) => result.status === 'rejected')
        .map((result) => errorText(result.reason)),
    })

    setLastUpdated(new Date())
    setRefreshing(false)
  }

  useEffect(() => {
    loadDashboard()

    const interval = setInterval(() => {
      loadDashboard()
    }, 60000)

    return () => clearInterval(interval)
  }, [])

  const incidents = state.incidents || []
  const components = state.components || []
  const alarms = state.alarms || []
  const services = state.services || []
  const dependencies = state.dependencies || []

  const openIncidents = incidents.filter(
    (i) => normalize(i.incidentStatus) !== 'RESOLVED'
  ).length

  const downComponents = components.filter(
    (c) => normalize(c.status) === 'DOWN'
  ).length

  const openAlarms = alarms.filter(
    (a) => normalize(a.alarmStatus) === 'OPEN'
  ).length

  // Incident severity distribution
  const severityData = useMemo(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

    return order
      .map((severity) => ({
        name: severity,
        value: incidents.filter(
          (incident) => normalize(incident.severity) === severity
        ).length,
      }))
      .filter((item) => item.value > 0)
  }, [state.incidents])

  // Incident trend: group incidents by date.
  const incidentTrendData = useMemo(() => {
    const counts = {}

    incidents.forEach((incident) => {
      const rawDate = incident.startedAt || incident.createdAt

      if (!rawDate) return

      const date = new Date(rawDate)
      if (Number.isNaN(date.getTime())) return

      const key = [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, '0'),
        String(date.getDate()).padStart(2, '0'),
      ].join('-')

      counts[key] = (counts[key] || 0) + 1
    })

    return Object.entries(counts)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-14)
      .map(([date, count]) => ({
        date: new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', {
          day: '2-digit',
          month: 'short',
        }),
        incidents: count,
      }))
  }, [state.incidents])

  // Component status distribution
  const componentHealthData = useMemo(() => {
    const statuses = ['UP', 'DOWN', 'MAINTENANCE', 'UNKNOWN']

    return statuses
      .map((status) => ({
        status,
        count: components.filter(
          (component) => normalize(component.status) === status
        ).length,
      }))
      .filter((item) => item.count > 0)
  }, [state.components])

  // Alarm severity distribution
  const alarmSeverityData = useMemo(() => {
    const order = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

    return order
      .map((severity) => ({
        severity,
        count: alarms.filter(
          (alarm) => normalize(alarm.severity) === severity
        ).length,
      }))
      .filter((item) => item.count > 0)
  }, [state.alarms])

  if (state.loading) {
    return <Spinner label="Loading dashboard…" />
  }

  const engineUp = state.engine?.status === 'UP'

  return (
    <>
      <div className="page-head dashboard-page-head">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">
            Live view of the network, incidents and analysis engine.
          </p>
          {lastUpdated && (
            <span className="dashboard-updated">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
        </div>

        <div className="head-actions">
          <button
            className="btn"
            onClick={() => loadDashboard(true)}
            disabled={refreshing}
          >
            {refreshing ? 'Refreshing…' : '↻ Refresh'}
          </button>
        </div>
      </div>

      {/* KPI CARDS */}
      <div className="stat-grid">
        <Stat
          label="Open incidents"
          value={openIncidents}
          to="/incidents"
          tone={openIncidents ? 'bad' : 'ok'}
        />

        <Stat
          label="Total incidents"
          value={incidents.length}
          to="/incidents"
        />

        <Stat
          label="Components down"
          value={downComponents}
          to="/components"
          tone={downComponents ? 'bad' : 'ok'}
        />

        <Stat
          label="Total components"
          value={components.length}
          to="/components"
        />

        <Stat
          label="Open alarms"
          value={openAlarms}
          to="/alarms"
          tone={openAlarms ? 'warn' : 'ok'}
        />

        <Stat label="Services" value={services.length} />

        <Stat
          label="Active dependencies"
          value={dependencies.length}
          to="/network"
        />

        <Stat
          label="Python engine"
          value={engineUp ? 'UP' : 'DOWN'}
          tone={engineUp ? 'ok' : 'bad'}
        />
      </div>

      {/* API ERRORS */}
      {state.errors?.length > 0 && (
        <div className="error-box">
          <strong>Some dashboard endpoints did not respond</strong>
          <ul>
            {state.errors.map((error, index) => (
              <li key={index}>{error}</li>
            ))}
          </ul>
        </div>
      )}

      {/* CHARTS ROW 1 */}
      <div className="dashboard-chart-grid">
        <ChartCard
          title="Incident severity"
          subtitle="Distribution of incidents by severity"
        >
          {severityData.length === 0 ? (
            <EmptyChart message="No incident severity data available." />
          ) : (
            <div className="chart-area chart-pie-area">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={severityData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="48%"
                    innerRadius={65}
                    outerRadius={100}
                    paddingAngle={4}
                    stroke="none"
                  >
                    {severityData.map((entry) => (
                      <Cell
                        key={entry.name}
                        fill={SEVERITY_COLORS[entry.name] || COLORS.muted}
                      />
                    ))}
                  </Pie>

                  <Tooltip content={<CustomTooltip />} />
                  <Legend
                    verticalAlign="bottom"
                    iconType="circle"
                    formatter={(value) => (
                      <span className="chart-legend-text">{value}</span>
                    )}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </ChartCard>

        <ChartCard
          title="Network incidents trend"
          subtitle="Incident creation over time"
        >
          {incidentTrendData.length === 0 ? (
            <EmptyChart message="Incident dates are not available yet." />
          ) : (
            <div className="chart-area">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={incidentTrendData}
                  margin={{ top: 12, right: 12, left: -18, bottom: 4 }}
                >
                  <CartesianGrid
                    stroke={COLORS.grid}
                    strokeDasharray="3 3"
                    vertical={false}
                  />

                  <XAxis
                    dataKey="date"
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={{ stroke: COLORS.grid }}
                    tickLine={false}
                  />

                  <YAxis
                    allowDecimals={false}
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />

                  <Tooltip content={<CustomTooltip />} />

                  <Line
                    type="monotone"
                    dataKey="incidents"
                    name="Incidents"
                    stroke={COLORS.blue}
                    strokeWidth={3}
                    dot={{ fill: COLORS.blue, r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}
        </ChartCard>
      </div>

      {/* CHARTS ROW 2 */}
      <div className="dashboard-chart-grid">
        <ChartCard
          title="Network component health"
          subtitle="Current component status"
        >
          {componentHealthData.length === 0 ? (
            <EmptyChart message="No component status data available." />
          ) : (
            <div className="chart-area">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={componentHealthData}
                  margin={{ top: 12, right: 12, left: -18, bottom: 4 }}
                >
                  <CartesianGrid
                    stroke={COLORS.grid}
                    strokeDasharray="3 3"
                    vertical={false}
                  />

                  <XAxis
                    dataKey="status"
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={{ stroke: COLORS.grid }}
                    tickLine={false}
                  />

                  <YAxis
                    allowDecimals={false}
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />

                  <Tooltip content={<CustomTooltip />} />

                  <Bar
                    dataKey="count"
                    name="Components"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={65}
                  >
                    {componentHealthData.map((entry) => {
                      const color =
                        entry.status === 'UP'
                          ? COLORS.green
                          : entry.status === 'DOWN'
                            ? COLORS.red
                            : entry.status === 'MAINTENANCE'
                              ? COLORS.amber
                              : COLORS.muted

                      return <Cell key={entry.status} fill={color} />
                    })}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </ChartCard>

        <ChartCard
          title="Alarm severity"
          subtitle="Distribution of network alarms"
        >
          {alarmSeverityData.length === 0 ? (
            <EmptyChart message="No alarm severity data available." />
          ) : (
            <div className="chart-area">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={alarmSeverityData}
                  layout="vertical"
                  margin={{ top: 8, right: 20, left: 8, bottom: 4 }}
                >
                  <CartesianGrid
                    stroke={COLORS.grid}
                    strokeDasharray="3 3"
                    horizontal={false}
                  />

                  <XAxis
                    type="number"
                    allowDecimals={false}
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={{ stroke: COLORS.grid }}
                    tickLine={false}
                  />

                  <YAxis
                    type="category"
                    dataKey="severity"
                    width={78}
                    tick={{ fill: COLORS.muted, fontSize: 11 }}
                    axisLine={false}
                    tickLine={false}
                  />

                  <Tooltip content={<CustomTooltip />} />

                  <Bar
                    dataKey="count"
                    name="Alarms"
                    radius={[0, 6, 6, 0]}
                    maxBarSize={28}
                  >
                    {alarmSeverityData.map((entry) => (
                      <Cell
                        key={entry.severity}
                        fill={SEVERITY_COLORS[entry.severity] || COLORS.muted}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </ChartCard>
      </div>

      {/* RECENT INCIDENTS */}
      <section className="card recent-incidents-card">
        <div className="card-head">
          <div>
            <h2>Recent incidents</h2>
            <p className="muted chart-subtitle">
              Latest network incidents requiring attention
            </p>
          </div>

          <Link className="btn btn-sm" to="/incidents">
            View all
          </Link>
        </div>

        {incidents.length === 0 ? (
          <p className="muted">No incidents yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Title</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Root</th>
                  <th />
                </tr>
              </thead>

              <tbody>
                {incidents.slice(0, 6).map((incident) => (
                  <tr key={incident.id}>
                    <td><code>{incident.incidentCode}</code></td>
                    <td>{incident.title}</td>
                    <td><Badge value={incident.severity} /></td>
                    <td><Badge value={incident.incidentStatus} /></td>
                    <td>
                      {incident.rootComponent?.componentCode || '—'}
                    </td>
                    <td>
                      <Link
                        className="btn btn-sm btn-primary"
                        to={`/incidents/${incident.id}`}
                      >
                        Analyse
                      </Link>
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

export default Dashboard
