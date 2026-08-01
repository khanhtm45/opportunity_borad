import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
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
            .post('/api/v1/auth/refresh', { refreshToken })
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
        window.location.href = '/login'
        return Promise.reject(e)
      }
    }
    return Promise.reject(error)
  }
)

export default api
