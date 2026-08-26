<script setup lang="ts">
import type { MerchantVO } from '@/apis/home'

defineProps<{
  shop: MerchantVO | null
  loading: boolean
}>()
</script>

<template>
  <!-- ====== 头部卡片（内边距 20px） ====== -->
  <div class="shop-head" v-loading="loading">
    <template v-if="shop">
      <van-image :src="shop.logoUrl" fit="cover" class="head-logo">
        <template #error>🏪</template>
      </van-image>
      <div class="head-info">
        <h2 class="head-name">{{ shop.name }}</h2>
        <div class="head-sub">
          <span class="head-rating">
            <van-icon name="star" color="#f59a23" size="12" />
            {{ shop.avgRating != null ? Number(shop.avgRating).toFixed(1) : '暂无' }}
          </span>
          <!-- 公告：一行显示，超出屏幕横向滚动（marquee） -->
          <div class="notice-wrap">
            <div class="notice-text" :class="{ marquee: (shop.notice || '').length > 12 }">
              {{ shop.notice || '本店暂无公告' }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.shop-head {
  display: flex;
  gap: 14px;
  margin: 0 10px;
  padding: 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}
.head-logo {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 34px;
}
.head-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}
.head-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.head-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.head-rating {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #f59a23;
  font-weight: 600;
}

/* 公告：一行显示，超出屏幕横向滚动 */
.notice-wrap {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  position: relative;
}
.notice-text {
  display: inline-block;
  font-size: 12px;
  color: #86909c;
  white-space: nowrap;
}
.notice-text.marquee {
  animation: notice-scroll 10s linear infinite;
  padding-right: 40px;
}
@keyframes notice-scroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
</style>
