<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-form @submit.prevent="handleLogin" label-position="top">
    <el-form-item label="用户名">
      <el-input v-model="form.username" placeholder="请输入管理员账号" size="large" />
    </el-form-item>
    <el-form-item label="密码">
      <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">
        登 录
      </el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
</style>
