import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register/index.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/',
    component: () => import('@/views/Layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/Home/index.vue'),
        meta: { title: '首页', tabbar: true },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/Orders/index.vue'),
        meta: { title: '订单', tabbar: true, requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile/index.vue'),
        meta: { title: '我的', tabbar: true },
      },
      {
        path: 'address',
        name: 'Address',
        component: () => import('@/views/Address/index.vue'),
        meta: { title: '我的收货地址', requiresAuth: true },
      },
      {
        path: 'address/edit',
        name: 'AddressEdit',
        component: () => import('@/views/Address/edit.vue'),
        meta: { title: '收货地址编辑', requiresAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：
// 1. 标记 requiresAuth 的页面（如订单页）必须登录，未登录跳转登录页；
// 2. 已登录访问登录/注册页时跳回首页；
// 3. token 过期（调用接口返回 401/403）由 request 拦截器清空登录态并跳转登录页。
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else if ((to.path === '/login' || to.path === '/register') && token && role === 'USER') {
    next('/home')
  } else {
    next()
  }
})

export default router
