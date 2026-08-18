<script setup lang="ts">
import { onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { HomeMerchant } from '@/apis/home'
import MerchantCard from './MerchantCard.vue'

const props = defineProps<{
  /** 商家列表 */
  merchants: HomeMerchant[]
  /** 加载中 */
  loading: boolean
}>()

const emit = defineEmits<{
  click: [merchant: HomeMerchant]
  /** 卡片进入视口：请求该商家的在售商品（懒加载） */
  visible: [merchant: HomeMerchant]
}>()

// ---- 商品懒加载：IntersectionObserver 监听卡片，进入视口（提前 200px）才发 visible ----
let observer: IntersectionObserver | null = null
const observed = new WeakSet<Element>()

const ensureObserve = () => {
  if (typeof IntersectionObserver === 'undefined') {
    // 兼容降级：不支持则全量触发一次
    props.merchants.forEach((m) => emit('visible', m))
    return
  }
  if (!observer) {
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          const el = entry.target as HTMLElement
          const id = Number(el.dataset.mid)
          const m = props.merchants.find((x) => x.id === id)
          if (m) emit('visible', m)
        })
      },
      // 底部负 rootMargin：减去固定底部导航栏高度（50px + 安全区余量），
      // 被 tabbar 盖住的卡片不算「可见」，避免为看不见的商家发请求
      { threshold: 0, rootMargin: '0px 0px -70px 0px' },
    )
  }
  // 列表变化（分类筛选/搜索）后重新挂接新卡片
  nextTick(() => {
    document.querySelectorAll('[data-mid]').forEach((el) => {
      if (!observed.has(el)) {
        observed.add(el)
        observer?.observe(el)
      }
    })
  })
}

watch(() => props.merchants, ensureObserve)
onMounted(ensureObserve)
onUnmounted(() => observer?.disconnect())
</script>

<template>
  <div class="merchant-list">
    <van-loading v-if="loading" class="loading" size="24" vertical>加载中...</van-loading>

    <template v-else-if="merchants.length">
      <div
        v-for="m in merchants"
        :key="m.id"
        class="card-wrap"
        :data-mid="m.id"
      >
        <MerchantCard :merchant="m" @click="emit('click', m)" />
      </div>
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
.card-wrap {
  margin-bottom: 10px;
  border-radius: 12px;
  overflow: hidden;
}
.card-wrap:last-child {
  margin-bottom: 0;
}
</style>
