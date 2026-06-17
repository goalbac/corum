import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('accessToken') || null)
  const member = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(accessToken) {
    token.value = accessToken
    localStorage.setItem('accessToken', accessToken)
  }

  function clearToken() {
    token.value = null
    member.value = null
    localStorage.removeItem('accessToken')
  }

  async function fetchMe() {
    try {
      const res = await api.get('/auth/me')
      member.value = res.data.data
    } catch {
      clearToken()
    }
  }

  async function hashPassword(username, password) {
    const msgBuffer = new TextEncoder().encode(username + password)
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer)
    return Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('')
  }

  async function login(username, password) {
    const hashedPassword = await hashPassword(username, password)
    const res = await api.post('/auth/login', { username, password: hashedPassword })
    setToken(res.data.data.accessToken)
    member.value = res.data.data.member
    await fetchMe()
    return member.value
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } finally {
      clearToken()
    }
  }

  return { token, member, isLoggedIn, login, logout, fetchMe, clearToken }
})
