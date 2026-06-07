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

  async function login(username, password) {
    const res = await api.post('/auth/login', { username, password })
    setToken(res.data.data.accessToken)
    await fetchMe()
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
