import api from './client.js'

// F04 Student: applications + notifications
// Backend trả PagedResponse {items, total, hasMore, nextCursor}
export const studentApi = {
  profile: () => api.get('/me/profile').then((r) => r.data),
  updateProfile: (body) => api.put('/me/profile', body).then((r) => r.data),
  /** Upload file lên S3 — purpose: cv | image | org-doc | opp-doc */
  upload: (file, purpose = 'cv') => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/me/uploads', fd, { params: { purpose } }).then((r) => r.data)
  },
  myApplications: (params = {}) => api.get('/me/applications', { params }).then((r) => r.data?.items || []),
  appDetail: (appId) => api.get(`/me/applications/${appId}`).then((r) => r.data),
  withdraw: (appId) => api.post(`/me/applications/${appId}/withdraw`).then((r) => r.data),
  notifications: (params = {}) => api.get('/me/notifications', { params }).then((r) => r.data?.items || []),
  markRead: (id) => api.post(`/me/notifications/${id}/read`).then((r) => r.data),
  apply: (oppId, { cvFile, coverLetter } = {}) =>
    api.post(`/opportunities/${oppId}/apply`, null, {
      params: {
        ...(cvFile ? { cvFile } : {}),
        ...(coverLetter ? { coverLetter } : {}),
      },
    }).then((r) => r.data),
}
