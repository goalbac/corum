import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  let eventSource = null
  let reconnectTimer = null

  async function fetchNotifications() {
    try {
      const res = await api.get('/notifications')
      notifications.value = res.data.data || []
    } catch { /* ignore */ }
  }

  async function fetchUnreadCount() {
    try {
      const res = await api.get('/notifications/unread-count')
      unreadCount.value = res.data.data?.count || 0
    } catch { /* ignore */ }
  }

  async function markAsRead(id) {
    try {
      await api.put(`/notifications/${id}/read`)
      const n = notifications.value.find(n => n.id === id)
      if (n && !n.isRead) {
        n.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch { /* ignore */ }
  }

  async function markAllAsRead() {
    try {
      await api.put('/notifications/read-all')
      notifications.value.forEach(n => { n.isRead = true })
      unreadCount.value = 0
    } catch { /* ignore */ }
  }

  function connect(token) {
    disconnect()
    const url = `/api/notifications/stream?token=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    eventSource.addEventListener('notification', (e) => {
      try {
        const notif = JSON.parse(e.data)
        notifications.value.unshift(notif)
        unreadCount.value++
      } catch { /* ignore */ }
    })

    eventSource.addEventListener('connect', () => {
      // 연결 성공
    })

    eventSource.onerror = () => {
      eventSource?.close()
      eventSource = null
      // 5초 후 재연결
      reconnectTimer = setTimeout(() => connect(token), 5000)
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
    notifications.value = []
    unreadCount.value = 0
  }

  return {
    notifications,
    unreadCount,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    connect,
    disconnect
  }
})
