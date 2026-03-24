import axios from 'axios'
import { showErrorMessage } from '../utils/message'

const baseURL = import.meta.env.VITE_ADMIN_API_BASE_URL || '/api/admin'

const request = axios.create({
  baseURL,
  timeout: 15000,
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.success) {
      return body.data
    }
    const message = body?.message || '请求失败'
    showErrorMessage(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    showErrorMessage(error?.message || '网络异常')
    return Promise.reject(error)
  },
)

export default request
