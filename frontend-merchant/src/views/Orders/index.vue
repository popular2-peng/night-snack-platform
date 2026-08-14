<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  Refresh, User, Phone, Location, Clock, Document, Coin, Van,
} from '@element-plus/icons-vue'
import {
  getMerchantOrdersApi, getMerchantOrderStatsApi, getMerchantOrderDetailApi,
  acceptOrderApi, startCookingApi, completeOrderApi, rejectOrderApi,
  type MerchantOrderListVO, type MerchantOrderDetailVO,
} from '@/apis/order'

// ---- 状态定义（美团风格：按处理阶段分 tab） ----
const TABS = [
  { key: '', label: '全部' },
  { key: 'PAID', label: '待接单' },
  { key: 'ACCEPTED', label: '已接单' },
  { key: 'COOKING', label: '制作中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' },
]

const statusMap: Record<string, { label: string; type: 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  PENDING_PAY: { label: '待支付', type: 'info' },
  PAID: { label: '待接单', type: 'warning' },
  ACCEPTED: { label: '已接单', type: 'primary' },
  COOKING: { label: '制作中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  REFUNDING: { label: '退款中', type: 'danger' },
  REFUNDED: { label: '已退款', type: 'info' },
}

// ---- 数据 ----
const activeTab = ref('')
const orders = ref<MerchantOrderListVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const stats = ref<Record<string, number>>({})

/** 上一轮待接单数量（新单通知用） */
let prevPaidCount: number | null = null

const fetchOrders = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const res = await getMerchantOrdersApi({
      page: page.value, size: size.value,
      status: activeTab.value || undefined,
    })
    orders.value = res.data.list || []
    total.value = res.data.total || 0
  } catch { if (!silent) ElMessage.error('获取订单列表失败') }
  finally { if (!silent) loading.value = false }
}

const fetchStats = async () => {
  try {
    const res = await getMerchantOrderStatsApi()
    stats.value = res.data || {}
    // 新订单通知：待接单数比上一轮多 → 提示商家
    const paid = stats.value.PAID || 0
    if (prevPaidCount !== null && paid > prevPaidCount) {
      ElNotification({
        title: '您有新的订单',
        message: `新增 ${paid - prevPaidCount} 单待接单，请及时处理`,
        type: 'warning',
        position: 'bottom-right',
        duration: 6000,
      })
    }
    prevPaidCount = paid
  } catch { /* 统计失败不阻塞 */ }
}

/** 15 秒轮询（美团商家端同为轮询模式） */
let pollTimer: number | null = null
const POLL_INTERVAL = 15_000

const startPolling = () => {
  stopPolling()
  pollTimer = window.setInterval(() => {
    fetchOrders(true)
    fetchStats()
  }, POLL_INTERVAL)
}
const stopPolling = () => {
  if (pollTimer !== null) { clearInterval(pollTimer); pollTimer = null }
}

// ---- tab 切换 ----
const switchTab = (key: string) => {
  activeTab.value = key
  page.value = 1
  fetchOrders()
}

// ---- 操作 ----
const actionLoading = ref<Record<string, boolean>>({})
const setActionLoading = (id: number, key: string, val: boolean) => {
  actionLoading.value[`${id}-${key}`] = val
}
const isActionLoading = (id: number, key: string) => !!actionLoading.value[`${id}-${key}`]

const refreshAll = () => { fetchOrders(); fetchStats() }

/** 接单 */
const handleAccept = async (row: MerchantOrderListVO) => {
  setActionLoading(row.id, 'accept', true)
  try {
    await acceptOrderApi(row.id)
    ElMessage.success(`订单 ${shortNo(row.orderNo)} 已接单，开始备餐吧`)
    refreshAll()
  } catch { /* 拦截器已提示 */ }
  finally { setActionLoading(row.id, 'accept', false) }
}

/** 拒单（填写原因，触发自动退款） */
const handleReject = async (row: MerchantOrderListVO) => {
  const { value: reason } = await ElMessageBox.prompt(
    `拒单后订单将自动取消并原路退款给用户，请填写原因（将展示给用户）：`,
    '拒单', {
      confirmButtonText: '确认拒单',
      cancelButtonText: '再想想',
      inputType: 'textarea',
      inputPlaceholder: '如：食材售罄 / 超出配送范围',
      inputValidator: (v: string) => (v && v.trim().length >= 2) || '请填写拒单原因（至少 2 个字）',
    })
  if (!reason) return
  setActionLoading(row.id, 'reject', true)
  try {
    await rejectOrderApi(row.id, reason.trim())
    ElMessage.success('已拒单，款项将自动原路退回')
    refreshAll()
  } catch { /* 拦截器已提示 */ }
  finally { setActionLoading(row.id, 'reject', false) }
}

/** 开始制作 */
const handleCooking = async (row: MerchantOrderListVO) => {
  setActionLoading(row.id, 'cooking', true)
  try {
    await startCookingApi(row.id)
    ElMessage.success('已开始制作')
    refreshAll()
  } catch { /* 拦截器已提示 */ }
  finally { setActionLoading(row.id, 'cooking', false) }
}

/** 完成出餐（自配送，超 30 分钟系统也会自动完成） */
const handleComplete = async (row: MerchantOrderListVO) => {
  await ElMessageBox.confirm(
    '确认已完成出餐并送达？商家自配送模式下订单将标记为已完成。',
    '完成出餐', { type: 'success', confirmButtonText: '确认完成' })
  setActionLoading(row.id, 'complete', true)
  try {
    await completeOrderApi(row.id)
    ElMessage.success('订单已完成')
    refreshAll()
  } catch { /* 拦截器已提示 */ }
  finally { setActionLoading(row.id, 'complete', false) }
}

// ---- 详情抽屉 ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MerchantOrderDetailVO | null>(null)

const openDetail = async (row: MerchantOrderListVO) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getMerchantOrderDetailApi(row.id)
    detail.value = res.data
  } catch { ElMessage.error('获取订单详情失败') }
  finally { detailLoading.value = false }
}

// ---- 格式化 ----
const formatMoney = (n: number) => (n != null ? '¥' + Number(n).toFixed(2) : '¥0.00')
const formatTime = (s: string) => (s ? s.replace('T', ' ').substring(0, 16) : '-')
const shortNo = (no: string) => (no && no.length > 14 ? '…' + no.slice(-12) : no || '-')

const parseAddress = (snapshot?: string) => {
  if (!snapshot) return null
  try { return JSON.parse(snapshot) as { receiver?: string; phone?: string; detail?: string } } catch { return null }
}

const itemCount = (row: MerchantOrderListVO) =>
  (row.items || []).reduce((sum, i) => sum + (i.quantity || 0), 0)

const operatorText = (o: string) => {
  const map: Record<string, string> = { USER: '用户', MERCHANT: '商家', ADMIN: '管理员', SYSTEM: '系统' }
  return map[o] || o
}

onMounted(() => { fetchOrders(); fetchStats(); startPolling() })
onUnmounted(stopPolling)
</script>

<template>
  <div class="orders-page">
    <!-- ====== 顶栏 ====== -->
    <div class="page-card top-bar fade-in">
      <div class="top-title">
        <h2><el-icon><Document /></el-icon>订单管理</h2>
        <span class="poll-hint"><el-icon class="spin-icon"><Refresh /></el-icon> 每 15 秒自动刷新</span>
      </div>
      <el-button :icon="Refresh" circle plain @click="refreshAll" title="立即刷新" />
    </div>

    <!-- ====== 状态 Tab（美团风：下划线 + 角标） ====== -->
    <div class="tab-bar fade-in">
      <div
        v-for="tab in TABS" :key="tab.key"
        class="tab-item" :class="{ active: activeTab === tab.key, alert: tab.key === 'PAID' && (stats.PAID || 0) > 0 }"
        @click="switchTab(tab.key)"
      >
        <span class="tab-label">{{ tab.label }}</span>
        <span v-if="tab.key && (stats[tab.key] || 0) > 0" class="tab-badge"
          :class="{ 'badge-alert': tab.key === 'PAID' }">{{ stats[tab.key] }}</span>
      </div>
    </div>

    <!-- ====== 订单卡片列表 ====== -->
    <div class="order-list" v-loading="loading">
      <template v-if="orders.length">
        <div v-for="row in orders" :key="row.id" class="order-card fade-in" @click="openDetail(row)">
          <!-- 卡片头：订单号 + 时间 + 状态 -->
          <div class="card-head">
            <div class="head-left">
              <span class="order-no">{{ shortNo(row.orderNo) }}</span>
              <span class="order-time">{{ formatTime(row.createdAt) }}</span>
            </div>
            <el-tag :type="statusMap[row.status]?.type || 'info'" effect="dark" round size="small">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </div>

          <!-- 收货信息 -->
          <div class="addr-line" v-if="parseAddress(row.addressSnapshot)">
            <el-icon><Location /></el-icon>
            <span class="addr-receiver">{{ parseAddress(row.addressSnapshot)?.receiver }}</span>
            <span class="addr-phone">{{ parseAddress(row.addressSnapshot)?.phone }}</span>
            <span class="addr-detail">{{ parseAddress(row.addressSnapshot)?.detail }}</span>
          </div>
          <div class="divider" />

          <!-- 商品明细 -->
          <div class="items">
            <div v-for="(it, i) in row.items" :key="i" class="item-row">
              <span class="item-name">{{ it.nameSnapshot }}</span>
              <span class="item-qty">×{{ it.quantity }}</span>
              <span class="item-subtotal">{{ formatMoney((it.priceSnapshot || 0) * (it.quantity || 0)) }}</span>
            </div>
          </div>

          <!-- 卡片底：合计 + 操作 -->
          <div class="card-foot" @click.stop>
            <div class="foot-total">
              <span class="total-label">共 {{ itemCount(row) }} 件，合计</span>
              <span class="total-amount">{{ formatMoney(row.totalAmount) }}</span>
            </div>
            <div class="foot-actions">
              <el-button size="small" plain @click="openDetail(row)">详情</el-button>
              <template v-if="row.status === 'PAID'">
                <el-button size="small" type="danger" plain
                  :loading="isActionLoading(row.id, 'reject')" @click="handleReject(row)">拒单</el-button>
                <el-button size="small" type="primary"
                  :loading="isActionLoading(row.id, 'accept')" @click="handleAccept(row)">接单</el-button>
              </template>
              <template v-else-if="row.status === 'ACCEPTED'">
                <el-button size="small" type="warning"
                  :loading="isActionLoading(row.id, 'cooking')" @click="handleCooking(row)">开始制作</el-button>
              </template>
              <template v-else-if="row.status === 'COOKING'">
                <el-button size="small" type="success"
                  :loading="isActionLoading(row.id, 'complete')" @click="handleComplete(row)">完成出餐</el-button>
              </template>
            </div>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading" :description="activeTab ? '该状态下暂无订单' : '暂无订单，坐等开张'">
        <template #image>
          <span style="font-size: 56px">🍜</span>
        </template>
      </el-empty>
    </div>

    <!-- ====== 分页 ====== -->
    <div v-if="total > size" class="pagination-wrap">
      <el-pagination
        v-model:current-page="page" :page-size="size" :total="total"
        layout="total, prev, pager, next" background
        @current-change="() => fetchOrders()"
      />
    </div>

    <!-- ====== 详情抽屉 ====== -->
    <el-drawer v-model="detailVisible" title="订单详情" size="520px" direction="rtl">
      <template v-if="detail" v-loading="detailLoading">
        <!-- 头部卡片 -->
        <div class="drawer-hero">
          <div class="hero-bg"></div>
          <div class="hero-content">
            <div class="hero-info">
              <div class="hero-name-row">
                <h2>{{ detail.orderNo }}</h2>
                <el-tag :type="statusMap[detail.status]?.type || 'info'" size="small" effect="dark" round>
                  {{ statusMap[detail.status]?.label || detail.status }}
                </el-tag>
              </div>
              <p class="hero-sub">{{ formatTime(detail.createdAt) }} 下单</p>
            </div>
          </div>
        </div>

        <!-- 收货信息 -->
        <div v-if="parseAddress(detail.addressSnapshot)" class="section">
          <div class="section-title">收货信息</div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label"><el-icon><User /></el-icon>收货人</span>
              <span class="info-value">{{ parseAddress(detail.addressSnapshot)?.receiver }}</span>
            </div>
            <div class="info-row">
              <span class="info-label"><el-icon><Phone /></el-icon>电话</span>
              <span class="info-value">{{ parseAddress(detail.addressSnapshot)?.phone }}</span>
            </div>
            <div class="info-row">
              <span class="info-label"><el-icon><Location /></el-icon>地址</span>
              <span class="info-value">{{ parseAddress(detail.addressSnapshot)?.detail }}</span>
            </div>
          </div>
        </div>

        <!-- 商品明细 -->
        <div class="section">
          <div class="section-title">商品明细</div>
          <div class="item-list">
            <div v-for="(it, i) in detail.items" :key="i" class="item-row">
              <span class="item-name">{{ it.nameSnapshot }}</span>
              <span class="item-qty">×{{ it.quantity }}</span>
              <span class="item-subtotal">{{ formatMoney((it.priceSnapshot || 0) * (it.quantity || 0)) }}</span>
            </div>
          </div>
          <div class="item-total">
            <span><el-icon><Coin /></el-icon> 合计</span>
            <span class="amount-text">{{ formatMoney(detail.totalAmount) }}</span>
          </div>
        </div>

        <!-- 支付信息 -->
        <div class="section">
          <div class="section-title">支付信息</div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label"><el-icon><Van /></el-icon>支付方式</span>
              <span class="info-value">{{ detail.payChannel === 'ALIPAY' ? '支付宝' : detail.payChannel === 'WECHAT_MOCK' ? '微信' : (detail.payChannel || '-') }}</span>
            </div>
            <div class="info-row">
              <span class="info-label"><el-icon><Clock /></el-icon>支付时间</span>
              <span class="info-value">{{ formatTime(detail.payTime) }}</span>
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
                :timestamp="formatTime(l.createdAt)" placement="top"
                :color="i === 0 ? '#F59A23' : '#C0C4CC'" :hollow="i !== 0"
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
.orders-page { max-width: 1100px; margin: 0 auto; }

/* ---- 顶栏 ---- */
.top-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; margin-bottom: 12px;
}
.top-title { display: flex; align-items: baseline; gap: 12px; }
.top-title h2 {
  margin: 0; font-size: 18px; font-weight: 600;
  display: flex; align-items: center; gap: 8px;
}
.top-title h2 .el-icon { color: var(--primary); }
.poll-hint {
  font-size: 12px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 4px;
}
.spin-icon { animation: spin 3s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ---- Tab 栏（美团风） ---- */
.tab-bar {
  display: flex; gap: 4px;
  background: var(--bg-white);
  border-radius: var(--border-radius-lg);
  padding: 0 12px; margin-bottom: 14px;
  box-shadow: var(--shadow-sm);
  overflow-x: auto;
}
.tab-item {
  position: relative;
  padding: 14px 16px 12px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-regular);
  white-space: nowrap;
  display: flex; align-items: center; gap: 6px;
  transition: color var(--transition-fast);
  border-bottom: 3px solid transparent;
}
.tab-item:hover { color: var(--primary); }
.tab-item.active {
  color: var(--primary);
  font-weight: 600;
  border-bottom-color: var(--primary);
}
.tab-badge {
  min-width: 18px; height: 18px; padding: 0 5px;
  border-radius: 9px;
  background: var(--bg);
  color: var(--text-secondary);
  font-size: 11px; line-height: 18px;
  text-align: center;
}
.tab-item.active .tab-badge { background: var(--primary-bg); color: var(--primary-dark); }
.tab-badge.badge-alert {
  background: var(--danger);
  color: #fff;
  animation: badge-pulse 2s ease-in-out infinite;
}
@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

/* ---- 订单卡片 ---- */
.order-list { min-height: 320px; }
.order-card {
  background: var(--bg-white);
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 16px 20px;
  margin-bottom: 14px;
  cursor: pointer;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}
.order-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }

.card-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 10px;
}
.head-left { display: flex; align-items: baseline; gap: 12px; }
.order-no {
  font-size: 15px; font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}
.order-time { font-size: 12px; color: var(--text-secondary); }

.addr-line {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--text-regular);
  padding: 8px 10px;
  background: var(--bg);
  border-radius: 8px;
}
.addr-line .el-icon { color: var(--primary); flex-shrink: 0; }
.addr-receiver { font-weight: 600; }
.addr-phone { color: var(--text-secondary); }
.addr-detail {
  color: var(--text-secondary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.divider { height: 1px; background: var(--border-light); margin: 12px 0; }

.items { display: flex; flex-direction: column; gap: 6px; }
.item-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.item-name {
  flex: 1; color: var(--text-primary);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.item-qty { color: var(--text-secondary); min-width: 36px; text-align: right; }
.item-subtotal { color: var(--text-regular); min-width: 76px; text-align: right; font-variant-numeric: tabular-nums; }

.card-foot {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 14px; padding-top: 12px;
  border-top: 1px dashed var(--border-color);
}
.foot-total { display: flex; align-items: baseline; gap: 8px; }
.total-label { font-size: 12px; color: var(--text-secondary); }
.total-amount {
  font-size: 20px; font-weight: 700; color: var(--primary-dark);
  font-variant-numeric: tabular-nums;
}
.foot-actions { display: flex; gap: 8px; }

/* ---- 分页 ---- */
.pagination-wrap {
  display: flex; justify-content: center;
  padding: 10px 0 20px;
}

/* ---- 详情抽屉 ---- */
.drawer-hero {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #2A2016 0%, #4A3620 60%, #F59A23 160%);
}
.hero-content { position: relative; padding: 24px 20px; }
.hero-info { flex: 1; min-width: 0; }
.hero-name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.hero-name-row h2 {
  margin: 0; font-size: 16px; font-weight: 700; color: #fff;
  font-variant-numeric: tabular-nums;
}
.hero-sub { margin: 0; font-size: 13px; color: rgba(255, 255, 255, 0.7); }

.section { margin-bottom: 20px; }
.section-title {
  font-size: 14px; font-weight: 600; color: var(--text-primary);
  margin-bottom: 12px; padding-left: 12px;
  border-left: 3px solid var(--primary);
  display: flex; align-items: center; gap: 6px;
}
.info-list {
  background: var(--bg);
  border-radius: 10px; padding: 4px 16px;
}
.info-row {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}
.info-row:last-child { border-bottom: none; }
.info-label {
  flex-shrink: 0; width: 76px;
  font-size: 13px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 4px;
}
.info-value { flex: 1; font-size: 13px; color: var(--text-primary); word-break: break-all; }

.item-list {
  background: var(--bg);
  border-radius: 10px; padding: 10px 16px;
}
.item-total {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 4px 0;
  font-size: 13px; color: var(--text-secondary);
}
.amount-text { font-size: 18px; font-weight: 700; color: var(--primary-dark); font-variant-numeric: tabular-nums; }

.timeline-wrap { padding: 0 4px; }
.timeline-card { margin-top: -4px; }
.timeline-action { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.timeline-detail { margin: 4px 0 0 0; font-size: 12px; color: var(--text-secondary); line-height: 1.5; }
:deep(.el-timeline-item__timestamp) { font-size: 12px; color: var(--text-secondary); }

@media (max-width: 640px) {
  .card-foot { flex-direction: column; align-items: stretch; gap: 10px; }
  .foot-total { justify-content: space-between; }
}
</style>
