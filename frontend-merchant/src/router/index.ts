import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/index.vue'),
    meta: { title: '商家登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register/index.vue'),
    meta: { title: '商家入驻' },
  },
  {
    path: '/',
    component: () => import('@/views/Layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/index.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'orders',
        redirect: '/orders/today',
        children: [
          {
            path: 'today',
            name: 'OrdersToday',
            component: () => import('@/views/Orders/index.vue'),
            meta: { title: '今日订单', mode: 'today' },
          },
          {
            path: 'all',
            name: 'OrdersAll',
            component: () => import('@/views/Orders/index.vue'),
            meta: { title: '全部订单', mode: 'all' },
          },
        ],
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/Products/index.vue'),
        meta: { title: '商品管理' },
      },
      {
        path: 'shop',
        name: 'Shop',
        component: () => import('@/views/Shop/index.vue'),
        meta: { title: '店铺设置' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if (to.path !== '/login' && to.path !== '/register' && !token) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token && role === 'MERCHANT') {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
