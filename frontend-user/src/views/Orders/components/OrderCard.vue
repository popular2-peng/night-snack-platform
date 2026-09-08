<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { deleteOrderApi, type UserOrder } from '@/apis/order'
import { payApi } from '@/apis/pay'
import OrderStatusTag from './OrderStatusTag.vue'

const router = useRouter()

const props = defineProps<{ order: UserOrder }>()

const emit = defineEmits<{
  /** 删除成功后通知父组件从列表移除 */
  deleted: [id: number]
  /** 支付成功后通知父组件更新订单状态为已支付 */
  paid: [orderNo: string]
}>()

const paying = ref(false)
/** 支付宝沙箱收款码（真实沙箱配置后返回，展示扫码） */
const alipayQr = ref<string | null>(null)
/** 收款码弹窗显隐（可写 computed 供 v-model 使用） */
const qrVisible = computed({
  get: () => !!alipayQr.value,
  set: (v: boolean) => { if (!v) alipayQr.value = null },
})

/** 待支付订单：立即支付（二次确认 → 发起支付） */
const onPayNow = () => {
  showDialog({
    title: '订单支付',
    message: `确认支付 ¥${Number(props.order.totalAmount ?? 0).toFixed(2)}？`,
    showCancelButton: true,
    confirmButtonText: '确认支付',
    confirmButtonColor: '#f59a23',
  })
    .then(async () => {
      paying.value = true
      try {
        // 使用下单时选择的渠道；微信 mock / 支付宝未配置降级均直接成功
        const res = await payApi(props.order.orderNo, props.order.payChannel === 'ALIPAY' ? 'ALIPAY' : 'WECHAT_MOCK')
        if (res.data?.qrCode) {
          alipayQr.value = res.data.qrCode // 真实支付宝沙箱：展示收款码等待扫码
          return
        }
        showToast('支付成功')
        emit('paid', props.order.orderNo)
      } catch { /* 拦截器已提示 */ }
      finally { paying.value = false }
    })
    .catch(() => { /* 用户取消（订单保持待支付，超时后自动取消） */ })
}

/** 单商品布局判断 */
const isSingle = () => (props.order.items?.length || 0) === 1

/** 更多 → 删除订单（软删，确认弹窗） */
const onMore = () => {
  showDialog({
    title: '删除订单',
    message: '删除后该订单将不再显示，确定删除吗？',
    showCancelButton: true,
    confirmButtonText: '删除',
    confirmButtonColor: '#F53F3F',
  })
    .then(async () => {
      try {
        await deleteOrderApi(props.order.id)
        showToast('订单已删除')
        emit('deleted', props.order.id)
      } catch { /* 拦截器已提示 */ }
    })
    .catch(() => { /* 用户取消 */ })
}

// ---- 点击卡片进入订单详情 ----
const goDetail = () => router.push(`/orders/${props.order.id}`)

// ---- 占位功能（后续开发） ----
const onAgain = () => showToast('再来一单功能开发中')

/** 待支付状态显示「立即支付」，其余状态显示「再来一单」 */
const isPendingPay = () => props.order.status === 'PENDING_PAY'

// ---- 格式化 ----
const formatMoney = (n: number) => '¥' + Number(n ?? 0).toFixed(2)
const formatTime = (s: string) => (s ? s.replace('T', ' ').substring(0, 16) : '-')
const totalCount = () => (props.order.items || []).reduce((sum, i) => sum + (i.quantity || 0), 0)
</script>

<template>
  <div class="order-card" @click="goDetail">
    <!-- 卡头：店铺名 + 状态 -->
    <div class="card-head">
      <span class="shop-name">{{ order.merchantName }}</span>
      <OrderStatusTag :status="order.status" />
    </div>

    <!-- 卡中：单商品（左图 + 名称/下单时间，右金额） -->
    <div v-if="isSingle()" class="body-single">
      <div class="item-left">
        <van-image :src="order.items[0]?.imageUrl ?? undefined" fit="cover" class="item-img">
          <template #error>🍽️</template>
        </van-image>
        <div class="item-meta">
          <span class="item-name">{{ order.items[0]?.nameSnapshot }}</span>
          <span class="order-time">下单：{{ formatTime(order.createdAt) }}</span>
        </div>
      </div>
      <span class="amount">{{ formatMoney(order.totalAmount) }}</span>
    </div>

    <!-- 卡中：多商品（左可滑动列表：图 + 下方商品名；右遮罩层：金额 + 共X件） -->
    <div v-else class="body-multi">
      <div class="multi-left">
        <div class="items-scroll">
          <div v-for="(it, i) in order.items" :key="i" class="thumb-item">
            <van-image :src="it.imageUrl ?? undefined" fit="cover" class="thumb-img">
              <template #error>🍽️</template>
            </van-image>
            <span class="thumb-name">{{ it.nameSnapshot }}</span>
          </div>
        </div>
        <span class="order-time">下单：{{ formatTime(order.createdAt) }}</span>
      </div>
      <div class="multi-right">
        <span class="amount">{{ formatMoney(order.totalAmount) }}</span>
        <span class="count">共{{ totalCount() }}件</span>
      </div>
    </div>

    <!-- 卡底：更多（删除）+ 立即支付（待支付）/ 再来一单（其他状态） -->
    <div class="card-foot" @click.stop>
      <span class="more" @click="onMore">更多</span>
      <van-button
        v-if="isPendingPay()"
        size="small"
        round
        class="pay-btn"
        :loading="paying"
        color="linear-gradient(135deg, #f59a23, #ff6f3c)"
        @click="onPayNow"
      >
        立即支付
      </van-button>
      <van-button v-else size="small" round plain class="again-btn" @click="onAgain">再来一单</van-button>
    </div>

    <!-- 支付宝收款码弹窗（真实沙箱返回二维码时展示） -->
    <van-popup v-model:show="qrVisible" round>
      <div class="qr-pop">
        <p class="qr-title">支付宝扫码支付</p>
        <img v-if="alipayQr" :src="alipayQr" class="qr-img" alt="支付宝收款码" />
        <p class="qr-tip">请使用支付宝 App 扫码支付（沙箱环境），支付完成后下拉刷新查看</p>
        <van-button round block color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="alipayQr = null">
          关闭
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.order-card {
  margin: 10px 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 卡头 */
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px dashed #f2f3f5;
}
.shop-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 单商品 */
.body-single {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.item-img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  flex-shrink: 0;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 24px;
}
.item-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}
.item-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.order-time {
  font-size: 11px;
  color: #86909c;
}

/* 多商品 */
.body-multi {
  position: relative;
  display: flex;
}
.multi-left {
  flex: 1;
  min-width: 0;
}
.multi-left .order-time {
  display: block;
  margin-top: 6px;
}
.items-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.items-scroll::-webkit-scrollbar {
  display: none;
}
.thumb-item {
  width: 64px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}
.thumb-img {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 24px;
}
.thumb-name {
  width: 100%;
  font-size: 10px;
  color: #666;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 右侧遮罩层（渐变盖在商品图上，显示金额 + 共X件） */
.multi-right {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 132px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: flex-end;
  gap: 2px;
  padding: 6px 10px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0), rgba(255, 255, 255, 0.96) 35%);
}

/* 金额（单商品右侧 / 多商品遮罩层共用） */
.amount {
  font-size: 15px;
  font-weight: 700;
  color: #f53f3f;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}
.count {
  font-size: 11px;
  color: #86909c;
}

/* 卡底 */
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
}
.more {
  font-size: 13px;
  color: #666;
  padding: 4px 0;
}
.again-btn {
  --van-button-default-color: #f59a23;
}
.pay-btn {
  border: none;
}

/* 支付宝收款码弹窗 */
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
