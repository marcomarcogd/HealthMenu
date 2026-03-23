import request from './request'

export const getTemplateDetail = (id) => request.get(`/templates/${id}`)

export const saveTemplateDesign = (id, payload) => request.post(`/templates/${id}/design`, payload)
