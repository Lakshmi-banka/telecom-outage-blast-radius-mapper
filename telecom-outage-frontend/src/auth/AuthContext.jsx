import { createContext, useContext, useMemo, useState } from 'react'
import { Api } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('user')
    return raw ? JSON.parse(raw) : null
  })

  async function login(email, password) {
    // Backend returns { token, tokenType, email, role }
    const { data } = await Api.login(email, password)
    localStorage.setItem('token', data.token)
    const profile = { email: data.email, role: data.role }
    localStorage.setItem('user', JSON.stringify(profile))
    setUser(profile)
    return profile
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  // Spring maps role "ADMIN" to authority "ROLE_ADMIN"; the login response
  // carries the bare role name, so compare against that.
  function hasRole(...roles) {
    if (!user?.role) return false
    return roles.includes(user.role)
  }

  const value = useMemo(() => ({ user, login, logout, hasRole }), [user])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
