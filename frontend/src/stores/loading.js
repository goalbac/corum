import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const active = ref(false)
  let timer = null

  function start() {
    clearTimeout(timer)
    active.value = true
  }

  function finish() {
    // 최소 표시 시간 보장 (80ms) — 너무 빠른 깜빡임 방지
    timer = setTimeout(() => { active.value = false }, 80)
  }

  return { active, start, finish }
})
