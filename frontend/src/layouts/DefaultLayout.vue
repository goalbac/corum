<template>
  <div class="layout">
    <AppHeader @toggle-mobile-menu="mobileMenuOpen = !mobileMenuOpen" />
    <AppBanner />

    <!-- 모바일 오버레이 -->
    <transition name="fade">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false" />
    </transition>

    <!-- 모바일 드로어 -->
    <transition name="slide-left">
      <div v-if="mobileMenuOpen" class="mobile-drawer">
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
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="drawer-panels">
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

            <button
              type="button"
              class="drawer-left-item drawer-left-item--fav"
              :class="{ active: mobileSelectedTop?.id === FAVORITES_TAB_ID }"
              @click="selectDrawerFavorites"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.1 8.6 22 9.3 17 14.1 18.2 21 12 17.8 5.8 21 7 14.1 2 9.3 8.9 8.6 12 2"></polygon></svg>
              즐겨찾는 메뉴
            </button>
          </nav>

          <div class="drawer-right" ref="drawerRightRef">
            <template v-if="mobileSelectedTop?.id === FAVORITES_TAB_ID">
              <p v-if="!favoriteMenuItems.length" class="drawer-fav-empty">즐겨찾는 메뉴가 없습니다</p>
              <button
                v-for="fav in favoriteMenuItems"
                :key="fav.id"
                type="button"
                class="drawer-fav-leaf"
                @click="handleDrawerNav(fav)"
              >
                <span v-if="favMenuPath(fav)" class="drawer-fav-path">{{ favMenuPath(fav) }}</span>
                <span class="drawer-fav-name">{{ fav.name }}</span>
              </button>
            </template>
            <template v-else-if="mobileSelectedTop">
              <template v-if="mobileSelectedTop.children?.length">
                <template v-for="group in mobileSelectedTop.children" :key="group.id">
                  <template v-if="group.children?.length">
                    <button type="button" class="drawer-group-header" @click="handleDrawerNav(group)">
                      <span>{{ group.name }}</span>
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
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
                        <span v-if="hasNewBoardPost(child)" class="drawer-new-dot"></span>
                      </button>
                    </div>
                  </template>
                  <template v-else>
                    <button
                      type="button"
                      class="drawer-leaf-item"
                      :class="{ active: isActiveSideMenu(group) }"
                      @click="handleDrawerNav(group)"
                    >
                      <span>{{ group.name }}</span>
                      <span v-if="hasNewBoardPost(group)" class="drawer-new-dot"></span>
                    </button>
                  </template>
                </template>
              </template>
              <template v-else>
                <button type="button" class="drawer-group-header" @click="handleDrawerNav(mobileSelectedTop)">
                  <span>{{ mobileSelectedTop.name }}</span>
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
                </button>
              </template>
            </template>
          </div>
        </div>
      </div>
    </transition>

    <!-- 본문 행 (사이드바 + 메인) -->
    <div class="body-row">

      <!-- 사이드바 -->
      <aside v-if="showSidebar" class="sidebar">
        <!-- 마이페이지 모드 -->
        <template v-if="isMypage">
          <div class="sidebar-mypage-profile">
            <div class="sidebar-mypage-avatar">
              <img v-if="authStore.member?.profileImageUrl" :src="resolveFileUrl(authStore.member.profileImageUrl)" alt="프로필 사진" />
              <span v-else>{{ authStore.member?.name?.charAt(0) || 'U' }}</span>
            </div>
            <div class="sidebar-mypage-meta">
              <div class="sidebar-mypage-name">{{ authStore.member?.name || '-' }}</div>
              <div class="sidebar-mypage-username">@{{ authStore.member?.username }}</div>
            </div>
          </div>
          <nav class="sidebar-tree">
            <button
              v-for="item in mypageNavItems"
              :key="item.key"
              type="button"
              class="tree-node"
              :class="{ 'is-active': mypageActiveTab === item.key }"
              style="padding-left: 10px"
              @click="handleMypageNavClick(item)"
            >
              <span class="tree-chevron-spacer"></span>
              <span class="tree-icon" v-html="item.icon"></span>
              <span class="tree-label">{{ item.label }}</span>
            </button>
          </nav>
        </template>

        <!-- 트리 메뉴 모드 -->
        <template v-else>
        <div v-if="!isDashboard" class="sidebar-header">
          <span class="sidebar-section-text">{{ activeTopMenu?.name?.toUpperCase() }}</span>
          <span class="sidebar-section-line"></span>
        </div>

        <nav class="sidebar-tree">
          <button
            v-if="isDashboard"
            type="button"
            class="tree-node is-active"
            style="padding-left: 10px"
          >
            <span class="tree-chevron-spacer"></span>
            <span class="tree-icon">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M3 11.5 12 4l9 7.5"/><path d="M5 9.5V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.5"/></svg>
            </span>
            <span class="tree-label">대시보드</span>
          </button>
          <template v-else v-for="node in sidebarFlatNodes" :key="node.id">
            <button
              type="button"
              class="tree-node"
              :class="{
                'is-active': isActiveSideMenu(node),
                'is-folder': node.isFolder,
              }"
              :style="{ paddingLeft: (10 + node.depth * 16) + 'px' }"
              @click="handleSideClick(node)"
            >
              <!-- 폴더 chevron -->
              <span class="tree-chevron" v-if="node.isFolder">
                <svg v-if="isOpen(node.id)" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg>
                <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="9 18 15 12 9 6"/></svg>
              </span>
              <span v-else class="tree-chevron-spacer"></span>

              <!-- 아이콘 -->
              <span class="tree-icon">
                <svg v-if="node.isFolder" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M21 19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="node.isExternal" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
                <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="14 3 14 9 20 9"/></svg>
              </span>

              <span class="tree-label">{{ node.name }}</span>

              <span v-if="hasNewBoardPost(node)" class="tree-new-badge">NEW</span>
              <svg v-if="node.isLocked" class="tree-lock" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></svg>
            </button>
          </template>
        </nav>

        <!-- 홈 사이드바 전용: 오늘 날짜 / 즐겨찾는 메뉴 / 오늘 일정 / 다가오는 일정 -->
        <template v-if="isDashboard">
          <!-- TODAY 날짜 카드 (컴팩트 가로형) -->
          <div class="today-card">
            <div class="today-card-decor-1"></div>
            <div class="today-card-body">
              <div class="today-card-daybox">
                <span class="today-card-day">{{ todayCard.day }}</span>
                <span class="today-card-weekday">{{ todayCard.weekday }}</span>
              </div>
              <span class="today-card-divider"></span>
              <div class="today-card-meta">
                <span class="today-card-date">{{ todayCard.dateLabel }}</span>
                <span class="today-card-sub">{{ todayCard.weekdayFull }} · {{ todayCard.lunar }}</span>
              </div>
            </div>
          </div>

          <!-- 주간 일정 (주간 스트립 + 일정) -->
          <div class="week-strip-card">
            <div class="week-strip-header">
              <span class="week-strip-title">주간 일정</span>
              <span class="week-strip-range">{{ weekRangeLabel }}</span>
            </div>
            <div class="week-strip-grid">
              <button
                v-for="d in weekDays"
                :key="d.dateKey"
                type="button"
                class="week-strip-cell"
                :class="{ 'is-today': d.isToday, 'is-selected': d.dateKey === selectedWeekDate && !d.isToday }"
                @click="selectedWeekDate = d.dateKey"
              >
                <span class="week-strip-dow" :class="{ sun: d.dow === '일', sat: d.dow === '토' }">{{ d.dow }}</span>
                <span class="week-strip-day">{{ d.day }}</span>
                <span class="week-strip-dot" :class="{ 'is-visible': d.hasEvent }"></span>
              </button>
            </div>
            <div class="week-strip-divider"></div>
            <div class="week-strip-selected-label">{{ selectedDayLabel }}</div>
            <ul class="today-events-list">
              <li v-if="!selectedDayEvents.length" class="sidebar-empty-text">일정이 없습니다</li>
              <li v-for="ev in selectedDayEvents" :key="ev.id" class="today-event-item">
                <span class="today-event-dot" :style="{ background: ev.calendarColor || 'var(--accent)' }"></span>
                <div class="today-event-body">
                  <div class="today-event-title">{{ ev.title }}</div>
                  <div class="today-event-time">{{ ev.isAllDay ? '종일' : formatEventTime(ev.startAt) }}</div>
                </div>
              </li>
            </ul>
          </div>

          <!-- 즐겨찾는 메뉴 -->
          <div class="sidebar-header sidebar-header--secondary">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="sidebar-section-icon"><polygon points="12 2 15.1 8.6 22 9.3 17 14.1 18.2 21 12 17.8 5.8 21 7 14.1 2 9.3 8.9 8.6 12 2"></polygon></svg>
            <span class="sidebar-section-text">즐겨찾는 메뉴</span>
            <span class="sidebar-section-line"></span>
          </div>
          <nav class="sidebar-tree">
            <p v-if="!favoriteMenuItems.length" class="sidebar-empty-text">즐겨찾는 메뉴가 없습니다</p>
            <button
              v-for="fav in favoriteMenuItems"
              :key="fav.id"
              type="button"
              class="tree-node fav-node"
              style="padding-left: 10px"
              @click="navigateMenu(fav)"
            >
              <span class="tree-icon">
                <svg v-if="fav.menuType === 'GROUP'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M21 19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2z"/></svg>
                <svg v-else-if="fav.pageType === 'CALENDAR'" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><rect x="3" y="4.5" width="18" height="16" rx="2.5"></rect><line x1="3" y1="9.5" x2="21" y2="9.5"></line></svg>
                <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"><path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="14 3 14 9 20 9"/></svg>
              </span>
              <span class="fav-node-body">
                <span v-if="favMenuPath(fav)" class="fav-node-path">{{ favMenuPath(fav) }}</span>
                <span class="tree-label">{{ fav.name }}</span>
              </span>
              <svg class="fav-node-chevron" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </nav>
        </template>
        </template>
      </aside>

      <!-- 메인 컬럼 -->
      <div class="main-col">
        <main class="main-content">
          <!-- 마이페이지 모바일 탭 바 (가로 스크롤, 사이드바가 숨겨지는 화면에서만 노출) -->
          <nav v-if="isMypage" class="mypage-mobile-tabs" aria-label="마이페이지 메뉴">
            <button
              v-for="item in mypageNavItems"
              :key="item.key"
              type="button"
              class="mp-mobile-tab"
              :class="{ active: mypageActiveTab === item.key }"
              @click="handleMypageNavClick(item)"
            >{{ item.label }}</button>
          </nav>

          <!-- 브레드크럼 + 페이지 제목 -->
          <div
            v-if="routeMenu && showSidebar && !isDetailPage && !isWritePage"
            class="page-header"
            :class="{ 'page-header--narrow': isNarrowContentPage }"
          >
            <div class="page-header-top">
              <nav v-if="breadcrumbs.length > 1" class="breadcrumb" aria-label="breadcrumb">
                <span v-for="(item, index) in breadcrumbs" :key="item.id || item.name" class="bc-wrap">
                  <span class="bc-item" :class="{ last: index === breadcrumbs.length - 1 }">{{ item.name }}</span>
                  <span v-if="index < breadcrumbs.length - 1" class="bc-sep">/</span>
                </span>
              </nav>
              <el-tooltip
                :content="favoriteMenuStore.isFavorite(routeMenu.id) ? '즐겨찾기 해제' : '즐겨찾기 추가'"
                placement="top"
              >
                <button
                  type="button"
                  class="favorite-star-btn"
                  :class="{ 'is-favorite': favoriteMenuStore.isFavorite(routeMenu.id) }"
                  @click="favoriteMenuStore.toggleFavorite(routeMenu.id)"
                >⭐</button>
              </el-tooltip>
              <el-popover
                v-if="isBoardPage && authStore.isLoggedIn"
                trigger="click"
                placement="bottom"
                width="200"
              >
                <template #reference>
                  <button
                    type="button"
                    class="board-notif-bell-btn"
                    :class="{ 'is-subscribed': boardSub.notifyNewPost || boardSub.notifyNewComment }"
                    title="게시판 알림 설정"
                  >🔔</button>
                </template>
                <div class="board-notif-popover-body">
                  <label class="board-notif-check-row">
                    <el-checkbox
                      :model-value="boardSub.notifyNewPost"
                      @change="v => boardNotificationStore.update(currentBoardId, { notifyNewPost: v })"
                    />
                    새 글 알림
                  </label>
                  <label class="board-notif-check-row">
                    <el-checkbox
                      :model-value="boardSub.notifyNewComment"
                      @change="v => boardNotificationStore.update(currentBoardId, { notifyNewComment: v })"
                    />
                    댓글 알림
                  </label>
                </div>
              </el-popover>
            </div>
            <h1 class="page-title">{{ routeMenu.name }}</h1>
            <p v-if="routeMenu.description" class="page-desc">{{ routeMenu.description }}</p>
          </div>

          <router-view v-slot="{ Component }">
            <Transition name="page-inner">
              <component :is="Component" :key="route.path" />
            </Transition>
          </router-view>
        </main>

        <AppFooter />
      </div>
    </div>
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
import { useFavoriteMenuStore } from '@/stores/favoriteMenu'
import { useBoardNotificationStore } from '@/stores/boardNotification'
import KoreanLunarCalendar from 'korean-lunar-calendar'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()
const favoriteMenuStore = useFavoriteMenuStore()
const boardNotificationStore = useBoardNotificationStore()
const mobileMenuOpen = ref(false)
const openMenuIds = ref([])
const mobileSelectedTop = ref(null)
const drawerRightRef = ref(null)

const routeMenu = computed(() => menuStore.findMenuByRouteParams(route.params))
const activeTopMenu = computed(() => routeMenu.value?.top || null)
// 직접 지정 URL(:customSlug+)로 들어온 게시글 상세/수정은 route.params.postId가 없고
// customSlug 배열 안에 글번호가 들어있으므로 parseCustomRoute로도 확인해야 한다
const isDetailPage = computed(() => !!route.params.postId || !!menuStore.parseCustomRoute(route.params).postId)
const isWritePage = computed(() => route.path.endsWith('/write'))
const isBoardPage = computed(() => routeMenu.value?.pageType === 'BOARD')
const currentBoardId = computed(() => routeMenu.value?.targetId ? Number(routeMenu.value.targetId) : null)
const boardSub = computed(() => boardNotificationStore.get(currentBoardId.value))
// 안내 페이지·캘린더는 본문 max-width에 맞춰 헤더(브레드크럼/제목)도 좁게 정렬
const isNarrowContentPage = computed(() => ['CONTENT', 'CALENDAR1'].includes(routeMenu.value?.pageType))
const sideMenus = computed(() => activeTopMenu.value?.children || [])
// 홈 대시보드: 연결된 상위 메뉴가 없어도 사이드바에 "홈"을 표시
const isDashboard = computed(() => route.name === 'Dashboard')
const isMypage = computed(() => route.name === 'MyPage')
const showSidebar = computed(() => isMypage.value || isDashboard.value || (!!activeTopMenu.value && sideMenus.value.length > 0))

// ===== 홈 사이드바: 오늘 날짜 / 즐겨찾는 메뉴 / 오늘 일정 / 다가오는 일정 =====
const favoriteMenuItems = computed(() =>
  favoriteMenuStore.favoriteIds
    .map(id => menuStore.findMenuById(id))
    .filter(Boolean)
)

// 동명 메뉴 구분용 경로 표시 (예: "한울인 안내 > 게시판")
function favMenuPath(menu) {
  const parts = []
  let current = menu?.parent
  while (current) {
    parts.unshift(current.name)
    current = current.parent
  }
  return parts.join(' > ')
}

const WEEKDAY_LABELS = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']
const WEEKDAY_FULL_LABELS = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일']
const todayCard = computed(() => {
  const now = new Date()
  const lunarCal = new KoreanLunarCalendar()
  lunarCal.setSolarDate(now.getFullYear(), now.getMonth() + 1, now.getDate())
  const lunar = lunarCal.getLunarCalendar()
  return {
    weekday: WEEKDAY_LABELS[now.getDay()],
    weekdayFull: WEEKDAY_FULL_LABELS[now.getDay()],
    day: now.getDate(),
    dateLabel: `${now.getFullYear()}년 ${now.getMonth() + 1}월 ${now.getDate()}일`,
    lunar: `음력 ${lunar.month}.${lunar.day}${lunar.intercalation ? ' (윤달)' : ''}`,
  }
})

const WEEKDAY_LABELS_KO = ['일', '월', '화', '수', '목', '금', '토']

function toDateKey(dt) {
  const d = new Date(dt)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const weekEvents = ref([])
const selectedWeekDate = ref(toDateKey(new Date()))

async function fetchWeekEvents() {
  try {
    const res = await api.get('/dashboard/week-events')
    weekEvents.value = res.data.data || []
  } catch {
    weekEvents.value = []
  }
}

function formatEventTime(dt) {
  if (!dt) return ''
  return new Date(dt).toTimeString().slice(0, 5)
}

// 이번 주 일(공휴일 포함) ~ 토 날짜 목록
const weekDates = computed(() => {
  const now = new Date()
  const sunday = new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay())
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(sunday)
    d.setDate(sunday.getDate() + i)
    return d
  })
})

const weekRangeLabel = computed(() => {
  const first = weekDates.value[0]
  const last = weekDates.value[6]
  return `${first.getMonth() + 1}.${first.getDate()} – ${last.getMonth() + 1}.${last.getDate()}`
})

const weekDays = computed(() => {
  const todayKey = toDateKey(new Date())
  const eventKeys = new Set(weekEvents.value.map(ev => toDateKey(ev.startAt)))
  return weekDates.value.map((d, i) => {
    const dateKey = toDateKey(d)
    return {
      dateKey,
      dow: WEEKDAY_LABELS_KO[i],
      day: d.getDate(),
      month: d.getMonth() + 1,
      isToday: dateKey === todayKey,
      hasEvent: eventKeys.has(dateKey),
    }
  })
})

const selectedDayLabel = computed(() => {
  const d = weekDays.value.find(w => w.dateKey === selectedWeekDate.value)
  if (!d) return ''
  return d.isToday ? `오늘 · ${d.month}월 ${d.day}일` : `${d.month}월 ${d.day}일 (${d.dow})`
})

const selectedDayEvents = computed(() =>
  weekEvents.value
    .filter(ev => toDateKey(ev.startAt) === selectedWeekDate.value)
    .slice()
    .sort((a, b) => new Date(a.startAt) - new Date(b.startAt))
)

watch(isDashboard, (on) => {
  if (on) fetchWeekEvents()
}, { immediate: true })

// ===== 마이페이지 사이드바 =====
const ICONS = {
  user: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>',
  lock: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="11" width="16" height="10" rx="2"></rect><path d="M8 11V8a4 4 0 0 1 8 0v3"></path></svg>',
  bell: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.7 21a2 2 0 0 1-3.4 0"></path></svg>',
  inquiry: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z"></path></svg>',
  report: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M10.3 3.8 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.8a2 2 0 0 0-3.4 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
  leave: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>',
}
// 쪽지함은 헤더 등 다른 경로로 접근 가능해 마이페이지 목록에서는 제외
const mypageNavItems = [
  { key: 'info',      label: '내 정보',      icon: ICONS.user },
  { key: 'password',  label: '비밀번호 변경', icon: ICONS.lock },
  { key: 'notif',     label: '알림 설정',     icon: ICONS.bell },
  { key: 'inquiries', label: '내 문의 내역',  icon: ICONS.inquiry },
  { key: 'reports',   label: '내 제보 내역',  icon: ICONS.report },
  { key: 'withdraw',  label: '회원 탈퇴',    icon: ICONS.leave },
]
const mypageActiveTab = computed(() => route.query.tab || 'info')
function handleMypageNavClick(item) {
  router.push({ name: 'MyPage', query: { tab: item.key } })
}

const activeSideMenuId = computed(() => routeMenu.value ? Number(routeMenu.value.id) : null)

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

// 사이드바 트리를 평탄화 (depth 정보 포함)
function flattenMenuTree(menus, depth = 0, result = []) {
  for (const menu of menus) {
    const isFolder = !!(menu.children?.length) || menu.menuType === 'GROUP'
    result.push({
      ...menu,
      depth,
      isFolder,
      isExternal: menu.menuType === 'LINK' && menu.newWindow,
      isLocked: !!menu.isLocked,
    })
    if (isFolder && isOpen(menu.id)) {
      flattenMenuTree(menu.children || [], depth + 1, result)
    }
  }
  return result
}

const sidebarFlatNodes = computed(() => {
  return flattenMenuTree(sideMenus.value)
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
  return activeSideMenuId.value === Number(menu.id)
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
  if (menu.isFolder || menu.children?.length) {
    toggleOpen(menu.id)
    return
  }
  navigateMenu(menu)
}

// ===== 모바일 드로어 =====
const FAVORITES_TAB_ID = '__favorites__'

function selectDrawerTop(menu) {
  if (menu.children?.length) {
    mobileSelectedTop.value = menu
    nextTick(() => { if (drawerRightRef.value) drawerRightRef.value.scrollTop = 0 })
  } else {
    handleDrawerNav(menu)
  }
}

function selectDrawerFavorites() {
  mobileSelectedTop.value = { id: FAVORITES_TAB_ID, name: '즐겨찾는 메뉴' }
  nextTick(() => { if (drawerRightRef.value) drawerRightRef.value.scrollTop = 0 })
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

watch(mobileMenuOpen, (open) => {
  if (open) {
    const initial = activeTopMenu.value || menuStore.topMenus.find(m => m.children?.length) || menuStore.topMenus[0] || null
    mobileSelectedTop.value = initial
  }
})

watch(activeTopMenu, menu => {
  menuStore.setActiveTopMenu(menu?.id ?? null)
}, { immediate: true })

watch([isBoardPage, currentBoardId], ([isBoard, boardId]) => {
  if (isBoard && boardId && authStore.isLoggedIn) {
    boardNotificationStore.fetch(boardId)
  }
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
  if (authStore.isLoggedIn) {
    favoriteMenuStore.fetchFavorites()
  }
})
</script>

<style scoped>
/* ===== 전체 레이아웃 ===== */
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  transition: background 0.25s;
}

/* ===== 본문 행: 사이드바 + 메인 ===== */
.body-row {
  display: flex;
  flex: 1;
  min-height: 0;
}

/* ===== 사이드바 ===== */
.sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 18px 14px;
  overflow-y: auto;
  position: sticky;
  top: var(--header-height);
  height: calc(100vh - var(--header-height));
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 8px 14px;
}

.sidebar-header--secondary {
  margin-top: 18px;
}

.sidebar-section-icon {
  flex-shrink: 0;
  color: var(--t3);
}

.sidebar-empty-text {
  padding: 8px 10px;
  margin: 0;
  font-size: 13px;
  color: var(--t3);
}

/* ===== 오늘 날짜 카드 (컴팩트 가로형) ===== */
.today-card {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  background: var(--accent);
  padding: 15px 16px;
  margin-top: 14px;
  margin-bottom: 4px;
  box-shadow: 0 6px 16px rgba(47, 95, 214, 0.26);
}

.today-card-decor-1 {
  position: absolute;
  right: -16px;
  top: -16px;
  width: 74px;
  height: 74px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.today-card-body {
  position: relative;
  display: flex;
  align-items: center;
  gap: 13px;
}

.today-card-daybox {
  display: flex;
  flex-direction: column;
  align-items: center;
  line-height: 1;
}

.today-card-day {
  font-size: 34px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #fff;
}

.today-card-weekday {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 3px;
}

.today-card-divider {
  width: 1px;
  height: 38px;
  background: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
}

.today-card-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.today-card-date {
  font-size: 15px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.01em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.today-card-sub {
  font-size: 11.5px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 이번 주 (주간 스트립 + 일정) ===== */
.week-strip-card {
  margin-top: 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 13px;
  box-shadow: var(--shadow-sm);
}

.week-strip-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.week-strip-title {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: var(--t1);
}

.week-strip-range {
  font-size: 11px;
  font-weight: 600;
  color: var(--t3);
}

.week-strip-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 12px;
}

.week-strip-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 5px 0;
  border: none;
  border-radius: 9px;
  background: transparent;
  cursor: pointer;
  transition: background 0.12s;
}

.week-strip-cell:hover { background: var(--surface2); }

.week-strip-cell.is-today { background: var(--accent); }
.week-strip-cell.is-today:hover { background: var(--accent); }

.week-strip-cell.is-selected { background: var(--accent-bg); }

.week-strip-dow {
  font-size: 9.5px;
  font-weight: 700;
  color: var(--t3);
}
.week-strip-dow.sun { color: var(--new); }
.week-strip-dow.sat { color: var(--accent); }
.week-strip-cell.is-today .week-strip-dow,
.week-strip-cell.is-today .week-strip-dow.sun,
.week-strip-cell.is-today .week-strip-dow.sat {
  color: rgba(255, 255, 255, 0.85);
}

.week-strip-day {
  font-size: 13px;
  font-weight: 800;
  color: var(--t1);
}
.week-strip-cell.is-today .week-strip-day { color: #fff; }
.week-strip-cell.is-selected .week-strip-day { color: var(--accent); }

.week-strip-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: transparent;
}
.week-strip-dot.is-visible { background: var(--accent); }
.week-strip-cell.is-today .week-strip-dot.is-visible { background: rgba(255, 255, 255, 0.9); }

.week-strip-divider {
  height: 1px;
  background: var(--border);
  margin: 2px 2px 9px;
}

.week-strip-selected-label {
  font-size: 12px;
  font-weight: 800;
  color: var(--accent);
  padding: 0 2px 7px;
}

.today-events-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.today-event-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  border-radius: 10px;
}

.today-event-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.today-event-body { flex: 1; min-width: 0; }

.today-event-title {
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.today-event-time {
  font-size: 11.5px;
  color: var(--t3);
  margin-top: 1px;
}

/* ===== 즐겨찾는 메뉴 ===== */
.fav-node { align-items: center; }

.fav-node-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.fav-node-path {
  font-size: 10.5px;
  font-weight: 600;
  color: var(--t3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fav-node-chevron {
  flex-shrink: 0;
  opacity: 0.5;
}

.sidebar-mypage-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px 16px;
  margin-bottom: 6px;
  border-bottom: 1px solid var(--border);
}

.sidebar-mypage-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 17px;
  flex-shrink: 0;
  overflow: hidden;
}
.sidebar-mypage-avatar img { width: 100%; height: 100%; object-fit: cover; }

.sidebar-mypage-meta { min-width: 0; }

.sidebar-mypage-name {
  font-size: 14.5px;
  font-weight: 800;
  letter-spacing: -0.02em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-mypage-username { font-size: 11px; color: var(--t3); margin-top: 2px; }

/* 마이페이지 모바일 탭 바 — 데스크톱에서는 사이드바가 대신하므로 숨김 */
.mypage-mobile-tabs {
  display: none;
}

.sidebar-section-text {
  font-size: 11.5px;
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--t3);
  white-space: nowrap;
}

.sidebar-section-line {
  flex: 1;
  height: 1px;
  background: var(--border);
}

/* ===== 트리 노드 ===== */
.sidebar-tree {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 10px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--t2);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
  line-height: 1.35;
}

.tree-node:hover { background: var(--surface2); color: var(--t1); }

.tree-node.is-active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 700;
}

.tree-node.is-folder { font-weight: 600; }

.tree-chevron {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.65;
  width: 13px;
}

.tree-chevron-spacer { width: 13px; flex-shrink: 0; }

.tree-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.7;
}

.tree-node.is-active .tree-icon { opacity: 1; }

.tree-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-new-badge {
  flex-shrink: 0;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--new);
  background: var(--surface);
  border: 1px solid var(--new);
  padding: 1px 5px;
  border-radius: 5px;
}

.tree-lock {
  flex-shrink: 0;
  color: var(--t3);
}

/* ===== 메인 컬럼 ===== */
.main-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 30px 32px 48px;
  overflow-y: auto;
}

/* ===== 페이지 헤더 (브레드크럼 + 제목) ===== */
.page-header {
  margin-bottom: 28px;
  max-width: 100%;
  margin-left: auto;
  margin-right: auto;
  /* 안내 페이지(좁음) ↔ 게시판(넓음) 전환 시 너비가 뚝 끊기지 않고 부드럽게 늘어나거나
     줄어들도록. max-width가 항상 구체적인 값(100% ↔ 864px)이어야 애니메이션된다 —
     한쪽이 none/auto면 브라우저가 보간하지 못하고 그냥 스냅해버린다 */
  transition: max-width 0.25s ease;
}

.page-header--narrow {
  max-width: 864px;
}

.page-header-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.favorite-star-btn {
  margin-left: auto;
  flex-shrink: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 2px 4px;
  filter: grayscale(1) opacity(0.55);
  transition: filter 0.15s, transform 0.15s;
}

.favorite-star-btn:hover { transform: scale(1.12); }

.favorite-star-btn.is-favorite { filter: none; }

.board-notif-bell-btn {
  flex-shrink: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
  padding: 2px 4px;
  filter: grayscale(1) opacity(0.55);
  transition: filter 0.15s, transform 0.15s;
}

.board-notif-bell-btn:hover { transform: scale(1.12); }

.board-notif-bell-btn.is-subscribed { filter: none; }

.board-notif-popover-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.board-notif-check-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  cursor: pointer;
}

.bc-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bc-item {
  font-size: 13px;
  color: var(--t3);
  font-weight: 500;
}

.bc-item.last {
  color: var(--t2);
  font-weight: 600;
}

.bc-sep {
  font-size: 13px;
  font-weight: 600;
  color: var(--t3);
  opacity: 0.5;
  margin: 0 4px;
}

.page-title {
  font-size: 30px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1.3;
  letter-spacing: -0.02em;
  margin: 0 0 8px;
}

.page-desc {
  font-size: 16px;
  color: var(--t2);
  line-height: 1.6;
  margin: 0;
}

/* ===== 모바일 오버레이/드로어 ===== */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
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

.drawer-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--border);
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

.drawer-user-name { font-size: 15px; font-weight: 700; color: var(--t1); }
.drawer-login-hint { font-size: 15px; font-weight: 600; color: var(--t2); }

.drawer-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: var(--surface2);
  color: var(--t2);
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
}
.drawer-close:hover { background: var(--border); color: var(--t1); }

.drawer-panels {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.drawer-left {
  width: 120px;
  flex-shrink: 0;
  background: var(--surface2);
  border-right: 1px solid var(--border);
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
.drawer-left-item:hover { color: var(--t1); }
.drawer-left-item.active {
  color: var(--t1);
  font-weight: 800;
  background: var(--surface);
  border-right: 2px solid var(--accent);
}

.drawer-left-item--fav {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  padding-top: 15px;
  border-top: 1px solid var(--border);
}

.drawer-fav-empty {
  margin: 0;
  padding: 14px 16px;
  font-size: 13px;
  color: var(--t3);
}

.drawer-fav-leaf {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s;
}
.drawer-fav-leaf:hover { background: var(--surface2); }

.drawer-fav-path {
  font-size: 11px;
  font-weight: 600;
  color: var(--t3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.drawer-fav-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--t1);
}

.drawer-right {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  padding: 10px 0 24px;
  background: var(--surface);
}

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
.drawer-group-header svg { color: var(--t3); flex-shrink: 0; }
.drawer-group-header:hover { color: var(--accent); }

.drawer-child-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px 10px 10px;
  border-bottom: 1px solid var(--border);
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
.drawer-child-btn.active { color: var(--accent); font-weight: 700; }

.drawer-leaf-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 16px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: transparent;
  color: var(--t1);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
}
.drawer-leaf-item:hover { background: var(--surface2); color: var(--accent); }
.drawer-leaf-item.active { color: var(--accent); font-weight: 700; }

.drawer-new-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--new);
  flex-shrink: 0;
}

/* ===== 트랜지션 ===== */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.slide-left-enter-active, .slide-left-leave-active { transition: transform 0.28s cubic-bezier(.4, 0, .2, 1); }
.slide-left-enter-from, .slide-left-leave-to { transform: translateX(-100%); }
.page-inner-enter-active, .page-inner-leave-active { transition: opacity 0.18s; }
.page-inner-enter-from, .page-inner-leave-to { opacity: 0; }

/* ===== 반응형 ===== */
@media (max-width: 900px) {
  .sidebar { display: none; }
  .main-content { padding: 20px 20px 40px; }

  .mypage-mobile-tabs {
    display: flex;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 10px;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--border);
    -webkit-overflow-scrolling: touch;
  }
  .mypage-mobile-tabs::-webkit-scrollbar { display: none; }

  .mp-mobile-tab {
    flex-shrink: 0;
    border: none;
    background: var(--surface2);
    color: var(--t2);
    font-weight: 600;
    font-size: 13px;
    padding: 8px 14px;
    border-radius: 18px;
    cursor: pointer;
    white-space: nowrap;
    transition: var(--transition);
  }
  .mp-mobile-tab.active { background: var(--accent); color: #fff; font-weight: 700; }
}

@media (max-width: 600px) {
  .main-content { padding: 16px 16px 32px; }
}
</style>
