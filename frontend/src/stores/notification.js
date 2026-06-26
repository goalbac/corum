import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const unreadMsgCount = ref(0)
  const toastNotif = ref(null)
  const isPushSubscribed = ref(false)
  let eventSource = null
  let reconnectTimer = null

  async function fetchNotifications() {
    try {
      const res = await api.get('/notifications')
      notifications.value = res.data.data || []
      unreadCount.value = notifications.value.filter(n => !n.isRead).length
    } catch { /* ignore */ }
  }

  async function fetchUnreadCount() {
    try {
      const res = await api.get('/notifications/unread-count')
      unreadCount.value = res.data.data?.count || 0
    } catch { /* ignore */ }
  }

  async function fetchUnreadMsgCount() {
    try {
      const res = await api.get('/messages/unread-count')
      unreadMsgCount.value = res.data.data?.count || 0
    } catch { /* ignore */ }
  }

  function decrementMsgCount(by = 1) {
    unreadMsgCount.value = Math.max(0, unreadMsgCount.value - by)
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

  async function remove(id) {
    try {
      await api.delete(`/notifications/${id}`)
      const idx = notifications.value.findIndex(n => n.id === id)
      if (idx !== -1) {
        if (!notifications.value[idx].isRead) {
          unreadCount.value = Math.max(0, unreadCount.value - 1)
        }
        notifications.value.splice(idx, 1)
      }
    } catch { /* ignore */ }
  }

  async function removeAll() {
    try {
      await api.delete('/notifications')
      notifications.value = []
      unreadCount.value = 0
    } catch { /* ignore */ }
  }

  function closeStream() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  function connect(token) {
    closeStream()
    const url = `/api/notifications/stream?token=${encodeURIComponent(token)}`
    eventSource = new EventSource(url)

    eventSource.addEventListener('notification', (e) => {
      try {
        const notif = JSON.parse(e.data)
        notifications.value.unshift(notif)
        unreadCount.value++
        if (notif.type === 'MESSAGE') unreadMsgCount.value++
        toastNotif.value = { ...notif, _ts: Date.now() }
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
    closeStream()
    notifications.value = []
    unreadCount.value = 0
    unreadMsgCount.value = 0
    toastNotif.value = null
  }

  // ===== Web Push =====

  function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
    const raw = atob(base64)
    return Uint8Array.from([...raw].map(c => c.charCodeAt(0)))
  }

  async function checkPushSubscribed() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      isPushSubscribed.value = false
      return
    }
    try {
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
      isPushSubscribed.value = !!sub
    } catch {
      isPushSubscribed.value = false
    }
  }

  async function subscribePush() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
      throw new Error('이 브라우저는 웹 푸시를 지원하지 않습니다.')
    }
    const keyRes = await api.get('/notifications/push/vapid-key')
    const vapidKey = keyRes.data.data?.vapidPublicKey
    if (!vapidKey) throw new Error('VAPID 공개키가 설정되지 않았습니다. 관리자에게 문의하세요.')

    const reg = await navigator.serviceWorker.register('/sw.js')
    await navigator.serviceWorker.ready

    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(vapidKey),
    })

    const json = sub.toJSON()
    await api.post('/notifications/push/subscribe', {
      endpoint: sub.endpoint,
      p256dh:   json.keys.p256dh,
      auth:     json.keys.auth,
    })
    isPushSubscribed.value = true
  }

  async function unsubscribePush() {
    try {
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
      if (sub) {
        await api.post('/notifications/push/unsubscribe', { endpoint: sub.endpoint })
        await sub.unsubscribe()
      }
    } catch { /* ignore */ }
    isPushSubscribed.value = false
  }

  return {
    notifications,
    unreadCount,
    unreadMsgCount,
    toastNotif,
    isPushSubscribed,
    fetchNotifications,
    fetchUnreadCount,
    fetchUnreadMsgCount,
    decrementMsgCount,
    markAsRead,
    markAllAsRead,
    remove,
    removeAll,
    connect,
    disconnect,
    checkPushSubscribed,
    subscribePush,
    unsubscribePush,
  }
})
