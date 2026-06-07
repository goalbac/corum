<template>
  <header class="app-header">
    <div class="header-inner">

      <!-- 로고 -->
      <router-link to="/" class="logo" @click="menuStore.setActiveTopMenu(null)">
        <span class="logo-text">Corum</span>
      </router-link>

      <!-- 상단 메뉴 (4개 고정) -->
      <nav class="top-nav">
        <a
          v-for="menu in menuStore.topMenus"
          :key="menu.id"
          class="top-nav-item"
          :class="{ active: menuStore.activeTopMenuId === menu.id }"
          @click="handleTopMenuClick(menu)"
        >
          {{ menu.name }}
          <span v-if="menu.hasNew" class="new-badge">N</span>
        </a>
      </nav>

      <!-- 우측 사용자 영역 -->
      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="28" class="user-avatar">
                {{ authStore.member?.name?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ authStore.member?.name }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/mypage')">마이페이지</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">로그아웃</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button size="small" @click="$router.push('/login')">로그인</el-button>
        </template>
      </div>

    </div>
  </header>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const router = useRouter()

function handleTopMenuClick(menu) {
  menuStore.setActiveTopMenu(menu.id)
  if (menu.menuType === 'LINK' && menu.url) {
    if (menu.newWindow) window.open(menu.url, '_blank')
    else router.push(menu.url)
  }
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: var(--header-height);
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 24px;
  gap: 32px;
}

.logo {
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -0.5px;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.top-nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}

.top-nav-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.top-nav-item.active {
  color: var(--color-primary);
  background: var(--color-primary-bg);
}

.new-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  background: var(--color-danger);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  border-radius: 50%;
}

.header-right {
  flex-shrink: 0;
  margin-left: auto;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.user-info:hover {
  background: var(--color-bg-hover);
}

.user-avatar {
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}
</style>
