<script setup lang="ts">
import type { HomeMerchant } from '@/apis/home'

defineProps<{
  /** 单个商家（含在售商品预览） */
  merchant: HomeMerchant
}>()

const emit = defineEmits<{
  /** 点击商家卡片 */
  click: [merchant: HomeMerchant]
}>()

/** 评分展示：有值显示一位小数，无值显示「暂无」 */
const ratingText = (rating: number | null) => (rating != null ? Number(rating).toFixed(1) : '暂无')
</script>

<template>
  <div class="merchant-card" @click="emit('click', merchant)">
    <!-- 左：商家 Logo，占屏幕 1/5 -->
    <div class="logo-box">
      <van-image
        :src="merchant.logoUrl || merchant.avatarUrl"
        fit="cover"
        class="logo"
      >
        <template #error>🏪</template>
      </van-image>
    </div>

    <!-- 右：上下两部分 -->
    <div class="info">
      <!-- 上：左店铺名 + 右评分 -->
      <div class="info-top">
        <span class="name">{{ merchant.name }}</span>
        <span class="rating">
          <van-icon name="star" color="#f59a23" size="12" />
          {{ ratingText(merchant.avgRating) }}
        </span>
      </div>

      <!-- 下：在售商品（上图片 / 中名称 / 下价格；懒加载：进入视口后请求） -->
      <div v-if="merchant.products.length" class="products">
        <div v-for="p in merchant.products" :key="p.id" class="product">
          <van-image :src="p.imageUrl" fit="cover" class="product-img">
            <template #error>🍽️</template>
          </van-image>
          <span class="product-name">{{ p.name }}</span>
          <span class="product-price">¥{{ p.price }}</span>
        </div>
      </div>
      <!-- 未加载：骨架占位 -->
      <div v-else-if="!merchant.productsLoaded" class="products-skeleton">
        <div v-for="i in 3" :key="i" class="skeleton-item">
          <van-skeleton-image class="skeleton-img" />
          <van-skeleton class="skeleton-line" title :row="0" />
        </div>
      </div>
      <!-- 已加载但无在售商品 -->
      <div v-else class="products-empty">暂无在售商品</div>
    </div>
  </div>
</template>

<style scoped>
.merchant-card {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
}

/* 左 Logo：1/5 */
.logo-box {
  width: 20%;
  flex-shrink: 0;
}
.logo {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 10px;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 32px;
}

/* 右信息区 */
.info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 上：店名 + 评分 */
.info-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.name {
  flex: 1;
  min-width: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--van-text-color);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rating {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  color: #f59a23;
  font-weight: 600;
}

/* 下：在售商品（固定小图，可左右滑动查看更多） */
.products {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  overflow-x: auto;
  flex-wrap: nowrap;
  scrollbar-width: none;
  -ms-overflow-style: none;
  padding-bottom: 4px;
}
.products::-webkit-scrollbar {
  display: none;
}
.product {
  flex-shrink: 0;
  width: 76px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.product-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 24px;
}
.product-name {
  width: 100%;
  font-size: 11px;
  color: var(--van-text-color-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}
.product-price {
  font-size: 12px;
  font-weight: 700;
  color: #f53f3f;
}

/* ---- 商品懒加载骨架 ---- */
.products-skeleton {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding-bottom: 4px;
}
.skeleton-item {
  flex-shrink: 0;
  width: 76px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.skeleton-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
}
.skeleton-line {
  width: 100%;
}
.products-empty {
  margin-top: 10px;
  font-size: 12px;
  color: var(--van-text-color-3);
}
</style>
