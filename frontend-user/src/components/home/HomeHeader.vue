<script setup lang="ts">
import { useRouter } from 'vue-router'

defineProps<{
  /** 默认地址中的区县名（无地址时为空，不展示） */
  district: string
}>()

const emit = defineEmits<{
  /** 点击区县跳转地址页 */
  goAddress: []
}>()

const router = useRouter()

/** 点击搜索框 → 跳搜索页（搜索走独立页面，重新发起接口请求） */
const goSearch = () => router.push('/search')
</script>

<template>
  <div class="home-header">
    <!-- 左：默认地址区县，占头部 1/4 -->
    <div v-if="district" class="district" @click="emit('goAddress')">
      <van-icon name="location-o" size="16" />
      <span class="district-text">{{ district }}</span>
      <van-icon name="arrow-down" size="10" />
    </div>

    <!-- 右：搜索栏（只读，点击进入搜索页） -->
    <van-search
      class="search"
      placeholder="搜索店铺 / 美食"
      shape="round"
      readonly
      @click="goSearch"
    />
  </div>
</template>

<style scoped>
.home-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 12px;
  background: linear-gradient(135deg, #f59a23 0%, #e07f0e 100%);
}

/* 区县：占头部 1/4 */
.district {
  width: 25%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.district-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 搜索栏：占剩余 3/4 */
.search {
  flex: 1;
  padding: 0;
}
.search :deep(.van-search__content) {
  background: #fff;
  border-radius: 20px;
}
</style>
