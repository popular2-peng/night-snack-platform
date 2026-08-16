<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh, Coin, Document, Bell, Goods, TrendCharts, ArrowRight,
} from '@element-plus/icons-vue'
import { getMerchantDashboardApi, type MerchantDashboardVO } from '@/apis/dashboard'
import { getMerchantOrdersApi, type MerchantOrderListVO } from '@/apis/order'
import { updateShopStatusApi } from '@/apis/shop'

const router = useRouter()

const loading = ref(true)
const dashboard = ref<MerchantDashboardVO | null>(null)
const recentOrders = ref<MerchantOrderListVO[]>([])
const recentLoading = ref(false)
const statusChanging = ref(false)

/** 今日日期 yyyy-MM-dd */
const todayStr = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 状态展示元信息（标签 + 进度条颜色 + tag 类型） */
const STATUS_META: Record<string, { label: string; color: string; type: 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  PAID: { label: '待接单', color: '#E6A23C', type: 'warning' },
  ACCEPTED: { label: '已接单', color: '#409EFF', type: 'primary' },
  COOKING: { label: '制作中', color: '#F56C6C', type: 'danger' },
  COMPLETED: { label: '已完成', color: '#67C23A', type: 'success' },
  CANCELLED: { label: '已取消', color: '#909399', type: 'info' },
  PENDING_PAY: { label: '待支付', color: '#C0C4CC', type: 'info' },
  REFUNDING: { label: '退款中', color: '#F56C6C', type: 'danger' },
  REFUNDED: { label: '已退款', color: '#909399', type: 'info' },
}

/** 状态展示顺序（待处理优先） */
const STATUS_ORDER = ['PAID', 'ACCEPTED', 'COOKING', 'COMPLETED', 'CANCELLED', 'PENDING_PAY', 'REFUNDING', 'REFUNDED']

/** 今日订单状态分布（转成列表，含占比） */
const statusList = computed(() => {
  const stats = dashboard.value?.orderStatusStats || {}
  const total = dashboard.value?.todayOrders || 0
  return STATUS_ORDER.map((code) => ({
    code,
    label: STATUS_META[code]?.label || code,
    color: STATUS_META[code]?.color || '#909399',
    count: stats[code] || 0,
    percent: total > 0 ? Math.round(((stats[code] || 0) / total) * 100) : 0,
  }))
})

const isOpen = computed(() => dashboard.value?.shopStatus === 'OPEN')

const formatMoney = (n: number) => (n != null ? '¥' + Number(n).toFixed(2) : '¥0.00')
const formatTime = (s: string) => (s ? s.replace('T', ' ').substring(0, 16) : '-')
const shortNo = (no: string) => (no && no.length > 14 ? '…' + no.slice(-12) : no || '-')

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantDashboardApi()
    dashboard.value = res.data
  } catch { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

const loadRecentOrders = async () => {
  recentLoading.value = true
  try {
    const res = await getMerchantOrdersApi({ page: 1, size: 5, dateStart: todayStr(), dateEnd: todayStr() })
    recentOrders.value = res.data.list || []
  } catch { /* 忽略 */ }
  finally { recentLoading.value = false }
}

/** 切换营业状态（val 为 el-switch change 事件的新值：'OPEN' 或 'CLOSED'） */
const toggleStatus = async (val: string | number | boolean) => {
  if (!dashboard.value) return
  const target = val === 'OPEN' ? 'OPEN' : 'CLOSED'
  statusChanging.value = true
  try {
    await updateShopStatusApi(target)
    dashboard.value.shopStatus = target
    ElMessage.success(target === 'OPEN' ? '已开始营业' : '已打烊')
  } catch { /* 拦截器已提示 */ }
  finally { statusChanging.value = false }
}

const go = (path: string) => router.push(path)

onMounted(() => {
  load()
  loadRecentOrders()
})
</script>

<template>
  <div class="dashboard-page">
    <!-- ====== 顶部：店铺信息 + 营业状态 + 刷新 ====== -->
    <div class="page-card hero-card fade-in">
      <div class="hero-left">
        <div class="shop-logo">{{ dashboard?.shopName?.charAt(0) || '店' }}</div>
        <div class="hero-info">
          <h2>{{ dashboard?.shopName || '我的店铺' }}</h2>
          <span class="hero-date">{{ todayStr() }}</span>
        </div>
      </div>
      <div class="hero-right">
        <div class="status-switch">
          <span class="status-text" :class="isOpen ? 'text-open' : 'text-closed'">
            {{ isOpen ? '营业中' : '已打烊' }}
          </span>
          <el-switch
            :model-value="dashboard?.shopStatus ?? 'CLOSED'" :loading="statusChanging"
            active-value="OPEN" inactive-value="CLOSED"
            active-text="营业" inactive-text="打烊"
            @change="toggleStatus"
          />
        </div>
        <el-button :icon="Refresh" circle plain :loading="loading" @click="load(); loadRecentOrders()" title="刷新" />
      </div>
    </div>

    <!-- ====== 核心指标卡 ====== -->
    <div class="stat-grid">
      <div class="stat-card fade-in" @click="go('/orders/today')">
        <div class="stat-icon" style="background:#EEF3FD;color:#409EFF;"><el-icon><Document /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">今日订单</span>
          <span class="stat-value">{{ dashboard?.todayOrders ?? '-' }}</span>
        </div>
        <span class="stat-more">查看 <el-icon><ArrowRight /></el-icon></span>
      </div>

      <div class="stat-card fade-in" @click="go('/orders/today')">
        <div class="stat-icon" style="background:#FFF3E6;color:#F59A23;"><el-icon><Coin /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">今日营业额</span>
          <span class="stat-value">{{ dashboard ? formatMoney(dashboard.todayRevenue) : '-' }}</span>
        </div>
        <span class="stat-more">查看 <el-icon><ArrowRight /></el-icon></span>
      </div>

      <div class="stat-card fade-in" :class="{ alert: (dashboard?.pendingAccept || 0) > 0 }" @click="go('/orders/today')">
        <div class="stat-icon" style="background:#FDE8E8;color:#F56C6C;"><el-icon><Bell /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">待接单</span>
          <span class="stat-value">{{ dashboard?.pendingAccept ?? '-' }}</span>
        </div>
        <span class="stat-more">去处理 <el-icon><ArrowRight /></el-icon></span>
      </div>

      <div class="stat-card fade-in" @click="go('/products')">
        <div class="stat-icon" style="background:#F0FAF0;color:#67C23A;"><el-icon><Goods /></el-icon></div>
        <div class="stat-body">
          <span class="stat-label">在售商品</span>
          <span class="stat-value">{{ dashboard?.onSaleProducts ?? '-' }}</span>
        </div>
        <span class="stat-more">管理 <el-icon><ArrowRight /></el-icon></span>
      </div>
    </div>

    <!-- ====== 主体两栏：今日状态分布 + 最新订单 ====== -->
    <div class="main-grid">
      <!-- 今日订单状态分布 -->
      <div class="page-card panel fade-in" v-loading="loading">
        <div class="panel-head">
          <span class="panel-title"><el-icon><TrendCharts /></el-icon>今日订单状态分布</span>
        </div>
        <div v-if="(dashboard?.todayOrders || 0) > 0" class="status-list">
          <div v-for="item in statusList.filter(s => s.count > 0)" :key="item.code" class="status-row">
            <span class="status-label">{{ item.label }}</span>
            <div class="status-bar">
              <el-progress :percentage="item.percent" :stroke-width="14" :color="item.color" :show-text="false" />
            </div>
            <span class="status-count">{{ item.count }} 单</span>
          </div>
        </div>
        <el-empty v-else description="今日暂无订单，坐等开张" :image-size="80">
          <template #image><span style="font-size: 48px">🍜</span></template>
        </el-empty>
      </div>

      <!-- 今日最新订单 -->
      <div class="page-card panel fade-in" v-loading="recentLoading">
        <div class="panel-head">
          <span class="panel-title"><el-icon><Document /></el-icon>今日最新订单</span>
          <span class="panel-more" @click="go('/orders/today')">全部订单 <el-icon><ArrowRight /></el-icon></span>
        </div>
        <div v-if="recentOrders.length" class="order-list">
          <div v-for="row in recentOrders" :key="row.id" class="order-item" @click="go('/orders/today')">
            <div class="order-main">
              <span class="order-no">{{ shortNo(row.orderNo) }}</span>
              <span class="order-time">{{ formatTime(row.createdAt) }}</span>
            </div>
            <el-tag :type="STATUS_META[row.status]?.type || 'info'" size="small" effect="plain" round>
              {{ STATUS_META[row.status]?.label || row.status }}
            </el-tag>
            <span class="order-amount">{{ formatMoney(row.totalAmount) }}</span>
          </div>
        </div>
        <el-empty v-else description="今日暂无订单" :image-size="80">
          <template #image><span style="font-size: 48px">🍜</span></template>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard-page { max-width: 1100px; margin: 0 auto; }

/* ---- 顶部 hero ---- */
.hero-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px; margin-bottom: 16px;
  background: linear-gradient(135deg, #2A2016 0%, #4A3620 55%, #F59A23 170%);
  border: none;
  color: #fff;
}
.hero-left { display: flex; align-items: center; gap: 14px; }
.shop-logo {
  width: 52px; height: 52px; border-radius: 12px;
  background: rgba(255, 255, 255, 0.16);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; font-weight: 700;
}
.hero-info h2 { margin: 0 0 4px; font-size: 20px; font-weight: 700; }
.hero-date { font-size: 12px; color: rgba(255, 255, 255, 0.6); }
.hero-right { display: flex; align-items: center; gap: 14px; }
.status-switch { display: flex; align-items: center; gap: 10px; }
.status-text { font-size: 13px; font-weight: 600; }
.text-open { color: #52C41A; }
.text-closed { color: rgba(255, 255, 255, 0.5); }
:deep(.el-switch) { --el-switch-on-color: #F59A23; }

/* ---- 指标卡 ---- */
.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  margin-bottom: 16px;
}
.stat-card {
  position: relative;
  display: flex; align-items: center; gap: 14px;
  background: var(--bg-white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 18px 20px;
  cursor: pointer;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
  overflow: hidden;
}
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-card.alert { box-shadow: 0 0 0 1px rgba(245, 108, 108, 0.4), var(--shadow-sm); }
.stat-icon {
  width: 46px; height: 46px; border-radius: 12px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px;
}
.stat-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.stat-label { font-size: 12px; color: var(--text-secondary); }
.stat-value {
  font-size: 22px; font-weight: 700; color: var(--text-primary);
  font-variant-numeric: tabular-nums; white-space: nowrap;
}
.stat-card.alert .stat-value { color: var(--danger); }
.stat-more {
  position: absolute; right: 12px; bottom: 10px;
  font-size: 11px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 2px;
}

/* ---- 主体两栏 ---- */
.main-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px;
  align-items: start;
}
.panel { padding: 18px 20px; min-height: 260px; }
.panel-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.panel-title {
  font-size: 15px; font-weight: 600; color: var(--text-primary);
  display: flex; align-items: center; gap: 8px;
}
.panel-title .el-icon { color: var(--primary); }
.panel-more {
  font-size: 12px; color: var(--primary); cursor: pointer;
  display: flex; align-items: center; gap: 2px;
}

/* 状态分布 */
.status-list { display: flex; flex-direction: column; gap: 14px; }
.status-row { display: flex; align-items: center; gap: 12px; }
.status-label {
  width: 52px; flex-shrink: 0;
  font-size: 13px; color: var(--text-regular); text-align: right;
}
.status-bar { flex: 1; }
.status-count {
  width: 44px; flex-shrink: 0;
  font-size: 13px; color: var(--text-secondary); text-align: right;
  font-variant-numeric: tabular-nums;
}

/* 最新订单 */
.order-list { display: flex; flex-direction: column; gap: 10px; }
.order-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 10px;
  background: var(--bg);
  cursor: pointer;
  transition: background var(--transition-fast);
}
.order-item:hover { background: var(--primary-bg); }
.order-main { flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.order-no {
  font-size: 13px; font-weight: 600; color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.order-time { font-size: 11px; color: var(--text-secondary); }
.order-amount {
  font-size: 14px; font-weight: 700; color: var(--primary-dark);
  font-variant-numeric: tabular-nums; flex-shrink: 0;
}

@media (max-width: 860px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
}
</style>
