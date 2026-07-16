import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('../views/HomeView.vue'),
        meta: { title: '物品市场' },
      },
      {
        path: 'items',
        redirect: { name: 'home' },
      },
      {
        path: 'items/new',
        name: 'item-create',
        component: () => import('../views/items/ItemCreateView.vue'),
        meta: { title: '发布物品', requiresAuth: true },
      },
      {
        path: 'items/:id/edit',
        name: 'item-edit',
        component: () => import('../views/items/ItemEditView.vue'),
        meta: { title: '编辑物品', requiresAuth: true },
      },
      {
        path: 'items/:id',
        name: 'item-detail',
        component: () => import('../views/items/ItemDetailView.vue'),
        meta: { title: '物品详情' },
      },
      {
        path: 'users/:id',
        name: 'user-profile',
        component: () => import('../views/users/PublicProfileView.vue'),
        meta: { title: '用户资料' },
      },
      {
        path: 'me/trades',
        name: 'my-trades',
        component: () => import('../views/trades/MyTradesView.vue'),
        meta: { title: '我的交易', requiresAuth: true },
      },
      {
        path: 'me/trades/:id',
        name: 'trade-detail',
        component: () => import('../views/trades/TradeDetailView.vue'),
        meta: { title: '交易详情', requiresAuth: true },
      },
      {
        path: 'me/items',
        name: 'my-items',
        component: () => import('../views/items/MyItemsView.vue'),
        meta: { title: '我的物品', requiresAuth: true },
      },
      {
        path: 'me/profile',
        name: 'profile',
        component: () => import('../views/profile/ProfileView.vue'),
        meta: { title: '个人资料', requiresAuth: true },
      },
      {
        path: 'me/reports',
        name: 'my-reports',
        component: () => import('../views/reports/MyReportsView.vue'),
        meta: { title: '我的举报', requiresAuth: true },
      },
      {
        path: 'me/reports/:id',
        name: 'my-report-detail',
        component: () => import('../views/reports/MyReportDetailView.vue'),
        meta: { title: '举报详情', requiresAuth: true },
      },
      {
        path: 'me/messages',
        name: 'messages',
        component: () => import('../views/messages/MessagesView.vue'),
        meta: { title: '消息中心', requiresAuth: true },
      },
      {
        path: 'me/disputes',
        name: 'my-disputes',
        component: () => import('../views/disputes/MyDisputesView.vue'),
        meta: { title: '我的争议', requiresAuth: true },
      },
      {
        path: 'me/disputes/:id',
        name: 'my-dispute-detail',
        component: () => import('../views/disputes/MyDisputeDetailView.vue'),
        meta: { title: '争议详情', requiresAuth: true },
      },
      {
        path: 'admin',
        name: 'admin-dashboard',
        component: () => import('../views/admin/AdminDashboardView.vue'),
        meta: { title: '管理概览', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/users',
        name: 'admin-users',
        component: () => import('../views/admin/AdminUsersView.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/categories',
        name: 'admin-categories',
        component: () => import('../views/admin/AdminCategoriesView.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/reports',
        name: 'admin-reports',
        component: () => import('../views/admin/AdminReportsView.vue'),
        meta: { title: '举报处理', requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'admin/disputes',
        name: 'admin-disputes',
        component: () => import('../views/admin/AdminDisputesView.vue'),
        meta: { title: '争议仲裁', requiresAuth: true, requiresAdmin: true },
      },
    ],
  },
  {
    path: '/auth',
    component: () => import('../layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('../views/auth/LoginView.vue'),
        meta: { title: '登录' },
      },
      {
        path: 'register',
        name: 'register',
        component: () => import('../views/auth/RegisterView.vue'),
        meta: { title: '注册' },
      },
    ],
  },
  {
    path: '/login',
    redirect: { name: 'login' },
  },
  {
    path: '/register',
    redirect: { name: 'register' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized) {
    await auth.initialize()
  }

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'home' }
  }

  if ((to.name === 'login' || to.name === 'register') && auth.isAuthenticated) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/'
    return redirect || '/'
  }

  return true
})

router.afterEach((to) => {
  const title = to.meta.title
  document.title = title ? `${title} · CampusEcho` : 'CampusEcho'
})

export default router
