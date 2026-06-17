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
        <!-- 상단 바: 로그인 정보 + 닫기 -->
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
            <i class="ti ti-x"></i>
          </button>
        </div>

        <!-- 두 패널 본문 -->
        <div class="drawer-panels">
          <!-- 왼쪽: 상위 메뉴 목록 -->
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

          <!-- 오른쪽: 선택된 상위 메뉴의 하위 항목 -->
          <div class="drawer-right" ref="drawerRightRef">
            <template v-if="mobileSelectedTop">
              <!-- 하위 메뉴가 있는 경우: 그룹 헤더 + 2열 그리드 -->
              <template v-if="mobileSelectedTop.children?.length">
                <template v-for="group in mobileSelectedTop.children" :key="group.id">
                  <!-- 그룹 자체에 자식이 있으면 헤더 + 자식 그리드 -->
                  <template v-if="group.children?.length">
                    <button type="button" class="drawer-group-header" @click="handleDrawerNav(group)">
                      <span>{{ group.name }}</span>
                      <i class="ti ti-chevron-right"></i>
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
                        <i v-if="hasNewBoardPost(child)" class="ti ti-point-filled lnb-new-dot"></i>
                      </button>
                    </div>
                  </template>
                  <!-- 자식 없는 리프: 직접 이동 항목으로 표시 -->
                  <template v-else>
                    <button
                      type="button"
                      class="drawer-leaf-item"
                      :class="{ active: isActiveSideMenu(group) }"
                      @click="handleDrawerNav(group)"
                    >
                      <span>{{ group.name }}</span>
                      <i v-if="hasNewBoardPost(group)" class="ti ti-point-filled lnb-new-dot"></i>
                    </button>
                  </template>
                </template>
              </template>
              <!-- 하위 메뉴 없음: 선택된 항목 자체로 이동 -->
              <template v-else>
                <button type="button" class="drawer-group-header" @click="handleDrawerNav(mobileSelectedTop)">
                  <span>{{ mobileSelectedTop.name }}</span>
                  <i class="ti ti-chevron-right"></i>
                </button>
              </template>
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
                    <i v-if="hasNewBoardPost(menu)" class="ti ti-point-filled lnb-new-dot"></i>
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
                      <i v-if="hasNewBoardPost(child)" class="ti ti-point-filled lnb-new-dot"></i>
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

// 드로어 열릴 때 현재 활성 대메뉴 또는 첫 메뉴 자동 선택
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

/* new dot */
.lnb-new-dot {
  font-size: 18px;
  color: var(--accent);
  flex-shrink: 0;
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
  font-size: 14px;
  color: var(--t3);
  font-weight: 500;
}

.bc-item.last {
  color: var(--accent-t);
  font-weight: 600;
}

.bc-sep {
  font-size: 12px;
  color: var(--t4);
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1.3;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 15px;
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
  width: 100vw;
  background: var(--surface);
  z-index: 201;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 상단 바 */
.drawer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  flex-shrink: 0;
  border-bottom: 0.5px solid var(--border2);
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

.drawer-user-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--t1);
}

.drawer-login-hint {
  font-size: 15px;
  font-weight: 600;
  color: var(--t2);
}

.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--surface2);
  font-size: 18px;
  color: var(--t2);
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
}
.drawer-close:hover { background: var(--border2); color: var(--t1); }

/* 두 패널 본문 */
.drawer-panels {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 왼쪽 패널 */
.drawer-left {
  width: 120px;
  flex-shrink: 0;
  background: var(--surface2);
  border-right: 0.5px solid var(--border2);
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
.drawer-left-item:hover { color: var(--t1); background: color-mix(in srgb, var(--border2) 50%, transparent); }
.drawer-left-item.active {
  color: var(--t1);
  font-weight: 800;
  background: var(--surface);
  border-right: 2px solid var(--accent);
}

/* 오른쪽 패널 */
.drawer-right {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 10px 0 24px;
  background: var(--surface);
}

/* 그룹 헤더 (2nd level with children) */
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
.drawer-group-header i { font-size: 13px; color: var(--t3); flex-shrink: 0; }
.drawer-group-header:hover { color: var(--accent-t); }
.drawer-group-header:hover i { color: var(--accent-t); }

/* 자식 2열 그리드 */
.drawer-child-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px 10px 10px;
  border-bottom: 0.5px solid var(--border2);
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
.drawer-child-btn.active { color: var(--accent-t); font-weight: 700; }

/* 리프 아이템 (자식 없는 2nd level) */
.drawer-leaf-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 0.5px solid var(--border2);
  background: transparent;
  color: var(--t1);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.drawer-leaf-item:hover { background: var(--surface2); color: var(--accent-t); }
.drawer-leaf-item.active { color: var(--accent-t); font-weight: 700; }

.lnb-new-dot {
  font-size: 18px;
  color: var(--accent);
  flex-shrink: 0;
}

/* ===== 트랜지션 ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active { transition: transform 0.28s cubic-bezier(.4,0,.2,1); }
.slide-left-enter-from, .slide-left-leave-to { transform: translateX(-100%); }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .page-container { padding: 0 0 28px; }
  .menu-shell { display: block; }
  .lnb { display: none; }
  .content-card {
    border: none;
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
