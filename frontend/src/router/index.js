import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'

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
        meta: { requiresAuth: true, title: '마이페이지' }
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
      { path: 'boards', name: 'AdminBoards', component: () => import('@/pages/admin/boards/AdminBoardsPage.vue') },
      { path: 'menus', name: 'AdminMenus', component: () => import('@/pages/admin/menus/AdminMenusPage.vue') },
      { path: 'display', name: 'AdminDisplay', component: () => import('@/pages/admin/display/AdminDisplayPage.vue') },
      { path: 'content-pages', name: 'AdminContentPages', component: () => import('@/pages/admin/content/AdminContentPagesPage.vue') },
      { path: 'dashboard-widgets', name: 'AdminDashboardWidgets', component: () => import('@/pages/admin/dashboard/AdminDashboardWidgetsPage.vue') },
      { path: 'inquiries', name: 'AdminInquiries', component: () => import('@/pages/admin/inquiries/AdminInquiriesPage.vue') },
      { path: 'groups', name: 'AdminGroups', component: () => import('@/pages/admin/groups/AdminGroupsPage.vue') },
      { path: 'settings', name: 'AdminSettings', component: () => import('@/pages/admin/settings/AdminSettingsPage.vue') }
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

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  if (to.meta.guest && authStore.isLoggedIn) {
    return next({ name: 'Dashboard' })
  }

  if (to.path.startsWith('/board/')) {
    await menuStore.fetchMenus()
    const redirected = redirectLegacyBoardRoute(to, menuStore)
    if (redirected) return next(redirected)
  }

  next()
})

export default router
