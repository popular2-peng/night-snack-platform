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
    <!-- 装饰光斑 -->
    <span class="deco deco-1" />
    <span class="deco deco-2" />

    <div class="header-row">
      <!-- 左：默认地址区县（毛玻璃胶囊） -->
      <div v-if="district" class="district" @click="emit('goAddress')">
        <van-icon name="location-o" size="16" />
        <span class="district-text">{{ district }}</span>
        <van-icon name="arrow-down" size="10" />
      </div>

      <!-- 右：搜索条（点击进搜索页），右侧「搜索」按钮 -->
      <div class="search" @click="goSearch">
        <van-icon name="search" class="search-icon" />
        <span class="search-placeholder">搜索店铺 / 美食</span>
        <span class="search-btn">搜索</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-header {
  position: relative;
  overflow: hidden;
  padding: calc(10px + env(safe-area-inset-top)) 14px 18px;
  background: linear-gradient(160deg, #f59a23 0%, #f0810d 55%, #e86f04 100%);
}

/* 装饰光斑 */
.deco {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.deco-1 {
  width: 150px;
  height: 150px;
  right: -40px;
  top: -60px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.22), transparent 70%);
}
.deco-2 {
  width: 100px;
  height: 100px;
  left: -30px;
  bottom: -50px;
  background: radial-gradient(circle, rgba(255, 214, 143, 0.35), transparent 70%);
}

.header-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 区县毛玻璃胶囊 */
.district {
  flex-shrink: 0;
  max-width: 30%;
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 9px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.district:active {
  background: rgba(255, 255, 255, 0.3);
}
.district-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 搜索条 */
.search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 4px 0 14px;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(122, 62, 0, 0.18);
  cursor: pointer;
  transition: transform 0.15s;
}
.search:active {
  transform: scale(0.985);
}
.search-icon {
  color: #a9aeb8;
  flex-shrink: 0;
}
.search-placeholder {
  flex: 1;
  color: #a9aeb8;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-btn {
  flex-shrink: 0;
  padding: 7px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 3px 8px rgba(245, 154, 35, 0.35);
}
</style>
