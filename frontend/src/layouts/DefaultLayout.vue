<template>
  <div class="layout">
    <AppHeader @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />
    <AppBanner />

    <!-- 모바일 오버레이 -->
    <transition name="fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    </transition>

    <!-- 모바일 드로어 -->
    <transition name="slide-left">
      <div v-if="mobileMenuOpen" class="mobile-drawer">
        <div class="drawer-topbar">
          <button class="drawer-user-area" @click="handleDrawerUserClick">
            <template v-if="authStore.isLoggedIn">
              <div class="drawer-avatar">{{ authStore.member?.name?.charAt(0) || 'U' }}</div>
              <span class="drawer-user-name">{{ authStore.member?.name }}님</span>
            </template>
            <template v-else>
              <span class="drawer-login-hint">로그인해주세요</span>
            </template>
          </button>
          <button class="drawer-close" @click="mobileMenuOpen = false" aria-label="메뉴 닫기">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="drawer-panels">
          <nav class="drawer-left" aria-label="대메뉴">
            <button
              v-for="menu in menuStore.topMenus"
              :key="menu.id"
              type="button"
              class="drawer-left-item"
              :class="{ active: mobileSelectedTop?.id === menu.id }"
              @click="selectDrawerTop(menu)"
            >
              {{ menu.name }}
            </button>
          </nav>

          <div class="drawer-right" ref="drawerRightRef">
            <template v-if="mobileSelectedTop">
              <template v-if="mobileSelectedTop.children?.length">
                <template v-for="group in mobileSelectedTop.children" :key="group.id">
                  <template v-if="group.children?.length">
                    <button type="button" class="drawer-group-header" @click="handleDrawerNav(group)">
                      <span>{{ group.name }}</span>
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
                    </button>
                    <div class="drawer-child-grid">
                      <button
                        v-for="child in group.children"
                        :key="child.id"
                        type="button"
                        class="drawer-child-btn"
                        :class="{ active: isActiveSideMenu(child) }"
                        @click="handleDrawerNav(child)"
                      >
                        {{ child.name }}
                        <span v-if="hasNewBoardPost(child)" class="drawer-new-dot"></span>
                      </button>
                    </div>
                  </template>
                  <template v-else>
                    <button
                      type="button"
                      class="drawer-leaf-item"
                      :class="{ active: isActiveSideMenu(group) }"
                      @click="handleDrawerNav(group)"
                    >
                      <span>{{ group.name }}</span>
                      <span v-if="hasNewBoardPost(group)" class="drawer-new-dot"></span>
                    </button>
                  </template>
                </template>
              </template>
              <template v-else>
                <button type="button" class="drawer-group-header" @click="handleDrawerNav(mobileSelectedTop)">
                  <span>{{ mobileSelectedTop.name }}</span>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
                </button>
              </template>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <!-- 본문 행 (사이드바 + 메인) -->
    <div class="body-row">

      <!-- 사이드바 -->
      <aside v-if="showSidebar" class="sidebar">
        <!-- 마이페이지 모드 -->
        <template v-if="isMypage">
          <div class="sidebar-mypage-profile">
            <div class="sidebar-mypage-avatar">
              <img v-if="authStore.member?.profileImageUrl" :src="authStore.member.profileImageUrl" alt="프로필 사진" />
              <span v-else>{{ authStore.member?.name?.charAt(0) || 'U' }}</span>
            </div>
            <div class="sidebar-mypage-meta">
              <div class="sidebar-mypage-name">{{ authStore.member?.name || '-' }}</div>
              <div class="sidebar-mypage-username">@{{ authStore.member?.username }}</div>
            </div>
          </div>
          <nav class="sidebar-tree">
            <button
              v-for="item in mypageNavItems"
              :key="item.key"
              type="button"
              class="tree-node"
              :class="{ 'is-active': mypageActiveTab === item.key }"
              style="padding-left: 10px"
              @click="handleMypageNavClick(item)"
            >
              <span class="tree-chevron-spacer"></span>
              <span class="tree-icon" v-html="item.icon"></span>
              <span class="tree-label">{{ item.label }}</span>
            </button>
          </nav>
        </template>

        <!-- 트리 메뉴 모드 -->
        <template v-else>
        <div class="sidebar-header">
          <span class="sidebar-section-text">{{ isDashboard ? '홈' : activeTopMenu?.name?.toUpperCase() }}</span>
          <span class="sidebar-section-line"></span>
        </div>

        <nav class="sidebar-tree">
          <button
            v-if="isDashboard"
            type="button"
            class="tree-node is-active"
            style="padding-left: 10px"
          >
            <span class="tree-chevron-spacer"></span>
            <span class="tree-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11.5 12 4l9 7.5"/><path d="M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5"/></svg>
            </span>
            <span class="tree-label">홈</span>
          </button>
          <template v-else v-for="node in sidebarFlatNodes" :key="node.id">
            <button
              type="button"
              class="tree-node"
              :class="{
                'is-active': isActiveSideMenu(node),
                'is-folder': node.isFolder,
              }"
              :style="{ paddingLeft: (10 + node.depth * 16) + 'px' }"
              @click="handleSideClick(node)"
            >
              <!-- 폴더 chevron -->
              <span class="tree-chevron" v-if="node.isFolder">
                <svg v-if="isOpen(node.id)" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>
                <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
              </span>
              <span v-else class="tree-chevron-spacer"></span>

              <!-- 아이콘 -->
              <span class="tree-icon">
                <svg v-if="node.isFolder" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M21 19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="node.isExternal" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="14 3 14 9 20 9"/></svg>
              </span>

              <span class="tree-label">{{ node.name }}</span>

              <span v-if="hasNewBoardPost(node)" class="tree-new-badge">NEW</span>
              <svg v-if="node.isLocked" class="tree-lock" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
            </button>
          </template>
        </nav>
        </template>
      </aside>

      <!-- 메인 컬럼 -->
      <div class="main-col">
        <main class="main-content">
          <!-- 마이페이지 모바일 탭 바 (가로 스크롤, 사이드바가 숨겨지는 화면에서만 노출) -->
          <nav v-if="isMypage" class="mypage-mobile-tabs" aria-label="마이페이지 메뉴">
            <button
              v-for="item in mypageNavItems"
              :key="item.key"
              type="button"
              class="mp-mobile-tab"
              :class="{ active: mypageActiveTab === item.key }"
              @click="handleMypageNavClick(item)"
            >{{ item.label }}</button>
          </nav>

          <!-- 브레드크럼 + 페이지 제목 -->
          <div
            v-if="routeMenu && showSidebar && !isDetailPage && !isWritePage"
            class="page-header"
            :class="{ 'page-header--narrow': isNarrowContentPage }"
          >
            <nav v-if="breadcrumbs.length > 1" class="breadcrumb" aria-label="breadcrumb">
              <span v-for="(item, index) in breadcrumbs" :key="item.id || item.name" class="bc-wrap">
                <span class="bc-item" :class="{ last: index === breadcrumbs.length - 1 }">{{ item.name }}</span>
                <span v-if="index < breadcrumbs.length - 1" class="bc-sep">/</span>
              </span>
            </nav>
            <h1 class="page-title">{{ routeMenu.name }}</h1>
            <p v-if="routeMenu.description" class="page-desc">{{ routeMenu.description }}</p>
          </div>

          <router-view v-slot="{ Component }">
            <Transition name="page-inner">
              <component :is="Component" :key="route.path" />
            </Transition>
          </router-view>
        </main>

        <AppFooter />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppBanner from '@/components/common/AppBanner.vue'
import AppFooter from '@/components/common/AppFooter.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const mobileMenuOpen = ref(false)
const openMenuIds = ref([])
const mobileSelectedTop = ref(null)
const drawerRightRef = ref(null)

const routeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const activeTopMenu = computed(() => menuStore.findTopMenu(route.params.menuId))
const isDetailPage = computed(() => !!route.params.postId)
const isWritePage = computed(() => route.path.endsWith('/write'))
// 안내 페이지·캘린더는 본문 max-width에 맞춰 헤더(브레드크럼/제목)도 좁게 정렬
const isNarrowContentPage = computed(() => ['CONTENT', 'CALENDAR'].includes(routeMenu.value?.pageType))
const sideMenus = computed(() => activeTopMenu.value?.children || [])
// 홈 대시보드: 연결된 상위 메뉴가 없어도 사이드바에 "홈"을 표시
const isDashboard = computed(() => route.name === 'Dashboard')
const isMypage = computed(() => route.name === 'MyPage')
const showSidebar = computed(() => isMypage.value || isDashboard.value || (!!activeTopMenu.value && sideMenus.value.length > 0))

// ===== 마이페이지 사이드바 =====
const ICONS = {
  user: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>',
  lock: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2"></rect><path d="M8 11V8a4 4 0 0 1 8 0v3"></path></svg>',
  bell: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.7 21a2 2 0 0 1-3.4 0"></path></svg>',
  inquiry: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z"></path></svg>',
  report: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M10.3 3.8 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.8a2 2 0 0 0-3.4 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
  leave: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>',
}
// 쪽지함은 헤더 등 다른 경로로 접근 가능해 마이페이지 목록에서는 제외
const mypageNavItems = [
  { key: 'info',      label: '내 정보',      icon: ICONS.user },
  { key: 'password',  label: '비밀번호 변경', icon: ICONS.lock },
  { key: 'notif',     label: '알림 설정',     icon: ICONS.bell },
  { key: 'inquiries', label: '내 문의 내역',  icon: ICONS.inquiry },
  { key: 'reports',   label: '내 제보 내역',  icon: ICONS.report },
  { key: 'withdraw',  label: '회원 탈퇴',    icon: ICONS.leave },
]
const mypageActiveTab = computed(() => route.query.tab || 'info')
function handleMypageNavClick(item) {
  router.push({ name: 'MyPage', query: { tab: item.key } })
}

const activeSideMenuId = computed(() => routeMenu.value ? Number(routeMenu.value.id) : null)

const breadcrumbs = computed(() => {
  const items = []
  if (activeTopMenu.value) items.push(activeTopMenu.value)
  const stack = []
  let current = routeMenu.value
  while (current && current.id !== activeTopMenu.value?.id) {
    stack.unshift(current)
    current = current.parent || null
  }
  return items.concat(stack)
})

// 사이드바 트리를 평탄화 (depth 정보 포함)
function flattenMenuTree(menus, depth = 0, result = []) {
  for (const menu of menus) {
    const isFolder = !!(menu.children?.length) || menu.menuType === 'GROUP'
    result.push({
      ...menu,
      depth,
      isFolder,
      isExternal: menu.menuType === 'LINK' && menu.newWindow,
      isLocked: !!menu.isLocked,
    })
    if (isFolder && isOpen(menu.id)) {
      flattenMenuTree(menu.children || [], depth + 1, result)
    }
  }
  return result
}

const sidebarFlatNodes = computed(() => {
  return flattenMenuTree(sideMenus.value)
})

function isOpen(id) {
  return openMenuIds.value.includes(Number(id))
}

function toggleOpen(id) {
  const numericId = Number(id)
  const index = openMenuIds.value.indexOf(numericId)
  if (index >= 0) openMenuIds.value.splice(index, 1)
  else openMenuIds.value.push(numericId)
}

function isActiveSideMenu(menu) {
  return activeSideMenuId.value === Number(menu.id)
}

function hasNewBoardPost(menu) {
  return menu?.menuType === 'PAGE' && menu?.pageType === 'BOARD' && menu?.hasNew
}

function navigateMenu(menu) {
  const url = menuStore.resolveMenuPath(menu)
  if (!url) return
  if (menu.newWindow) window.open(url, '_blank')
  else router.push(url)
}

function handleSideClick(menu) {
  if (menu.isFolder || menu.children?.length) {
    toggleOpen(menu.id)
    return
  }
  navigateMenu(menu)
}

// ===== 모바일 드로어 =====
function selectDrawerTop(menu) {
  if (menu.children?.length) {
    mobileSelectedTop.value = menu
    nextTick(() => { if (drawerRightRef.value) drawerRightRef.value.scrollTop = 0 })
  } else {
    handleDrawerNav(menu)
  }
}

function handleDrawerNav(menu) {
  mobileMenuOpen.value = false
  navigateMenu(menu)
}

function handleDrawerUserClick() {
  mobileMenuOpen.value = false
  if (authStore.isLoggedIn) router.push('/mypage')
  else router.push('/login')
}

watch(mobileMenuOpen, (open) => {
  if (open) {
    const initial = activeTopMenu.value || menuStore.topMenus.find(m => m.children?.length) || menuStore.topMenus[0] || null
    mobileSelectedTop.value = initial
  }
})

watch(activeTopMenu, menu => {
  menuStore.setActiveTopMenu(menu?.id ?? null)
}, { immediate: true })

watch(routeMenu, menu => {
  if (!menu) return
  const parents = []
  let current = menu.parent
  while (current) {
    parents.push(Number(current.id))
    current = current.parent
  }
  openMenuIds.value = [...new Set([...openMenuIds.value, ...parents])]
}, { immediate: true })

onMounted(async () => {
  await menuStore.fetchMenus()
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
})
</script>

<style scoped>
/* ===== 전체 레이아웃 ===== */
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  transition: background 0.25s;
}

/* ===== 본문 행: 사이드바 + 메인 ===== */
.body-row {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* ===== 사이드바 ===== */
.sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 18px 14px;
  overflow-y: auto;
  position: sticky;
  top: var(--header-height);
  height: calc(100vh - var(--header-height));
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 8px 14px;
}

.sidebar-mypage-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px 16px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--border);
}

.sidebar-mypage-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 17px;
  flex-shrink: 0;
  overflow: hidden;
}
.sidebar-mypage-avatar img { width: 100%; height: 100%; object-fit: cover; }

.sidebar-mypage-meta { min-width: 0; }

.sidebar-mypage-name {
  font-size: 14.5px;
  font-weight: 800;
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-mypage-username { font-size: 11px; color: var(--t3); margin-top: 2px; }

/* 마이페이지 모바일 탭 바 — 데스크톱에서는 사이드바가 대신하므로 숨김 */
.mypage-mobile-tabs {
  display: none;
}

.sidebar-section-text {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--t3);
  white-space: nowrap;
}

.sidebar-section-line {
  flex: 1;
  height: 1px;
  background: var(--border);
}

/* ===== 트리 노드 ===== */
.sidebar-tree {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--t2);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
  line-height: 1.35;
}

.tree-node:hover { background: var(--surface2); color: var(--t1); }

.tree-node.is-active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 700;
}

.tree-node.is-folder { font-weight: 600; }

.tree-chevron {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.65;
  width: 13px;
}

.tree-chevron-spacer { width: 13px; flex-shrink: 0; }

.tree-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.7;
}

.tree-node.is-active .tree-icon { opacity: 1; }

.tree-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-new-badge {
  flex-shrink: 0;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--new);
  background: var(--surface);
  border: 1px solid var(--new);
  padding: 1px 5px;
  border-radius: 5px;
}

.tree-lock {
  flex-shrink: 0;
  color: var(--t3);
}

/* ===== 메인 컬럼 ===== */
.main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 30px 42px 48px;
  overflow-y: auto;
}

/* ===== 페이지 헤더 (브레드크럼 + 제목) ===== */
.page-header {
  margin-bottom: 28px;
}

.page-header--narrow {
  max-width: 864px;
  margin-left: auto;
  margin-right: auto;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.bc-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bc-item {
  font-size: 13px;
  color: var(--t3);
  font-weight: 500;
}

.bc-item.last {
  color: var(--t2);
  font-weight: 600;
}

.bc-sep {
  font-size: 13px;
  font-weight: 600;
  color: var(--t3);
  opacity: 0.5;
  margin: 0 4px;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1.3;
  letter-spacing: -0.02em;
  margin: 0 0 8px;
}

.page-desc {
  font-size: 16px;
  color: var(--t2);
  line-height: 1.6;
  margin: 0;
}

/* ===== 모바일 오버레이/드로어 ===== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  backdrop-filter: blur(2px);
}

.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 100vw;
  background: var(--surface);
  z-index: 201;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.drawer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}

.drawer-user-area {
  display: flex;
  align-items: center;
  gap: 9px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.drawer-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.drawer-user-name { font-size: 15px; font-weight: 700; color: var(--t1); }
.drawer-login-hint { font-size: 15px; font-weight: 600; color: var(--t2); }

.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--surface2);
  color: var(--t2);
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
}
.drawer-close:hover { background: var(--border); color: var(--t1); }

.drawer-panels {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.drawer-left {
  width: 120px;
  flex-shrink: 0;
  background: var(--surface2);
  border-right: 1px solid var(--border);
  overflow-y: auto;
  padding: 8px 0;
}

.drawer-left-item {
  width: 100%;
  padding: 13px 14px;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  line-height: 1.4;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
  word-break: keep-all;
}
.drawer-left-item:hover { color: var(--t1); }
.drawer-left-item.active {
  color: var(--t1);
  font-weight: 800;
  background: var(--surface);
  border-right: 2px solid var(--accent);
}

.drawer-right {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 10px 0 24px;
  background: var(--surface);
}

.drawer-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 14px 16px 6px;
  border: none;
  background: transparent;
  color: var(--t1);
  font-size: 15px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
  margin-top: 10px;
}
.drawer-group-header:first-child { margin-top: 0; }
.drawer-group-header svg { color: var(--t3); flex-shrink: 0; }
.drawer-group-header:hover { color: var(--accent); }

.drawer-child-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px 10px 10px;
  border-bottom: 1px solid var(--border);
}

.drawer-child-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 9px 6px;
  border: none;
  background: transparent;
  color: var(--t2);
  font-size: 13.5px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: color 0.15s, background 0.15s;
  word-break: keep-all;
}
.drawer-child-btn:hover { color: var(--t1); background: var(--surface2); }
.drawer-child-btn.active { color: var(--accent); font-weight: 700; }

.drawer-leaf-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  color: var(--t1);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.drawer-leaf-item:hover { background: var(--surface2); color: var(--accent); }
.drawer-leaf-item.active { color: var(--accent); font-weight: 700; }

.drawer-new-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--new);
  flex-shrink: 0;
}

/* ===== 트랜지션 ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active { transition: transform 0.28s cubic-bezier(.4, 0, .2, 1); }
.slide-left-enter-from, .slide-left-leave-to { transform: translateX(-100%); }
.page-inner-enter-active, .page-inner-leave-active { transition: opacity 0.18s; }
.page-inner-enter-from, .page-inner-leave-to { opacity: 0; }

/* ===== 반응형 ===== */
@media (max-width: 900px) {
  .sidebar { display: none; }
  .main-content { padding: 20px 20px 40px; }

  .mypage-mobile-tabs {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 10px;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--border);
    -webkit-overflow-scrolling: touch;
  }
  .mypage-mobile-tabs::-webkit-scrollbar { display: none; }

  .mp-mobile-tab {
    flex-shrink: 0;
    border: none;
    background: var(--surface2);
    color: var(--t2);
    font-weight: 600;
    font-size: 13px;
    padding: 8px 14px;
    border-radius: 18px;
    cursor: pointer;
    white-space: nowrap;
    transition: var(--transition);
  }
  .mp-mobile-tab.active { background: var(--accent); color: #fff; font-weight: 700; }
}

@media (max-width: 600px) {
  .main-content { padding: 16px 16px 32px; }
}
</style>
