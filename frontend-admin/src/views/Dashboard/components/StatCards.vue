<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getDashboardStats } from '@/apis/admin'
import { useAuthStore } from '@/stores/auth'
import StatCard from '@/components/StatCard.vue'
import { User, Shop, Document, Money } from '@element-plus/icons-vue'

interface Stats { totalUsers: number; totalMerchants: number; todayOrders: number; todayRevenue: number }

const stats = ref<Stats>({ totalUsers: 0, totalMerchants: 0, todayOrders: 0, todayRevenue: 0 })
const loading = ref(true)

const authStore = useAuthStore()

const today = computed(() => {
  const d = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <!-- 欢迎横幅 -->
  <div class="welcome-banner fade-in">
    <div class="welcome-text">
      <h3>👋 欢迎回来，{{ authStore.username }}</h3>
      <p>祝你工作愉快！以下是平台的运营数据概览</p>
    </div>
    <div class="welcome-date">
      <strong>{{ today }}</strong>
    </div>
  </div>

  <!-- 统计卡片 -->
  <el-row :gutter="20" v-loading="loading">
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="总用户数" :value="stats.totalUsers" color="#409EFF" :icon="User" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="总商家数" :value="stats.totalMerchants" color="#67C23A" :icon="Shop" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="今日订单" :value="stats.todayOrders" color="#E6A23C" :icon="Document" />
    </el-col>
    <el-col :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <StatCard title="今日营收" :value="'¥' + Number(stats.todayRevenue).toFixed(2)" color="#F56C6C" :icon="Money" />
    </el-col>
  </el-row>
</template>
