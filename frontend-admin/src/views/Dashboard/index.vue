<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getDashboardStats, getUserTrend, getApplicationTrend, getOrderStatusDistribution, getRecentOrders,
  type DashboardStats, type TrendPoint, type OrderStatusDistItem,
} from '@/apis/admin'
import StatCards from './components/StatCards.vue'
import TrendChart from './components/TrendChart.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()

const stats = ref<DashboardStats | null>(null)
const orderStatus = ref<OrderStatusDistItem[]>([])
const recentOrders = ref<any[]>([])
const loading = ref(true)

/** 注册用户趋势：独立的天数与数据 */
const userDays = ref(7)
const userTrend = ref<TrendPoint[] | null>(null)
const userTrendLoading = ref(false)

/** 商家入驻趋势：独立的天数与数据 */
const applicationDays = ref(7)
const applicationTrend = ref<TrendPoint[] | null>(null)
const applicationTrendLoading = ref(false)

const STATUS_COLORS: Record<string, string> = {
  'PENDING_PAY': '#909399',
  'PAID': '#409EFF',
  'ACCEPTED': '#E6A23C',
  'COOKING': '#F56C6C',
  'COMPLETED': '#67C23A',
  'CANCELLED': '#F56C6C',
  'REFUNDING': '#E6A23C',
  'REFUNDED': '#909399',
}
const STATUS_LABEL: Record<string, string> = {
  'PENDING_PAY': '待支付',
  'PAID': '已支付',
  'ACCEPTED': '已接单',
  'COOKING': '制作中',
  'COMPLETED': '已完成',
  'CANCELLED': '已取消',
  'REFUNDING': '退款中',
  'REFUNDED': '已退款',
}
const STATUS_TAG: Record<string, 'info' | 'success' | 'warning' | 'danger' | ''> = {
  'PENDING_PAY': 'info',
  'PAID': '',
  'ACCEPTED': 'warning',
  'COOKING': 'danger',
  'COMPLETED': 'success',
  'CANCELLED': 'info',
  'REFUNDING': 'warning',
  'REFUNDED': 'info',
}

/** 当天日期 yyyy-MM-dd（本地时区） */
const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const updatedAtText = computed(() => {
  const t = stats.value?.updatedAt
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
})

const formatMoney = (n: number) => '¥' + Number(n ?? 0).toFixed(2)

/** 今日订单状态分布饼图 */
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' as const, formatter: '{b}: {c} 单 ({d}%)' },
  legend: { bottom: 0, textStyle: { fontSize: 11 } },
  series: [{
    name: '订单状态',
    type: 'pie' as const,
    radius: ['40%', '70%'],
    center: ['50%', '44%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{c}单', fontSize: 10, lineHeight: 14 },
    data: orderStatus.value.map((s) => ({
      name: s.label,
      value: s.count,
      itemStyle: { color: STATUS_COLORS[s.status] || '#909399' },
    })),
  }],
}))

/** 加载注册用户趋势（独立请求） */
const loadUserTrend = async () => {
  userTrendLoading.value = true
  try {
    const res = await getUserTrend(userDays.value)
    userTrend.value = res.data
  } finally {
    userTrendLoading.value = false
  }
}

/** 加载商家入驻趋势（独立请求） */
const loadApplicationTrend = async () => {
  applicationTrendLoading.value = true
  try {
    const res = await getApplicationTrend(applicationDays.value)
    applicationTrend.value = res.data
  } finally {
    applicationTrendLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const [sRes, osRes, roRes] = await Promise.all([
      getDashboardStats(),
      getOrderStatusDistribution(todayStr()),
      getRecentOrders(10),
    ])
    stats.value = sRes.data
    orderStatus.value = osRes.data || []
    recentOrders.value = roRes.data || []
    await Promise.all([loadUserTrend(), loadApplicationTrend()])
  } finally {
    loading.value = false
  }
}

const goOrders = () => router.push('/orders')

onMounted(load)
</script>

<template>
  <div class="fade-in dashboard-page">
    <!-- 页头 -->
    <div class="page-header dashboard-header">
      <h2>首页统计</h2>
      <div class="header-right">
        <span class="update-text">数据更新于 {{ updatedAtText }}</span>
        <el-button size="small" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- 核心指标卡片 -->
    <StatCards v-if="stats" :stats="stats" />
    <el-skeleton v-else :rows="2" animated style="margin-bottom: 20px" />

    <!-- 2×2 图表区（4 等分，宽高一致） -->
    <div class="chart-grid">
      <!-- 注册用户趋势 -->
      <el-card shadow="hover" v-loading="userTrendLoading" class="panel-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#EEF3FD;color:#5B8DEF;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            </span>
            注册用户趋势
            <el-radio-group v-model="userDays" size="small" class="card-days" @change="loadUserTrend">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-body">
          <TrendChart v-if="userTrend" :data="userTrend" color="#5B8DEF" unit="人" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </div>
      </el-card>

      <!-- 商家入驻趋势 -->
      <el-card shadow="hover" v-loading="applicationTrendLoading" class="panel-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#F0FAF0;color:#52C41A;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><path d="M9 22V12h6v10"/></svg>
            </span>
            商家入驻趋势
            <el-radio-group v-model="applicationDays" size="small" class="card-days" @change="loadApplicationTrend">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="chart-body">
          <TrendChart v-if="applicationTrend" :data="applicationTrend" color="#52C41A" unit="家" />
          <el-empty v-else description="暂无数据" :image-size="60" />
        </div>
      </el-card>

      <!-- 今日订单状态分布 -->
      <el-card shadow="hover" v-loading="loading" class="panel-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#F0FAF0;color:#52C41A;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a10 10 0 0 1 0 20"/><path d="M2 12h20"/></svg>
            </span>
            今日订单状态分布
          </div>
        </template>
        <div class="chart-body">
          <v-chart v-if="orderStatus.length" :option="pieOption" style="height: 100%" autoresize />
          <el-empty v-else description="今日暂无订单" :image-size="60" />
        </div>
      </el-card>

      <!-- 最新订单 -->
      <el-card shadow="hover" v-loading="loading" class="panel-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#F0FAF0;color:#52C41A;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 3v18"/></svg>
            </span>
            最新订单
            <span class="view-all" @click="goOrders">查看全部 →</span>
          </div>
        </template>
        <div class="table-body">
          <el-table :data="recentOrders" stripe size="small" height="100%" class="compact-table" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" min-width="150" show-overflow-tooltip />
            <el-table-column prop="merchantName" label="商家" min-width="110" show-overflow-tooltip />
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small" disable-transitions>
                  {{ STATUS_LABEL[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="90" align="right">
              <template #default="{ row }">
                <span class="money-cell">{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page {
  height: calc(100vh - var(--header-height) - 48px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.dashboard-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.dashboard-header h2 {
  margin: 0;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.update-text {
  font-size: 12px;
  color: var(--text-secondary);
}

/* 2×2 图表区，占满剩余空间 */
.chart-grid {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 20px;
}

.panel-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  border: 1px solid var(--border-light);
}
.panel-card :deep(.el-card__header) {
  padding: 12px 18px;
  flex-shrink: 0;
}
.panel-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}
.card-title-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.card-days {
  margin-left: auto;
  flex-shrink: 0;
}
.view-all {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  color: var(--primary);
  cursor: pointer;
}

.chart-body {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chart-body > * {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: 100%;
}
.table-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.money-cell {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.compact-table :deep(.el-table__header th) {
  padding: 8px 0;
}
.compact-table :deep(.el-table__body td) {
  padding: 6px 0;
}
</style>
