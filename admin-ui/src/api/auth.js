import axios from 'axios'
import { showErrorMessage } from '../utils/message'
import { redirectToLogin } from '../utils/auth-redirect'

const authRequest = axios.create({
  baseURL: import.meta.env.VITE_AUTH_API_BASE_URL || '/api/auth',
  timeout: 15000,
  withCredentials: true,
})

authRequest.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body?.success) {
      return body.data
    }
    const message = body?.message || '请求失败'
    if (!response.config?.silentErrorMessage) {
      showErrorMessage(message)
    }
    return Promise.reject(new Error(message))
  },
  (error) => {
    const message = error?.response?.data?.message || error?.message || '网络异常'
    if (!error?.config?.silentErrorMessage) {
      showErrorMessage(message)
    }
    if (error?.response?.status === 401 && !error?.config?.skipAuthRedirect) {
      redirectToLogin()
    }
    return Promise.reject(new Error(message))
  },
)

export const login = (payload, config = {}) => authRequest.post('/login', payload, config)
export const fetchCurrentUser = (config = {}) => authRequest.get('/me', config)
export const logout = (config = {}) => authRequest.post('/logout', null, config)
