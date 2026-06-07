<template>
  <header class="app-header">
    <div class="header-inner">

      <!-- 모바일 햄버거 -->
      <button class="hamburger" @click="emit('toggle-mobile-menu')" aria-label="메뉴">
        <i class="ti ti-menu-2"></i>
      </button>

      <!-- 로고 -->
      <router-link to="/" class="logo" @click="menuStore.setActiveTopMenu(null)">
        Corum
      </router-link>

      <!-- 상단 메뉴 (데스크탑) -->
      <nav class="top-nav">
        <a
          v-for="menu in menuStore.topMenus"
          :key="menu.id"
          class="nav-item"
          :class="{ active: menuStore.activeTopMenuId === menu.id }"
          @click="handleTopMenuClick(menu)"
        >
          {{ menu.name }}
          <span v-if="menu.hasNew" class="nav-dot"></span>
        </a>
      </nav>

      <!-- 우측 -->
      <div class="header-right">
        <!-- 라이트/다크 토글 -->
        <div class="theme-pill">
          <button
            class="tp-btn"
            :class="{ active: !themeStore.isDark }"
            @click="!themeStore.isDark || themeStore.toggle()"
            aria-label="라이트 모드"
          >
            <i class="ti ti-sun"></i>
            <span class="tp-label">라이트</span>
          </button>
          <button
            class="tp-btn"
            :class="{ active: themeStore.isDark }"
            @click="themeStore.isDark || themeStore.toggle()"
            aria-label="다크 모드"
          >
            <i class="ti ti-moon"></i>
            <span class="tp-label">다크</span>
          </button>
        </div>

        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click">
            <div class="user-btn">
              <div class="avatar">{{ authStore.member?.name?.charAt(0) || 'U' }}</div>
              <span class="user-name">{{ authStore.member?.name }}</span>
              <i class="ti ti-chevron-down" style="font-size:12px;color:var(--t3)"></i>
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
          <button class="login-btn" @click="$router.push('/login')">로그인</button>
        </template>
      </div>

    </div>
  </header>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useThemeStore } from '@/stores/theme'
import { useRouter } from 'vue-router'

const emit = defineEmits(['toggle-mobile-menu'])
const authStore = useAuthStore()
const menuStore = useMenuStore()
const themeStore = useThemeStore()
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
  background: var(--surface);
  box-shadow: var(--shadow-hdr);
  z-index: 100;
  transition: background 0.25s;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  gap: 0;
  max-width: 1440px;
  margin: 0 auto;
}

.hamburger {
  display: none;
  background: none;
  border: none;
  font-size: 20px;
  color: var(--t2);
  padding: 4px;
  margin-right: 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}
.hamburger:hover { background: var(--surface2); color: var(--t1); }

.logo {
  font-size: 17px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: -0.8px;
  margin-right: 28px;
  flex-shrink: 0;
}

.top-nav {
  display: flex;
  gap: 2px;
  flex: 1;
}

.nav-item {
  position: relative;
  padding: 6px 13px;
  border-radius: var(--radius-xs);
  font-size: 13px;
  font-weight: 400;
  color: var(--t2);
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}
.nav-item:hover { color: var(--t1); background: var(--surface2); }
.nav-item.active { color: var(--accent); background: var(--accent-bg); font-weight: 500; }

.nav-dot {
  position: absolute;
  top: 5px; right: 5px;
  width: 5px; height: 5px;
  background: var(--new);
  border-radius: 50%;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.theme-pill {
  display: flex;
  align-items: center;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  border-radius: 20px;
  overflow: hidden;
  padding: 2px;
  gap: 1px;
}
.tp-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 12px;
  border-radius: 16px;
  transition: var(--transition);
  cursor: pointer;
  font-family: inherit;
}
.tp-btn.active { background: var(--accent); color: #fff; }
.tp-btn:not(.active):hover { color: var(--t1); }

.user-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  padding: 4px 8px 4px 4px;
  border-radius: 20px;
  border: 0.5px solid var(--border);
  background: var(--surface2);
  transition: var(--transition);
}
.user-btn:hover { background: var(--surface); box-shadow: var(--shadow); }

.avatar {
  width: 26px; height: 26px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--t1);
}

.login-btn {
  padding: 6px 14px;
  border-radius: var(--radius-xs);
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  border: none;
  font-family: inherit;
  transition: var(--transition);
}
.login-btn:hover { background: var(--accent-t); }

@media (max-width: 768px) {
  .hamburger { display: flex; }
  .top-nav { display: none; }
  .tp-label { display: none; }
  .tp-btn { padding: 5px 8px; }
  .user-name { display: none; }
  .logo { margin-right: auto; }
  .header-right { gap: 6px; }
}
</style>
