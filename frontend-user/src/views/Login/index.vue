<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showToast } from 'vant'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({ phone: '', password: '' })
const loading = ref(false)

const PHONE_REGEX = /^1[3-9]\d{9}$/
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*()_+]{6,20}$/

const validate = () => {
  if (!form.phone) { showToast('请输入手机号'); return false }
  if (!PHONE_REGEX.test(form.phone)) { showToast('请输入正确的11位手机号'); return false }
  if (!form.password) { showToast('请输入密码'); return false }
  if (!PASSWORD_REGEX.test(form.password)) { showToast('密码6-20位，需含字母和数字'); return false }
  return true
}

const handleLogin = async () => {
  if (!validate()) return
  loading.value = true
  try {
    await authStore.login(form)
    showToast('登录成功')
    // 从需登录页面跳转过来时（如订单页），登录后回跳原页面
    const redirect = (route.query.redirect as string) || '/home'
    router.replace(redirect)
  } catch (e: any) {
    if (e?.message === '该账号不是用户账号') {
      showToast('该账号不是用户账号')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 顶部品牌区 -->
    <div class="hero">
      <div class="hero-deco deco-1" />
      <div class="hero-deco deco-2" />
      <div class="hero-logo">🍜</div>
      <h1 class="hero-title">夜宵外卖</h1>
      <p class="hero-subtitle">深夜的每一口，都是治愈</p>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card fade-in-scale">
      <van-form @submit="handleLogin">
        <van-cell-group inset>
          <van-field
            v-model="form.phone"
            type="tel"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            maxlength="11"
            :rules="[{ required: true, message: '请输入手机号' }]"
            clearable
          />
          <van-field
            v-model="form.password"
            type="password"
            name="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请输入密码' }]"
            @keyup.enter="handleLogin"
          />
        </van-cell-group>

        <div class="btn-wrap">
          <van-button
            round
            block
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            native-type="submit"
          >
            登 录
          </van-button>
        </div>
      </van-form>

      <p class="register-tip">
        还没有账号？
        <router-link to="/register" class="register-link">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1A1510 0%, #2A1F12 45%, #F6F7F9 45%);
}

/* ---- 顶部品牌区 ---- */
.hero {
  position: relative;
  padding: 72px 24px 56px;
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
  width: 76px;
  height: 76px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  border-radius: 22px;
  box-shadow: 0 10px 30px rgba(245, 154, 35, 0.45);
}
.hero-title {
  color: #fff;
  font-size: 26px;
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

/* ---- 表单卡片 ---- */
.form-card {
  margin: 0 20px;
  background: #fff;
  border-radius: 16px;
  padding: 24px 12px 20px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.btn-wrap { padding: 20px 12px 0; }
.login-btn {
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  box-shadow: 0 6px 18px rgba(245, 154, 35, 0.35);
}

.register-tip {
  text-align: center;
  color: #86909C;
  font-size: 13px;
  margin: 18px 0 0;
}
.register-link {
  color: #F59A23;
  font-weight: 600;
  text-decoration: none;
}
</style>
