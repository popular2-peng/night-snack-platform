<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive({ phone: '', password: '' })
const loading = ref(false)

/** 手机号：11位数字 */
const PHONE_REGEX = /^1[3-9]\d{9}$/

/** 密码：6-20位，必须包含字母和数字 */
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*()_+]{6,20}$/

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_REGEX, message: '请输入正确的11位手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: PASSWORD_REGEX, message: '6-20位，必须包含字母和数字', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    // 非商家账号时给出明确提示
    if (e?.message === '该账号不是商家账号') {
      ElMessage.error('该账号不是商家账号，请使用商家账号登录')
    }
    // 其他错误已由请求拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin" label-position="top">
    <el-form-item label="手机号" prop="phone">
      <el-input
        v-model="form.phone"
        placeholder="请输入商家手机号"
        size="large"
        maxlength="11"
      >
        <template #prefix>
          <el-icon><User /></el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input
        v-model="form.password"
        type="password"
        placeholder="请输入密码"
        size="large"
        show-password
        @keyup.enter="handleLogin"
      >
        <template #prefix>
          <el-icon><Lock /></el-icon>
        </template>
      </el-input>
    </el-form-item>
    <el-form-item class="login-btn-item">
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        class="login-btn"
        @click="handleLogin"
      >
        登 录
      </el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.login-btn-item {
  margin-top: 24px;
  margin-bottom: 0;
}
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 8px;
  border-radius: 10px;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}
.login-btn:active {
  transform: translateY(0);
}
</style>
