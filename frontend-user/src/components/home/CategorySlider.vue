<script setup lang="ts">
import type { CategoryVO } from '@/apis/home'

defineProps<{
  /** 分类列表 */
  categories: CategoryVO[]
  /** 当前选中分类 ID（0 表示「全部」） */
  activeId?: number
}>()

const emit = defineEmits<{
  select: [category: CategoryVO]
}>()

/** 「全部」分类项（约定 id=0 表示清除分类筛选） */
const ALL = { id: 0, name: '全部', icon: '🍽️' } as CategoryVO
</script>

<template>
  <div class="category-slider">
    <!-- 全部（清除分类筛选） -->
    <div
      class="category-item"
      :class="{ active: activeId === 0 }"
      @click="emit('select', ALL)"
    >
      <span class="category-icon">{{ ALL.icon }}</span>
      <span class="category-name">{{ ALL.name }}</span>
    </div>

    <div
      v-for="c in categories"
      :key="c.id"
      class="category-item"
      :class="{ active: activeId === c.id }"
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
  transition: transform 0.15s;
}
.category-item.active .category-icon {
  background: #ff6f3c;
  transform: scale(1.05);
}
.category-name {
  font-size: 12px;
  color: var(--van-text-color-2);
  white-space: nowrap;
}
.category-item.active .category-name {
  color: #ff6f3c;
  font-weight: 600;
}
</style>
