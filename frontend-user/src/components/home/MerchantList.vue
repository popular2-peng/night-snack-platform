<script setup lang="ts">
import type { HomeMerchant } from '@/apis/home'
import MerchantCard from './MerchantCard.vue'

defineProps<{
  /** 商家列表 */
  merchants: HomeMerchant[]
  /** 加载中 */
  loading: boolean
}>()

const emit = defineEmits<{
  click: [merchant: HomeMerchant]
}>()
</script>

<template>
  <div class="merchant-list">
    <van-loading v-if="loading" class="loading" size="24" vertical>加载中...</van-loading>

    <template v-else-if="merchants.length">
      <MerchantCard
        v-for="m in merchants"
        :key="m.id"
        :merchant="m"
        class="card"
        @click="emit('click', m)"
      />
    </template>

    <van-empty v-else description="附近暂无营业中的商家" />
  </div>
</template>

<style scoped>
.merchant-list {
  padding: 8px 12px 16px;
}
.loading {
  padding: 40px 0;
}
.card {
  margin-bottom: 10px;
  border-radius: 12px;
  overflow: hidden;
}
.card:last-child {
  margin-bottom: 0;
}
</style>
