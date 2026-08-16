<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Component } from 'vue'
import { Document, Coin, User, Shop } from '@element-plus/icons-vue'
import type { DashboardStats } from '@/apis/admin'

interface KpiCard {
  title: string
  value: string
  change: number | null
  color: string
  icon: Component
  link: string
}

const props = defineProps<{ stats: DashboardStats }>()
const router = useRouter()

const formatMoney = (n: number) => '¥' + Number(n ?? 0).toFixed(2)

const cards = computed<KpiCard[]>(() => [
  { title: '今日订单', value: String(props.stats.todayOrders), change: props.stats.todayOrdersChange, color: '#409EFF', icon: Document, link: '/orders' },
  { title: '今日营业额', value: formatMoney(props.stats.todayRevenue), change: props.stats.todayRevenueChange, color: '#67C23A', icon: Coin, link: '/orders' },
  { title: '今日新增用户', value: String(props.stats.todayNewUsers), change: props.stats.todayNewUsersChange, color: '#E6A23C', icon: User, link: '/users' },
  { title: '待审核商家', value: String(props.stats.pendingApplications), change: null, color: '#F56C6C', icon: Shop, link: '/applications' },
])

const go = (link: string) => router.push(link)

const changeText = (c: number | null) => {
  if (c == null) return ''
  if (c > 0) return `↑ ${c.toFixed(0)}%`
  if (c < 0) return `↓ ${Math.abs(c).toFixed(0)}%`
  return '→ 持平'
}

const changeClass = (c: number | null) => {
  if (c == null) return 'change-none'
  if (c > 0) return 'change-up'
  if (c < 0) return 'change-down'
  return 'change-flat'
}
</script>

<template>
  <el-row :gutter="20">
    <el-col v-for="c in cards" :key="c.title" :xs="24" :sm="12" :lg="6" style="margin-bottom: 20px">
      <el-card shadow="hover" class="kpi-card" @click="go(c.link)">
        <div class="kpi-body">
          <div class="kpi-icon" :style="{ background: c.color + '18', color: c.color }">
            <el-icon :size="22"><component :is="c.icon" /></el-icon>
          </div>
          <div class="kpi-info">
            <div class="kpi-value">{{ c.value }}</div>
            <div class="kpi-title">{{ c.title }}</div>
          </div>
        </div>
        <div class="kpi-footer">
          <span class="kpi-change" :class="changeClass(c.change)">{{ changeText(c.change) }}</span>
          <span class="kpi-link">查看详情 →</span>
        </div>
        <div class="kpi-border" :style="{ background: c.color }" />
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.kpi-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--border-light);
}
.kpi-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}
.kpi-card :deep(.el-card__body) {
  padding: 20px 20px 16px;
}
.kpi-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.kpi-body {
  display: flex;
  align-items: center;
  gap: 14px;
}
.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kpi-info {
  flex: 1;
  min-width: 0;
}
.kpi-value {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kpi-title {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}
.kpi-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-light);
}
.kpi-change {
  font-size: 12px;
  font-weight: 600;
}
.change-up { color: #52C41A; }
.change-down { color: #F56C6C; }
.change-flat { color: #909399; }
.change-none { visibility: hidden; }
.kpi-link {
  font-size: 12px;
  color: var(--primary);
  font-weight: 500;
}
</style>
