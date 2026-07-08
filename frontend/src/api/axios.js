import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'

// 로컬 개발(Vite 프록시)·nginx 동일 오리진 배포는 상대경로 '/api'로 충분하지만,
// 프론트(Cloudflare Pages)와 백엔드(Cloudflare Tunnel)가 서로 다른 도메인에 떠 있는
// 배포에서는 절대 URL이 필요하다. 빌드 시점에 VITE_API_BASE_URL을 주입하면 그걸 쓴다.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
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

    if (status === 401) {
      // 토큰을 들고 있었는데 401이 왔다면 세션 만료 — 로그인으로 이동.
      // 토큰이 없는 상태의 401은 이메일 인증/비밀번호 재설정 링크처럼 로그인 세션과
      // 무관한 API 자체의 비즈니스 로직상 실패이므로 강제 이동시키지 않고 호출부가 처리하게 둔다.
      if (hasToken) {
        redirectToLogin()
        return Promise.reject(error)
      }
      if (message) ElMessage.error(message)
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
