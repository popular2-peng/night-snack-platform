<script setup lang="ts">
import type { CategoryVO } from '@/apis/home'

defineProps<{
  /** 分类列表 */
  categories: CategoryVO[]
}>()

const emit = defineEmits<{
  select: [category: CategoryVO]
}>()
</script>

<template>
  <div class="category-slider">
    <div
      v-for="c in categories"
      :key="c.id"
      class="category-item"
      @click="emit('select', c)"
    >
      <span class="category-icon">{{ c.icon || '🍜' }}</span>
      <span class="category-name">{{ c.name }}</span>
    </div>
  </div>
</template>

<style scoped>
.category-slider {
  display: flex;
  gap: 4px;
  overflow-x: auto;
  padding: 14px 12px;
  background: #fff;
  -webkit-overflow-scrolling: touch;
}
.category-slider::-webkit-scrollbar {
  display: none;
}

.category-item {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 64px;
  cursor: pointer;
}
.category-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: #fff7ec;
  border-radius: 50%;
}
.category-name {
  font-size: 12px;
  color: var(--van-text-color-2);
  white-space: nowrap;
}
</style>
