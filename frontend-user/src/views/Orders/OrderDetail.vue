<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { getOrderDetailApi, cancelOrderApi, applyRefundApi, type OrderDetail } from '@/apis/order'
import { payApi } from '@/apis/pay'

const route = useRoute()
const router = useRouter()

const orderId = Number(route.params.id)
const order = ref<OrderDetail | null>(null)
const loading = ref(true)
const acting = ref(false)
/** 支付宝沙箱收款码 */
const alipayQr = ref<string | null>(null)
/** 收款码弹窗显隐（可写 computed 供 v-model 使用） */
const qrVisible = computed({
  get: () => !!alipayQr.value,
  set: (v: boolean) => { if (!v) alipayQr.value = null },
})

/** 状态头部文案（参考美团：主状态 + 副说明） */
const STATUS_HEAD: Record<string, { title: string; sub: string }> = {
  PENDING_PAY: { title: '待支付', sub: '请尽快完成支付，超时订单将自动取消' },
  PAID: { title: '已支付', sub: '等待商家接单' },
  ACCEPTED: { title: '已接单', sub: '商家已接单，开始备餐' },
  COOKING: { title: '制作中', sub: '商家正在制作，请耐心等待' },
  COMPLETED: { title: '已完成', sub: '感谢惠顾，欢迎再次光临' },
  CANCELLED: { title: '已取消', sub: '订单已取消' },
  REFUNDING: { title: '退款处理中', sub: '退款申请处理中，请耐心等待' },
  REFUNDED: { title: '已退款', sub: '退款已原路退回' },
}

const head = computed(() => STATUS_HEAD[order.value?.status || ''] ?? { title: order.value?.status || '', sub: '' })

/** 底部操作按钮组（按状态） */
const actions = computed(() => {
  const s = order.value?.status
  if (s === 'PENDING_PAY') return [{ key: 'cancel', label: '取消订单', type: 'plain' }, { key: 'pay', label: '立即支付', type: 'primary' }]
  if (s === 'PAID' || s === 'ACCEPTED' || s === 'COOKING') return [{ key: 'refund', label: '申请退款', type: 'plain' }]
  return []
})

const statusLogs = computed(() => order.value?.statusLogs || [])

// ---- 格式化 ----
const formatMoney = (n: number | null | undefined) => '¥' + Number(n ?? 0).toFixed(2)
const formatTime = (s: string | null | undefined) => (s ? s.replace('T', ' ').substring(0, 19) : '-')
const channelName = (c: string | null | undefined) =>
  c === 'ALIPAY' ? '支付宝' : c === 'WECHAT_MOCK' ? '微信支付' : '-'
const parseAddress = () => {
  try {
    return JSON.parse(order.value?.addressSnapshot || 'null')
  } catch { return null }
}

// ---- 加载 ----
const load = async () => {
  loading.value = true
  try {
    const res = await getOrderDetailApi(orderId)
    order.value = res.data
  } catch { showToast('加载订单失败') }
  finally { loading.value = false }
}
load()

// ---- 操作 ----
/** 取消订单（仅待支付） */
const onCancel = () => {
  showDialog({
    title: '取消订单',
    message: '确定取消该订单吗？',
    showCancelButton: true,
    confirmButtonText: '确认取消',
    confirmButtonColor: '#F53F3F',
  })
    .then(async () => {
      acting.value = true
      try {
        await cancelOrderApi(orderId)
        showToast('订单已取消')
        load()
      } catch { /* 拦截器已提示 */ }
      finally { acting.value = false }
    })
    .catch(() => {})
}

/** 立即支付（二次确认 → 发起支付） */
const onPay = () => {
  showDialog({
    title: '订单支付',
    message: `确认支付 ${formatMoney(order.value?.totalAmount)}？`,
    showCancelButton: true,
    confirmButtonText: '确认支付',
    confirmButtonColor: '#f59a23',
  })
    .then(async () => {
      acting.value = true
      try {
        const res = await payApi(order.value!.orderNo, order.value?.payChannel === 'ALIPAY' ? 'ALIPAY' : 'WECHAT_MOCK')
        if (res.data?.qrCode) {
          alipayQr.value = res.data.qrCode
          return
        }
        showToast('支付成功')
        load()
      } catch { /* 拦截器已提示 */ }
      finally { acting.value = false }
    })
    .catch(() => {})
}

/** 申请退款（1分钟内即时退；超1分钟转商家确认） */
const onRefund = () => {
  showDialog({
    title: '申请退款',
    message: '支付成功 1 分钟内申请将即时退款；超过 1 分钟需商家确认后处理。确定申请退款吗？',
    showCancelButton: true,
    confirmButtonText: '申请退款',
    confirmButtonColor: '#f59a23',
  })
    .then(async () => {
      acting.value = true
      try {
        const res = await applyRefundApi(orderId)
        showToast(res.data || '退款申请已提交')
        load()
      } catch { /* 拦截器已提示 */ }
      finally { acting.value = false }
    })
    .catch(() => {})
}

/** 底部按钮分发 */
const onAction = (key: string) => {
  if (key === 'cancel') onCancel()
  else if (key === 'pay') onPay()
  else if (key === 'refund') onRefund()
}
</script>

<template>
  <div class="detail-page">
    <van-nav-bar title="订单详情" left-arrow fixed placeholder @click-left="router.back()" />

    <div v-if="order" class="detail-body">
      <!-- 状态头 -->
      <div class="status-head">
        <p class="status-title">{{ head.title }}</p>
        <p class="status-sub">{{ head.sub }}</p>
      </div>

      <!-- 收货地址 -->
      <div v-if="parseAddress()" class="card addr-card">
        <van-icon name="location-o" color="#f59a23" size="18" />
        <div class="addr-info">
          <p class="addr-line">
            {{ parseAddress().receiver }} <span class="addr-phone">{{ parseAddress().phone }}</span>
          </p>
          <p class="addr-detail">{{ parseAddress().detail }}</p>
        </div>
      </div>

      <!-- 店铺 + 商品 -->
      <div class="card">
        <div class="shop-row">
          <van-icon name="shop-o" color="#f59a23" size="16" />
          <span class="shop-name">{{ order.merchantName }}</span>
        </div>
        <div v-for="it in order.items" :key="it.productId + '-' + it.specId" class="goods-row">
          <van-image :src="it.imageUrl ?? undefined" fit="cover" class="goods-img">
            <template #error>🍽️</template>
          </van-image>
          <div class="goods-info">
            <p class="goods-name">{{ it.nameSnapshot }}</p>
            <p class="goods-price">{{ formatMoney(it.priceSnapshot) }}</p>
          </div>
          <span class="goods-qty">x{{ it.quantity }}</span>
        </div>
        <div class="amount-row">
          <span>实付金额</span>
          <span class="amount-price">{{ formatMoney(order.totalAmount) }}</span>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="card info-card">
        <div class="info-row"><span>订单编号</span><span>{{ order.orderNo }}</span></div>
        <div class="info-row"><span>下单时间</span><span>{{ formatTime(order.createdAt) }}</span></div>
        <div class="info-row"><span>支付方式</span><span>{{ channelName(order.payChannel) }}</span></div>
        <div class="info-row" v-if="order.payTime"><span>支付时间</span><span>{{ formatTime(order.payTime) }}</span></div>
      </div>

      <!-- 订单流转 -->
      <div class="card info-card">
        <div class="info-title">订单流转</div>
        <div v-for="(log, i) in statusLogs" :key="i" class="log-row">
          <span class="log-dot" />
          <div class="log-body">
            <p class="log-text">
              {{ log.reason }}<span v-if="log.operator && log.operator !== 'USER' && log.operator !== 'system'" class="log-operator">（{{ log.operator }}）</span>
            </p>
            <p class="log-time">{{ formatTime(log.createdAt) }}</p>
          </div>
        </div>
      </div>

      <!-- 底部操作条 -->
      <div v-if="actions.length" class="action-bar">
        <template v-for="a in actions" :key="a.key">
          <van-button
            v-if="a.type === 'plain'"
            round
            plain
            class="action-btn"
            :loading="acting"
            @click="onAction(a.key)"
          >
            {{ a.label }}
          </van-button>
          <van-button
            v-else
            round
            class="action-btn primary"
            :loading="acting"
            color="linear-gradient(135deg, #f59a23, #ff6f3c)"
            @click="onAction(a.key)"
          >
            {{ a.label }}
          </van-button>
        </template>
      </div>
    </div>
    <van-loading v-else-if="loading" class="page-loading" color="#f59a23" vertical>加载中...</van-loading>

    <!-- 支付宝收款码弹窗 -->
    <van-popup v-model:show="qrVisible" round>
      <div class="qr-pop">
        <p class="qr-title">支付宝扫码支付</p>
        <img v-if="alipayQr" :src="alipayQr" class="qr-img" alt="支付宝收款码" />
        <p class="qr-tip">请使用支付宝 App 扫码支付（沙箱环境），支付完成后刷新查看</p>
        <van-button round block color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="alipayQr = null; load()">
          完成
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding-bottom: 90px;
}
.detail-body {
  padding: 10px;
}
.page-loading {
  margin-top: 40vh;
}

/* 状态头 */
.status-head {
  padding: 10px 4px 14px;
  color: #1d2129;
}
.status-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}
.status-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #86909c;
}

/* 卡片 */
.card {
  background: #fff;
  border-radius: 12px;
  padding: 4px 14px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 地址 */
.addr-card {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px;
}
.addr-info {
  flex: 1;
  min-width: 0;
}
.addr-line {
  margin: 0 0 3px;
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}
.addr-phone {
  font-size: 12px;
  font-weight: 400;
  color: #86909c;
  margin-left: 8px;
}
.addr-detail {
  margin: 0;
  font-size: 12px;
  color: #86909c;
}

/* 商品 */
.shop-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.shop-name {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.goods-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.goods-img {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f2f3f5;
  font-size: 26px;
  overflow: hidden;
}
.goods-info {
  flex: 1;
  min-width: 0;
}
.goods-name {
  margin: 0 0 6px;
  font-size: 14px;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-price {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1d2129;
}
.goods-qty {
  font-size: 13px;
  color: #86909c;
}
.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 6px;
  font-size: 13px;
  color: #4e5969;
}
.amount-price {
  font-size: 17px;
  font-weight: 700;
  color: #f53f3f;
}

/* 信息 */
.info-card {
  padding: 12px 14px;
}
.info-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  padding: 4px 0 8px;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}
.info-row span:first-child {
  color: #86909c;
}
.info-row span:last-child {
  color: #1d2129;
  max-width: 65%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.log-row {
  display: flex;
  gap: 10px;
  padding: 6px 0;
}
.log-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f59a23;
  margin-top: 6px;
  flex-shrink: 0;
}
.log-body {
  flex: 1;
}
.log-text {
  margin: 0;
  font-size: 13px;
  color: #1d2129;
}
.log-operator {
  color: #86909c;
  font-size: 12px;
}
.log-time {
  margin: 2px 0 0;
  font-size: 11px;
  color: #c9cdd4;
}

/* 底部操作条 */
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 8px 16px;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}
.action-btn {
  min-width: 110px;
  border-color: #c9cdd4;
  color: #4e5969;
}
.action-btn.primary {
  border: none;
}

/* 收款码弹窗 */
.qr-pop {
  width: 280px;
  padding: 24px 20px 20px;
  text-align: center;
}
.qr-title {
  margin: 0 0 14px;
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
}
.qr-img {
  width: 200px;
  height: 200px;
  border-radius: 8px;
  background: #f2f3f5;
}
.qr-tip {
  margin: 12px 0 14px;
  font-size: 12px;
  color: #86909c;
}
</style>
