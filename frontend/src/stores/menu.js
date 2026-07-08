import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loading = ref(false)
  const loaded = ref(false)
  const activeTopMenuId = ref(null)

  const topMenus = computed(() => menus.value)

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
    // menu.url은 백엔드 Menu.resolveUrl() 결과 — 자동 넘버링이면 /{id}, 직접 지정이면 그 값.
    // 라우터의 ':customSlug' 한 단계 경로가 이 값을 그대로 받아 findByUrl로 되찾는다.
    return menu.url || `/menu/${menu.id}`
  }

  // 커스텀 URL(예: /notice) 또는 자동 넘버링(예: /12) 경로로 접속했을 때
  // 어떤 메뉴인지 찾기 위한 매칭 (menu.url과 정확히 일치해야 함)
  function findByUrl(path) {
    if (!path) return null
    return flatMenus.value.find(menu => menu.menuType === 'PAGE' && menu.url === path) || null
  }

  // '/menu/:menuId'와 ':customSlug+' 두 라우트 모두에서 현재 메뉴를 찾기 위한 공용 헬퍼
  // customSlug는 반복 가능한(+) 파라미터라 여러 단계 경로(예: news/notice)에서는 배열로 온다
  function findMenuByRouteParams(params) {
    if (!params) return null
    if (params.menuId != null) return findMenuById(params.menuId)
    if (params.customSlug) {
      const segments = Array.isArray(params.customSlug) ? params.customSlug : [params.customSlug]
      return findByUrl(`/${segments.join('/')}`)
    }
    return null
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
    findByUrl,
    findMenuByRouteParams,
    firstNavigableMenu,
    resolveMenuPath,
    setActiveTopMenu
  }
})
