<script setup lang="ts">
import { ref, computed } from 'vue'
import type { CartItem } from '@/apis/cart'

const props = defineProps<{
  /** 当前商家购物车条目（空数组时整个栏隐藏） */
  items: CartItem[]
  merchantName: string
}>()

const emit = defineEmits<{
  (e: 'update', cartKey: string, newQty: number): void
  (e: 'clear'): void
  (e: 'checkout'): void
}>()

const panelVisible = ref(false)

/** 总数量（所有条目合计） */
const totalQty = computed(() => props.items.reduce((s, i) => s + i.quantity, 0))
/** 总价 */
const totalPrice = computed(() => props.items.reduce((s, i) => s + Number(i.price) * i.quantity, 0))
const totalPriceText = computed(() => '¥' + totalPrice.value.toFixed(2))

/** 加减数量：减到 0 即移除条目 */
const changeQty = (item: CartItem, delta: number) => {
  emit('update', item.cartKey, item.quantity + delta)
}
</script>

<template>
  <!-- ====== 底部购物车栏（仅当前商家有购物车商品时显示） ====== -->
  <div v-if="items.length" class="cart-bar">
    <div class="bar-left" @click="panelVisible = true">
      <div class="cart-icon">
        <van-icon name="shopping-cart" size="22" color="#fff" />
        <span v-if="totalQty > 0" class="badge">{{ totalQty > 99 ? '99+' : totalQty }}</span>
      </div>
      <div class="bar-price">
        <span class="price">{{ totalPriceText }}</span>
      </div>
    </div>
    <div class="bar-checkout" @click="emit('checkout')">去结算</div>
  </div>

  <!-- ====== 购物车弹出面板 ====== -->
  <van-popup :show="panelVisible" position="bottom" round safe-area-inset-bottom @update:show="panelVisible = $event">
    <div class="cart-panel">
      <div class="panel-head">
        <span class="panel-title">购物车 · {{ merchantName }}</span>
        <span class="panel-clear" @click="emit('clear'); panelVisible = false">清空</span>
      </div>

      <div v-if="items.length" class="panel-list">
        <div v-for="item in items" :key="item.cartKey" class="panel-row">
          <van-image :src="item.imageUrl" fit="cover" class="panel-img">
            <template #error>🍽️</template>
          </van-image>
          <div class="panel-info">
            <p class="panel-name">{{ item.productName }}</p>
            <p class="panel-spec">{{ item.specName || '' }}</p>
            <p class="panel-price">¥{{ Number(item.price).toFixed(2) }}</p>
          </div>
          <div class="panel-ctl">
            <span class="ctl-minus" @click="changeQty(item, -1)">-</span>
            <span class="ctl-num">{{ item.quantity }}</span>
            <span class="ctl-plus" @click="changeQty(item, 1)">+</span>
          </div>
        </div>
      </div>
      <van-empty v-else description="购物车是空的" image-size="80" />

      <div class="panel-foot">
        <span class="foot-total">合计：<b>{{ totalPriceText }}</b></span>
        <span class="foot-checkout" @click="emit('checkout')">去结算</span>
      </div>
    </div>
  </van-popup>
</template>

<style scoped>
/* ---- 底部栏 ---- */
.cart-bar {
  position: fixed;
  left: 12px;
  right: 12px;
  bottom: calc(12px + env(safe-area-inset-bottom));
  z-index: 10;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #2f2f33;
  border-radius: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
  padding: 0 4px 0 8px;
}
.bar-left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 100%;
  cursor: pointer;
}
.cart-icon {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  display: flex;
  align-items: center;
  justify-content: center;
}
.badge {
  position: absolute;
  top: -4px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff2e2e;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
}
.bar-price {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}
.bar-checkout {
  flex-shrink: 0;
  height: 40px;
  padding: 0 22px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 40px;
  cursor: pointer;
}

/* ---- 弹出面板 ---- */
.cart-panel {
  padding: 14px 16px calc(12px + env(safe-area-inset-bottom));
  max-height: 55vh;
  display: flex;
  flex-direction: column;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.panel-clear {
  font-size: 12px;
  color: #86909c;
  cursor: pointer;
}
.panel-list {
  flex: 1;
  overflow-y: auto;
}
.panel-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
}
.panel-img {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f2f3f5;
  font-size: 26px;
  overflow: hidden;
}
.panel-info {
  flex: 1;
  min-width: 0;
}
.panel-name {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.panel-spec {
  margin: 2px 0 0;
  font-size: 11px;
  color: #86909c;
}
.panel-price {
  margin: 2px 0 0;
  font-size: 13px;
  font-weight: 700;
  color: #f53f3f;
}
.panel-ctl {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ctl-minus {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #c9cdd4;
  color: #4e5969;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
}
.ctl-num {
  min-width: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  text-align: center;
}
.ctl-plus {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
  cursor: pointer;
}
.panel-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
}
.foot-total {
  font-size: 13px;
  color: #4e5969;
}
.foot-total b {
  font-size: 18px;
  color: #f53f3f;
}
.foot-checkout {
  padding: 8px 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
</style>
