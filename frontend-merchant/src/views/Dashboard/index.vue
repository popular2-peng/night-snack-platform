<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <div class="placeholder-page">
    <span class="placeholder-icon">🛠️</span>
    <span class="placeholder-title">欢迎回来，{{ authStore.username }}</span>
    <span class="placeholder-desc">商家工作台建设中，敬请期待</span>
  </div>
</template>
