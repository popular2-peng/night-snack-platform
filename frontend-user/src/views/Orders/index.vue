<script setup lang="ts">
import { ref } from 'vue'
import { getOrdersApi, type UserOrder } from '@/apis/order'
import OrderCard from './components/OrderCard.vue'

/** 订单类型 tab：key 为空=全部；支持逗号分隔多状态（退款中/已退款 = REFUNDING,REFUNDED） */
const TABS = [
  { key: '', label: '全部' },
  { key: 'PENDING_PAY', label: '待支付' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
  { key: 'REFUNDING,REFUNDED', label: '退款中/已退款' },
]

// ---- 数据 ----
const keyword = ref('')
const activeTab = ref('')
const orders = ref<UserOrder[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

/** 上拉加载更多 */
const loadMore = async () => {
  try {
    const res = await getOrdersApi({
      page: page.value,
      size,
      status: activeTab.value || undefined,
      keyword: keyword.value.trim() || undefined,
    })
    const list = res.data.list || []
    orders.value = page.value === 1 ? list : [...orders.value, ...list]
    total.value = res.data.total || 0
    finished.value = orders.value.length >= total.value
    page.value += 1
  } catch {
    finished.value = true
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

/** 重置并重新加载（搜索 / 切 tab 共用） */
const reload = async () => {
  page.value = 1
  orders.value = []
  finished.value = false
  await loadMore()
}

const onSearch = () => reload()
const switchTab = (key: string) => {
  activeTab.value = key
  reload()
}
const onRefresh = async () => {
  page.value = 1
  orders.value = []
  finished.value = false
  await loadMore()
}

/** 子卡片删除成功后：从列表移除并修正总数 */
const onOrderDeleted = (id: number) => {
  orders.value = orders.value.filter((x) => x.id !== id)
  total.value = Math.max(0, total.value - 1)
}
</script>

<template>
  <div class="orders-page">
    <!-- 顶部搜索栏（粘性） -->
    <div class="search-bar">
      <van-search
        v-model="keyword"
        placeholder="搜索商家、商品"
        shape="round"
        @search="onSearch"
        @clear="onSearch"
      />
    </div>

    <!-- 订单类型 tab（粘性） -->
    <div class="tabs-bar">
      <div
        v-for="t in TABS"
        :key="t.key"
        class="tab-item"
        :class="{ active: activeTab === t.key }"
        @click="switchTab(t.key)"
      >
        {{ t.label }}
      </div>
    </div>

    <!-- 订单列表（下拉刷新 + 上拉加载） -->
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="loadMore">
        <OrderCard
          v-for="o in orders"
          :key="o.id"
          :order="o"
          @deleted="onOrderDeleted"
        />

        <van-empty v-if="finished && !orders.length" description="暂无订单" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding-bottom: 20px;
}

/* ---- 搜索栏（粘性顶部） ---- */
.search-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #fff;
}
.search-bar :deep(.van-search__content) {
  background: #f6f7f9;
}

/* ---- 订单类型 tab（粘性） ---- */
.tabs-bar {
  position: sticky;
  top: 54px;
  z-index: 19;
  display: flex;
  background: #fff;
  border-bottom: 1px solid #f2f3f5;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.tabs-bar::-webkit-scrollbar {
  display: none;
}
.tab-item {
  flex-shrink: 0;
  padding: 12px 16px;
  font-size: 14px;
  color: #666;
  border-bottom: 2px solid transparent;
  transition: color 0.2s;
}
.tab-item.active {
  color: #f59a23;
  font-weight: 600;
  border-bottom-color: #f59a23;
}
</style>
