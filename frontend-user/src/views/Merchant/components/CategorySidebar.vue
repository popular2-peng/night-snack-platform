<script setup lang="ts">
import { ref, watch } from 'vue'

/** 左分类项（名称列表，顺序即分组顺序） */
const props = defineProps<{
  cats: string[]
  activeIndex: number
}>()

const emit = defineEmits<{
  (e: 'select', idx: number): void
}>()

const catCol = ref<HTMLElement | null>(null)

/** active 变化时，左栏自动滚动让当前分类保持可视（居中） */
watch(
  () => props.activeIndex,
  (idx) => {
    const target = catCol.value?.querySelectorAll<HTMLElement>('.cat-item')[idx]
    if (target && catCol.value) {
      const targetTop = target.offsetTop - catCol.value.clientHeight / 2 + target.clientHeight / 2
      catCol.value.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' })
    }
  },
)
</script>

<template>
  <!-- 左：分类列表（区域内可滑动） -->
  <div ref="catCol" class="cat-col">
    <div
      v-for="(cat, i) in cats"
      :key="cat"
      class="cat-item"
      :class="{ active: activeIndex === i }"
      @click="emit('select', i)"
    >
      {{ cat }}
    </div>
  </div>
</template>

<style scoped>
.cat-col {
  width: 88px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #f6f7f9;
  padding: 4px 0 20px;
}
.cat-col::-webkit-scrollbar {
  display: none;
}
.cat-item {
  position: relative;
  padding: 14px 8px;
  font-size: 13px;
  color: #4e5969;
  text-align: center;
  cursor: pointer;
  word-break: break-all;
}
.cat-item.active {
  background: #fff;
  color: #f0810d;
  font-weight: 700;
}
.cat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, #f59a23, #ff6f3c);
}
</style>
