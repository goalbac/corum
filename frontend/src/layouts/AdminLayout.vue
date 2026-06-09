<template>
  <div class="admin-layout">
    <AppHeader @toggle-mobile-menu="mobileOpen = !mobileOpen" />

    <div class="admin-body">
      <aside class="admin-sidebar" :class="{ open: mobileOpen }">
        <template v-if="sidebarLoaded">
          <template v-for="section in sidebar" :key="section.id">
            <!-- 자식이 없는 단독 메뉴 (대시보드 등) -->
            <div v-if="!section.children?.length" class="sidebar-section">
              <router-link :to="section.url" class="sidebar-item" exact-active-class="active">
                <i :class="section.icon"></i> {{ section.name }}
              </router-link>
            </div>
            <!-- 섹션 그룹 -->
            <div v-else class="sidebar-section">
              <div class="section-title">{{ section.name }}</div>
              <router-link
                v-for="child in section.children"
                :key="child.id"
                :to="child.url"
                class="sidebar-item"
                active-class="active"
              >
                <i :class="child.icon"></i> {{ child.name }}
              </router-link>
            </div>
          </template>
        </template>
        <!-- 로딩 중 폴백: 기존 하드코딩 구조 -->
        <template v-else>
          <div class="sidebar-section">
            <router-link to="/admin" class="sidebar-item" exact-active-class="active">
              <i class="ti ti-layout-dashboard"></i> 대시보드
            </router-link>
          </div>
        </template>
      </aside>

      <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false" />

      <main class="admin-main">
        <div class="admin-container">
          <router-view v-slot="{ Component }">
            <Transition name="page-inner">
              <component :is="Component" :key="$route.path" />
            </Transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const authStore  = useAuthStore()
const menuStore  = useMenuStore()
const router     = useRouter()
const mobileOpen = ref(false)
const sidebar    = ref([])
const sidebarLoaded = ref(false)

onMounted(async () => {
  await menuStore.fetchMenus()
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const m = authStore.member
  if (m && !m.admin && !m.isAdmin) {
    router.push('/')
    return
  }

  // 동적 사이드바 로드
  try {
    const res = await api.get('/admin/sidebar')
    sidebar.value = res.data.data || []
  } catch {
    // API 실패 시 기본 구조 폴백
    sidebar.value = getDefaultSidebar()
  } finally {
    sidebarLoaded.value = true
  }
})

function getDefaultSidebar() {
  return [
    { id: 0, name: '대시보드', url: '/admin', icon: 'ti ti-layout-dashboard', children: [] },
    { id: 1, name: '콘텐츠', children: [
      { id: 10, name: '메뉴 관리',        url: '/admin/menus',             icon: 'ti ti-menu-2' },
      { id: 11, name: '게시판 관리',      url: '/admin/boards',            icon: 'ti ti-layout-list' },
      { id: 12, name: '대시보드 관리',    url: '/admin/dashboard-widgets', icon: 'ti ti-layout-dashboard' },
      { id: 13, name: '캘린더 관리',      url: '/admin/calendars',         icon: 'ti ti-calendar' },
      { id: 14, name: '안내 페이지 관리', url: '/admin/content-pages',     icon: 'ti ti-file-text' },
      { id: 15, name: '팝업/배너 관리',   url: '/admin/display',           icon: 'ti ti-speakerphone' },
    ]},
    { id: 2, name: '운영', children: [
      { id: 20, name: '문의 관리', url: '/admin/inquiries', icon: 'ti ti-mail' },
      { id: 21, name: '로그',      url: '/admin/stats',     icon: 'ti ti-list-details' },
    ]},
    { id: 3, name: '회원', children: [
      { id: 30, name: '회원 관리', url: '/admin/members', icon: 'ti ti-users' },
      { id: 31, name: '그룹 관리', url: '/admin/groups',  icon: 'ti ti-shield' },
      { id: 32, name: '약관 관리', url: '/admin/terms',   icon: 'ti ti-file-check' },
    ]},
    { id: 4, name: '설정', children: [
      { id: 40, name: '사이트 설정',      url: '/admin/settings',           icon: 'ti ti-settings' },
      { id: 41, name: '관리자 메뉴 권한', url: '/admin/admin-permissions',  icon: 'ti ti-lock' },
    ]},
  ]
}
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); transition: background .25s; }
.admin-body { display: flex; margin-top: var(--header-height); min-height: calc(100vh - var(--header-height)); }
.admin-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: var(--surface);
  border-right: 0.5px solid var(--border);
  padding: 16px 0;
  position: sticky;
  top: var(--header-height);
  height: calc(100vh - var(--header-height));
  overflow-y: auto;
  transition: background .25s;
}
.sidebar-section { margin-bottom: 20px; }
.section-title {
  font-size: 12px;
  font-weight: 800;
  color: var(--t3);
  text-transform: uppercase;
  padding: 0 16px;
  margin-bottom: 4px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  font-size: 14px;
  color: var(--t2);
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  text-decoration: none;
}
.sidebar-item i { font-size: 16px; }
.sidebar-item:hover { color: var(--t1); background: var(--surface2); }
.sidebar-item.active { color: var(--accent); background: var(--accent-bg); font-weight: 800; }
.sidebar-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--accent);
  border-radius: 0 3px 3px 0;
}
.admin-main { flex: 1; min-width: 0; }
.admin-container { padding: 24px; max-width: 1160px; }
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  z-index: 99;
  display: none;
}
@media (max-width: 768px) {
  .admin-sidebar {
    position: fixed;
    left: 0;
    top: var(--header-height);
    bottom: 0;
    z-index: 100;
    transform: translateX(-100%);
    transition: transform .25s;
  }
  .admin-sidebar.open { transform: translateX(0); }
  .mobile-overlay { display: block; }
  .admin-container { padding: 16px; }
}
</style>
