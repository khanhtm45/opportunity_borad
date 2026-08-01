import axios from 'axios'

// Production (GitHub Pages): set VITE_API_URL = https://your-api.example.com/api/v1
// Local/dev: fallback /api/v1 (Vite proxy → :8080)
const API_BASE = import.meta.env.VITE_API_URL || '/api/v1'

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

// Gắn access token vào mọi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Tự refresh token khi 401
let refreshing = null
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (!refreshToken) throw new Error('no refresh')
        refreshing =
          refreshing ||
          axios
            .post(`${API_BASE}/auth/refresh`, { refreshToken })
            .then((r) => {
              localStorage.setItem('accessToken', r.data.accessToken)
              return r.data.accessToken
            })
        const token = await refreshing
        refreshing = null
        original.headers.Authorization = `Bearer ${token}`
        return api(original)
      } catch (e) {
        refreshing = null
        localStorage.clear()
        const base = import.meta.env.BASE_URL || '/'
        window.location.href = `${base}login`.replace(/\/{2,}/g, '/')
        return Promise.reject(e)
      }
    }
    return Promise.reject(error)
  }
)

export default api
