<template>
  <header class="app-header">
    <div class="header-inner">
      <button class="hamburger" @click="emit('toggle-mobile-menu')" aria-label="메뉴 열기">
        <i class="ti ti-menu-2"></i>
      </button>

      <router-link to="/" class="logo" @click="menuStore.setActiveTopMenu(null)">
        <img v-if="logoUrl" :src="logoUrl" :alt="siteName || 'Corum'" class="logo-img" />
        <span v-else>{{ siteName || 'Corum' }}</span>
      </router-link>

      <nav class="top-nav" aria-label="주 메뉴">
        <button
          v-for="menu in menuStore.topMenus"
          :key="menu.id"
          type="button"
          class="nav-item"
          :class="{ active: menuStore.activeTopMenuId === menu.id }"
          @click="handleTopMenuClick(menu)"
        >
          {{ menu.name }}
          <span v-if="menu.hasNew" class="nav-dot"></span>
        </button>
      </nav>

      <div class="header-right">
        <div class="theme-pill">
          <button
            class="tp-btn"
            :class="{ active: !themeStore.isDark }"
            @click="themeStore.isDark && themeStore.toggle()"
            aria-label="라이트 모드"
          >
            <i class="ti ti-sun"></i>
            <span class="tp-label">라이트</span>
          </button>
          <button
            class="tp-btn"
            :class="{ active: themeStore.isDark }"
            @click="!themeStore.isDark && themeStore.toggle()"
            aria-label="다크 모드"
          >
            <i class="ti ti-moon"></i>
            <span class="tp-label">다크</span>
          </button>
        </div>

        <template v-if="authStore.isLoggedIn">
          <el-dropdown trigger="click" popper-class="user-menu-popper">
            <button type="button" class="user-btn">
              <div class="avatar">
                <img
                  v-if="authStore.member?.profileImageUrl && !headerAvatarError"
                  :src="authStore.member.profileImageUrl"
                  class="avatar-img"
                  alt=""
                  @error="headerAvatarError = true"
                />
                <span v-else>{{ authStore.member?.name?.charAt(0) || 'U' }}</span>
              </div>
              <span class="user-name">{{ authStore.member?.name }}</span>
              <i class="ti ti-chevron-down user-arrow"></i>
            </button>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <div class="user-menu-profile">
                  <div class="menu-avatar">
                    <img
                      v-if="authStore.member?.profileImageUrl && !headerAvatarError"
                      :src="authStore.member.profileImageUrl"
                      alt=""
                    />
                    <span v-else>{{ authStore.member?.name?.charAt(0) || 'U' }}</span>
                  </div>
                  <div class="menu-profile-text">
                    <strong>{{ authStore.member?.name || authStore.member?.username || 'User' }}</strong>
                    <span>{{ authStore.member?.email || authStore.member?.username }}</span>
                  </div>
                </div>
                <el-dropdown-item @click="$router.push('/mypage')">마이페이지</el-dropdown-item>
                <el-dropdown-item
                  v-if="authStore.member?.isAdmin || authStore.member?.admin"
                  @click="$router.push('/admin')"
                >
                  <span class="admin-link">
                    <i class="ti ti-settings"></i> 관리자
                  </span>
                </el-dropdown-item>
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
import { ref, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useThemeStore } from '@/stores/theme'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const emit = defineEmits(['toggle-mobile-menu'])
const authStore = useAuthStore()
const headerAvatarError = ref(false)
watch(() => authStore.member?.profileImageUrl, () => { headerAvatarError.value = false })
const menuStore = useMenuStore()
const themeStore = useThemeStore()
const router = useRouter()

const siteName = ref('')
const logoUrl = ref('')

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const d = res.data.data
    siteName.value = d.siteName || ''
    logoUrl.value = d.logoUrl || ''
  } catch { /* ignore */ }
})

async function handleTopMenuClick(menu) {
  await menuStore.fetchMenus()
  const url = menuStore.resolveMenuPath(menu)
  if (!url) {
    menuStore.setActiveTopMenu(menu.id)
    return
  }
  if (menu.newWindow) window.open(url, '_blank')
  else router.push(url)
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
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
  max-width: 1440px;
  margin: 0 auto;
}

.hamburger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  background: transparent;
  border: none;
  font-size: 21px;
  color: var(--t2);
  margin-right: 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}

.hamburger:hover { background: var(--surface2); color: var(--t1); }

.logo {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 800;
  color: var(--accent);
  margin-right: 34px;
  flex-shrink: 0;
}

.logo-img {
  height: 32px;
  max-width: 140px;
  object-fit: contain;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-item {
  position: relative;
  padding: 8px 15px;
  border: none;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
  transition: var(--transition);
}

.nav-item:hover { color: var(--t1); background: var(--surface2); }
.nav-item.active { color: var(--accent); background: var(--accent-bg); }

.nav-dot {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 5px;
  height: 5px;
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
  padding: 2px;
  gap: 1px;
}

.tp-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 13px;
  border-radius: 16px;
  transition: var(--transition);
}

.tp-btn.active { background: var(--accent); color: #fff; }
.tp-btn:not(.active):hover { color: var(--t1); }

.user-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  min-height: 38px;
  padding: 4px 9px 4px 5px;
  border-radius: 999px;
  border: 0;
  background: transparent;
  transition: var(--transition);
}

.user-btn:hover {
  background: var(--surface2);
  box-shadow: inset 0 0 0 1px var(--border2);
}

.user-btn:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--accent-bg);
}

.avatar {
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
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--t1);
}

.user-arrow { font-size: 13px; color: var(--t3); }
.admin-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: inherit;
  font-weight: 700;
}

:global(.user-menu-popper.el-popper) {
  border: 0;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(15,23,42,0.14), 0 2px 8px rgba(15,23,42,0.06);
}

:global(.user-menu-popper .el-popper__arrow) { display: none; }

:global(.user-menu-popper .el-dropdown-menu) {
  width: 248px;
  padding: 8px;
  border: 0.5px solid var(--border2);
  border-radius: 14px;
  background: var(--surface);
}

:global(.user-menu-popper .el-dropdown-menu__item) {
  min-height: 42px;
  border-radius: 8px;
  padding: 0 12px;
  color: var(--t1);
  font-size: 14px !important;
  font-weight: 700;
  line-height: 1;
}

:global(.user-menu-popper .el-dropdown-menu__item:hover),
:global(.user-menu-popper .el-dropdown-menu__item:focus) {
  background: var(--surface2);
  color: var(--t1);
}

:global(.user-menu-popper .el-dropdown-menu__item--divided) {
  margin-top: 8px;
  border-top: 0.5px solid var(--border2);
}

:global(.user-menu-popper .el-dropdown-menu__item--divided::before) {
  display: none;
}

:global(.user-menu-popper .el-dropdown-menu__item:last-child) {
  color: var(--new);
}

.user-menu-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 10px 14px;
  margin-bottom: 6px;
  border-bottom: 0.5px solid var(--border2);
}

.menu-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-t));
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.3);
}

.menu-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-profile-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.menu-profile-text strong {
  color: var(--t1);
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-profile-text span {
  color: var(--t3);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-btn {
  padding: 7px 15px;
  border-radius: var(--radius-xs);
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border: none;
  transition: var(--transition);
}

.login-btn:hover { background: var(--accent-t); }

@media (max-width: 768px) {
  .hamburger { display: flex; }
  .top-nav { display: none; }
  .tp-label { display: none; }
  .tp-btn { padding: 6px 8px; }
  .user-name { display: none; }
  .logo { margin-right: auto; }
  .header-right { gap: 6px; }
}
</style>
