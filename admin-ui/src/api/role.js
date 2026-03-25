import request from './request'

export const listRoles = () => request.get('/roles')
export const listRolePermissions = () => request.get('/roles/permissions')
export const saveRole = (payload) => request.post('/roles', payload)
export const deleteRole = (id) => request.delete(`/roles/${id}`)
