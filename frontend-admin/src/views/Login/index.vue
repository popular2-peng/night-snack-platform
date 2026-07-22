<script setup lang="ts">
import LoginForm from './components/LoginForm.vue'
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">夜宵外卖平台</h1>
      <p class="login-subtitle">管理后台</p>
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
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.login-title {
  text-align: center;
  font-size: 26px;
  color: #303133;
  margin-bottom: 4px;
}
.login-subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 32px;
}
</style>
