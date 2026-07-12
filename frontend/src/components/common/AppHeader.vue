<template>
  <header class="app-header">
    <div class="header-inner">
      <button class="hamburger" @click="emit('toggle-mobile-menu')" aria-label="메뉴 열기">
        <i class="ti ti-menu-2"></i>
      </button>

      <router-link to="/" class="logo" @click="menuStore.setActiveTopMenu(null)">
        <img v-if="logoUrl" :src="logoUrl" :alt="siteName || 'Corum'" class="logo-full-img" />
        <template v-else>
          <div class="logo-mark">
            <span class="logo-letter">C</span>
          </div>
          <div class="logo-text">
            <span class="logo-name">{{ siteName || '코럼' }}</span>
          </div>
        </template>
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
          <!-- 검색 버튼 (추후 사용 예정, 현재는 숨김) -->
          <button v-if="false" class="icon-btn" aria-label="검색">
            <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </button>

          <!-- 알림: 데스크톱 드롭다운 / 모바일 전체화면 -->
          <div v-if="!isMobile" class="notif-wrapper">
            <button type="button" class="notif-btn" aria-label="알림" @click.stop="toggleDesktopNotif">
              <i class="ti ti-bell"></i>
              <span v-if="notifStore.unreadCount > 0" class="notif-badge">
                {{ notifStore.unreadCount > 99 ? '99+' : notifStore.unreadCount }}
              </span>
            </button>
            <Teleport to="body">
              <div v-if="notifDesktopOpen" class="notif-overlay" @click="notifDesktopOpen = false" />
              <Transition name="notif-drop">
                <div v-if="notifDesktopOpen" class="notif-dropdown" @click.stop>
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
                      @click="handleNotifClick(n); notifDesktopOpen = false"
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
              </Transition>
            </Teleport>
          </div>

          <!-- 모바일 알림 버튼 -->
          <button v-else type="button" class="notif-btn" aria-label="알림" @click="openMobileNotif">
            <i class="ti ti-bell"></i>
            <span v-if="notifStore.unreadCount > 0" class="notif-badge">
              {{ notifStore.unreadCount > 99 ? '99+' : notifStore.unreadCount }}
            </span>
          </button>

          <!-- 모바일 전체화면 알림 패널 -->
          <Teleport to="body">
            <Transition name="notif-panel">
              <div v-if="mobileNotifOpen" class="mobile-notif-panel">
                <div class="mobile-notif-header">
                  <span class="mobile-notif-title">알림</span>
                  <div class="mobile-notif-actions">
                    <button
                      v-if="notifStore.unreadCount > 0"
                      class="notif-action-btn"
                      @click="notifStore.markAllAsRead()"
                    >모두 읽음</button>
                    <button
                      v-if="notifStore.notifications.length > 0"
                      class="notif-action-btn delete"
                      @click="notifStore.removeAll()"
                    >모두 삭제</button>
                  </div>
                  <button class="mobile-notif-close" @click="mobileNotifOpen = false" aria-label="닫기">
                    <i class="ti ti-x"></i>
                  </button>
                </div>
                <div class="mobile-notif-list">
                  <div v-if="notifStore.notifications.length === 0" class="notif-empty">
                    새 알림이 없습니다
                  </div>
                  <div
                    v-for="n in notifStore.notifications.slice(0, 50)"
                    :key="n.id"
                    class="notif-item"
                    :class="{ unread: !n.isRead }"
                    @click="handleNotifClick(n); mobileNotifOpen = false"
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
            </Transition>
          </Teleport>

          <!-- 구분선 + 테마 토글 (PC 전용, 모바일은 헤더 공간 절약을 위해 숨김) -->
          <div v-if="!isMobile" class="hdr-divider"></div>
          <button
            v-if="!isMobile"
            class="icon-btn"
            @click="themeStore.toggle()"
            :aria-label="themeStore.isDark ? '라이트 모드' : '다크 모드'"
          >
            <svg v-if="themeStore.isDark" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4.5"/><path d="M12 2v2.5M12 19.5V22M4.2 4.2l1.8 1.8M18 18l1.8 1.8M2 12h2.5M19.5 12H22M4.2 19.8 6 18M18 6l1.8-1.8"/></svg>
            <svg v-else width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8z"/></svg>
          </button>

          <!-- 사용자 드롭다운 -->
          <div class="user-wrapper">
            <button type="button" class="user-btn" @click.stop="userMenuOpen = !userMenuOpen">
              <div class="avatar-wrap">
                <div class="avatar">
                  <img
                    v-if="authStore.member?.profileImageUrl && !headerAvatarError"
                    :src="resolveFileUrl(authStore.member.profileImageUrl)"
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
            <Teleport to="body">
              <div v-if="userMenuOpen" class="notif-overlay" @click="userMenuOpen = false" />
              <Transition name="notif-drop">
                <div v-if="userMenuOpen" class="user-dropdown" @click.stop>
                  <div class="user-menu-profile">
                    <div class="menu-avatar">
                      <img
                        v-if="authStore.member?.profileImageUrl && !headerAvatarError"
                        :src="resolveFileUrl(authStore.member.profileImageUrl)"
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
                  <div class="user-menu-items">
                    <button class="user-menu-item" @click="$router.push('/mypage'); userMenuOpen = false">
                      <i class="ti ti-user menu-item-icon"></i>마이페이지
                    </button>
                    <button class="user-menu-item" @click="$router.push('/messages'); userMenuOpen = false">
                      <i class="ti ti-mail menu-item-icon"></i>쪽지함
                      <span v-if="unreadMsgCount > 0" class="msg-badge">{{ unreadMsgCount }}</span>
                    </button>
                    <button
                      v-if="authStore.member?.isAdmin || authStore.member?.admin"
                      class="user-menu-item"
                      @click="$router.push('/admin'); userMenuOpen = false"
                    >
                      <i class="ti ti-settings menu-item-icon"></i>관리자
                    </button>
                    <button class="user-menu-item user-menu-item--danger" @click="handleLogout(); userMenuOpen = false">
                      <i class="ti ti-logout menu-item-icon"></i>로그아웃
                    </button>
                  </div>
                </div>
              </Transition>
            </Teleport>
          </div>
        </template>
        <template v-else>
          <!-- 비로그인: 테마 토글(추후 사용 예정, 현재는 숨김) + 로그인 버튼 -->
          <div class="hdr-divider"></div>
          <button
            v-if="false"
            class="icon-btn"
            @click="themeStore.toggle()"
            :aria-label="themeStore.isDark ? '라이트 모드' : '다크 모드'"
          >
            <svg v-if="themeStore.isDark" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4.5"/><path d="M12 2v2.5M12 19.5V22M4.2 4.2l1.8 1.8M18 18l1.8 1.8M2 12h2.5M19.5 12H22M4.2 19.8 6 18M18 6l1.8-1.8"/></svg>
            <svg v-else width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8z"/></svg>
          </button>
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
import { useFavoriteMenuStore } from '@/stores/favoriteMenu'
import { useSiteStore } from '@/stores/site'
import { useRouter } from 'vue-router'
import { resolveFileUrl } from '@/utils/fileUrl'

const emit = defineEmits(['toggle-mobile-menu'])
const authStore = useAuthStore()
const headerAvatarError = ref(false)
watch(() => authStore.member?.profileImageUrl, () => { headerAvatarError.value = false })
const menuStore = useMenuStore()
const themeStore = useThemeStore()
const notifStore = useNotificationStore()
const favoriteMenuStore = useFavoriteMenuStore()
const siteStore = useSiteStore()
const router = useRouter()

const siteName = computed(() => siteStore.siteName)
const logoUrl  = computed(() => siteStore.logoUrl)
const unreadMsgCount = computed(() => notifStore.unreadMsgCount)

// 모바일 여부 (768px 기준)
const isMobile = ref(false)
const mobileNotifOpen = ref(false)
const notifDesktopOpen = ref(false)
const userMenuOpen = ref(false)

function updateIsMobile() {
  isMobile.value = window.innerWidth <= 768
}

async function toggleDesktopNotif() {
  if (!notifDesktopOpen.value) {
    await notifStore.fetchNotifications()
  }
  notifDesktopOpen.value = !notifDesktopOpen.value
}

async function openMobileNotif() {
  await notifStore.fetchNotifications()
  mobileNotifOpen.value = true
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})

onUnmounted(() => {
  notifStore.disconnect()
  window.removeEventListener('resize', updateIsMobile)
})

watch(() => authStore.isLoggedIn, async (loggedIn) => {
  if (!loggedIn) {
    notifStore.disconnect()
    favoriteMenuStore.reset()
    return
  }
  await notifStore.fetchUnreadMsgCount()
  await notifStore.fetchNotifications()
  await notifStore.fetchUnreadCount()
  favoriteMenuStore.fetchFavorites(true)
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
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--header-height);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  transition: background 0.25s;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 26px;
  gap: 0;
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
  gap: 11px;
  flex-shrink: 0;
  /* 헤더 좌측 padding(26px)을 포함해 사이드바 폭과 정확히 맞춤 -> 메뉴가 사이드바 끝 지점부터 시작 */
  width: calc(var(--sidebar-width) - 26px);
  box-sizing: border-box;
  text-decoration: none;
  overflow: hidden;
}

.logo-full-img {
  height: calc(var(--header-height) - 18px);
  width: auto;
  max-width: 100%;
  object-fit: contain;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.logo-letter {
  color: #fff;
  font-weight: 800;
  font-size: 18px;
  letter-spacing: -0.04em;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.12;
  min-width: 0;
}

.logo-name {
  font-weight: 800;
  font-size: 17px;
  letter-spacing: -0.02em;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-nav {
  display: flex;
  align-items: stretch;
  gap: 2px;
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
}
.top-nav::-webkit-scrollbar { display: none; }

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 17px;
  border: none;
  background: transparent;
  color: var(--t2);
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.15s;
}

.nav-item:hover { color: var(--t1); }
.nav-item.active { color: var(--t1); font-weight: 700; }

.nav-item.active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--accent);
}

.nav-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--new);
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}


/* 헤더 아이콘 버튼 공통 */
.icon-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  border-radius: 10px;
  font-size: 19px;
  color: var(--t2);
  transition: background 0.15s, color 0.15s;
  cursor: pointer;
}
.icon-btn:hover { background: var(--surface2); color: var(--t1); }

/* 알림 버튼 */
.notif-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  background: transparent;
  border-radius: 10px;
  font-size: 19px;
  color: var(--t2);
  transition: background 0.15s, color 0.15s;
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

/* 알림 래퍼 */
.notif-wrapper { position: relative; }

/* 알림 오버레이 (클릭 외부 닫기) */
.notif-overlay {
  position: fixed;
  inset: 0;
  z-index: 8999;
}

/* 알림 드롭다운 */
.notif-dropdown {
  position: fixed;
  top: var(--header-height, 64px);
  right: 20px;
  z-index: 9000;
  width: 380px;
  background: var(--surface);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.14), 0 2px 8px rgba(15, 23, 42, 0.06);
  border: 1px solid var(--border);
}

/* 드롭 트랜지션 */
.notif-drop-enter-active,
.notif-drop-leave-active { transition: opacity 0.18s, transform 0.18s cubic-bezier(.4,0,.2,1); }
.notif-drop-enter-from,
.notif-drop-leave-to { opacity: 0; transform: translateY(-8px); }

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px 12px;
  border-bottom: 0.5px solid var(--border2);
}

.notif-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--t1);
}

.notif-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notif-action-btn {
  font-size: 13px;
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
  max-height: 440px;
  overflow-y: auto;
}

.notif-empty {
  padding: 36px 18px;
  text-align: center;
  color: var(--t3);
  font-size: 14px;
  font-weight: 600;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 18px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
  border-bottom: 0.5px solid var(--border2);
}
.notif-item:last-child { border-bottom: none; }

.notif-item:hover { background: var(--surface2); }
.notif-item.unread { background: color-mix(in srgb, var(--accent) 6%, transparent); }
.notif-item.unread:hover { background: color-mix(in srgb, var(--accent) 10%, transparent); }

.notif-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

.notif-icon.comment { background: #dbeafe; color: #2563eb; }
.notif-icon.post    { background: #dcfce7; color: #16a34a; }
.notif-icon.inquiry { background: #fef3c7; color: #d97706; }
.notif-icon.message { background: #f3e8ff; color: #9333ea; }

.notif-body { flex: 1; min-width: 0; }

.notif-item-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  line-height: 1.4;
  margin: 0 0 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-item-content {
  font-size: 13px;
  color: var(--t3);
  line-height: 1.4;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-item-time {
  font-size: 12px;
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

/* 구분선 */
.hdr-divider {
  width: 1px;
  height: 24px;
  background: var(--border);
  margin: 0 6px;
  flex-shrink: 0;
}

/* 사용자 버튼 */
.user-btn {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 5px 9px 5px 5px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 24px;
  cursor: pointer;
  margin-left: 4px;
  transition: background 0.15s;
}

.user-btn:hover { background: var(--surface2); }

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

/* 사용자 드롭다운 래퍼 */
.user-wrapper { position: relative; }

/* 사용자 드롭다운 패널 */
.user-dropdown {
  position: fixed;
  top: var(--header-height, 64px);
  right: 20px;
  z-index: 9000;
  width: 248px;
  background: var(--surface);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.14), 0 2px 8px rgba(15, 23, 42, 0.06);
  border: 1px solid var(--border);
  padding: 8px;
}

/* 사용자 메뉴 항목 */
.user-menu-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--t1);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  text-align: left;
  transition: background 0.14s;
}

.user-menu-item:hover { background: var(--surface2); }
.user-menu-item--danger { color: #ef4444; margin-top: 4px; border-top: 0.5px solid var(--border2); border-radius: 0 0 8px 8px; }

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
  .nav-item { padding: 0 12px; font-size: 14px; }
  .logo { width: auto; }
  .header-inner { gap: 20px; }
  .user-name { max-width: 80px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

@media (max-width: 900px) {
  .hamburger { display: flex; }
  .top-nav { display: none; }
  .user-name { display: none; }
  .user-arrow { display: none; }
  .logo { width: auto; }
  .header-right { gap: 2px; }
  .header-inner { padding: 0 14px; gap: 10px; }
  .icon-btn { width: 36px; height: 36px; }
  .hdr-divider { margin: 0 2px; }

  /* 아바타 바깥 원형 버튼(테두리+배경) 제거 -> 프로필 원 하나만 보이도록
     -webkit-appearance 리셋 필요: 네이티브 버튼 배경이 CSS background보다 위에 그려짐 */
  .user-btn {
    -webkit-appearance: none;
    appearance: none;
    border: none;
    background: transparent;
    padding: 0;
    margin-left: 0;
    border-radius: 50%;
  }
  .user-btn:hover { background: transparent; }
}

/* ===== 모바일 전체화면 알림 패널 ===== */
.mobile-notif-panel {
  position: fixed;
  inset: 0;
  background: var(--surface);
  z-index: 9000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.mobile-notif-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  height: 54px;
  flex-shrink: 0;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface);
}

.mobile-notif-title {
  font-size: 17px;
  font-weight: 800;
  color: var(--t1);
  flex: 1;
}

.mobile-notif-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mobile-notif-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--surface2);
  color: var(--t2);
  border-radius: 50%;
  font-size: 17px;
  cursor: pointer;
  flex-shrink: 0;
  margin-left: 4px;
}
.mobile-notif-close:hover { background: var(--border2); color: var(--t1); }

.mobile-notif-list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 트랜지션 */
.notif-panel-enter-active,
.notif-panel-leave-active { transition: opacity 0.22s, transform 0.22s cubic-bezier(.4,0,.2,1); }
.notif-panel-enter-from,
.notif-panel-leave-to { opacity: 0; transform: translateY(16px); }

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
