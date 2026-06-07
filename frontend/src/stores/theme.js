import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') === 'dark')

  function apply() {
    if (isDark.value) {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
    apply()
  }

  apply()
  return { isDark, toggle }
})
