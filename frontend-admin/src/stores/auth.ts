import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi } from '@/apis/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!token.value)

  const login = async (form: { username: string; password: string }) => {
    const res = await loginApi(form)
    token.value = res.data.accessToken
    username.value = res.data.username
    role.value = res.data.role

    localStorage.setItem('token', res.data.accessToken)
    localStorage.setItem('username', res.data.username)
    localStorage.setItem('role', res.data.role)
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.clear()
  }

  return { token, username, role, isLoggedIn, login, logout }
})
