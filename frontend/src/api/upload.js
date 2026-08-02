import api from './client.js'
import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1'

const multipartHeaders = { 'Content-Type': undefined }

/** Upload authenticated → { url: ob-s3://…, viewUrl, key, encryptedAtRest } */
export function uploadFile(file, purpose = 'image') {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/me/uploads', fd, {
    params: { purpose },
    headers: multipartHeaders,
  }).then((r) => r.data)
}

/** Đăng ký NTD — chưa có JWT */
export function uploadGuest(file, purpose = 'org-doc') {
  const fd = new FormData()
  fd.append('file', file)
  return axios.post(`${API_BASE}/uploads/guest`, fd, {
    params: { purpose },
    headers: multipartHeaders,
  }).then((r) => r.data)
}

/** Đưa viewUrl relative → absolute khi FE khác host với API */
export function mediaSrc(viewOrUrl) {
  if (!viewOrUrl) return ''
  if (viewOrUrl.startsWith('http://') || viewOrUrl.startsWith('https://')) return viewOrUrl
  if (viewOrUrl.startsWith('/api/')) {
    const base = import.meta.env.VITE_API_URL
    if (base) {
      // VITE_API_URL = …/api/v1 → origin + path
      try {
        const u = new URL(base)
        return `${u.origin}${viewOrUrl}`
      } catch { /* fallthrough */ }
    }
    return viewOrUrl
  }
  return viewOrUrl
}
