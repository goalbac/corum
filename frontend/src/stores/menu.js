import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loading = ref(false)

  // 상단 고정 메뉴 4개
  const topMenus = computed(() => menus.value.slice(0, 4))

  // 현재 활성 상단 메뉴 ID
  const activeTopMenuId = ref(null)

  // 현재 활성 상단 메뉴의 하위 메뉴
  const sideMenus = computed(() => {
    if (!activeTopMenuId.value) return []
    const top = menus.value.find(m => m.id === activeTopMenuId.value)
    return top ? top.children || [] : []
  })

  async function fetchMenus() {
    loading.value = true
    try {
      const res = await api.get('/menus')
      menus.value = res.data.data || []
    } catch (e) {
      console.error('메뉴 로딩 실패', e)
    } finally {
      loading.value = false
    }
  }

  function setActiveTopMenu(menuId) {
    activeTopMenuId.value = menuId
  }

  return { menus, topMenus, sideMenus, activeTopMenuId, loading, fetchMenus, setActiveTopMenu }
})
