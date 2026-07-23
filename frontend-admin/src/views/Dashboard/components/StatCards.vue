<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getDashboardStats, getOrderStatusDistribution, getRecentOrders } from '@/apis/admin'
import { useAuthStore } from '@/stores/auth'
import StatCard from '@/components/StatCard.vue'
import { User, Shop, Document, Clock } from '@element-plus/icons-vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册 ECharts 组件
use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface Stats { totalUsers: number; totalMerchants: number; totalOrders: number; pendingApplications: number }
interface OrderStat { status: string; label: string; count: number }
interface RecentOrder { orderNo: string; username: string; merchantName: string; status: string; totalAmount: number; createdAt: string }

const stats = ref<Stats>({ totalUsers: 0, totalMerchants: 0, totalOrders: 0, pendingApplications: 0 })
const orderStats = ref<OrderStat[]>([])
const recentOrders = ref<RecentOrder[]>([])
const loading = ref(true)

const authStore = useAuthStore()

const today = computed(() => {
  const d = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

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

type TagType = 'info' | 'success' | 'warning' | 'danger' | ''

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

const STATUS_TAG: Record<string, TagType> = {
  'PENDING_PAY': 'info',
  'PAID': '',
  'ACCEPTED': 'warning',
  'COOKING': 'danger',
  'COMPLETED': 'success',
  'CANCELLED': 'info',
  'REFUNDING': 'warning',
  'REFUNDED': 'info',
}

/** 格式化时间，去掉秒和 T */
function formatTime(iso: string) {
  if (!iso) return ''
  return iso.replace('T', ' ').substring(0, 16)
}

/** 饼图配置 */
const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item' as const,
    formatter: '{b}: {c} 单 ({d}%)',
  },
  legend: {
    bottom: 0,
    formatter: (name: string) => {
      const item = orderStats.value.find(s => s.label === name)
      if (item) {
        const total = orderStats.value.reduce((sum, s) => sum + s.count, 0)
        const pct = total > 0 ? ((item.count / total) * 100).toFixed(1) : '0'
        return `${name}  ${item.count}单  ${pct}%`
      }
      return name
    },
    textStyle: { fontSize: 12 },
  },
  series: [{
    name: '订单状态',
    type: 'pie' as const,
    radius: ['45%', '75%'],
    center: ['50%', '48%'],
    avoidLabelOverlap: true,
    itemStyle: {
      borderRadius: 6,
      borderColor: '#fff',
      borderWidth: 2,
    },
    label: {
      show: true,
      position: 'outside' as const,
      formatter: '{d}%',
      fontSize: 12,
      lineHeight: 16,
    },
    labelLine: {
      length: 16,
      length2: 28,
      smooth: true,
    },
    emphasis: {
      label: { fontSize: 16, fontWeight: 'bold' },
      scaleSize: 12,
    },
    data: orderStats.value.map(s => ({
      name: s.label,
      value: s.count,
      itemStyle: { color: STATUS_COLORS[s.status] || '#909399' },
    })),
  }],
}))

onMounted(async () => {
  try {
    const [sRes, oRes, rRes] = await Promise.all([
      getDashboardStats(),
      getOrderStatusDistribution(),
      getRecentOrders(10),
    ])
    stats.value = sRes.data
    orderStats.value = oRes.data || []
    recentOrders.value = rRes.data || []
  } finally {
    loading.value = false
  }
})

/** 格式化金额 */
function formatMoney(n: number) {
  return '¥' + Number(n).toFixed(2)
}
</script>

<template>
  <!-- 欢迎横幅 -->
  <div class="welcome-banner fade-in">
    <div class="welcome-text">
      <h3>👋 欢迎回来，{{ authStore.username }}</h3>
      <p>祝你工作愉快！以下是平台运营数据概览</p>
    </div>
    <div class="welcome-date">
      <strong>{{ today }}</strong>
    </div>
  </div>

  <!-- 核心统计卡片 -->
  <el-row :gutter="20" v-loading="loading" element-loading-text="加载中...">
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="总用户数" :value="stats.totalUsers" color="#409EFF" :icon="User" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="总商家数" :value="stats.totalMerchants" color="#67C23A" :icon="Shop" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="总订单数" :value="stats.totalOrders" color="#E6A23C" :icon="Document" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="待审核入驻" :value="stats.pendingApplications" color="#F56C6C" :icon="Clock" />
    </el-col>
  </el-row>

  <!-- 图表 + 最新订单 -->
  <el-row :gutter="20" style="margin-top: 8px">
    <!-- 订单状态分布饼图 -->
    <el-col :xs="24" :lg="12" style="margin-bottom: 20px">
      <el-card shadow="hover" v-loading="loading" class="chart-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#EEF3FD;color:#5B8DEF;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 2a10 10 0 0 1 0 20"/><path d="M2 12h20"/></svg>
            </span>
            订单状态分布
          </div>
        </template>
        <v-chart :option="pieOption" style="height: 360px" autoresize />
      </el-card>
    </el-col>

    <!-- 最新订单 -->
    <el-col :xs="24" :lg="12" style="margin-bottom: 20px">
      <el-card shadow="hover" v-loading="loading" class="table-card">
        <template #header>
          <div class="card-title">
            <span class="card-title-icon" style="background:#F0FAF0;color:#52C41A;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 3v18"/></svg>
            </span>
            最新订单
          </div>
        </template>
        <el-table :data="recentOrders" stripe size="small" height="360" class="compact-table" style="width: 100%">
          <el-table-column prop="orderNo" label="订单号" min-width="120" show-overflow-tooltip />
          <el-table-column prop="username" label="用户" min-width="60" />
          <el-table-column prop="merchantName" label="商家" min-width="80" show-overflow-tooltip />
          <el-table-column label="状态" min-width="70" align="center">
            <template #default="{ row }">
              <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small" disable-transitions>
                {{ STATUS_LABEL[row.status] || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="金额" min-width="75" align="right">
            <template #default="{ row }">
              <span class="money-cell">{{ formatMoney(row.totalAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时间" min-width="115">
            <template #default="{ row }">
              <span class="time-cell">{{ formatTime(row.createdAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.card-title-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.money-cell {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}
.time-cell {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.compact-table :deep(.el-table__header th) {
  padding: 10px 0;
}
.compact-table :deep(.el-table__body td) {
  padding: 8px 0;
}
/* 小屏时表格可横向滚动 */
@media (max-width: 768px) {
  .compact-table {
    overflow-x: auto;
  }
}
</style>
