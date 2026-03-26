import request from './request'

export const saveCustomer = (data) => request.post('/customers', data)
export const deleteCustomer = (id) => request.delete(`/customers/${id}`)
