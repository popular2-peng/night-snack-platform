<script setup lang="ts">
import LoginForm from './components/LoginForm.vue'
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="bg-circle bg-circle-1" />
    <div class="bg-circle bg-circle-2" />
    <div class="bg-circle bg-circle-3" />

    <div class="login-card fade-in-scale">
      <div class="login-logo">
        <span class="logo-emoji">🍜</span>
      </div>
      <h1 class="login-title">夜宵外卖平台</h1>
      <p class="login-subtitle">系统管理后台</p>
      <LoginForm />
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(140deg, #1A1D2E 0%, #1F2744 40%, #1A2F55 100%);
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆 */
.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.06;
  background: #fff;
}
.bg-circle-1 {
  width: 500px;
  height: 500px;
  top: -150px;
  right: -100px;
}
.bg-circle-2 {
  width: 300px;
  height: 300px;
  bottom: -80px;
  left: -60px;
}
.bg-circle-3 {
  width: 200px;
  height: 200px;
  top: 40%;
  left: 15%;
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px 40px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.25);
  position: relative;
  z-index: 1;
}
.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.logo-emoji {
  font-size: 48px;
  width: 72px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  border-radius: 18px;
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.35);
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
  margin-bottom: 36px;
  font-size: 14px;
  letter-spacing: 2px;
}
</style>
