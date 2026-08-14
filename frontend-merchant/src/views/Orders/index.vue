<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import {
  Refresh, User, Phone, Location, Clock, Document, Coin, Van, Calendar,
} from '@element-plus/icons-vue'
import {
  getMerchantOrdersApi, getMerchantOrderStatsApi, getMerchantOrderDetailApi,
  acceptOrderApi, startCookingApi, completeOrderApi, rejectOrderApi,
  type MerchantOrderListVO, type MerchantOrderDetailVO,
} from '@/apis/order'

const route = useRoute()

/** 页面模式：today 今日订单（0 点之后）/ all 全部订单（可选时间范围） */
const mode = computed<'today' | 'all'>(() => (route.meta.mode === 'all' ? 'all' : 'today'))
const pageTitle = computed(() => (mode.value === 'today' ? '今日订单' : '全部订单'))

/** 今日日期字符串（yyyy-MM-dd） */
const todayStr = () => {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

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

/** 全部订单：可选下单时间范围（yyyy-MM-dd 起止，含当天） */
const dateRange = ref<string[]>([])

const fetchOrders = async (silent = false) => {
  if (!silent) loading.value = true
  try {
    const params: {
      page: number
      size: number
      status?: string
      dateStart?: string
      dateEnd?: string
    } = {
      page: page.value, size: size.value,
      status: activeTab.value || undefined,
    }
    if (mode.value === 'today') {
      // 今日订单：0 点之后（dateStart = dateEnd = 今天）
      params.dateStart = todayStr()
      params.dateEnd = todayStr()
    } else if (dateRange.value?.[0]) {
      params.dateStart = dateRange.value[0]
      params.dateEnd = dateRange.value[1] || dateRange.value[0]
    }
    const res = await getMerchantOrdersApi(params)
    orders.value = res.data.list || []
    total.value = res.data.total || 0
  } catch { if (!silent) ElMessage.error('获取订单列表失败') }
  finally { if (!silent) loading.value = false }
}

const fetchStats = async () => {
  // 全部订单页不展示角标，无需拉统计；今日订单页按当天范围统计
  if (mode.value !== 'today') {
    stats.value = {}
    return
  }
  try {
    const res = await getMerchantOrderStatsApi({ dateStart: todayStr(), dateEnd: todayStr() })
    stats.value = res.data || {}
  } catch { /* 统计失败不阻塞 */ }
}

// ---- WebSocket 实时推送（新订单通知，替代轮询） ----
const ws = ref<WebSocket | null>(null)
const wsConnected = ref(false)
let reconnectTimer: number | null = null
let heartbeatTimer: number | null = null

/** 新订单提示音（WebAudio 双音，无需音频资源文件） */
const playDing = () => {
  try {
    const Ctx = window.AudioContext || (window as any).webkitAudioContext
    const ctx = new Ctx()
    const playTone = (freq: number, start: number, dur: number) => {
      const osc = ctx.createOscillator()
      const gain = ctx.createGain()
      osc.connect(gain); gain.connect(ctx.destination)
      osc.type = 'sine'; osc.frequency.value = freq
      gain.gain.setValueAtTime(0.25, ctx.currentTime + start)
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + start + dur)
      osc.start(ctx.currentTime + start); osc.stop(ctx.currentTime + start + dur)
    }
    playTone(880, 0, 0.18)
    playTone(1174, 0.18, 0.25)
  } catch { /* 浏览器不支持音频则静默 */ }
}

const connectWs = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const socket = new WebSocket(`${proto}://${location.host}/ws/merchant?token=${encodeURIComponent(token)}`)
  ws.value = socket

  socket.onopen = () => {
    wsConnected.value = true
    socket.send(JSON.stringify({ type: 'PING' }))
  }
  socket.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data)
      if (msg.type === 'NEW_ORDER') {
        playDing()
        ElNotification({
          title: '您有新的订单',
          message: `订单 ${shortNo(msg.data.orderNo)} · ${formatMoney(msg.data.totalAmount)}，请及时接单`,
          type: 'warning',
          position: 'bottom-right',
          duration: 8000,
        })
        // 今日订单页刷新列表 + 当日角标；全部订单页只刷新列表
        if (mode.value === 'today') {
          fetchStats()
          fetchOrders(true)
        } else {
          fetchOrders(true)
        }
      }
    } catch { /* 非 JSON 帧忽略 */ }
  }
  socket.onclose = () => {
    wsConnected.value = false
    scheduleReconnect()
  }
  socket.onerror = () => { socket.close() }
}

/** 断线 5 秒后自动重连 */
const scheduleReconnect = () => {
  if (reconnectTimer !== null) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connectWs()
  }, 5000)
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

onMounted(() => {
  fetchOrders()
  fetchStats()
  connectWs()
  // 心跳保活：30 秒 PING（服务端回 PONG）
  heartbeatTimer = window.setInterval(() => {
    if (ws.value?.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify({ type: 'PING' }))
    }
  }, 30_000)
})

onUnmounted(() => {
  if (heartbeatTimer !== null) clearInterval(heartbeatTimer)
  if (reconnectTimer !== null) clearTimeout(reconnectTimer)
  ws.value?.close()
})

// 今日订单 ⇄ 全部订单切换（同一组件复用）：重置筛选并重新拉取
watch(() => route.path, () => {
  page.value = 1
  dateRange.value = []
  fetchOrders()
  fetchStats()
})
</script>

<template>
  <div class="orders-page">
    <!-- ====== 顶栏 ====== -->
    <div class="page-card top-bar fade-in">
      <div class="top-title">
        <h2><el-icon><Document /></el-icon>{{ pageTitle }}</h2>
        <span class="ws-hint" :class="wsConnected ? 'ws-on' : 'ws-off'">
          <span class="ws-dot" />
          {{ wsConnected ? '实时接单中（WebSocket）' : '连接断开，自动重连中…' }}
        </span>
      </div>
      <el-button :icon="Refresh" circle plain @click="refreshAll" title="立即刷新" />
    </div>

    <!-- ====== 时间范围（仅全部订单，可选） ====== -->
    <div v-if="mode === 'all'" class="page-card date-bar fade-in">
      <span class="date-label"><el-icon><Calendar /></el-icon>下单时间</span>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="true"
        style="width: 280px"
        @change="page = 1; fetchOrders()"
      />
      <span class="date-tip">不选时间范围则查询全部历史订单（含当天边界）</span>
    </div>

    <!-- ====== 状态 Tab（美团风：下划线 + 角标） ====== -->
    <div class="tab-bar fade-in">
      <div
        v-for="tab in TABS" :key="tab.key"
        class="tab-item" :class="{ active: activeTab === tab.key, alert: tab.key === 'PAID' && (stats.PAID || 0) > 0 }"
        @click="switchTab(tab.key)"
      >
        <span class="tab-label">{{ tab.label }}</span>
        <!-- 角标仅在今日订单页展示（当日各状态数量），全部订单页不显示数字 -->
        <span
          v-if="mode === 'today' && tab.key && (stats[tab.key] || 0) > 0"
          class="tab-badge"
          :class="{ 'badge-alert': tab.key === 'PAID' }"
        >{{ stats[tab.key] }}</span>
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
/* ---- 实时连接状态 ---- */
.ws-hint {
  font-size: 12px;
  display: flex; align-items: center; gap: 6px;
  padding: 3px 10px;
  border-radius: 20px;
}
.ws-dot { width: 8px; height: 8px; border-radius: 50%; }
.ws-on { color: var(--success); background: var(--success-bg); }
.ws-on .ws-dot { background: var(--success); box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.15); }
.ws-off { color: var(--danger); background: var(--danger-bg); }
.ws-off .ws-dot { background: var(--danger); animation: blink 1.2s ease-in-out infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }

/* ---- 时间范围栏 ---- */
.date-bar {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px; margin-bottom: 12px;
}
.date-label {
  font-size: 13px; color: var(--text-regular); font-weight: 600;
  display: flex; align-items: center; gap: 6px;
  flex-shrink: 0;
}
.date-label .el-icon { color: var(--primary); }
.date-tip { font-size: 12px; color: var(--text-secondary); }

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
