import axios from 'axios'
import { showErrorMessage } from '../utils/message'
import { redirectToLogin } from '../utils/auth-redirect'

const baseURL = import.meta.env.VITE_ADMIN_API_BASE_URL || '/api/admin'

const request = axios.create({
  baseURL,
  timeout: 15000,
  withCredentials: true,
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
    const message = resolveRequestErrorMessage(error)
    if (!error?.config?.silentErrorMessage) {
      showErrorMessage(message)
    }
    if (error?.response?.status === 401 && !error?.config?.skipAuthRedirect) {
      redirectToLogin()
    }
    return Promise.reject(createRequestError(error, message))
  },
)

export default request

function resolveRequestErrorMessage(error) {
  const responseMessage = error?.response?.data?.message
  if (responseMessage) {
    return responseMessage
  }

  if (error?.code === 'ECONNABORTED' || /timeout/i.test(error?.message || '')) {
    const url = error?.config?.url || ''
    if (url.includes('/menus/ai/parse')) {
      return 'AI 解析超时，请缩短文本或稍后重试'
    }
    if (url.includes('/menus/ai/generate-image')) {
      return 'AI 生图超时，请稍后重试'
    }
    if (url.includes('/menus/init') && looksLikeAiInit(error?.config?.data)) {
      return 'AI 预填超时，请缩短文本或稍后重试'
    }
    return '请求超时，请稍后重试'
  }

  return error?.message || '网络异常'
}

function createRequestError(error, message) {
  const wrapped = new Error(message)
  wrapped.status = error?.response?.status
  wrapped.code = error?.response?.data?.code || error?.code
  return wrapped
}

function looksLikeAiInit(rawData) {
  if (!rawData) {
    return false
  }
  if (typeof rawData === 'string') {
    try {
      const parsed = JSON.parse(rawData)
      return parsed?.useAi === true
    } catch {
      return rawData.includes('"useAi":true')
    }
  }
  return rawData?.useAi === true
}
