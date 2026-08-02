import api from './client.js'

// F05 Provider — backend dùng /api/v1/opportunities (không prefix /provider)
export const providerApi = {
  list: () => api.get('/provider/opportunities').then((r) => r.data?.items || []),
  create: (body) => api.post('/opportunities', body).then((r) => r.data),
  update: (id, body) => api.put(`/opportunities/${id}`, body).then((r) => r.data),
  submit: (id) => api.post(`/opportunities/${id}/submit`).then((r) => r.data),
  hide: (id) => api.post(`/opportunities/${id}/hide`).then((r) => r.data),
  show: (id) => api.post(`/opportunities/${id}/show`).then((r) => r.data),
  close: (id) => api.post(`/opportunities/${id}/close`).then((r) => r.data),
  extend: (id, epoch) => api.post(`/opportunities/${id}/extend?newDeadlineEpoch=${epoch}`).then((r) => r.data),
  featureRequest: (id) => api.post(`/opportunities/${id}/feature-request`).then((r) => r.data),
  // Ứng tuyển: backend dùng /api/v1/provider/applications (ApplicationController)
  applications: (id) => api.get(`/provider/opportunities/${id}/applications`).then((r) => r.data),
  appDetail: (appId) => api.get(`/provider/applications/${appId}`).then((r) => r.data),
  setAppStatus: (appId, status, note) =>
    api.put(`/provider/applications/${appId}/status`, { status, note }).then((r) => r.data),
  exportCsv: (oppId) =>
    api.get(`/provider/applications/export`, { params: { fmt: 'csv', oppId }, responseType: 'blob' })
      .then((r) => r.data),
  stats: () => api.get('/provider/stats').then((r) => r.data),
  orgProfile: () => api.get('/provider/org').then((r) => r.data),
  updateOrgProfile: (body) => api.put('/provider/org', body).then((r) => r.data),
  orgDocuments: () => api.get('/provider/org/documents').then((r) => r.data || []),
  addOrgDocument: (body) => api.post('/provider/org/documents', body).then((r) => r.data),
  deleteOrgDocument: (docId) => api.delete(`/provider/org/documents/${docId}`).then((r) => r.data),
  oppDocuments: (oppId) => api.get(`/provider/opportunities/${oppId}/documents`).then((r) => r.data || []),
}
