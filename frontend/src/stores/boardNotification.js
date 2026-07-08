import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

export const useBoardNotificationStore = defineStore('boardNotification', () => {
  const subs = ref({}) // boardId -> { notifyNewPost, notifyNewComment }

  function get(boardId) {
    return subs.value[boardId] || { notifyNewPost: false, notifyNewComment: false }
  }

  async function fetch(boardId) {
    if (!boardId) return
    try {
      const res = await api.get(`/boards/${boardId}/notification-subscription`)
      subs.value[boardId] = {
        notifyNewPost: !!res.data.data.notifyNewPost,
        notifyNewComment: !!res.data.data.notifyNewComment,
      }
    } catch { /* ignore */ }
  }

  async function update(boardId, patch) {
    try {
      const merged = { ...get(boardId), ...patch }
      const res = await api.put(`/boards/${boardId}/notification-subscription`, merged)
      subs.value[boardId] = {
        notifyNewPost: !!res.data.data.notifyNewPost,
        notifyNewComment: !!res.data.data.notifyNewComment,
      }
    } catch { /* ignore */ }
  }

  function reset() {
    subs.value = {}
  }

  return { subs, get, fetch, update, reset }
})
