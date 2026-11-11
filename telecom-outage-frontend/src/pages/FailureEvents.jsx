
import { useEffect, useState } from 'react'
import { Api, errorText } from '../api/client'
import Spinner from '../components/Spinner'
import Badge from '../components/Badge'
import ErrorBox from '../components/ErrorBox'

function formatDate(value) {
  return value
    ? String(value).replace('T', ' ').slice(0, 19)
    : '—'
}

function getStatus(event) {
  return String(
    event.eventStatus || event.event_status || 'UNKNOWN'
  ).toUpperCase()
}

export default function FailureEvents() {
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('ALL')

  async function load() {
    setLoading(true)

    try {
      const response = await Api.failureEvents()

      // Api.failureEvents() returns an Axios response.
      const data = response?.data

      setEvents(Array.isArray(data) ? data : [])
      setError('')
    } catch (e) {
      setError(errorText(e))
      setEvents([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  // Filter failure events by their current status.
  const visible = events.filter((event) => {
    if (filter === 'ALL') return true
    return getStatus(event) === filter
  })

  const active = events.filter(
    (event) => getStatus(event) === 'ACTIVE'
  ).length

  const otherStatuses = events.length - active

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Failure monitoring</h1>
          <p className="muted">
            Failure events recorded by the network monitoring
            and simulation workflows.
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={load}
          disabled={loading}
        >
          {loading ? 'Refreshing…' : '↻ Refresh'}
        </button>
      </div>

      <ErrorBox message={error} onRetry={load} />

      {/* Failure event statistics */}
      <div className="stat-grid stat-grid-4">
        <div className="stat">
          <span className="stat-value">{events.length}</span>
          <span className="stat-label">Total events</span>
        </div>

        <div className="stat stat-bad">
          <span className="stat-value">{active}</span>
          <span className="stat-label">Active events</span>
        </div>

        <div className="stat stat-ok">
          <span className="stat-value">{otherStatuses}</span>
          <span className="stat-label">Other statuses</span>
        </div>
      </div>

      {/* Failure events table */}
      <section className="card">
        <div className="card-head">
          <h2>Failure events</h2>

          <select
            className="inline-select"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            {[
              'ALL',
              'ACTIVE',
              'RECOVERED',
              'RECOVERING',
              'FAILED'
            ].map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </div>

        {loading ? (
          <Spinner />
        ) : visible.length === 0 ? (
          <p className="muted">
            No failure events found for this filter.
          </p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Component ID</th>
                  <th>Component code</th>
                  <th>Component name</th>
                  <th>Failure type</th>
                  <th>Status</th>
                  <th>Started</th>
                  <th>Scheduled recovery</th>
                  <th>Recovered</th>
                </tr>
              </thead>

              <tbody>
                {visible.map((event) => {
                  const eventId =
                    event.failureEventId ?? event.id

                  const componentId =
                    event.componentId ??
                    event.failedComponentId ??
                    event.failed_component_id

                  const componentCode =
                    event.componentCode ??
                    event.failedComponent?.componentCode ??
                    '—'

                  const componentName =
                    event.componentName ??
                    event.failedComponent?.componentName ??
                    '—'

                  return (
                    <tr key={eventId}>
                      <td>#{eventId ?? '—'}</td>

                      <td>
                        {componentId ?? '—'}
                      </td>

                      <td>
                        {componentCode}
                      </td>

                      <td>
                        {componentName}
                      </td>

                      <td>
                        {event.failureType ||
                          event.failure_type ||
                          '—'}
                      </td>

                      <td>
                        <Badge value={getStatus(event)} />
                      </td>

                      <td className="muted small">
                        {formatDate(
                          event.startedAt || event.started_at
                        )}
                      </td>

                      <td className="muted small">
                        {formatDate(
                          event.scheduledRecoveryAt ||
                          event.scheduled_recovery_at
                        )}
                      </td>

                      <td className="muted small">
                        {formatDate(
                          event.recoveredAt ||
                          event.recovered_at
                        )}
                      </td>
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
