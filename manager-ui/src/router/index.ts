import { createRouter, createWebHistory } from 'vue-router'
import { adminSession } from '../api/admin/auth'
import { userMe } from '../api/user/auth'

/** 管理员端路由：全部页面归类在 /admin 下 */
const ADMIN_PUBLIC_PATHS = new Set([
  '/admin/login',
  '/admin/register',
  '/admin/forgot',
])
const USER_PROTECTED_PATHS = new Set([
  '/portal/cart',
  '/portal/checkout',
  '/portal/profile',
  '/portal/orders',
  '/portal/favorites',
])

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to) {
    const pending = sessionStorage.getItem('portal:pending-restore')
    if (pending === 'home' && to.name === 'portal-home') return false
    if (pending === 'list' && to.name === 'portal-list') return false
    if (to.name === 'portal-home' && (to.hash === '#new-arrivals' || to.hash === '#hot-ranking')) {
      return false
    }
    return { top: 0 }
  },
  routes: [
    { path: '/', redirect: '/portal' },
    { path: '/portal', name: 'portal-home', component: () => import('../views/portal/PortalHomeView.vue') },
    { path: '/portal/list', name: 'portal-list', component: () => import('../views/portal/PortalListView.vue') },
    { path: '/portal/book/:id', name: 'portal-book', component: () => import('../views/portal/PortalBookDetailView.vue') },
    { path: '/portal/detail', name: 'portal-detail', component: () => import('../views/portal/PortalDetailView.vue') },
    { path: '/portal/cart', name: 'portal-cart', component: () => import('../views/user/UserCartView.vue') },
    { path: '/portal/checkout', name: 'portal-checkout', component: () => import('../views/user/UserCheckoutView.vue') },
    { path: '/portal/profile', name: 'portal-profile', component: () => import('../views/user/UserProfileView.vue') },
    { path: '/portal/orders', name: 'portal-orders', component: () => import('../views/user/UserOrdersView.vue') },
    { path: '/portal/favorites', name: 'portal-favorites', component: () => import('../views/user/UserFavoritesView.vue') },
    // 兼容旧的 /user/* 路径（重定向到门户端）
    { path: '/user/cart', redirect: '/portal/cart' },
    { path: '/user/checkout', redirect: '/portal/checkout' },
    { path: '/user/profile', redirect: '/portal/profile' },
    { path: '/user/orders', redirect: '/portal/orders' },
    { path: '/user/favorites', redirect: '/portal/favorites' },
    {
      path: '/user/login',
      name: 'user-login',
      component: () => import('../views/user/UserLoginRegisterView.vue'),
    },
    {
      path: '/user/wechat-login',
      name: 'user-wechat-login',
      component: () => import('../views/user/UserWechatLoginView.vue'),
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('../views/admin/AdminLoginView.vue'),
    },
    {
      path: '/admin/register',
      name: 'admin-register',
      component: () => import('../views/admin/AdminRegisterView.vue'),
    },
    {
      path: '/admin/forgot',
      name: 'admin-forgot',
      component: () => import('../views/admin/AdminForgotPasswordView.vue'),
    },
    {
      path: '/admin/dashboard',
      name: 'admin-dashboard',
      component: () => import('../views/admin/AdminDashboardView.vue'),
    },
    {
      path: '/admin/books',
      name: 'admin-books',
      component: () => import('../views/admin/AdminBooksView.vue'),
    },
    {
      path: '/admin/orders',
      name: 'admin-orders',
      component: () => import('../views/admin/AdminOrdersView.vue'),
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('../views/admin/AdminUsersView.vue'),
    },
    {
      path: '/admin/categories',
      name: 'admin-categories',
      component: () => import('../views/admin/AdminCategoriesView.vue'),
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  if (USER_PROTECTED_PATHS.has(to.path)) {
    try {
      const r = await userMe()
      if (r.success && r.data) {
        next()
        return
      }
    } catch {
      /* 忽略网络异常，统一回首页 */
    }
    const redirect = typeof to.fullPath === 'string' && to.fullPath.startsWith('/portal') ? to.fullPath : '/portal/favorites'
    next({ path: '/user/login', query: { redirect }, replace: true })
    return
  }

  if (!to.path.startsWith('/admin')) {
    next()
    return
  }
  if (ADMIN_PUBLIC_PATHS.has(to.path)) {
    const bounceIfLoggedIn = ['/admin/login', '/admin/register', '/admin/forgot']
    if (bounceIfLoggedIn.includes(to.path)) {
      try {
        const r = await adminSession()
        if (r.success && r.data?.loggedIn) {
          next({ path: '/admin/dashboard' })
          return
        }
      } catch {
        /* 后端未启动时仍允许进入 */
      }
    }
    next()
    return
  }
  try {
    const r = await adminSession()
    if (r.success && r.data?.loggedIn) {
      next()
    } else {
      next({ path: '/admin/login', query: { redirect: to.fullPath } })
    }
  } catch {
    next({ path: '/admin/login', query: { redirect: to.fullPath } })
  }
})

export default router
