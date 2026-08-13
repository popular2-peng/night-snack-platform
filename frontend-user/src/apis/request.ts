import axios, { type InternalAxiosRequestConfig } from 'axios'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 单飞刷新：并发多个 401/1003 时只发起一次 /api/auth/refresh 请求
let refreshPromise: Promise<boolean> | null = null

/** 用 refreshToken 换取新双令牌，成功回写 localStorage */
const doRefresh = async (): Promise<boolean> => {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return false
  try {
    // 用裸 axios（不走本实例，避免拦截器递归）
    const { data } = await axios.post('/api/auth/refresh', { refreshToken })
    if (data.code === 0) {
      localStorage.setItem('token', data.data.accessToken)
      localStorage.setItem('refreshToken', data.data.refreshToken)
      return true
    }
    return false
  } catch {
    return false
  }
}

/** 刷新失败 / 二次重试失败：清会话并跳登录页 */
const redirectToLogin = () => {
  localStorage.clear()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// 请求拦截器：注入 Token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Token 过期处理：刷新 → 重试原请求一次；失败则回登录页
 * （1003 业务码与 HTTP 401 两条路径共用）
 */
const handleTokenExpired = async (config: InternalAxiosRequestConfig & { _retry?: boolean }) => {
  if (config._retry) {
    // 已重试过仍失败 → 不再刷新，直接回登录页
    redirectToLogin()
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null
    })
  }
  const ok = await refreshPromise
  if (!ok) {
    redirectToLogin()
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }
  config._retry = true
  config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
  return request(config)
}

// 响应拦截器：统一错误处理 + Token 过期自动续期
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 业务码 1003（Token 已过期）：刷新后重试原请求一次
    if (data.code === 1003) {
      return handleTokenExpired(response.config as InternalAxiosRequestConfig & { _retry?: boolean })
    }
    if (data.code !== 0) {
      showToast(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg))
    }
    return data
  },
  (error) => {
    if (error.response?.status === 401) {
      // 401 未认证：刷新后重试一次（refresh 失败会自动回登录页）
      return handleTokenExpired(error.config as InternalAxiosRequestConfig & { _retry?: boolean })
    }
    if (error.response?.status === 403) {
      // 403 权限不足：刷新无意义，直接回登录页
      redirectToLogin()
    } else {
      showToast('网络异常，请稍后再试')
    }
    return Promise.reject(error)
  },
)

export default request
