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
  <div class="category-wrap">
    <div class="category-slider">
      <!-- 全部（清除分类筛选） -->
      <span
        class="category-chip"
        :class="{ active: activeId === 0 }"
        @click="emit('select', ALL)"
      >
        <span class="chip-icon">{{ ALL.icon }}</span>
        {{ ALL.name }}
      </span>

      <span
        v-for="c in categories"
        :key="c.id"
        class="category-chip"
        :class="{ active: activeId === c.id }"
        @click="emit('select', c)"
      >
        <span class="chip-icon">{{ c.icon || '🍜' }}</span>
        {{ c.name }}
      </span>
    </div>
    <!-- 右侧渐隐遮罩：提示可继续滑动 -->
    <span class="fade-mask" />
  </div>
</template>

<style scoped>
.category-wrap {
  position: relative;
  margin: -12px 10px 0;
  background: #fff;
  border-radius: 16px 16px 0 0;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.03);
}

.category-slider {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding: 14px 12px 12px;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}
.category-slider::-webkit-scrollbar {
  display: none;
}

/* 胶囊标签 */
.category-chip {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border-radius: 18px;
  background: #f6f7f9;
  color: #4e5969;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
}
.category-chip:active {
  transform: scale(0.96);
}
.chip-icon {
  font-size: 14px;
  line-height: 1;
}
.category-chip.active {
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 10px rgba(245, 154, 35, 0.35);
}

/* 右侧渐隐遮罩（示意可横向滑动更多） */
.fade-mask {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 36px;
  pointer-events: none;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), #fff 75%);
  border-radius: 0 16px 0 0;
}
</style>
