<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <div class="home-page">
    <!-- 顶部问候 -->
    <div class="home-header">
      <p class="greeting">晚上好，{{ authStore.username }}</p>
      <p class="greeting-sub">今晚想吃点什么？</p>
    </div>

    <div class="placeholder-page">
      <span class="placeholder-icon">🏪</span>
      <span class="placeholder-title">附近美食</span>
      <span class="placeholder-desc">商家列表、品类导航建设中，敬请期待</span>
    </div>
  </div>
</template>

<style scoped>
.home-header {
  padding: 24px 20px 20px;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border-radius: 0 0 24px 24px;
  color: #fff;
}
.greeting {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px;
}
.greeting-sub {
  font-size: 13px;
  margin: 0;
  opacity: 0.85;
}
</style>
