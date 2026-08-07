import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi } from '@/apis/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isUser = computed(() => role.value === 'USER')

  const login = async (form: { phone: string; password: string }) => {
    const res = await loginApi(form)
    if (res.data.role !== 'USER') {
      throw new Error('该账号不是用户账号')
    }
    token.value = res.data.accessToken
    username.value = res.data.username
    role.value = res.data.role
    userId.value = String(res.data.userId)

    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('role', res.data.role)
    localStorage.setItem('userId', String(res.data.userId))
  }

  /** 注册即登录：后端返回 LoginVO，直接保存会话 */
  const register = async (form: { phone: string; password: string }) => {
    const res = await registerApi(form)
    token.value = res.data.accessToken
    username.value = res.data.username
    role.value = res.data.role
    userId.value = String(res.data.userId)

    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('role', res.data.role)
    localStorage.setItem('userId', String(res.data.userId))
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    localStorage.clear()
  }

  return { token, username, role, userId, isLoggedIn, isUser, login, register, logout }
})
