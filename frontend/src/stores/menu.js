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

  // ':customSlug+' 경로(예: /news/notice, /news/notice/10177, /news/notice/write,
  // /news/notice/10177/edit)를 메뉴 + 글 조회/작성/수정 모드로 해석한다.
  // 1) 경로 전체가 그대로 어떤 메뉴의 url과 일치하면 목록/조회 모드
  // 2) 아니면 마지막 조각이 write/edit/글번호인지 보고 나머지를 메뉴 url로 다시 조회
  //    (이때 해당 메뉴가 게시판(BOARD) 타입일 때만 인정 — 아니면 그냥 404 처리)
  function parseCustomRoute(params) {
    const empty = { menu: null, postId: null, isWrite: false, isEdit: false }
    if (!params) return empty
    if (params.menuId != null) {
      return { menu: findMenuById(params.menuId), postId: params.postId ?? null, isWrite: false, isEdit: false }
    }
    if (!params.customSlug) return empty

    const segments = Array.isArray(params.customSlug) ? params.customSlug : [params.customSlug]
    const exact = findByUrl(`/${segments.join('/')}`)
    if (exact) return { menu: exact, postId: null, isWrite: false, isEdit: false }
    if (!segments.length) return empty

    const last = segments[segments.length - 1]

    if (last === 'write') {
      const menu = findByUrl(`/${segments.slice(0, -1).join('/')}`)
      if (menu?.pageType === 'BOARD') return { menu, postId: null, isWrite: true, isEdit: false }
    } else if (last === 'edit' && segments.length >= 2 && /^\d+$/.test(segments[segments.length - 2])) {
      const postId = segments[segments.length - 2]
      const menu = findByUrl(`/${segments.slice(0, -2).join('/')}`)
      if (menu?.pageType === 'BOARD') return { menu, postId, isWrite: false, isEdit: true }
    } else if (/^\d+$/.test(last)) {
      const menu = findByUrl(`/${segments.slice(0, -1).join('/')}`)
      if (menu?.pageType === 'BOARD') return { menu, postId: last, isWrite: false, isEdit: false }
    }

    return empty
  }

  // '/menu/:menuId'와 ':customSlug+' 두 라우트 모두에서 현재 메뉴만 필요할 때 쓰는 헬퍼
  function findMenuByRouteParams(params) {
    return parseCustomRoute(params).menu
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
    parseCustomRoute,
    firstNavigableMenu,
    resolveMenuPath,
    setActiveTopMenu
  }
})
