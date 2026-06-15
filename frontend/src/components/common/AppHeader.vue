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

        <template v-if="authStore.isLoggedIn">
          <!-- 알림 드롭다운 -->
          <el-dropdown trigger="click" popper-class="notif-popper" @visible-change="onNotifDropdown">
            <button type="button" class="notif-btn" aria-label="알림">
              <i class="ti ti-bell"></i>
              <span v-if="notifStore.unreadCount > 0" class="notif-badge">
                {{ notifStore.unreadCount > 99 ? '99+' : notifStore.unreadCount }}
              </span>
            </button>
            <template #dropdown>
              <div class="notif-dropdown">
                <div class="notif-header">
                  <span class="notif-title">알림</span>
                  <div class="notif-actions">
                    <button
                      v-if="notifStore.unreadCount > 0"
                      class="notif-action-btn"
                      @click.stop="notifStore.markAllAsRead()"
                    >모두 읽음</button>
                    <button
                      v-if="notifStore.notifications.length > 0"
                      class="notif-action-btn delete"
                      @click.stop="notifStore.removeAll()"
                    >모두 삭제</button>
                  </div>
                </div>
                <div class="notif-list">
                  <div v-if="notifStore.notifications.length === 0" class="notif-empty">
                    새 알림이 없습니다
                  </div>
                  <div
                    v-for="n in notifStore.notifications.slice(0, 20)"
                    :key="n.id"
                    class="notif-item"
                    :class="{ unread: !n.isRead }"
                    @click="handleNotifClick(n)"
                  >
                    <div class="notif-icon" :class="n.type.toLowerCase()">
                      <i :class="notifIcon(n.type)"></i>
                    </div>
                    <div class="notif-body">
                      <p class="notif-item-title">{{ n.title }}</p>
                      <p v-if="n.content" class="notif-item-content">{{ n.content }}</p>
                      <p class="notif-item-time">{{ formatTime(n.createdAt) }}</p>
                    </div>
                    <button
                      class="notif-del-btn"
                      title="삭제"
                      @click.stop="notifStore.remove(n.id)"
                    ><i class="ti ti-x"></i></button>
                  </div>
                </div>
              </div>
            </template>
          </el-dropdown>

          <!-- 사용자 드롭다운 -->
          <el-dropdown trigger="click" popper-class="user-menu-popper">
            <button type="button" class="user-btn">
              <div class="avatar-wrap">
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
                <span v-if="unreadMsgCount > 0" class="avatar-badge msg">{{ unreadMsgCount > 9 ? '9+' : unreadMsgCount }}</span>
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
                <!-- 라이트/다크 토글 -->
                <div class="user-menu-theme">
                  <button
                    class="utm-btn"
                    :class="{ active: !themeStore.isDark }"
                    @click.stop="themeStore.isDark && themeStore.toggle()"
                  >
                    <i class="ti ti-sun"></i> 라이트
                  </button>
                  <button
                    class="utm-btn"
                    :class="{ active: themeStore.isDark }"
                    @click.stop="!themeStore.isDark && themeStore.toggle()"
                  >
                    <i class="ti ti-moon"></i> 다크
                  </button>
                </div>
                <el-dropdown-item @click="$router.push('/mypage')">
                  <i class="ti ti-user menu-item-icon"></i>마이페이지
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/messages')">
                  <i class="ti ti-mail menu-item-icon"></i>쪽지함
                  <span v-if="unreadMsgCount > 0" class="msg-badge">{{ unreadMsgCount }}</span>
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="authStore.member?.isAdmin || authStore.member?.admin"
                  @click="$router.push('/admin')"
                >
                  <i class="ti ti-settings menu-item-icon"></i>관리자
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <i class="ti ti-logout menu-item-icon"></i>로그아웃
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <!-- 비로그인: 테마 토글 + 로그인 버튼 -->
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
          <button class="login-btn" @click="$router.push('/login')">로그인</button>
        </template>
      </div>
    </div>
  </header>

  <!-- 알림 토스트 -->
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-container">
      <div
        v-for="t in toasts"
        :key="t._key"
        class="toast-card"
        @click="handleToastClick(t)"
      >
        <div class="notif-icon" :class="t.type?.toLowerCase()">
          <i :class="notifIcon(t.type)"></i>
        </div>
        <div class="notif-body">
          <p class="notif-item-title">{{ t.title }}</p>
          <p v-if="t.content" class="notif-item-content">{{ t.content }}</p>
          <p class="notif-item-time">방금 전</p>
        </div>
        <button class="toast-close-btn" @click.stop="removeToast(t._key)">
          <i class="ti ti-x"></i>
        </button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useThemeStore } from '@/stores/theme'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'
import api from '@/api/axios'

const emit = defineEmits(['toggle-mobile-menu'])
const authStore = useAuthStore()
const headerAvatarError = ref(false)
watch(() => authStore.member?.profileImageUrl, () => { headerAvatarError.value = false })
const menuStore = useMenuStore()
const themeStore = useThemeStore()
const notifStore = useNotificationStore()
const router = useRouter()

const siteName = ref('')
const logoUrl  = ref('')
const unreadMsgCount = computed(() => notifStore.unreadMsgCount)

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const d = res.data.data
    siteName.value = d.siteName || ''
    logoUrl.value  = d.logoUrl  || ''
  } catch { /* ignore */ }
})

onUnmounted(() => {
  notifStore.disconnect()
})

watch(() => authStore.isLoggedIn, async (loggedIn) => {
  if (!loggedIn) {
    notifStore.disconnect()
    return
  }
  await notifStore.fetchUnreadMsgCount()
  await notifStore.fetchNotifications()
  await notifStore.fetchUnreadCount()
  const token = authStore.token
  if (token) notifStore.connect(token)
}, { immediate: true })

const toasts = ref([])
let toastKey = 0

watch(() => notifStore.toastNotif, (notif) => {
  if (!notif) return
  const key = ++toastKey
  toasts.value.push({ ...notif, _key: key })
  setTimeout(() => removeToast(key), 4000)
})

function removeToast(key) {
  const idx = toasts.value.findIndex(t => t._key === key)
  if (idx !== -1) toasts.value.splice(idx, 1)
}

function handleToastClick(t) {
  removeToast(t._key)
  if (t.linkUrl) router.push(t.linkUrl)
}

async function onNotifDropdown(visible) {
  if (visible) await notifStore.fetchNotifications()
}

function handleNotifClick(n) {
  notifStore.markAsRead(n.id)
  if (n.linkUrl) router.push(n.linkUrl)
}

function notifIcon(type) {
  const map = {
    COMMENT:  'ti ti-message-circle',
    POST:     'ti ti-file-text',
    INQUIRY:  'ti ti-help-circle',
    MESSAGE:  'ti ti-mail',
  }
  return map[type] || 'ti ti-bell'
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now - d) / 1000)
  if (diff < 60) return '방금 전'
  if (diff < 3600) return Math.floor(diff / 60) + '분 전'
  if (diff < 86400) return Math.floor(diff / 3600) + '시간 전'
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

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
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}
.top-nav::-webkit-scrollbar { display: none; }

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
  flex-shrink: 0;
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

/* 알림 버튼 */
.notif-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 50%;
  font-size: 19px;
  color: var(--t2);
  transition: var(--transition);
  cursor: pointer;
}

.notif-btn:hover { background: var(--surface2); color: var(--t1); }

.notif-badge {
  position: absolute;
  top: 1px;
  right: 0;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: #ef4444;
  color: #fff;
  font-size: 9px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  line-height: 1;
}

/* 알림 드롭다운 */
:global(.notif-popper.el-popper) {
  border: 0 !important;
  border-radius: 14px !important;
  overflow: hidden !important;
  box-shadow: 0 12px 36px rgba(15,23,42,0.14), 0 2px 8px rgba(15,23,42,0.06) !important;
  padding: 0 !important;
}

:global(.notif-popper .el-popper__arrow) { display: none !important; }

.notif-dropdown {
  width: 320px;
  background: var(--surface);
  border-radius: 14px;
  overflow: hidden;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 0.5px solid var(--border2);
}

.notif-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--t1);
}

.notif-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notif-action-btn {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}

.notif-action-btn:hover { opacity: 0.75; }
.notif-action-btn.delete { color: var(--t3); }
.notif-action-btn.delete:hover { color: #ef4444; opacity: 1; }

.notif-list {
  max-height: 360px;
  overflow-y: auto;
}

.notif-empty {
  padding: 28px 16px;
  text-align: center;
  color: var(--t3);
  font-size: 13px;
  font-weight: 600;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.notif-item:hover { background: var(--surface2); }
.notif-item.unread { background: color-mix(in srgb, var(--accent) 6%, transparent); }
.notif-item.unread:hover { background: color-mix(in srgb, var(--accent) 10%, transparent); }

.notif-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.notif-icon.comment { background: #dbeafe; color: #2563eb; }
.notif-icon.post    { background: #dcfce7; color: #16a34a; }
.notif-icon.inquiry { background: #fef3c7; color: #d97706; }
.notif-icon.message { background: #f3e8ff; color: #9333ea; }

.notif-body { flex: 1; min-width: 0; }

.notif-item-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--t1);
  line-height: 1.4;
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-item-content {
  font-size: 12px;
  color: var(--t3);
  line-height: 1.4;
  margin: 0 0 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-item-time {
  font-size: 11px;
  color: var(--t3);
  margin: 0;
  font-weight: 500;
}

.notif-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent);
  flex-shrink: 0;
  margin-top: 6px;
}

.notif-del-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  border-radius: 50%;
  font-size: 12px;
  color: var(--t3);
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  transition: background 0.15s, color 0.15s;
}

.notif-del-btn:hover { background: var(--border2); color: #ef4444; }
.notif-item:hover .notif-del-btn { display: flex; }
.notif-item:hover .notif-dot { display: none; }

/* 사용자 버튼 */
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

.avatar-wrap {
  position: relative;
  flex-shrink: 0;
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
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-badge {
  position: absolute;
  bottom: -2px;
  right: -4px;
  min-width: 14px;
  height: 14px;
  border-radius: 7px;
  font-size: 8px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
  border: 1.5px solid var(--surface);
}

.avatar-badge.msg { background: #9333ea; color: #fff; }

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--t1);
}

.user-arrow { font-size: 13px; color: var(--t3); }

.msg-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 0 4px;
  margin-left: 6px;
}

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

:global(.user-menu-popper .el-dropdown-menu__item) {
  display: flex !important;
  align-items: center;
  gap: 8px;
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

/* 드롭다운 내 테마 토글 */
.user-menu-theme {
  display: flex;
  gap: 6px;
  padding: 6px 8px 10px;
  border-bottom: 0.5px solid var(--border2);
  margin-bottom: 4px;
}

.utm-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 7px 0;
  border: 0.5px solid var(--border2);
  border-radius: 8px;
  background: var(--surface2);
  color: var(--t3);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.utm-btn.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.utm-btn:not(.active):hover {
  background: var(--border2);
  color: var(--t1);
}

/* 드롭다운 메뉴 아이콘 */
.menu-item-icon {
  font-size: 15px;
  width: 18px;
  flex-shrink: 0;
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

@media (max-width: 1100px) {
  .tp-label { display: none; }
  .tp-btn { padding: 5px 8px; }
  .nav-item { padding: 7px 10px; font-size: 14px; }
  .user-name { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

@media (max-width: 768px) {
  .hamburger { display: flex; }
  .top-nav { display: none; }
  .tp-btn { padding: 6px 8px; }
  .user-name { display: none; }
  .user-arrow { display: none; }
  .logo { margin-right: auto; }
  .header-right { gap: 6px; }
}

/* 알림 토스트 */
:global(.toast-container) {
  position: fixed;
  top: calc(var(--header-height, 56px) + 10px);
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}

:global(.toast-card) {
  pointer-events: all;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  width: 300px;
  background: var(--surface);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(15,23,42,0.16), 0 2px 8px rgba(15,23,42,0.08);
  border: 0.5px solid var(--border2);
  cursor: pointer;
}

:global(.toast-card:hover) { background: var(--surface2); }

:global(.toast-close-btn) {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: none;
  background: transparent;
  border-radius: 50%;
  font-size: 12px;
  color: var(--t3);
  cursor: pointer;
  flex-shrink: 0;
  margin-top: 2px;
  transition: background 0.15s, color 0.15s;
}

:global(.toast-close-btn:hover) { background: var(--border2); color: #ef4444; }

:global(.toast-enter-active),
:global(.toast-leave-active) { transition: opacity 0.25s, transform 0.25s; }
:global(.toast-enter-from) { opacity: 0; transform: translateX(40px); }
:global(.toast-leave-to) { opacity: 0; transform: translateX(40px); }
</style>
