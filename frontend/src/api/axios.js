import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

let sessionExpiredHandled = false
let lastServerErrorAt = 0

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (!token) return config

  if (isTokenExpired(token)) {
    handleSessionExpired()
    return Promise.reject(createSessionExpiredError())
  }

  config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.isSessionExpired) {
      return Promise.reject(error)
    }

    const status = error.response?.status
    const message = error.response?.data?.message
    const token = localStorage.getItem('accessToken')

    if (status === 401 || (status === 403 && token && isTokenExpired(token))) {
      handleSessionExpired()
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.error('접근 권한이 없습니다.')
      return Promise.reject(error)
    }

    if (status >= 500) {
      showServerError()
      return Promise.reject(error)
    }

    if (message) {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

function isTokenExpired(token) {
  const payload = parseJwtPayload(token)
  if (!payload?.exp) return false
  return payload.exp * 1000 <= Date.now() + 5000
}

function parseJwtPayload(token) {
  try {
    const base64 = token.split('.')[1]?.replace(/-/g, '+').replace(/_/g, '/')
    if (!base64) return null
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((char) => `%${(`00${char.charCodeAt(0).toString(16)}`).slice(-2)}`)
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

function handleSessionExpired() {
  localStorage.removeItem('accessToken')
  if (!sessionExpiredHandled) {
    sessionExpiredHandled = true
    ElMessage.warning('로그인 시간이 만료되었습니다. 다시 로그인해주세요.')
  }

  if (window.location.pathname !== '/login') {
    const redirect = `${window.location.pathname}${window.location.search}`
    window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
  }
}

function createSessionExpiredError() {
  const error = new Error('Session expired')
  error.isSessionExpired = true
  return error
}

function showServerError() {
  const now = Date.now()
  if (now - lastServerErrorAt < 2000) return
  lastServerErrorAt = now
  ElMessage.error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.')
}

export default api
