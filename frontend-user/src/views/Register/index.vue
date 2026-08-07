<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerApi } from '@/apis/auth'
import { showToast } from 'vant'

const router = useRouter()

const form = reactive({ username: '', password: '', confirmPassword: '', phone: '' })
const loading = ref(false)

const ACCOUNT_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*()_+]{6,20}$/
const PHONE_REGEX = /^1[3-9]\d{9}$/

const validate = () => {
  if (!ACCOUNT_REGEX.test(form.username)) { showToast('账号4-20位，字母开头，支持字母/数字/下划线'); return false }
  if (!PASSWORD_REGEX.test(form.password)) { showToast('密码6-20位，需包含字母和数字'); return false }
  if (form.password !== form.confirmPassword) { showToast('两次输入的密码不一致'); return false }
  if (!PHONE_REGEX.test(form.phone)) { showToast('请输入正确的11位手机号'); return false }
  return true
}

const handleRegister = async () => {
  if (!validate()) return
  loading.value = true
  try {
    await registerApi({
      username: form.username,
      password: form.password,
      phone: form.phone,
    })
    showToast('注册成功，请登录')
    router.push('/login')
  } catch {
    // 错误已由拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="hero">
      <div class="hero-deco deco-1" />
      <div class="hero-deco deco-2" />
      <div class="hero-logo">🥢</div>
      <h1 class="hero-title">注册账号</h1>
      <p class="hero-subtitle">开启你的深夜美食之旅</p>
    </div>

    <div class="form-card fade-in-scale">
      <van-form @submit="handleRegister">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            label="账号"
            placeholder="字母开头，4-20位"
            clearable
          />
          <van-field
            v-model="form.password"
            type="password"
            label="密码"
            placeholder="6-20位，含字母和数字"
            clearable
          />
          <van-field
            v-model="form.confirmPassword"
            type="password"
            label="确认密码"
            placeholder="再次输入密码"
            clearable
          />
          <van-field
            v-model="form.phone"
            type="tel"
            label="手机号"
            placeholder="用于登录和接收订单通知"
            maxlength="11"
            clearable
          />
        </van-cell-group>

        <div class="btn-wrap">
          <van-button
            round
            block
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            native-type="submit"
          >
            注 册
          </van-button>
        </div>
      </van-form>

      <p class="login-tip">
        已有账号？
        <router-link to="/login" class="login-link">直接登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1A1510 0%, #2A1F12 45%, #F6F7F9 45%);
}

.hero {
  position: relative;
  padding: 56px 24px 44px;
  text-align: center;
  overflow: hidden;
}
.hero-deco {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 154, 35, 0.2), transparent 70%);
}
.deco-1 { width: 280px; height: 280px; top: -60px; right: -40px; }
.deco-2 { width: 200px; height: 200px; bottom: -40px; left: -60px; }

.hero-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(245, 154, 35, 0.45);
}
.hero-title {
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 2px;
}
.hero-subtitle {
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  margin: 0;
  letter-spacing: 1px;
}

.form-card {
  margin: 0 20px;
  background: #fff;
  border-radius: 16px;
  padding: 24px 12px 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.btn-wrap { padding: 20px 12px 0; }
.register-btn {
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  box-shadow: 0 6px 18px rgba(245, 154, 35, 0.35);
}

.login-tip {
  text-align: center;
  color: #86909C;
  font-size: 13px;
  margin: 18px 0 0;
}
.login-link {
  color: #F59A23;
  font-weight: 600;
  text-decoration: none;
}
</style>
