<template>
  <div class="layout">
    <AppHeader @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />
    <!-- 고정 헤더 높이만큼 공간 확보 -->
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
                <span v-if="sub.hasNew" class="new-badge">N</span>
              </button>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <main class="main-content">
      <div class="page-container">
        <template v-if="showsMenuLayout">
          <div class="menu-shell">
            <aside class="lnb" aria-label="하위 메뉴">
              <div class="lnb-title">{{ activeTopMenu?.name }}</div>
              <div class="lnb-list">
                <template v-for="menu in sideMenus" :key="menu.id">
                  <button
                    type="button"
                    class="lnb-item"
                    :class="{ active: isActiveSideMenu(menu), parent: menu.children?.length }"
                    @click="handleSideClick(menu)"
                  >
                    <span>{{ menu.name }}</span>
                    <span v-if="menu.hasNew" class="new-badge">N</span>
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
                      <span v-if="child.hasNew" class="new-badge">N</span>
                    </button>
                  </div>
                </template>
              </div>
            </aside>

            <section class="content-area">
              <div v-if="routeMenu" class="page-header">
                <div v-if="breadcrumbs.length" class="breadcrumb">
                  <span v-for="(item, index) in breadcrumbs" :key="item.id || item.name" class="bc-wrap">
                    <span class="bc-item" :class="{ last: index === breadcrumbs.length - 1 }">
                      {{ item.name }}
                    </span>
                    <i v-if="index < breadcrumbs.length - 1" class="ti ti-chevron-right bc-arrow"></i>
                  </span>
                </div>
                <h1 class="page-title">{{ routeMenu.name }}</h1>
                <p v-if="routeMenu.description" class="page-desc">{{ routeMenu.description }}</p>
              </div>
              <router-view />
            </section>
          </div>
        </template>

        <template v-else>
          <router-view />
        </template>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppBanner from '@/components/common/AppBanner.vue'
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
.layout {
  background: var(--bg);
  transition: background 0.25s;
}

/* 고정 헤더 높이만큼 공간을 차지해 배너와 콘텐츠를 헤더 아래로 내림 */
.header-spacer {
  height: var(--header-height);
  flex-shrink: 0;
}

.page-container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 22px 40px;
}

.menu-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 32px;
  align-items: start;
}

.lnb {
  position: sticky;
  top: calc(var(--header-height) + 24px);
  background: transparent;
}

.lnb-title {
  min-height: 44px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: var(--accent);
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  border-radius: var(--radius-xs);
}

.lnb-list {
  padding: 10px 0;
}

.lnb-item {
  width: 100%;
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border: none;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  font-size: 16px;
  font-weight: 600;
  text-align: left;
  transition: var(--transition);
}

.lnb-item:hover {
  background: var(--surface2);
  color: var(--t1);
}

.lnb-item.active {
  background: var(--accent-bg);
  color: var(--accent-t);
  font-weight: 800;
}

.lnb-item.parent {
  font-weight: 700;
}

.lnb-item.child {
  min-height: 38px;
  padding-left: 28px;
  font-size: 15px;
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
  font-size: 14px;
  transition: transform 0.2s;
}

.lnb-arrow.rotated { transform: rotate(90deg); }

.content-area {
  min-width: 0;
  color: var(--t1);
}

.page-header {
  margin-bottom: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.bc-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
}

.bc-item {
  font-size: 13px;
  color: var(--t3);
}

.bc-item.last {
  color: var(--accent);
  font-weight: 600;
}

.bc-arrow {
  font-size: 11px;
  color: var(--t3);
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1.3;
  margin-bottom: 6px;
}

.page-desc {
  font-size: 14px;
  color: var(--t3);
  line-height: 1.6;
  margin-bottom: 4px;
}

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

.drawer-close:hover {
  background: var(--surface2);
  color: var(--t1);
}

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
}

.drawer-top-item {
  justify-content: space-between;
  padding: 12px 12px;
  font-size: 16px;
  font-weight: 800;
}

.drawer-top-item i {
  font-size: 14px;
  transition: transform 0.2s;
}

.drawer-top-item i.rotated { transform: rotate(180deg); }

.drawer-sub-item {
  padding: 10px 12px 10px 26px;
  font-size: 15px;
  font-weight: 600;
}

.drawer-top-item:hover,
.drawer-sub-item:hover {
  background: var(--surface2);
  color: var(--t1);
}

.drawer-top-item.active,
.drawer-sub-item.active {
  background: var(--accent-bg);
  color: var(--accent-t);
}

.fade-enter-active,
.fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
.slide-left-enter-active,
.slide-left-leave-active { transition: transform 0.25s cubic-bezier(.4,0,.2,1); }
.slide-left-enter-from,
.slide-left-leave-to { transform: translateX(-100%); }

@media (max-width: 768px) {
  .page-container { padding: 18px 14px 32px; }
  .menu-shell { display: block; }
  .lnb { display: none; }
  .page-title { font-size: 18px; }
  .page-header { margin-bottom: 16px; }
}
</style>
