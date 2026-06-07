<template>
  <div class="admin-layout">
    <AppHeader @toggle-mobile-menu="mobileOpen = !mobileOpen" />

    <div class="admin-body">
      <aside class="admin-sidebar" :class="{ open: mobileOpen }">
        <div class="sidebar-section">
          <div class="section-title">회원</div>
          <router-link to="/admin/members" class="sidebar-item" active-class="active">
            <i class="ti ti-users"></i> 회원 관리
          </router-link>
          <router-link to="/admin/groups" class="sidebar-item" active-class="active">
            <i class="ti ti-shield"></i> 그룹 관리
          </router-link>
        </div>

        <div class="sidebar-section">
          <div class="section-title">콘텐츠</div>
          <router-link to="/admin/boards" class="sidebar-item" active-class="active">
            <i class="ti ti-layout-list"></i> 게시판 관리
          </router-link>
          <router-link to="/admin/menus" class="sidebar-item" active-class="active">
            <i class="ti ti-menu-2"></i> 메뉴 관리
          </router-link>
        </div>

        <div class="sidebar-section">
          <div class="section-title">운영</div>
          <router-link to="/admin/inquiries" class="sidebar-item" active-class="active">
            <i class="ti ti-mail"></i> 문의 관리
          </router-link>
          <router-link to="/admin/settings" class="sidebar-item" active-class="active">
            <i class="ti ti-settings"></i> 사이트 설정
          </router-link>
        </div>
      </aside>

      <div v-if="mobileOpen" class="mobile-overlay" @click="mobileOpen = false" />

      <main class="admin-main">
        <div class="admin-container">
          <router-view />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const router = useRouter()
const mobileOpen = ref(false)

onMounted(async () => {
  await menuStore.fetchMenus()
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (authStore.member && !authStore.member.admin) {
    router.push('/')
  }
})
</script>

<style scoped>
.admin-layout { min-height: 100vh; background: var(--bg); transition: background .25s; }

.admin-body {
  display: flex;
  margin-top: var(--header-height);
  min-height: calc(100vh - var(--header-height));
}

.admin-sidebar {
  width: 200px;
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
  font-size: 11px;
  font-weight: 600;
  color: var(--t3);
  text-transform: uppercase;
  padding: 0 16px;
  margin-bottom: 4px;
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  text-decoration: none;
}
.sidebar-item i { font-size: 15px; }
.sidebar-item:hover { color: var(--t1); background: var(--surface2); }
.sidebar-item.active {
  color: var(--accent);
  background: var(--accent-bg);
  font-weight: 500;
}
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
.admin-container { padding: 24px; max-width: 1100px; }

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
