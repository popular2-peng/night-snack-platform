<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

const placeholderMap: Record<string, { icon: string; title: string; desc: string }> = {
  '/orders':   { icon: '📋', title: '订单管理', desc: '接单、制作、完成全流程管理，敬请期待' },
  '/products': { icon: '🍜', title: '商品管理', desc: '菜品、规格、库存管理，敬请期待' },
  '/shop':     { icon: '🏪', title: '店铺设置', desc: '店铺信息、营业时间、配送范围设置，敬请期待' },
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
