import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 중복 리다이렉트 방지 플래그
let redirectingToLogin = false

function redirectToLogin() {
  if (redirectingToLogin) return
  if (window.location.pathname === '/login') return
  redirectingToLogin = true
  localStorage.removeItem('accessToken')
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${redirect}`
}

// 요청 인터셉터 — 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 응답 인터셉터 — 에러 처리
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message
    const hasToken = !!localStorage.getItem('accessToken')

    if (status === 401 || (status === 403 && !hasToken)) {
      redirectToLogin()
      return Promise.reject(error)
    }

    if (status === 403) {
      ElMessage.error(message || '접근 권한이 없습니다.')
      return Promise.reject(error)
    }

    if (status >= 500) {
      ElMessage.error('서버 오류가 발생했습니다.')
      return Promise.reject(error)
    }

    if (message) {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  }
)

export default api
