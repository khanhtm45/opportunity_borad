import api from './client.js'

// F04 Student: applications + notifications
// Backend trả PagedResponse {items, total, hasMore, nextCursor}
export const studentApi = {
  myApplications: (params = {}) => api.get('/me/applications', { params }).then((r) => r.data?.items || []),
  appDetail: (appId) => api.get(`/me/applications/${appId}`).then((r) => r.data),
  withdraw: (appId) => api.post(`/me/applications/${appId}/withdraw`).then((r) => r.data),
  notifications: (params = {}) => api.get('/me/notifications', { params }).then((r) => r.data?.items || []),
  markRead: (id) => api.post(`/me/notifications/${id}/read`).then((r) => r.data),
}
