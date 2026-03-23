import request from './request'

export const listTemplates = () => request.get('/templates')
export const saveTemplate = (data) => request.post('/templates', data)
export const copyTemplate = (id, data) => request.post(`/templates/${id}/copy`, data)
export const deleteTemplate = (id) => request.delete(`/templates/${id}`)
export const updateTemplateStatus = (id, status) => request.post(`/templates/${id}/status`, null, { params: { status } })
export const previewTemplate = (id) => request.get(`/templates/${id}/preview`)
