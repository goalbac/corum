import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useSiteStore } from '@/stores/site'
import api from '@/api/axios'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/RegisterPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/verify-email',
    name: 'VerifyEmail',
    component: () => import('@/pages/VerifyEmailPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/pages/ResetPasswordPage.vue'),
    meta: { guest: true }
  },
  {
    path: '/terms',
    name: 'Terms',
    component: () => import('@/pages/TermsPage.vue')
  },
  {
    path: '/terms-agreement',
    name: 'TermsAgreement',
    component: () => import('@/pages/TermsAgreementPage.vue'),
    meta: { requiresAuth: true, allowPendingTerms: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/pages/DashboardPage.vue'),
        meta: { title: '대시보드' }
      },
      {
        path: 'mypage',
        name: 'MyPage',
        component: () => import('@/pages/MyPage.vue'),
        meta: { requiresAuth: true, title: '마이페이지', allowPendingPasswordChange: true }
      },
      {
        path: 'messages',
        name: 'Message',
        component: () => import('@/pages/message/MessagePage.vue'),
        meta: { requiresAuth: true, title: '쪽지' }
      },
      {
        path: 'inquiry',
        name: 'Inquiry',
        component: () => import('@/pages/inquiry/InquiryPage.vue'),
        meta: { title: '문의하기' }
      },
      {
        path: 'report',
        name: 'SystemReport',
        component: () => import('@/pages/inquiry/SystemReportPage.vue'),
        meta: { title: '오류 제보 / 기능 제안', requiresAuth: true }
      },
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/pages/calendar/CalendarPage.vue'),
        meta: { title: '캘린더' }
      },
      {
        path: 'menu/:menuId',
        name: 'MenuPage',
        component: () => import('@/pages/MenuPage.vue')
      },
      {
        path: 'menu/:menuId/posts/:postId',
        name: 'MenuBoardDetail',
        component: () => import('@/pages/board/BoardDetailPage.vue')
      },
      {
        path: 'menu/:menuId/write',
        name: 'MenuBoardWrite',
        component: () => import('@/pages/board/BoardWritePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'menu/:menuId/posts/:postId/edit',
        name: 'MenuBoardEdit',
        component: () => import('@/pages/board/BoardWritePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'board/:boardId',
        name: 'BoardList',
        component: () => import('@/pages/board/BoardListPage.vue')
      },
      {
        path: 'board/:boardId/posts/:postId',
        name: 'BoardDetail',
        component: () => import('@/pages/board/BoardDetailPage.vue')
      },
      {
        path: 'board/:boardId/write',
        name: 'BoardWrite',
        component: () => import('@/pages/board/BoardWritePage.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'board/:boardId/posts/:postId/edit',
        name: 'BoardEdit',
        component: () => import('@/pages/board/BoardWritePage.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('@/pages/admin/AdminDashboardPage.vue') },
      { path: 'members', name: 'AdminMembers', component: () => import('@/pages/admin/members/AdminMembersPage.vue') },
      { path: 'terms', name: 'AdminTerms', component: () => import('@/pages/admin/terms/AdminTermsPage.vue') },
      { path: 'boards', name: 'AdminBoards', component: () => import('@/pages/admin/boards/AdminBoardsPage.vue') },
      { path: 'menus', name: 'AdminMenus', component: () => import('@/pages/admin/menus/AdminMenusPage.vue') },
      { path: 'display', name: 'AdminDisplay', component: () => import('@/pages/admin/display/AdminDisplayPage.vue') },
      { path: 'content-pages', name: 'AdminContentPages', component: () => import('@/pages/admin/content/AdminContentPagesPage.vue') },
      { path: 'dashboard-widgets', name: 'AdminDashboardWidgets', component: () => import('@/pages/admin/dashboard/AdminDashboardWidgetsPage.vue') },
      { path: 'inquiries', name: 'AdminInquiries', component: () => import('@/pages/admin/inquiries/AdminInquiriesPage.vue') },
      { path: 'groups', name: 'AdminGroups', component: () => import('@/pages/admin/groups/AdminGroupsPage.vue') },
      { path: 'stats', name: 'AdminStats', component: () => import('@/pages/admin/stats/AdminStatsPage.vue') },
      { path: 'settings', name: 'AdminSettings', component: () => import('@/pages/admin/settings/AdminSettingsPage.vue') },
      { path: 'admin-permissions', name: 'AdminMenuPermissions', component: () => import('@/pages/admin/settings/AdminMenuPermissionsPage.vue') },
      { path: 'calendars', name: 'AdminCalendars', component: () => import('@/pages/admin/calendar/AdminCalendarPage.vue') },
      { path: 'posts', name: 'AdminPosts', component: () => import('@/pages/admin/posts/AdminPostsPage.vue') },
      { path: 'comments', name: 'AdminComments', component: () => import('@/pages/admin/comments/AdminCommentsPage.vue') },
      { path: 'tools', name: 'AdminTools', component: () => import('@/pages/admin/tools/AdminToolsPage.vue') }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/pages/NotFoundPage.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

let adminAccessCache = null
let adminAccessCacheToken = null

function normalizeAdminPath(path) {
  if (!path) return ''
  if (path.length > 1 && path.endsWith('/')) return path.slice(0, -1)
  return path
}

function flattenAdminUrls(items = []) {
  return items.flatMap(item => [
    item.url,
    ...flattenAdminUrls(item.children || [])
  ]).filter(Boolean)
}

async function getAccessibleAdminUrls(authStore) {
  const cacheToken = authStore.token
  if (adminAccessCache && adminAccessCacheToken === cacheToken) {
    return adminAccessCache
  }

  const res = await api.get('/admin/sidebar')
  adminAccessCache = new Set(flattenAdminUrls(res.data.data || []).map(normalizeAdminPath))
  adminAccessCacheToken = cacheToken
  return adminAccessCache
}

function canAccessAdminPath(path, accessibleUrls) {
  const adminPath = normalizeAdminPath(path)
  return [...accessibleUrls].some(url => {
    if (url === '/admin') return adminPath === url
    return adminPath === url || adminPath.startsWith(`${url}/`)
  })
}

function firstAccessibleAdminPath(accessibleUrls) {
  return [...accessibleUrls].find(url => url !== '/admin') || [...accessibleUrls][0] || '/'
}

function redirectLegacyBoardRoute(to, menuStore) {
  if (!to.path.startsWith('/board/')) return null

  const boardMenu = menuStore.findBoardMenu(to.params.boardId)
  if (!boardMenu) return null

  if (to.name === 'BoardDetail') {
    return { name: 'MenuBoardDetail', params: { menuId: boardMenu.id, postId: to.params.postId }, query: to.query, hash: to.hash }
  }
  if (to.name === 'BoardWrite') {
    return { name: 'MenuBoardWrite', params: { menuId: boardMenu.id }, query: to.query, hash: to.hash }
  }
  if (to.name === 'BoardEdit') {
    return { name: 'MenuBoardEdit', params: { menuId: boardMenu.id, postId: to.params.postId }, query: to.query, hash: to.hash }
  }
  return { name: 'MenuPage', params: { menuId: boardMenu.id }, query: to.query, hash: to.hash }
}

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const menuStore = useMenuStore()
  const siteStore = useSiteStore()

  await siteStore.fetchSettings()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  // 사이트 전체 로그인 필수 모드 — 로그인/가입/이메일인증/비번찾기 등 게스트 전용
  // 페이지(meta.guest)는 그렇지 않으면 아무도 로그인할 방법이 없으므로 예외로 둔다
  if (siteStore.requireLoginSiteWide && !to.meta.guest && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
  if (to.meta.guest && authStore.isLoggedIn) {
    return next({ name: 'Dashboard' })
  }
  if (authStore.isLoggedIn && authStore.member?.requiresTermsAgreement && !to.meta.allowPendingTerms) {
    return next({ name: 'TermsAgreement' })
  }
  if (authStore.isLoggedIn && authStore.member?.mustChangePassword && !to.meta.allowPendingPasswordChange) {
    return next({ name: 'MyPage', query: { tab: 'password', forced: '1' } })
  }

  if (to.path.startsWith('/admin')) {
    if (!authStore.member?.isAdmin && !authStore.member?.admin) {
      return next({ name: 'Dashboard' })
    }

    try {
      const accessibleUrls = await getAccessibleAdminUrls(authStore)
      if (!accessibleUrls.size) {
        return next({ name: 'Dashboard' })
      }
      if (!canAccessAdminPath(to.path, accessibleUrls)) {
        return next(firstAccessibleAdminPath(accessibleUrls))
      }
    } catch {
      return next({ name: 'Dashboard' })
    }
  }

  if (to.path.startsWith('/board/')) {
    await menuStore.fetchMenus()
    const redirected = redirectLegacyBoardRoute(to, menuStore)
    if (redirected) return next(redirected)
  }

  next()
})

// 페이지 이동 시 방문 집계 (관리자 경로 제외)
router.afterEach((to) => {
  if (to.path.startsWith('/admin')) return
  api.post('/page-view').catch(() => {})
})

export default router
