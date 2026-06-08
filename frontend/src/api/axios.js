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

function redirectToLogin(message) {
  if (redirectingToLogin) return
  redirectingToLogin = true
  localStorage.removeItem('accessToken')
  ElNotification({
    title: '로그인 필요',
    message,
    type: 'warning',
    duration: 2500,
  })
  setTimeout(() => {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.href = `/login?redirect=${redirect}`
  }, 800)
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

    if (status === 401) {
      redirectToLogin('세션이 만료되었습니다. 다시 로그인해 주세요.')
      return Promise.reject(error)
    }

    if (status === 403) {
      // 토큰이 없는 상태(비로그인)에서의 403은 인증 문제이므로 로그인으로 이동
      if (!hasToken) {
        redirectToLogin('로그인이 필요한 페이지입니다.')
        return Promise.reject(error)
      }
      ElMessage.error('접근 권한이 없습니다.')
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
