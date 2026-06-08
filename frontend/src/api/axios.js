import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

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

    if (status === 401) {
      localStorage.removeItem('accessToken')
      window.location.href = '/login'
      return Promise.reject(error)
    }

    if (status === 403) {
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
