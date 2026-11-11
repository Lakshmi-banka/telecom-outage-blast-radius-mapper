import axios from 'axios'

// When VITE_API_BASE_URL is unset we use '/api', which the Vite dev server
// proxies to http://localhost:8080 (see vite.config.js).
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

export const api = axios.create({ baseURL })

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Normalise every backend error into a readable string.
// The backend's ApiExceptionHandler returns { error, detail } for real
// failures; a bare 401/403 means the token is missing or expired.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!window.location.hash.includes('/login')) {
        window.location.hash = '#/login'
      }
    }

    let message
    if (!error.response) {
      message = 'Cannot reach the backend. Is Spring Boot running on port 8080?'
    } else if (typeof data === 'string' && data.trim()) {
      message = data
    } else if (data?.detail) {
      message = `${data.error || 'Error'}: ${data.detail}`
    } else if (data?.error) {
      message = data.error
    } else if (data?.errors) {
      message = Object.entries(data.errors).map(([k, v]) => `${k}: ${v}`).join(', ')
    } else if (status === 403) {
      message = 'Forbidden. Your role does not allow this action.'
    } else {
      message = `Request failed with status ${status}`
    }

    error.friendlyMessage = message
    return Promise.reject(error)
  }
)

export function errorText(e) {
  return e?.friendlyMessage || e?.message || 'Unexpected error'
}

// ---------------------------------------------------------------------------
// Endpoint wrappers -- these mirror the Spring controllers exactly.
// ---------------------------------------------------------------------------
export const Api = {
  login: (email, password) => api.post('/auth/login', { email, password }),

  pythonHealth: () => api.get('/python/health'),

  components: () => api.get('/components'),
  component: (id) => api.get(`/components/${id}`),
  createComponent: (body) => api.post('/components', body),
  updateComponentStatus: (id, status) => api.patch(`/components/${id}/status`, { status }),

  incidents: () => api.get('/incidents'),
  incident: (id) => api.get(`/incidents/${id}`),
  createIncident: (body) => api.post('/incidents', body),
  updateIncidentStatus: (id, incidentStatus) =>
    api.patch(`/incidents/${id}/status`, { incidentStatus }),

  blastRadiusBfs: (id) => api.post(`/incidents/${id}/blast-radius`),
  blastRadiusDfs: (id) => api.post(`/incidents/${id}/blast-radius/dfs`),
  networkAnalysis: (incidentId) => api.get(`/network/${incidentId}/analysis`),

  alarms: () => api.get('/alarms'),
  alarmsByStatus: (status) => api.get(`/alarms/status/${status}`),
  correlateAlarms: (windowMinutes, groupBy) =>
    api.post('/alarms/correlate', null, { params: { windowMinutes, groupBy } }),

  services: () => api.get('/services'),
  dependencies: () => api.get('/network/dependencies'),

  simulate: (incidentId, failedComponentIds, traversal) =>
    api.post('/simulations/outage', { incidentId, failedComponentIds, traversal }),

  failureEvents: () => api.get('/failure-events'),
  restorations: () => api.get('/restorations'),
  restorationsByIncident: (incidentId) => api.get(`/restorations/incident/${incidentId}`),
  createRestoration: (body) => api.post('/restorations', body),
  updateRestorationStatus: (id, restorationStatus) =>
    api.patch(`/restorations/${id}/status`, { restorationStatus }),
  verifyRestoration: (id, approved, notes) =>
    api.patch(`/restorations/${id}/verify`, { approved, notes }),

  // ADMIN only -- flat JDBC rows, snake_case column names
  adminList: (table) => api.get(`/admin/data/${table}`),
  adminDelete: (table, id) => api.delete(`/admin/data/${table}/${id}`),
}
