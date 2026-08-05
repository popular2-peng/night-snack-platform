<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const placeholderMap: Record<string, { icon: string; title: string; desc: string }> = {
  '/orders': { icon: '🧾', title: '订单中心', desc: '订单查询、支付、售后，敬请期待' },
  '/home':   { icon: '🏪', title: '附近美食', desc: '附近商家浏览，敬请期待' },
}

const info = placeholderMap[route.path] || { icon: '🚧', title: '建设中', desc: '敬请期待' }
</script>

<template>
  <div class="placeholder-page">
    <span class="placeholder-icon">{{ info.icon }}</span>
    <span class="placeholder-title">{{ info.title }}</span>
    <span class="placeholder-desc">{{ info.desc }}</span>
  </div>
</template>
