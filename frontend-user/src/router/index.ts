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
        component: () => import('@/views/PlaceholderPage.vue'),
        meta: { title: '订单', tabbar: true, placeholder: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile/index.vue'),
        meta: { title: '我的', tabbar: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：首页等页面无需登录即可浏览，不做强制跳转；
// 仅当调用需要鉴权的接口返回 401/403 时，由 request 拦截器清空登录态并跳转登录页。
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if ((to.path === '/login' || to.path === '/register') && token && role === 'USER') {
    next('/home')
  } else {
    next()
  }
})

export default router
