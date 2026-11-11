import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'

import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Incidents from './pages/Incidents'
import IncidentDetail from './pages/IncidentDetail'
import ComponentsPage from './pages/ComponentsPage'
import Network from './pages/Network'
import Alarms from './pages/Alarms'
import Simulation from './pages/Simulation'
import Restorations from './pages/Restorations'
import FailureEvents from './pages/FailureEvents'
import AdminData from './pages/AdminData'

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route index element={<Dashboard />} />
            <Route path="incidents" element={<Incidents />} />
            <Route path="incidents/:id" element={<IncidentDetail />} />
            <Route path="components" element={<ComponentsPage />} />
            <Route path="network" element={<Network />} />
            <Route path="alarms" element={<Alarms />} />
            <Route path="failures" element={<FailureEvents />} />
            <Route path="simulation" element={<Simulation />} />
            <Route path="restorations" element={<Restorations />} />
            <Route path="admin" element={<AdminData />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </HashRouter>
    </AuthProvider>
  )
}
