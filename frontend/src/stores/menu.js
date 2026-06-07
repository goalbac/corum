import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loading = ref(false)
  const loaded = ref(false)
  const activeTopMenuId = ref(null)

  const topMenus = computed(() => menus.value.slice(0, 4))

  const flatMenus = computed(() => {
    const result = []
    const walk = (items = [], parent = null, top = null) => {
      items.forEach(menu => {
        const entry = { ...menu, parent: parent || null, top: top || null }
        entry.top = entry.top || entry
        result.push(entry)
        if (menu.children?.length) walk(menu.children, entry, entry.top)
      })
    }
    walk(menus.value)
    return result
  })

  const activeTopMenu = computed(() => findMenuById(activeTopMenuId.value))
  const sideMenus = computed(() => activeTopMenu.value?.children || [])

  async function fetchMenus(force = false) {
    if (loaded.value && !force) return
    loading.value = true
    try {
      const res = await api.get('/menus')
      menus.value = res.data.data || []
      loaded.value = true
    } catch (e) {
      console.error('메뉴 로딩 실패', e)
    } finally {
      loading.value = false
    }
  }

  function findMenuById(menuId) {
    if (menuId == null) return null
    const id = Number(menuId)
    return flatMenus.value.find(menu => Number(menu.id) === id) || null
  }

  function findTopMenu(menuId) {
    return findMenuById(menuId)?.top || null
  }

  function findBoardMenu(boardId) {
    if (boardId == null) return null
    const id = Number(boardId)
    return flatMenus.value.find(menu =>
      menu.menuType === 'PAGE' &&
      menu.pageType === 'BOARD' &&
      Number(menu.targetId) === id
    ) || null
  }

  function firstNavigableMenu(menu) {
    if (!menu) return null
    if (menu.menuType !== 'GROUP') return menu
    for (const child of menu.children || []) {
      const found = firstNavigableMenu(child)
      if (found) return found
    }
    return null
  }

  function resolveMenuPath(menu) {
    if (!menu) return null
    if (menu.menuType === 'LINK') return menu.url || null
    if (menu.menuType === 'GROUP') return resolveMenuPath(firstNavigableMenu(menu))
    if (menu.pageType === 'DASHBOARD') return '/'
    if (menu.url && !menu.urlAuto) return menu.url
    return `/menu/${menu.id}`
  }

  function setActiveTopMenu(menuId) {
    activeTopMenuId.value = menuId == null ? null : Number(menuId)
  }

  return {
    menus,
    topMenus,
    flatMenus,
    sideMenus,
    activeTopMenu,
    activeTopMenuId,
    loading,
    loaded,
    fetchMenus,
    findMenuById,
    findTopMenu,
    findBoardMenu,
    firstNavigableMenu,
    resolveMenuPath,
    setActiveTopMenu
  }
})
