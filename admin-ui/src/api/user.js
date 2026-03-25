import request from './request'

export const listUsers = () => request.get('/users')
export const saveUser = (payload) => request.post('/users', payload)
export const resetUserPassword = (id, payload) => request.post(`/users/${id}/reset-password`, payload)
export const deleteUser = (id) => request.delete(`/users/${id}`)
