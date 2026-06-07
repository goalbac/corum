<template>
  <div class="layout">

    <AppHeader @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />

    <!-- 모바일 오버레이 -->
    <transition name="fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    </transition>

    <!-- 모바일 드로어 -->
    <transition name="slide-left">
      <div v-if="mobileMenuOpen" class="mobile-drawer">
        <div class="drawer-header">
          <span class="drawer-logo">Corum</span>
          <button class="drawer-close" @click="mobileMenuOpen = false">
            <i class="ti ti-x"></i>
          </button>
        </div>
        <div class="drawer-nav">
          <div
            v-for="menu in menuStore.topMenus"
            :key="menu.id"
            class="drawer-top-item"
            :class="{ active: menuStore.activeTopMenuId === menu.id }"
            @click="handleMobileTopMenu(menu)"
          >
            {{ menu.name }}
            <i class="ti ti-chevron-down" style="font-size:12px;transition:transform .2s"
               :style="menuStore.activeTopMenuId === menu.id ? 'transform:rotate(180deg)' : ''"></i>
          </div>
          <template v-if="menuStore.sideMenus.length">
            <div
              v-for="sub in menuStore.sideMenus"
              :key="sub.id"
              class="drawer-sub-item"
              @click="handleMobileSubMenu(sub)"
            >
              {{ sub.name }}
              <span v-if="sub.hasNew" class="new-badge">N</span>
            </div>
          </template>
        </div>
      </div>
    </transition>

    <main class="main-content">
      <div class="page-container">

        <!-- 사이드바 + 컨텐츠 레이아웃 (메뉴 선택 시) -->
        <template v-if="menuStore.activeTopMenuId && menuStore.sideMenus.length">
          <div class="page-layout">

            <!-- 페이지 헤더 -->
            <div class="page-top">
              <div class="breadcrumb" v-if="breadcrumbs.length">
                <span v-for="(bc, i) in breadcrumbs" :key="i" class="bc-wrap">
                  <span class="bc-item" :class="{ last: i === breadcrumbs.length - 1 }">{{ bc }}</span>
                  <i v-if="i < breadcrumbs.length - 1" class="ti ti-chevron-right bc-arrow"></i>
                </span>
              </div>
              <h1 class="page-title" v-if="activeTopMenu">{{ activeTopMenu.name }}</h1>
              <p class="page-desc" v-if="activeTopMenu?.description">{{ activeTopMenu.description }}</p>
            </div>

            <!-- 카드형 레이아웃: 왼쪽 메뉴 | 오른쪽 컨텐츠 -->
            <div class="content-card">
              <!-- 왼쪽 사이드 메뉴 -->
              <aside class="side-menu">
                <div
                  v-for="menu in menuStore.sideMenus"
                  :key="menu.id"
                >
                  <div
                    class="side-item"
                    :class="{ active: isActive(menu) }"
                    @click="handleSideClick(menu)"
                  >
                    <span class="side-label">{{ menu.name }}</span>
                    <span v-if="menu.hasNew" class="new-badge">N</span>
                    <i v-if="menu.children?.length" class="ti ti-chevron-right side-arrow"
                       :class="{ rotated: isOpen(menu.id) }"></i>
                  </div>
                  <!-- 2depth 하위 -->
                  <template v-if="menu.children?.length && isOpen(menu.id)">
                    <div
                      v-for="child in menu.children"
                      :key="child.id"
                      class="side-item child"
                      :class="{ active: isActive(child) }"
                      @click="handleSideClick(child)"
                    >
                      <span class="side-label">{{ child.name }}</span>
                      <span v-if="child.hasNew" class="new-badge">N</span>
                    </div>
                  </template>
                </div>
              </aside>

              <!-- 오른쪽 컨텐츠 -->
              <div class="side-content">
                <router-view />
              </div>
            </div>
          </div>
        </template>

        <!-- 메뉴 미선택 (대시보드 등) -->
        <template v-else>
          <router-view />
        </template>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import { useMenuStore } from '@/stores/menu'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const menuStore = useMenuStore()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const route = useRoute()
const router = useRouter()
const mobileMenuOpen = ref(false)
const openMenuIds = ref([])

const activeTopMenu = computed(() =>
  menuStore.menus.find(m => m.id === menuStore.activeTopMenuId)
)

const breadcrumbs = computed(() => {
  if (!activeTopMenu.value) return []
  const crumbs = [activeTopMenu.value.name]
  return crumbs
})

function isActive(menu) {
  if (!menu.url) return false
  return route.path === menu.url || route.path.startsWith(menu.url + '/')
}

function isOpen(id) {
  return openMenuIds.value.includes(id)
}

function resolveMenuUrl(menu) {
  if (menu.menuType === 'LINK') return menu.url || null
  if (menu.menuType === 'GROUP') return null
  // PAGE 유형별 라우팅
  switch (menu.pageType) {
    case 'BOARD':     return menu.targetId ? `/board/${menu.targetId}` : null
    case 'CALENDAR':  return '/calendar'
    case 'DASHBOARD': return '/'
    case 'CONTENT':   return menu.targetId ? `/page/${menu.targetId}` : (menu.url || `/${menu.id}`)
    default:
      if (menu.url && !menu.urlAuto) return menu.url
      return `/${menu.id}`
  }
}

function handleSideClick(menu) {
  if (menu.children?.length) {
    const idx = openMenuIds.value.indexOf(menu.id)
    if (idx >= 0) openMenuIds.value.splice(idx, 1)
    else openMenuIds.value.push(menu.id)
    return
  }
  const url = resolveMenuUrl(menu)
  if (!url) return
  if (menu.newWindow) window.open(url, '_blank')
  else router.push(url)
}

function handleMobileTopMenu(menu) {
  menuStore.setActiveTopMenu(menu.id)
}

function handleMobileSubMenu(menu) {
  mobileMenuOpen.value = false
  const url = resolveMenuUrl(menu)
  if (!url) return
  if (menu.newWindow) window.open(url, '_blank')
  else router.push(url)
}

onMounted(async () => {
  await menuStore.fetchMenus()
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
})
</script>

<style scoped>
.layout { min-height: 100vh; background: var(--bg); transition: background 0.25s; }

.main-content {
  margin-top: var(--header-height);
  min-height: calc(100vh - var(--header-height));
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
}

/* 페이지 상단 */
.page-top { margin-bottom: 16px; }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.bc-wrap { display: flex; align-items: center; gap: 4px; }
.bc-item { font-size: 12px; color: var(--t3); }
.bc-item.last { color: var(--t2); }
.bc-arrow { font-size: 11px; color: var(--t3); }

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--t1);
  letter-spacing: -0.5px;
}
.page-desc {
  font-size: 13px;
  color: var(--t3);
  margin-top: 4px;
}

/* 카드형 레이아웃 */
.content-card {
  display: flex;
  gap: 0;
  background: var(--surface);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  border: 0.5px solid var(--border2);
  overflow: hidden;
  min-height: 500px;
}

/* 왼쪽 사이드 메뉴 */
.side-menu {
  width: 180px;
  flex-shrink: 0;
  border-right: 0.5px solid var(--border);
  padding: 12px 0;
  background: var(--surface2);
  transition: background 0.25s;
}

.side-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}
.side-item:hover { color: var(--t1); background: var(--surface); }
.side-item.active {
  color: var(--accent);
  background: var(--accent-bg);
  font-weight: 500;
}
.side-item.active::before {
  content: '';
  position: absolute;
  left: 0; top: 5px; bottom: 5px;
  width: 3px;
  background: var(--accent);
  border-radius: 0 3px 3px 0;
}
.side-item.child {
  padding-left: 28px;
  font-size: 12.5px;
}

.side-label { flex: 1; }

.side-arrow {
  font-size: 12px;
  color: var(--t3);
  transition: transform 0.2s;
}
.side-arrow.rotated { transform: rotate(90deg); }

/* 오른쪽 컨텐츠 */
.side-content {
  flex: 1;
  padding: 24px;
  min-width: 0;
  overflow: auto;
}

/* 모바일 오버레이 */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 200;
  backdrop-filter: blur(2px);
}

/* 모바일 드로어 */
.mobile-drawer {
  position: fixed;
  top: 0; left: 0; bottom: 0;
  width: 280px;
  background: var(--surface);
  z-index: 201;
  box-shadow: 4px 0 24px rgba(0,0,0,0.12);
  overflow-y: auto;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 0.5px solid var(--border);
  height: var(--header-height);
}
.drawer-logo {
  font-size: 17px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: -0.8px;
}
.drawer-close {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--t2);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--radius-xs);
}
.drawer-nav { padding: 8px 0; }
.drawer-top-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--t1);
  cursor: pointer;
  transition: var(--transition);
}
.drawer-top-item:hover { background: var(--surface2); }
.drawer-top-item.active { color: var(--accent); background: var(--accent-bg); }
.drawer-sub-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px 10px 36px;
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
  transition: var(--transition);
}
.drawer-sub-item:hover { background: var(--surface2); color: var(--t1); }

/* 트랜지션 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active { transition: transform 0.25s cubic-bezier(.4,0,.2,1); }
.slide-left-enter-from, .slide-left-leave-to { transform: translateX(-100%); }

/* 모바일 */
@media (max-width: 768px) {
  .page-container { padding: 16px 12px; }
  .content-card { flex-direction: column; }
  .side-menu { width: 100%; border-right: none; border-bottom: 0.5px solid var(--border); padding: 8px 0; display: none; }
  .side-content { padding: 16px; }
  .page-title { font-size: 17px; }
}
</style>