import request from './request'

export const listDictTypes = () => request.get('/dicts/types')
export const saveDictType = (data) => request.post('/dicts/types', data)
export const deleteDictType = (id) => request.delete(`/dicts/types/${id}`)
export const listDictItems = (dictTypeId) => request.get('/dicts/items', { params: { dictTypeId } })
export const saveDictItem = (data) => request.post('/dicts/items', data)
export const deleteDictItem = (id) => request.delete(`/dicts/items/${id}`)
