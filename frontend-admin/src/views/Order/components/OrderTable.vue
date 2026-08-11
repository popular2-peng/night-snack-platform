<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listAdminOrders, getAdminOrderDetail, getDashboardStats, getOrderStatusDistribution,
  batchDeleteOrders,
} from '@/apis/admin'
import { ElMessage } from 'element-plus'
import {
  ShoppingBag, Coin, Timer, Van, User, Document,
} from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import StatsStrip from '@/components/StatsStrip.vue'

// ---- 类型 ----
interface OrderItem {
  id: number; orderNo: string; username: string; merchantName: string
  status: string; totalAmount: number; payChannel: string; createdAt: string
}
interface OrderDetail {
  id: number; orderNo: string; userId: number; merchantId: number
  merchantName: string; status: string; totalAmount: number
  addressSnapshot: string; payChannel: string; payTime: string
  items: DetailItem[]; statusLogs: StatusLog[]; createdAt: string
}
interface DetailItem {
  productId: number; specId: number; nameSnapshot: string
  priceSnapshot: number; quantity: number; imageUrl: string
}
interface StatusLog { fromStatus: string; toStatus: string; operator: string; reason: string; createdAt: string }

// ---- 状态映射 ----
const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  PENDING_PAY: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'primary' },
  ACCEPTED: { label: '已接单', type: 'info' },
  COOKING: { label: '制作中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  REFUNDING: { label: '退款中', type: 'danger' },
  REFUNDED: { label: '已退款', type: 'info' },
}
const payChannelMap: Record<string, string> = {
  ALIPAY: '支付宝', WECHAT_MOCK: '微信', WECHAT: '微信',
}

// ---- 数据 ----
const orders = ref<OrderItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const payChannelFilter = ref('')
const dateRange = ref<string[]>(['', ''])
const loading = ref(false)

// 统计条
const stats = ref({ total: 0, pending: 0, paid: 0, cooking: 0, completed: 0 })

// 详情抽屉
const detailVisible = ref(false)
const detail = ref<OrderDetail | null>(null)
const detailLoading = ref(false)

// ---- 批量删除 ----
const selectedIds = ref<number[]>([])
const batchLoading = ref(false)

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选订单'); return }
  batchLoading.value = true
  try {
    const res = await batchDeleteOrders([...selectedIds.value])
    ElMessage.success(res.data.message || '批量删除完成')
    selectedIds.value = []
    fetchOrders()
    fetchStats()
  } catch { /* 拦截器已提示 */ }
  finally { batchLoading.value = false }
}

const statusOptions = [
  { label: '待支付', value: 'PENDING_PAY' },
  { label: '已支付', value: 'PAID' },
  { label: '已接单', value: 'ACCEPTED' },
  { label: '制作中', value: 'COOKING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
]

const payChannelOptions = [
  { label: '支付宝', value: 'ALIPAY' },
  { label: '微信', value: 'WECHAT_MOCK' },
]

// ---- 数据获取 ----
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (payChannelFilter.value) params.payChannel = payChannelFilter.value
    if (dateRange.value?.[0]) params.dateStart = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1]
    const res = await listAdminOrders(params)
    orders.value = res.data.list || []
    total.value = res.data.total || 0
  } catch { ElMessage.error('获取订单列表失败') }
  finally { loading.value = false }
}

const fetchStats = async () => {
  try {
    const [dashboardRes, distRes] = await Promise.all([
      getDashboardStats(), getOrderStatusDistribution(),
    ])
    stats.value.total = dashboardRes.data.totalOrders || 0
    const dist = distRes.data || []
    const countOf = (s: string) => dist.find((i: any) => i.status === s)?.count || 0
    stats.value.pending = countOf('PENDING_PAY')
    stats.value.paid = countOf('PAID') + countOf('ACCEPTED')
    stats.value.cooking = countOf('COOKING')
    stats.value.completed = countOf('COMPLETED')
  } catch { /* 统计失败不阻塞页面 */ }
}

const handleSearch = () => { page.value = 1; fetchOrders() }
const handleReset = () => { payChannelFilter.value = ''; page.value = 1; fetchOrders() }
const handleRefresh = () => { fetchOrders(); fetchStats() }
const handleSizeChange = (val: number) => { size.value = val; fetchOrders() }
const handlePageChange = (val: number) => { page.value = val; fetchOrders() }

// ---- 格式化 ----
const formatMoney = (n: number) => { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '-' }

// ---- 详情抽屉 ----
const openDetail = async (row: OrderItem) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getAdminOrderDetail(row.id)
    detail.value = res.data
  } catch { ElMessage.error('获取订单详情失败') }
  finally { detailLoading.value = false }
}

// 地址快照 JSON → 展示对象
const addressInfo = () => {
  if (!detail.value?.addressSnapshot) return null
  try { return JSON.parse(detail.value.addressSnapshot) } catch { return null }
}

// 商品件数合计
const totalQuantity = () => {
  return detail.value?.items?.reduce((sum, i) => sum + (i.quantity || 0), 0) || 0
}

// 状态日志文案（operator 补充中文说明）
const operatorText = (o: string) => {
  const map: Record<string, string> = { USER: '用户', MERCHANT: '商家', ADMIN: '管理员', SYSTEM: '系统' }
  return map[o] || o
}

onMounted(() => { fetchOrders(); fetchStats() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条（公共组件） ====== -->
    <StatsStrip :items="[
      { value: stats.total, label: '订单总数' },
      { value: stats.pending, label: '待支付', color: 'warning' },
      { value: stats.paid, label: '已支付/接单', color: 'primary' },
      { value: stats.cooking, label: '制作中', color: 'info' },
      { value: stats.completed, label: '已完成', color: 'success' },
    ]" />

    <!-- ====== 搜索 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:pay-channel-value="payChannelFilter"
      v-model:date-range="dateRange"
      placeholder="搜索订单号/用户名/商家名"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      :pay-channel-options="payChannelOptions"
      pay-channel-placeholder="支付方式"
      date-label="下单"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="handleRefresh"
    />

    <!-- ====== 批量操作栏 ====== -->
    <div v-if="selectedIds.length" class="batch-bar">
      <span class="batch-count">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" :loading="batchLoading" @click="handleBatchDelete">批量删除</el-button>
      <el-button link @click="selectedIds = []">取消选择</el-button>
    </div>

    <!-- ====== 表格 ====== -->
    <el-table :data="orders" stripe v-loading="loading" style="width: 100%"
      @selection-change="(rows: OrderItem[]) => selectedIds = rows.map(r => r.id)">
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column type="index" label="编号" width="60" align="center" />

      <!-- 订单号 -->
      <el-table-column label="订单号" min-width="170" align="left">
        <template #default="{ row }">
          <span class="order-no" @click="openDetail(row)">{{ row.orderNo }}</span>
        </template>
      </el-table-column>

      <!-- 下单用户 -->
      <el-table-column label="下单用户" min-width="120" align="left" show-overflow-tooltip>
        <template #default="{ row }">{{ row.username || '-' }}</template>
      </el-table-column>

      <!-- 商家 -->
      <el-table-column prop="merchantName" label="商家" min-width="140" align="left" show-overflow-tooltip />

      <!-- 金额 -->
      <el-table-column label="金额" width="110" align="center">
        <template #default="{ row }">
          <span class="amount-text">{{ formatMoney(row.totalAmount) }}</span>
        </template>
      </el-table-column>

      <!-- 支付渠道 -->
      <el-table-column label="支付方式" width="100" align="center">
        <template #default="{ row }">
          {{ payChannelMap[row.payChannel] || row.payChannel || '-' }}
        </template>
      </el-table-column>

      <!-- 状态 -->
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'" size="small" effect="plain">
            {{ statusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 下单时间 -->
      <el-table-column label="下单时间" min-width="165" align="center" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>

      <!-- 操作 -->
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" background
        @size-change="handleSizeChange" @current-change="handlePageChange"
      />
    </div>

    <!-- ====== 详情 Drawer（与用户/商家详情同风格） ====== -->
    <el-drawer v-model="detailVisible" title="订单详情" size="520px" direction="rtl">
      <template v-if="detail" v-loading="detailLoading">
        <!-- 头部卡片 -->
        <div class="drawer-hero">
          <div class="hero-bg"></div>
          <div class="hero-content">
            <div class="hero-logo order-logo">
              <el-icon :size="30"><Document /></el-icon>
            </div>
            <div class="hero-info">
              <div class="hero-name-row">
                <h2>{{ detail.orderNo }}</h2>
                <el-tag :type="statusMap[detail.status]?.type || 'info'" size="small" effect="dark" round>
                  {{ statusMap[detail.status]?.label || detail.status }}
                </el-tag>
              </div>
              <p class="hero-sub">{{ detail.merchantName }} · {{ formatTime(detail.createdAt) }}</p>
            </div>
          </div>
        </div>

        <!-- 数据卡片 -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-icon" style="background:#EEF4FF;color:#5B8DEF"><el-icon><Coin /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num">{{ formatMoney(detail.totalAmount) }}</span>
              <span class="stat-label">订单金额</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#F0FAF0;color:#67C23A"><el-icon><ShoppingBag /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num">{{ totalQuantity() }}</span>
              <span class="stat-label">商品件数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#FFF8ED;color:#E6A23C"><el-icon><Timer /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num stat-num-sm">{{ payChannelMap[detail.payChannel] || '-' }}</span>
              <span class="stat-label">支付方式</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#FDF2F2;color:#F56C6C"><el-icon><User /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num stat-num-sm">{{ formatTime(detail.payTime) }}</span>
              <span class="stat-label">支付时间</span>
            </div>
          </div>
        </div>

        <!-- 商品明细 -->
        <div class="section">
          <div class="section-title">商品明细</div>
          <div class="item-list">
            <div v-for="(it, i) in detail.items" :key="i" class="item-row">
              <el-image v-if="it.imageUrl" :src="it.imageUrl" fit="cover" class="item-img"
                :preview-src-list="[it.imageUrl]" :preview-teleported="true" />
              <div v-else class="item-img item-img-placeholder">🍜</div>
              <div class="item-info">
                <span class="item-name">{{ it.nameSnapshot }}</span>
                <span class="item-price">{{ formatMoney(it.priceSnapshot) }} × {{ it.quantity }}</span>
              </div>
              <span class="item-subtotal">{{ formatMoney((it.priceSnapshot || 0) * (it.quantity || 0)) }}</span>
            </div>
          </div>
          <div class="item-total">
            <span>合计</span>
            <span class="amount-text">{{ formatMoney(detail.totalAmount) }}</span>
          </div>
        </div>

        <!-- 收货信息 -->
        <div v-if="addressInfo()" class="section">
          <div class="section-title">收货信息</div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label"><el-icon><User /></el-icon>收货人</span>
              <span class="info-value">{{ addressInfo().receiver }}</span>
            </div>
            <div class="info-row">
              <span class="info-label"><el-icon><Van /></el-icon>电话</span>
              <span class="info-value">{{ addressInfo().phone }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">地址</span>
              <span class="info-value">{{ addressInfo().detail }}</span>
            </div>
          </div>
        </div>

        <!-- 状态流转 -->
        <div v-if="detail.statusLogs?.length" class="section">
          <div class="section-title">状态流转</div>
          <div class="timeline-wrap">
            <el-timeline>
              <el-timeline-item
                v-for="(l, i) in [...detail.statusLogs].reverse()" :key="i"
                :timestamp="formatTime(l.createdAt)"
                placement="top"
                :color="i === 0 ? '#5B8DEF' : '#C0C4CC'"
                :hollow="i !== 0"
              >
                <div class="timeline-card">
                  <span class="timeline-action">
                    {{ l.fromStatus ? (statusMap[l.fromStatus]?.label || l.fromStatus) + ' → ' : '' }}
                    {{ statusMap[l.toStatus]?.label || l.toStatus }}
                  </span>
                  <p v-if="l.reason" class="timeline-detail">{{ operatorText(l.operator) }}：{{ l.reason }}</p>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
/* ---- 批量操作栏 ---- */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #EEF4FF;
  border-radius: 8px;
  flex-wrap: wrap;
}
.batch-count {
  font-size: 13px;
  font-weight: 600;
  color: #5B8DEF;
  margin-right: 4px;
}

/* ---- 订单号链接样式 ---- */
.order-no {
  color: var(--primary);
  cursor: pointer;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.order-no:hover { text-decoration: underline; }
.amount-text {
  color: var(--text-primary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

/* ---- 头部卡片 ---- */
.drawer-hero {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #5B8DEF 0%, #7FA9F5 40%, #A3C4FA 100%);
}
.hero-content {
  position: relative;
  display: flex; align-items: center; gap: 16px;
  padding: 24px 20px;
}
.hero-logo {
  width: 72px; height: 72px; border-radius: 12px;
  border: 3px solid rgba(255,255,255,.4);
  flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.order-logo {
  background: rgba(255,255,255,.25);
  color: #fff;
}
.hero-info { flex: 1; min-width: 0; }
.hero-name-row {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 6px;
}
.hero-name-row h2 {
  margin: 0; font-size: 18px; font-weight: 700; color: #fff;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hero-sub {
  margin: 0; font-size: 13px; color: rgba(255,255,255,.8);
}

/* ---- 数据卡片 ---- */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px;
  margin-bottom: 20px;
}
.stat-card {
  background: var(--bg, #f8f9fc);
  border-radius: 10px; padding: 14px 10px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  text-align: center;
  transition: transform .15s, box-shadow .15s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,.06)); }
.stat-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
}
.stat-body { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 17px; font-weight: 700; color: var(--text-primary, #303133); line-height: 1.3; font-variant-numeric: tabular-nums; }
.stat-num-sm { font-size: 14px; }
.stat-label { font-size: 11px; color: var(--text-secondary, #909399); margin-top: 2px; }

/* ---- 分区 ---- */
.section { margin-bottom: 20px; }
.section-title {
  font-size: 14px; font-weight: 600; color: var(--text-primary, #303133);
  margin-bottom: 12px; padding-left: 12px;
  border-left: 3px solid #5B8DEF;
  display: flex; align-items: center; gap: 6px;
}

/* ---- 商品明细 ---- */
.item-list {
  background: var(--bg, #f8f9fc);
  border-radius: 10px; padding: 8px 16px;
}
.item-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-color, #EBEEF5);
}
.item-row:last-child { border-bottom: none; }
.item-img {
  width: 44px; height: 44px; border-radius: 8px;
  flex-shrink: 0;
}
.item-img-placeholder {
  background: #EEF3FD; color: #5B8DEF;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}
.item-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.item-name {
  font-size: 13px; font-weight: 600; color: var(--text-primary, #303133);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.item-price { font-size: 12px; color: var(--text-secondary, #909399); }
.item-subtotal {
  font-size: 13px; font-weight: 600; color: var(--text-primary, #303133);
  font-variant-numeric: tabular-nums;
}
.item-total {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 4px 0;
  font-size: 13px; color: var(--text-secondary, #909399);
}

/* ---- 信息行 ---- */
.info-list {
  background: var(--bg, #f8f9fc);
  border-radius: 10px; padding: 4px 16px;
}
.info-row {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #EBEEF5);
}
.info-row:last-child { border-bottom: none; }
.info-label {
  flex-shrink: 0; width: 80px;
  font-size: 13px; color: var(--text-secondary, #909399);
  display: flex; align-items: center; gap: 4px;
}
.info-value { flex: 1; font-size: 13px; color: var(--text-primary, #303133); word-break: break-all; }

/* ---- 时间线 ---- */
.timeline-wrap { padding: 0 4px; }
.timeline-card { margin-top: -4px; }
.timeline-action {
  font-size: 14px; font-weight: 600; color: var(--text-primary, #303133);
}
.timeline-detail {
  margin: 4px 0 0 0; font-size: 12px; color: var(--text-secondary, #909399);
  line-height: 1.5;
}
:deep(.el-timeline-item__timestamp) {
  font-size: 12px; color: var(--text-secondary, #909399);
}
:deep(.el-timeline-item__node) { transition: all .3s; }

@media (max-width: 480px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .hero-content { flex-direction: column; text-align: center; }
  .hero-name-row { justify-content: center; }
}
</style>
