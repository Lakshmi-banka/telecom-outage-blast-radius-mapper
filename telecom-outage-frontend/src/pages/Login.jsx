import { useState } from 'react'
import { useNavigate, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { errorText } from '../api/client'

export default function Login() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(errorText(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <div className="brand brand-lg">
          <span className="brand-mark">BR</span>
          <div>
            <strong>Blast-Radius Mapper</strong>
            <small>Telecom Network Outage</small>
          </div>
        </div>

        <label>
          Email
          <input type="email" value={email} required autoFocus
                 placeholder="you@example.com"
                 onChange={(e) => setEmail(e.target.value)} />
        </label>

        <label>
          Password
          <input type="password" value={password} required
                 placeholder="••••••••"
                 onChange={(e) => setPassword(e.target.value)} />
        </label>

        {error && <div className="error-box"><p>{error}</p></div>}

        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>

        <p className="hint">
          Click here to login 
        </p>
      </form>
    </div>
  )
}
