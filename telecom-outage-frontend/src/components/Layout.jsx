import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { Api } from '../api/client'

const NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/incidents', label: 'Incidents' },
  { to: '/components', label: 'Components' },
  { to: '/network', label: 'Network' },
  { to: '/alarms', label: 'Alarms' },
  { to: '/failures', label: 'Failure Monitoring' },
  { to: '/simulation', label: 'Simulation' },
  { to: '/restorations', label: 'Restorations' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [engine, setEngine] = useState('checking')

  // Live indicator for the Python engine, polled through Spring.
  useEffect(() => {
    let alive = true
    const check = () => Api.pythonHealth()
      .then((r) => alive && setEngine(r.data?.status === 'UP' ? 'up' : 'down'))
      .catch(() => alive && setEngine('down'))
    check()
    const timer = setInterval(check, 30000)
    return () => { alive = false; clearInterval(timer) }
  }, [])

  function signOut() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">BR</span>
          <div>
            <strong>Blast-Radius Mapper</strong>
            <small>Telecom Network Outage</small>
          </div>
        </div>

        <nav>
          {NAV.map((item) => (
            <NavLink key={item.to} to={item.to} end={item.end}
                     className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
              {item.label}
            </NavLink>
          ))}
          {user?.role === 'ADMIN' && (
            <NavLink to="/admin" className={({ isActive }) => isActive ? 'nav-item active' : 'nav-item'}>
              Admin Data
            </NavLink>
          )}
        </nav>

        <div className="sidebar-foot">
          <div className={`engine engine-${engine}`}>
            <i className="dot" />
            Python engine: {engine === 'checking' ? 'checking…' : engine.toUpperCase()}
          </div>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div />
          <div className="user-box">
            <div className="user-meta">
              <strong>{user?.email}</strong>
              <small>{user?.role}</small>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={signOut}>Sign out</button>
          </div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
