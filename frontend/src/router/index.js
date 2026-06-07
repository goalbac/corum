import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 로그인
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/LoginPage.vue'),
    meta: { guest: true }
  },

  // 회원가입
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/RegisterPage.vue'),
    meta: { guest: true }
  },

  // 메인 레이아웃
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

      // 쪽지
      {
        path: 'messages',
        name: 'Message',
        component: () => import('@/pages/message/MessagePage.vue'),
        meta: { requiresAuth: true }
      },

      // 문의하기
      {
        path: 'inquiry',
        name: 'Inquiry',
        component: () => import('@/pages/inquiry/InquiryPage.vue'),
      },

      // 캘린더
      {
        path: 'calendar',
        name: 'Calendar',
        component: () => import('@/pages/calendar/CalendarPage.vue'),
      },

      // 게시판
      {
        path: 'board/:boardId',
        name: 'BoardList',
        component: () => import('@/pages/board/BoardListPage.vue'),
      },
      {
        path: 'board/:boardId/posts/:postId',
        name: 'BoardDetail',
        component: () => import('@/pages/board/BoardDetailPage.vue'),
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
      },
    ]
  },

  // 관리자
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '',         name: 'AdminDashboard', component: () => import('@/pages/admin/AdminDashboardPage.vue') },
      { path: 'members',   name: 'AdminMembers',  component: () => import('@/pages/admin/members/AdminMembersPage.vue') },
      { path: 'boards',    name: 'AdminBoards',   component: () => import('@/pages/admin/boards/AdminBoardsPage.vue') },
      { path: 'menus',     name: 'AdminMenus',    component: () => import('@/pages/admin/menus/AdminMenusPage.vue') },
      { path: 'inquiries', name: 'AdminInquiries', component: () => import('@/pages/admin/inquiries/AdminInquiriesPage.vue') },
      { path: 'groups',    name: 'AdminGroups',   component: () => import('@/pages/admin/AdminDashboardPage.vue') },
      { path: 'settings',  name: 'AdminSettings', component: () => import('@/pages/admin/AdminDashboardPage.vue') },
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

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }
  if (to.meta.guest && authStore.isLoggedIn) {
    return next({ name: 'Dashboard' })
  }
  next()
})

export default router
