<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ProductVO } from '@/apis/home'

const props = defineProps<{
  visible: boolean
  product: ProductVO | null
  /** 加购请求中（按钮 loading） */
  addLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'confirm', specId: number, qty: number): void
}>()

const specSelected = ref<number | null>(null)
const specQty = ref(1)

/** 打开弹窗时重置选中规格与数量 */
watch(
  () => props.visible,
  (v) => {
    if (v) {
      specSelected.value = props.product?.specs?.[0]?.id ?? null
      specQty.value = 1
    }
  },
)

/** 商品价格展示：基础价 + 第一个规格加价后最低价 */
const minPrice = (p: ProductVO) => {
  const deltas = (p.specs || []).map((s) => Number(s.priceDelta || 0))
  const min = deltas.length ? Math.min(...deltas) : 0
  return '¥' + (Number(p.price) + min).toFixed(2)
}
</script>

<template>
  <!-- ====== 规格选择弹窗 ====== -->
  <van-popup
    :show="visible"
    position="bottom"
    round
    safe-area-inset-bottom
    @update:show="emit('update:visible', $event)"
  >
    <div class="spec-pop">
      <div class="spec-head">
        <van-image :src="product?.imageUrl" fit="cover" class="spec-img">
          <template #error>🍽️</template>
        </van-image>
        <div class="spec-title">
          <p class="spec-name">{{ product?.name }}</p>
          <p class="spec-price">{{ product ? minPrice(product) : '' }}</p>
        </div>
        <van-icon name="cross" class="spec-close" @click="emit('update:visible', false)" />
      </div>

      <p class="spec-label">规格</p>
      <div class="spec-list">
        <span
          v-for="s in product?.specs || []"
          :key="s.id"
          class="spec-chip"
          :class="{ active: specSelected === s.id }"
          @click="specSelected = s.id!"
        >
          {{ s.name }}{{ Number(s.priceDelta) > 0 ? ` +¥${s.priceDelta}` : '' }}
        </span>
      </div>

      <div class="spec-qty">
        <span class="spec-label">数量</span>
        <van-stepper v-model="specQty" :min="1" :max="99" theme="round" button-size="24" />
      </div>

      <van-button
        type="primary"
        block
        round
        class="spec-confirm"
        :loading="addLoading"
        @click="specSelected != null && emit('confirm', specSelected, specQty)"
      >
        加入购物车
      </van-button>
    </div>
  </van-popup>
</template>

<style scoped>
.spec-pop {
  padding: 20px 20px calc(16px + env(safe-area-inset-bottom));
}
.spec-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.spec-img {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f3f5;
  font-size: 30px;
  overflow: hidden;
}
.spec-title {
  flex: 1;
  min-width: 0;
}
.spec-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
}
.spec-price {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #f53f3f;
}
.spec-close {
  font-size: 18px;
  color: #a9aeb8;
  cursor: pointer;
}
.spec-label {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}
.spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.spec-chip {
  padding: 7px 14px;
  border-radius: 16px;
  background: #f6f7f9;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}
.spec-chip.active {
  background: #fff4e6;
  color: #f0810d;
  font-weight: 600;
  border: 1px solid #f59a23;
}
.spec-qty {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.spec-qty .spec-label {
  margin: 16px 0 0;
}
.spec-confirm {
  margin-top: 18px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  border: none;
}
</style>
