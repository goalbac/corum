import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useLoadingStore = defineStore('loading', () => {
  const active = ref(false)
  let pending = 0
  let timer = null

  // 동시에 여러 API 호출이 겹칠 수 있으므로(예: 목록+카운트 병렬 호출) 진행 중인
  // 요청 수를 세어, 전부 끝났을 때만 숨긴다. 하나라도 먼저 끝났다고 꺼버리면
  // 아직 로딩 중인데 바가 사라지는 문제가 생긴다.
  function start() {
    pending++
    clearTimeout(timer)
    active.value = true
  }

  function finish() {
    pending = Math.max(0, pending - 1)
    if (pending === 0) {
      // 최소 표시 시간 보장 (80ms) — 너무 빠른 깜빡임 방지
      timer = setTimeout(() => { active.value = false }, 80)
    }
  }

  return { active, start, finish }
})
