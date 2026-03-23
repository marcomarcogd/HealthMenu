import axios from 'axios'

const publicRequest = axios.create({
  baseURL: import.meta.env.VITE_PUBLIC_API_BASE_URL || '/api/public',
  timeout: 15000,
})

publicRequest.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.success) {
      return body.data
    }
    return Promise.reject(new Error(body?.message || 'Request failed'))
  },
  (error) => Promise.reject(error),
)

export const getPublicMenuById = (id) => publicRequest.get(`/menus/${id}`)
export const getPublicMenuByToken = (token) => publicRequest.get(`/menus/share/${token}`)
