import api from './client.js'

// F06 Admin / Moderation
// Backend trả PagedResponse {items, total, hasMore, nextCursor} cho queue & users
export const adminApi = {
  moderationQueue: (params = {}) => api.get('/admin/moderation-queue', { params }).then((r) => r.data?.items || []),
  approve: (id) => api.post(`/admin/opportunities/${id}/approve`).then((r) => r.data),
  reject: (id, reason) => api.post(`/admin/opportunities/${id}/reject`, { reason }).then((r) => r.data),
  feature: (id, featuredUntil) =>
    api.post(`/admin/opportunities/${id}/feature`, { featuredUntil }).then((r) => r.data),
  analytics: () => api.get('/admin/analytics').then((r) => r.data),
  users: (params = {}) => api.get('/admin/users', { params }).then((r) => r.data?.items || []),
  verifyOrg: (userId) => api.post(`/admin/users/${userId}/verify-org`).then((r) => r.data),
  categories: () => api.get('/categories').then((r) => r.data),
  auditLogs: (params = {}) => api.get('/admin/audit-logs', { params }).then((r) => r.data),
}
