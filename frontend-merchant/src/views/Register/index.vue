<script setup lang="ts">
import RegisterForm from './components/RegisterForm.vue'
</script>

<template>
  <div class="register-page">
    <!-- 背景装饰 -->
    <div class="bg-circle bg-circle-1" />
    <div class="bg-circle bg-circle-2" />
    <div class="bg-grid" />

    <div class="register-card fade-in-scale">
      <div class="register-header">
        <div class="register-logo">
          <span class="logo-emoji">🏪</span>
        </div>
        <h1 class="register-title">商家入驻申请</h1>
        <p class="register-subtitle">加入夜宵外卖平台，开启深夜生意</p>
      </div>

      <RegisterForm />

      <p class="register-login-tip">
        已有店铺账号？
        <router-link to="/login" class="login-link">直接登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: linear-gradient(140deg, #191510 0%, #2A1F12 40%, #3D2B12 100%);
  position: relative;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 154, 35, 0.16), transparent 70%);
}
.bg-circle-1 {
  width: 640px; height: 640px;
  top: -220px; right: -160px;
}
.bg-circle-2 {
  width: 460px; height: 460px;
  bottom: -140px; left: -120px;
  background: radial-gradient(circle, rgba(255, 180, 74, 0.1), transparent 70%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 75%);
}

.register-card {
  width: 560px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 40px 44px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}
.register-logo { display: flex; justify-content: center; margin-bottom: 12px; }
.logo-emoji {
  font-size: 36px;
  width: 64px; height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}
.register-title { font-size: 22px; color: #303133; margin: 0 0 4px; font-weight: 600; }
.register-subtitle { color: #909399; font-size: 13px; margin: 0; letter-spacing: 1px; }

.register-login-tip {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 18px;
}
.login-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
.login-link:hover { text-decoration: underline; }
</style>
