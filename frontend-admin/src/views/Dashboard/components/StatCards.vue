<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboardStats } from '@/apis/admin'
import StatCard from '@/components/StatCard.vue'

interface Stats { totalUsers: number; totalMerchants: number; todayOrders: number; todayRevenue: number }

const stats = ref<Stats>({ totalUsers: 0, totalMerchants: 0, todayOrders: 0, todayRevenue: 0 })

onMounted(async () => {
  const res = await getDashboardStats()
  stats.value = res.data
})
</script>

<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <StatCard title="总用户数" :value="stats.totalUsers" color="#409EFF" />
    </el-col>
    <el-col :span="6">
      <StatCard title="总商家数" :value="stats.totalMerchants" color="#67C23A" />
    </el-col>
    <el-col :span="6">
      <StatCard title="今日订单" :value="stats.todayOrders" color="#E6A23C" />
    </el-col>
    <el-col :span="6">
      <StatCard title="今日营收" :value="'¥' + Number(stats.todayRevenue).toFixed(2)" color="#F56C6C" />
    </el-col>
  </el-row>
</template>
