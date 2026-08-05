<script setup lang="ts">
import LoginForm from './components/LoginForm.vue'
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-circle bg-circle-1" />
    <div class="bg-circle bg-circle-2" />
    <div class="bg-circle bg-circle-3" />
    <div class="bg-grid" />

    <div class="login-card fade-in-scale">
      <div class="login-logo">
        <span class="logo-emoji">🍢</span>
      </div>
      <h1 class="login-title">夜宵外卖平台</h1>
      <p class="login-subtitle">商 家 端 · MERCHANT PANEL</p>
      <LoginForm />
      <p class="login-register-tip">
        还没有店铺账号？
        <router-link to="/register" class="register-link">立即入驻</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #191510 0%, #2A1F12 40%, #3D2B12 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 154, 35, 0.18), transparent 70%);
}
.bg-circle-1 {
  width: 600px; height: 600px;
  top: -200px; right: -150px;
}
.bg-circle-2 {
  width: 400px; height: 400px;
  bottom: -120px; left: -100px;
  background: radial-gradient(circle, rgba(255, 180, 74, 0.12), transparent 70%);
}
.bg-circle-3 {
  width: 260px; height: 260px;
  top: 30%; left: 12%;
  background: radial-gradient(circle, rgba(245, 154, 35, 0.08), transparent 70%);
}

/* 背景网格 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 75%);
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 44px 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
  position: relative;
  z-index: 1;
}

.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.logo-emoji {
  font-size: 42px;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}

.login-title {
  text-align: center;
  font-size: 26px;
  color: #303133;
  margin-bottom: 4px;
  font-weight: 600;
}
.login-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
  font-size: 13px;
  letter-spacing: 3px;
}

.login-register-tip {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 20px;
}
.register-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
.register-link:hover {
  text-decoration: underline;
}
</style>
