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

/** 饼图配置 */
const pieOption = computed(() => ({
  tooltip: { trigger: 'item' as const },
  legend: { bottom: 0 },
  series: [{
    name: '订单状态',
    type: 'pie' as const,
    radius: ['45%', '75%'],
    avoidLabelOverlap: false,
    itemStyle: {
      borderRadius: 6,
      borderColor: '#fff',
      borderWidth: 2,
    },
    label: { show: true, formatter: '{b}\n{d}%' },
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
      <el-card shadow="hover" v-loading="loading">
        <template #header>
          <span class="card-title">📊 订单状态分布</span>
        </template>
        <v-chart :option="pieOption" style="height: 320px" autoresize />
      </el-card>
    </el-col>

    <!-- 最新订单 -->
    <el-col :xs="24" :lg="12" style="margin-bottom: 20px">
      <el-card shadow="hover" v-loading="loading">
        <template #header>
          <span class="card-title">📋 最新订单</span>
        </template>
        <el-table :data="recentOrders" stripe size="small" max-height="320">
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="username" label="用户" width="80" />
          <el-table-column prop="merchantName" label="商家" width="100" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="85">
            <template #default="{ row }">
              <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small" disable-transitions>
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="金额" width="85">
            <template #default="{ row }">
              {{ formatMoney(row.totalAmount) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>
