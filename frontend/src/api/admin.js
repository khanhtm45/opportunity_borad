import api from './client.js'

// F06 Admin / Moderation
// Backend trả PagedResponse {items, total, hasMore, nextCursor} cho queue & users
export const adminApi = {
  moderationQueue: (params = {}) => api.get('/admin/moderation-queue', { params }).then((r) => r.data?.items || []),
  approve: (id) => api.post(`/admin/opportunities/${id}/approve`).then((r) => r.data),
  reject: (id, reason) => api.post(`/admin/opportunities/${id}/reject`, { reason }).then((r) => r.data),
  requestUpdate: (id, reason) =>
    api.post(`/admin/opportunities/${id}/request-update`, { reason }).then((r) => r.data),
  feature: (id, featuredUntil) =>
    api.post(`/admin/opportunities/${id}/feature`, { featuredUntil }).then((r) => r.data),
  analytics: () => api.get('/admin/analytics').then((r) => r.data),
  users: (params = {}) => api.get('/admin/users', { params }).then((r) => r.data?.items || []),
  verifyOrg: (userId) => api.post(`/admin/users/${userId}/verify-org`).then((r) => r.data),
  // Lớp 1 — tổ chức / thuế
  aiScanOrgByUser: (userId, apply = true) =>
    api.post(`/admin/users/${userId}/ai-scan-org`, null, { params: { apply } }).then((r) => r.data),
  aiScanOrg: (orgId, apply = true) =>
    api.post(`/admin/orgs/${orgId}/ai-scan`, null, { params: { apply } }).then((r) => r.data),
  taxCheckOrgByUser: (userId) =>
    api.get(`/admin/users/${userId}/tax-check-org`).then((r) => r.data),
  taxCheckOrg: (orgId) =>
    api.get(`/admin/orgs/${orgId}/tax-check`).then((r) => r.data),
  // Lớp 2 — hồ sơ tin đăng
  aiScanOpportunity: (oppId, apply = true) =>
    api.post(`/admin/opportunities/${oppId}/ai-scan`, null, { params: { apply } }).then((r) => r.data),
  orgDocuments: (orgId) => api.get(`/admin/orgs/${orgId}/documents`).then((r) => r.data || []),
  oppDocuments: (oppId) => api.get(`/admin/opportunities/${oppId}/documents`).then((r) => r.data || []),
  categories: () => api.get('/categories').then((r) => r.data),
  auditLogs: (params = {}) => api.get('/admin/audit-logs', { params }).then((r) => r.data),
}
