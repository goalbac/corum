<template>
  <div class="layout">
    <AppHeader @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />
    <div class="header-spacer" />
    <AppBanner />

    <transition name="fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    </transition>

    <transition name="slide-left">
      <div v-if="mobileMenuOpen" class="mobile-drawer">
        <div class="drawer-header">
          <router-link to="/" class="drawer-logo" @click="mobileMenuOpen = false">Corum</router-link>
          <button class="drawer-close" @click="mobileMenuOpen = false" aria-label="메뉴 닫기">
            <i class="ti ti-x"></i>
          </button>
        </div>
        <div class="drawer-nav">
          <div v-for="menu in menuStore.topMenus" :key="menu.id">
            <button
              type="button"
              class="drawer-top-item"
              :class="{ active: activeTopMenu?.id === menu.id }"
              @click="handleMobileTopMenu(menu)"
            >
              <span>{{ menu.name }}</span>
              <i class="ti ti-chevron-down" :class="{ rotated: activeTopMenu?.id === menu.id }"></i>
            </button>
            <template v-if="activeTopMenu?.id === menu.id">
              <button
                v-for="sub in menu.children || []"
                :key="sub.id"
                type="button"
                class="drawer-sub-item"
                :class="{ active: isActiveSideMenu(sub) }"
                @click="handleMobileMenu(sub)"
              >
                {{ sub.name }}
                <span v-if="hasNewBoardPost(sub)" class="new-badge new-post-badge">새 글</span>
              </button>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <!-- flex column으로 footer를 항상 하단에 -->
    <main class="main-content">
      <div class="page-container">
        <template v-if="showsMenuLayout">
          <div class="menu-shell">

            <!-- 왼쪽: 마이페이지 스타일 사이드바 -->
            <aside class="lnb" aria-label="하위 메뉴">
              <!-- 그라데이션 헤더 (섹션명만) -->
              <div class="lnb-header">
                <div class="lnb-section-name">{{ activeTopMenu?.name }}</div>
              </div>

              <!-- 메뉴 목록 -->
              <div class="lnb-list">
                <template v-for="menu in sideMenus" :key="menu.id">
                  <button
                    type="button"
                    class="lnb-item"
                    :class="{ active: isActiveSideMenu(menu), parent: menu.children?.length }"
                    @click="handleSideClick(menu)"
                  >
                    <span>{{ menu.name }}</span>
                    <i v-if="menu.menuType === 'LINK'" class="ti ti-external-link lnb-link-icon"></i>
                    <span v-if="hasNewBoardPost(menu)" class="new-badge new-post-badge">새 글</span>
                    <i
                      v-if="menu.children?.length"
                      class="ti ti-chevron-right lnb-arrow"
                      :class="{ rotated: isOpen(menu.id) }"
                    ></i>
                  </button>

                  <div v-if="menu.children?.length && isOpen(menu.id)" class="lnb-children">
                    <button
                      v-for="child in menu.children"
                      :key="child.id"
                      type="button"
                      class="lnb-item child"
                      :class="{ active: isActiveSideMenu(child) }"
                      @click="handleSideClick(child)"
                    >
                      <span>{{ child.name }}</span>
                      <i v-if="child.menuType === 'LINK'" class="ti ti-external-link lnb-link-icon"></i>
                      <span v-if="hasNewBoardPost(child)" class="new-badge new-post-badge">새 글</span>
                    </button>
                  </div>
                </template>
              </div>
            </aside>

            <!-- 오른쪽: 흰색 카드 -->
            <section class="content-area">
              <div class="content-card">
                <!-- 브레드크럼 + 페이지 제목 + 설명 -->
                <div v-if="routeMenu" class="page-header">
                  <nav v-if="breadcrumbs.length > 1" class="breadcrumb" aria-label="breadcrumb">
                    <span v-for="(item, index) in breadcrumbs" :key="item.id || item.name" class="bc-wrap">
                      <span class="bc-item" :class="{ last: index === breadcrumbs.length - 1 }">
                        {{ item.name }}
                      </span>
                      <i v-if="index < breadcrumbs.length - 1" class="ti ti-chevron-right bc-sep"></i>
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
              </div>
            </section>

          </div>
        </template>

        <template v-else>
          <router-view v-slot="{ Component }">
            <Transition name="page-inner">
              <component :is="Component" :key="route.path" />
            </Transition>
          </router-view>
        </template>
      </div>

      <AppFooter />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
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

const routeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const activeTopMenu = computed(() => menuStore.findTopMenu(route.params.menuId))
const sideMenus = computed(() => activeTopMenu.value?.children || [])
const showsMenuLayout = computed(() => !!activeTopMenu.value && sideMenus.value.length > 0)

const activeSideMenuIds = computed(() => {
  const ids = new Set()
  let current = routeMenu.value
  while (current) {
    ids.add(Number(current.id))
    current = current.parent || null
  }
  return ids
})

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
  return activeSideMenuIds.value.has(Number(menu.id))
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
  if (menu.children?.length) {
    toggleOpen(menu.id)
    const first = menuStore.firstNavigableMenu(menu)
    if (first && first.id !== menu.id) navigateMenu(first)
    return
  }
  navigateMenu(menu)
}

function handleMobileTopMenu(menu) {
  if (activeTopMenu.value?.id === menu.id) {
    navigateMenu(menu)
    return
  }
  const first = menuStore.firstNavigableMenu(menu)
  navigateMenu(first || menu)
}

function handleMobileMenu(menu) {
  mobileMenuOpen.value = false
  handleSideClick(menu)
}

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
/* ===== 전체 레이아웃: sticky footer ===== */
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  transition: background 0.25s;
}

.header-spacer {
  height: var(--header-height);
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 22px 40px;
}

/* ===== 메뉴 셸 ===== */
.menu-shell {
  display: grid;
  grid-template-columns: 230px minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

/* ===== LNB 사이드바 (마이페이지 스타일) ===== */
.lnb {
  position: sticky;
  top: calc(var(--header-height) + 24px);
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* 그라데이션 헤더 */
.lnb-header {
  padding: 20px 18px 18px;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 50%, #e05fc4 100%);
  position: relative;
  overflow: hidden;
}

/* 헤더 내 미묘한 패턴 */
.lnb-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.15) 0%, transparent 50%),
    radial-gradient(circle at 10% 80%, rgba(255,255,255,0.08) 0%, transparent 40%);
  pointer-events: none;
}

.lnb-section-name {
  font-size: 18px;
  font-weight: 800;
  color: #fff;
  position: relative;
  z-index: 1;
  letter-spacing: -0.2px;
}

.lnb-list {
  padding: 8px;
}

.lnb-item {
  width: 100%;
  min-height: 40px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  transition: var(--transition);
  cursor: pointer;
}

.lnb-item:hover {
  background: var(--surface2);
  color: var(--t1);
}

.lnb-item.active {
  background: var(--accent-bg);
  color: var(--accent-t);
  font-weight: 700;
}

.lnb-item.parent { font-weight: 700; }

.lnb-item.child {
  min-height: 36px;
  padding-left: 26px;
  font-size: 13px;
  font-weight: 500;
}

.lnb-item span:first-child {
  flex: 1;
  min-width: 0;
}

.lnb-children {
  display: flex;
  flex-direction: column;
}

.lnb-arrow {
  color: var(--t3);
  font-size: 13px;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.lnb-arrow.rotated { transform: rotate(90deg); }

.lnb-link-icon {
  font-size: 11px;
  color: rgba(255,255,255,0.6);
  flex-shrink: 0;
}

.lnb-item:not(.active) .lnb-link-icon {
  color: var(--t4);
}

/* new 배지 */
.new-badge {
  font-size: 10px;
  background: var(--new);
  color: #fff;
  border-radius: 3px;
  padding: 1px 4px;
  font-weight: 700;
  flex-shrink: 0;
}

.new-post-badge {
  min-width: 0;
  padding: 1px 5px;
  border-radius: 6px;
  font-size: 10px;
  line-height: 1.5;
  letter-spacing: 0;
  white-space: nowrap;
}

/* ===== 오른쪽 콘텐츠 ===== */
.content-area {
  min-width: 0;
}

.content-card {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* 페이지 헤더 (브레드크럼 + 제목 + 설명) */
.page-header {
  padding: 22px 28px 18px;
  border-bottom: 1px solid var(--border2);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.bc-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bc-item {
  font-size: 12px;
  color: var(--t3);
  font-weight: 500;
}

.bc-item.last {
  color: var(--accent-t);
  font-weight: 600;
}

.bc-sep {
  font-size: 10px;
  color: var(--t4);
}

.page-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1.3;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: var(--t3);
  line-height: 1.6;
  margin: 0;
}

/* ===== 모바일 오버레이/드로어 ===== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.45);
  z-index: 200;
  backdrop-filter: blur(2px);
}

.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: min(320px, 86vw);
  background: var(--surface);
  z-index: 201;
  box-shadow: 4px 0 24px rgba(0,0,0,0.18);
  overflow-y: auto;
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  height: var(--header-height);
  border-bottom: 0.5px solid var(--border);
}

.drawer-logo {
  font-size: 18px;
  font-weight: 800;
  color: var(--accent);
}

.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  font-size: 22px;
  color: var(--t2);
  border-radius: var(--radius-xs);
}

.drawer-close:hover { background: var(--surface2); color: var(--t1); }

.drawer-nav { padding: 10px; }

.drawer-top-item,
.drawer-sub-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  border: none;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  text-align: left;
  transition: var(--transition);
  cursor: pointer;
}

.drawer-top-item {
  justify-content: space-between;
  padding: 12px 12px;
  font-size: 16px;
  font-weight: 800;
}

.drawer-top-item i { font-size: 14px; transition: transform 0.2s; }
.drawer-top-item i.rotated { transform: rotate(180deg); }

.drawer-sub-item {
  padding: 10px 12px 10px 26px;
  font-size: 15px;
  font-weight: 600;
}

.drawer-top-item:hover,
.drawer-sub-item:hover { background: var(--surface2); color: var(--t1); }

.drawer-top-item.active,
.drawer-sub-item.active { background: var(--accent-bg); color: var(--accent-t); }

/* ===== 트랜지션 ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active { transition: transform 0.25s cubic-bezier(.4,0,.2,1); }
.slide-left-enter-from, .slide-left-leave-to { transform: translateX(-100%); }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .page-container { padding: 18px 14px 28px; }
  .menu-shell { display: block; }
  .lnb { display: none; }
}
</style>
