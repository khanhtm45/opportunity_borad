import { createContext, useContext, useEffect, useState } from 'react'
import api from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  const loadMe = async () => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      setLoading(false)
      return
    }
    try {
      const { data } = await api.get('/auth/me')
      setUser(data)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadMe()
  }, [])

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    await loadMe()
    return data
  }

  const register = async (payload) => {
    if (payload?.role === 'PROVIDER') {
      const { data } = await api.post('/auth/providers/register', {
        orgName: payload.orgName || payload.fullName,
        website: payload.website || undefined,
        description: payload.description || undefined,
        contactEmail: payload.email,
        contactPhone: payload.contactPhone || undefined,
        taxCode: payload.taxCode || undefined,
        address: payload.address || undefined,
        industry: payload.industry || undefined,
        companySize: payload.companySize || undefined,
        contactFullName: payload.fullName,
        password: payload.password,
        documents: payload.documents,
      })
      return data
    }
    const { data } = await api.post('/auth/register', payload)
    return data
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {}
    localStorage.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, setUser, loading, login, register, logout, loadMe }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
