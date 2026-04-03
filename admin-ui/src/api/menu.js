import axios from 'axios'
import request from './request'

const AI_PARSE_TIMEOUT = 60000
const AI_INIT_TIMEOUT = 60000
const AI_IMAGE_TIMEOUT = 120000

export const listMenus = (params = {}) => request.get('/menus', { params })
export const initMenuForm = (payload) => request.post('/menus/init', {
  ...payload,
  customerId: payload.customerId ? String(payload.customerId) : null,
  templateId: payload.templateId ? String(payload.templateId) : null,
}, payload?.useAi ? { timeout: AI_INIT_TIMEOUT } : undefined)
export const getMenuDetail = (id) => request.get(`/menus/${id}`)
export const saveMenu = (payload) => request.post('/menus', {
  ...payload,
  customerId: payload.customerId ? String(payload.customerId) : null,
  templateId: payload.templateId ? String(payload.templateId) : null,
})
export const parseMenuText = (sourceText) => request.post('/menus/ai/parse', { sourceText }, { timeout: AI_PARSE_TIMEOUT })
export const generateMenuImage = (prompt) => request.post('/menus/ai/generate-image', { prompt }, { timeout: AI_IMAGE_TIMEOUT })
export const publishMenu = (id) => request.post(`/menus/${id}/publish`)
export const publishMenus = (ids) => request.post('/menus/batch/publish', {
  ids: normalizeIds(ids),
})
export const deleteMenu = (id) => request.delete(`/menus/${id}`)
export const uploadMenuImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

const downloadClient = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_BASE_URL || '/api/admin',
  timeout: 60000,
})

export async function downloadMenuExcel(id) {
  try {
    const response = await downloadClient.get(`/menus/${id}/export/excel`, {
      responseType: 'blob',
    })
    triggerBlobDownload(response.data, response.headers, `menu-${id}.xlsx`)
  } catch (error) {
    const message = await resolveDownloadError(error)
    throw new Error(message)
  }
}

export async function downloadMenusExcel(ids) {
  try {
    const response = await downloadClient.post('/menus/batch/export/excel', {
      ids: normalizeIds(ids),
    }, {
      responseType: 'blob',
    })
    triggerBlobDownload(response.data, response.headers, 'menu-batch-export.zip')
  } catch (error) {
    const message = await resolveDownloadError(error)
    throw new Error(message)
  }
}

function normalizeIds(ids = []) {
  return ids
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0)
}

function triggerBlobDownload(data, headers = {}, fallbackName) {
  const blob = new Blob([data], {
    type: headers['content-type'] || 'application/octet-stream',
  })
  const fileName = resolveFileName(headers['content-disposition']) || fallbackName
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

function resolveFileName(contentDisposition) {
  if (!contentDisposition) {
    return ''
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const plainMatch = contentDisposition.match(/filename="?([^"]+)"?/i)
  return plainMatch?.[1] || ''
}

async function resolveDownloadError(error) {
  const blob = error?.response?.data
  if (blob instanceof Blob) {
    try {
      const text = await blob.text()
      const body = JSON.parse(text)
      if (body?.message) {
        return body.message
      }
    } catch {
      return error?.message || '导出失败'
    }
  }
  return error?.message || '导出失败'
}
