<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getCartApi, updateCartQtyApi, removeCartItemApi, type CartGroup, type CartItem } from '@/apis/cart'
import { listAddressesApi, maskPhone, type AddressVO } from '@/apis/address'
import { createOrderApi } from '@/apis/order'
import { payApi } from '@/apis/pay'

const router = useRouter()

// ---- 数据（购物车按商家分组） ----
const groups = ref<CartGroup[]>([])
const addresses = ref<AddressVO[]>([])
const selectedAddrId = ref<number | null>(null)
/** 地址选择弹出层 */
const addrPickerVisible = ref(false)
const loading = ref(false)
const submitting = ref(false)

/** 当前选中地址 */
const selectedAddr = computed(() => addresses.value.find((a) => a.id === selectedAddrId.value) ?? null)

/** 全部商家商品合计 */
const goodsTotal = computed(() =>
  groups.value.reduce((s, g) => s + g.items.reduce((s2, i) => s2 + Number(i.price) * i.quantity, 0), 0),
)

/** 支付方式：微信（mock 直付）/ 支付宝（沙箱，未配置时降级 mock） */
const payChannel = ref<'WECHAT_MOCK' | 'ALIPAY'>('WECHAT_MOCK')

/** 支付宝二维码（沙箱预下单返回，展示用） */
const alipayQr = ref<string | null>(null)
/** 二维码弹窗显隐（可写 computed 供 v-model 使用） */
const alipayQrVisible = computed({
  get: () => !!alipayQr.value,
  set: (v: boolean) => { if (!v) alipayQr.value = null },
})

/** 是否可提交（有地址 + 有商品） */
const canSubmit = computed(() => !!selectedAddr.value && groups.value.length > 0)

// ---- 支付确认与超时倒计时（参考美团：下单后二次确认支付，取消则进入自动取消倒计时） ----
/** 后端超时自动取消时长（分钟），与订单服务一致 */
const PAY_TIMEOUT_MIN = 15
/** 已创建待支付订单（确认支付/倒计时用） */
const pendingOrders = ref<{ orderNo: string; totalAmount: number; createdAt: string }[]>([])
const confirmPayVisible = ref(false)
const countdownVisible = ref(false)
const countdownText = ref('15:00')
const payExpired = ref(false)
let countdownTimer: number | undefined

/** 待支付合计（弹窗展示） */
const pendingTotal = computed(() => pendingOrders.value.reduce((s, o) => s + Number(o.totalAmount), 0))

/** 确认支付：逐单发起支付（微信 mock / 支付宝未配置降级均直接成功；真实沙箱返回收款码则弹扫码） */
const confirmPay = async () => {
  if (!pendingOrders.value.length) return
  submitting.value = true
  try {
    for (const o of pendingOrders.value) {
      const payRes = await payApi(o.orderNo, payChannel.value)
      if (payRes.data?.qrCode) {
        alipayQr.value = payRes.data.qrCode // 真实沙箱：展示二维码等待扫码
        submitting.value = false
        confirmPayVisible.value = false
        return
      }
    }
    showToast('支付成功')
    router.replace('/orders')
  } catch {
    showToast('支付失败，可到订单列表继续支付')
    router.replace('/orders')
  } finally {
    submitting.value = false
    confirmPayVisible.value = false
  }
}

/** 取消支付：进入自动取消倒计时弹窗（以最早订单创建时间 + 15 分钟为截止） */
const cancelPay = () => {
  confirmPayVisible.value = false
  const created = pendingOrders.value.map((o) => o.createdAt).sort()[0] || new Date().toISOString()
  const deadline = new Date(created).getTime() + PAY_TIMEOUT_MIN * 60 * 1000
  payExpired.value = false
  countdownVisible.value = true
  window.clearInterval(countdownTimer)
  tickCountdown(deadline)
  countdownTimer = window.setInterval(() => tickCountdown(deadline), 1000)
}

/** 倒计时每秒刷新；归零提示超时自动取消 */
const tickCountdown = (deadline: number) => {
  const remain = Math.max(0, Math.floor((deadline - Date.now()) / 1000))
  countdownText.value =
    String(Math.floor(remain / 60)).padStart(2, '0') + ':' + String(remain % 60).padStart(2, '0')
  if (remain <= 0) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
    payExpired.value = true
  }
}

/** 倒计时弹窗「去支付」：回到确认支付弹窗 */
const goPayAgain = () => {
  window.clearInterval(countdownTimer)
  countdownTimer = undefined
  countdownVisible.value = false
  confirmPayVisible.value = true
}

/** 离开页面清理倒计时定时器 */
onBeforeUnmount(() => {
  window.clearInterval(countdownTimer)
})

// ---- 加载 ----
onMounted(async () => {
  loading.value = true
  try {
    const [cartRes, addrRes] = await Promise.all([getCartApi(), listAddressesApi()])
    groups.value = cartRes.data?.groups || []
    addresses.value = addrRes.data || []
    // 默认选默认地址，没有则选第一个
    const def = addresses.value.find((a) => a.isDefault)
    selectedAddrId.value = (def ?? addresses.value[0])?.id ?? null
  } catch {
    showToast('加载结算信息失败')
  } finally {
    loading.value = false
  }
})

// ---- 数量加减（与购物车联动，减到 0 移除） ----
const changeQty = async (group: CartGroup, item: CartItem, delta: number) => {
  const newQty = item.quantity + delta
  try {
    if (newQty <= 0) await removeCartItemApi(item.cartKey)
    else await updateCartQtyApi(item.cartKey, newQty)
    // 本地同步数量，避免整单刷新闪烁
    if (newQty <= 0) {
      group.items = group.items.filter((i) => i.cartKey !== item.cartKey)
      if (!group.items.length) groups.value = groups.value.filter((g) => g !== group)
    } else {
      item.quantity = newQty
    }
  } catch { /* 拦截器已提示 */ }
}

// ---- 提交订单（按商家逐组下单，每商家一单；成功后弹支付确认，不自动支付） ----
const onSubmit = async () => {
  if (!canSubmit.value) {
    if (!selectedAddr.value) showToast('请先选择收货地址')
    return
  }
  submitting.value = true
  try {
    const created: { orderNo: string; totalAmount: number; createdAt: string }[] = []
    for (const group of groups.value) {
      const items = group.items.map((i) => ({
        productId: i.productId,
        specId: i.specId, // 无规格商品为 null，后端按商品基础价计价
        quantity: i.quantity,
      }))
      const res = await createOrderApi({
        merchantId: group.merchantId,
        addressId: selectedAddr.value!.id,
        items,
        payChannel: payChannel.value,
        clientToken: crypto.randomUUID(),
      })
      const data = res.data
      if (data?.orderNo) {
        created.push({
          orderNo: data.orderNo,
          totalAmount: data.totalAmount ?? 0,
          createdAt: data.createdAt ?? new Date().toISOString(),
        })
      }
    }
    if (!created.length) {
      showToast('下单失败')
      return
    }
    // 弹二次确认支付（mock 支付也不能直接成功）
    pendingOrders.value = created
    confirmPayVisible.value = true
  } catch {
    // 拦截器已提示（库存/配送范围/重复提交等）；部分商家下单失败时其余商家订单已创建
    showToast('部分订单提交失败，请到订单列表查看')
  } finally { submitting.value = false }
}

/** 购物车为空：返回点餐页继续选购 */
const goBackToShop = () => {
  router.back()
}
</script>

<template>
  <div class="checkout-page">
    <van-nav-bar title="确认订单" left-arrow fixed placeholder @click-left="router.back()" />

    <!-- ====== 收货地址卡片（点击弹出地址选择层） ====== -->
    <div class="addr-card" v-loading="loading" @click="addrPickerVisible = true">
      <template v-if="selectedAddr">
        <van-icon name="location-o" color="#f59a23" size="20" class="addr-icon" />
        <div class="addr-info">
          <p class="addr-line1">
            <span class="addr-name">{{ selectedAddr.receiver }}</span>
            <span class="addr-phone">{{ maskPhone(selectedAddr.phone) }}</span>
          </p>
          <p class="addr-detail">{{ selectedAddr.detail }}</p>
        </div>
        <van-icon name="arrow" color="#c9cdd4" />
      </template>
      <template v-else>
        <van-icon name="add" color="#f59a23" size="20" class="addr-icon" />
        <span class="addr-empty">请选择收货地址</span>
        <van-icon name="arrow" color="#c9cdd4" />
      </template>

      <!-- 地址选择弹出层（美团式：点击地址卡片弹列表） -->
      <van-popup
        v-model:show="addrPickerVisible"
        position="bottom"
        round
        safe-area-inset-bottom
      >
        <div class="addr-picker">
          <div class="picker-head">
            <span class="picker-title">选择收货地址</span>
            <span class="picker-manage" @click="router.push('/address')">管理</span>
          </div>
          <div
            v-for="a in addresses"
            :key="a.id"
            class="picker-item"
            :class="{ active: a.id === selectedAddrId }"
            @click="selectedAddrId = a.id; addrPickerVisible = false"
          >
            <div class="picker-info">
              <p class="picker-line1">
                <span class="picker-name">{{ a.receiver }}</span>
                <span class="picker-phone">{{ maskPhone(a.phone) }}</span>
                <van-tag v-if="a.isDefault" type="warning" plain>默认</van-tag>
              </p>
              <p class="picker-detail">{{ a.detail }}</p>
            </div>
            <van-icon name="success" color="#f59a23" v-if="a.id === selectedAddrId" />
          </div>
          <div v-if="!addresses.length" class="picker-empty" @click="router.push('/address/edit')">
            <van-icon name="add" /> 新增收货地址
          </div>
        </div>
      </van-popup>
    </div>

    <!-- ====== 商家与商品清单（按商家分组，每组一单） ====== -->
    <div v-for="group in groups" :key="group.merchantId" class="goods-card">
      <div class="shop-row">
        <van-icon name="shop-o" color="#f59a23" size="18" />
        <span class="shop-name">{{ group.merchantName }}</span>
        <span class="shop-tip">配送约 30 分钟</span>
      </div>

      <div v-for="item in group.items" :key="item.cartKey" class="goods-row">
        <van-image :src="item.imageUrl" fit="cover" class="goods-img">
          <template #error>🍽️</template>
        </van-image>
        <div class="goods-info">
          <p class="goods-name">{{ item.productName }}</p>
          <p class="goods-spec">{{ item.specName || '' }}</p>
          <div class="goods-bottom">
            <span class="goods-price">¥{{ Number(item.price).toFixed(2) }}</span>
            <div class="qty-ctl">
              <span class="ctl-minus" @click="changeQty(group, item, -1)">-</span>
              <span class="ctl-num">{{ item.quantity }}</span>
              <span class="ctl-plus" @click="changeQty(group, item, 1)">+</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 金额明细（每组） -->
      <div class="amount-row">
        <span>商品金额</span>
        <span>¥{{ group.items.reduce((s, i) => s + Number(i.price) * i.quantity, 0).toFixed(2) }}</span>
      </div>
      <div class="amount-row">
        <span>配送费</span>
        <span class="free">¥0.00</span>
      </div>
    </div>

    <van-empty v-if="!groups.length && !loading" description="购物车是空的" image-size="80">
      <van-button round type="primary" size="small" color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="goBackToShop">
        去点餐
      </van-button>
    </van-empty>

    <!-- ====== 支付方式（美团式单选） ====== -->
    <div v-if="groups.length" class="pay-card">
      <div class="pay-title">支付方式</div>
      <div class="pay-option" :class="{ active: payChannel === 'WECHAT_MOCK' }" @click="payChannel = 'WECHAT_MOCK'">
        <span class="pay-icon wechat">微</span>
        <span class="pay-name">微信支付</span>
        <van-icon name="success" color="#f59a23" class="pay-check" v-if="payChannel === 'WECHAT_MOCK'" />
      </div>
      <div class="pay-option" :class="{ active: payChannel === 'ALIPAY' }" @click="payChannel = 'ALIPAY'">
        <span class="pay-icon alipay">支</span>
        <span class="pay-name">支付宝</span>
        <van-icon name="success" color="#f59a23" class="pay-check" v-if="payChannel === 'ALIPAY'" />
      </div>
    </div>

    <!-- ====== 确认支付弹窗（二次确认：确认即支付成功，取消进入倒计时） ====== -->
    <van-popup v-model:show="confirmPayVisible" position="bottom" round safe-area-inset-bottom>
      <div class="pay-confirm">
        <p class="confirm-title">订单支付</p>
        <p class="confirm-amount">¥{{ pendingTotal.toFixed(2) }}</p>
        <p v-if="pendingOrders.length > 1" class="confirm-count">共 {{ pendingOrders.length }} 笔订单</p>
        <p class="confirm-order-no" v-else>订单号：{{ pendingOrders[0]?.orderNo }}</p>

        <div class="confirm-channel">
          <span class="channel-icon" :class="payChannel === 'WECHAT_MOCK' ? 'wechat' : 'alipay'">
            {{ payChannel === 'WECHAT_MOCK' ? '微' : '支' }}
          </span>
          <span>{{ payChannel === 'WECHAT_MOCK' ? '微信支付' : '支付宝' }}</span>
        </div>

        <div class="confirm-btns">
          <van-button round plain class="btn-cancel" @click="cancelPay">取消</van-button>
          <van-button
            round
            class="btn-ok"
            :loading="submitting"
            color="linear-gradient(135deg, #f59a23, #ff6f3c)"
            @click="confirmPay"
          >
            确认支付
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- ====== 超时自动取消倒计时弹窗（取消支付后弹出，参考美团） ====== -->
    <van-popup v-model:show="countdownVisible" round :close-on-click-overlay="false">
      <div class="countdown-pop">
        <van-icon name="clock-o" size="44" color="#f59a23" />
        <p class="cd-title">{{ payExpired ? '订单已超时自动取消' : '订单未支付' }}</p>
        <p v-if="!payExpired" class="cd-tip">支付剩余时间</p>
        <p v-if="!payExpired" class="cd-time">{{ countdownText }}</p>
        <p class="cd-note" v-if="!payExpired">超时未支付订单将自动取消，已选商品及时支付不迷路</p>
        <p class="cd-note" v-else>可在订单列表中查看</p>
        <div class="cd-btns" v-if="!payExpired">
          <van-button round plain class="btn-cancel" @click="countdownVisible = false">稍后再说</van-button>
          <van-button round class="btn-ok" color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="goPayAgain">
            去支付
          </van-button>
        </div>
        <div class="cd-btns" v-else>
          <van-button round block class="btn-ok" color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="router.replace('/orders')">
            查看订单
          </van-button>
        </div>
      </div>
    </van-popup>

    <!-- 支付宝扫码支付弹窗（沙箱配置后返回二维码时展示） -->
    <van-popup v-model:show="alipayQrVisible" round>
      <div class="qr-pop">
        <p class="qr-title">支付宝扫码支付</p>
        <img v-if="alipayQr" :src="alipayQr" class="qr-img" alt="支付宝收款码" />
        <p class="qr-tip">请使用支付宝 App 扫码支付（沙箱环境）</p>
        <van-button round block color="linear-gradient(135deg, #f59a23, #ff6f3c)" @click="alipayQr = null; router.replace('/orders')">
          我已支付
        </van-button>
      </div>
    </van-popup>

    <!-- 底部结算栏 -->
    <div class="checkout-bar" v-if="groups.length">
      <div class="bar-total">
        <span class="total-label">合计</span>
        <span class="total-price">¥{{ goodsTotal.toFixed(2) }}</span>
      </div>
      <div
        class="bar-submit"
        :class="{ disabled: !canSubmit }"
        @click="onSubmit"
      >
        <span v-if="submitting">提交中...</span>
        <span v-else>提交订单</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding-bottom: 80px;
}

/* ---- 地址卡片 ---- */
.addr-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}
.addr-icon {
  flex-shrink: 0;
}
.addr-info {
  flex: 1;
  min-width: 0;
}
.addr-line1 {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0 0 4px;
}
.addr-name {
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
}
.addr-phone {
  font-size: 13px;
  color: #4e5969;
}
.addr-detail {
  margin: 0;
  font-size: 13px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.addr-empty {
  flex: 1;
  font-size: 15px;
  color: #4e5969;
}

/* 地址选择弹出层 */
.addr-picker {
  padding: 16px 16px calc(12px + env(safe-area-inset-bottom));
  max-height: 55vh;
  overflow-y: auto;
}
.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.picker-title {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.picker-manage {
  font-size: 12px;
  color: #86909c;
  cursor: pointer;
}
.picker-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
}
.picker-info {
  flex: 1;
  min-width: 0;
}
.picker-line1 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 3px;
}
.picker-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.picker-phone {
  font-size: 12px;
  color: #86909c;
}
.picker-detail {
  margin: 0;
  font-size: 12px;
  color: #86909c;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-empty {
  padding: 24px 0;
  text-align: center;
  font-size: 14px;
  color: #f59a23;
  cursor: pointer;
}

/* ---- 商家与商品 ---- */
.goods-card {
  margin: 0 10px 10px;
  padding: 4px 16px 8px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.shop-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 0;
  border-bottom: 1px solid #f2f3f5;
}
.shop-name {
  font-size: 15px;
  font-weight: 700;
  color: #1d2129;
}
.shop-tip {
  margin-left: auto;
  font-size: 12px;
  color: #86909c;
}

.goods-row {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
}
.goods-img {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f2f3f5;
  font-size: 28px;
  overflow: hidden;
}
.goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.goods-name {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-spec {
  margin: 0 0 6px;
  font-size: 11px;
  color: #86909c;
}
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.goods-price {
  font-size: 15px;
  font-weight: 700;
  color: #f53f3f;
}
.qty-ctl {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ctl-minus {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid #c9cdd4;
  color: #4e5969;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
}
.ctl-num {
  min-width: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  text-align: center;
}
.ctl-plus {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 14px;
  line-height: 22px;
  text-align: center;
  cursor: pointer;
}

/* 金额明细 */
.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 13px;
  color: #4e5969;
}
.amount-row .free {
  color: #86909c;
}

/* ---- 支付方式 ---- */
.pay-card {
  margin: 0 10px;
  padding: 4px 16px 10px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.pay-title {
  padding: 12px 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}
.pay-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f2f3f5;
  cursor: pointer;
}
.pay-option:last-child {
  border-bottom: none;
}
.pay-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  line-height: 28px;
  text-align: center;
  flex-shrink: 0;
}
.pay-icon.wechat {
  background: #07c160;
}
.pay-icon.alipay {
  background: #1677ff;
}
.pay-name {
  flex: 1;
  font-size: 14px;
  color: #1d2129;
}
.pay-check {
  font-size: 18px;
}

/* ---- 确认支付弹窗 ---- */
.pay-confirm {
  padding: 24px 20px calc(16px + env(safe-area-inset-bottom));
  text-align: center;
}
.confirm-title {
  margin: 0 0 6px;
  font-size: 15px;
  color: #4e5969;
}
.confirm-amount {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: #f53f3f;
}
.confirm-count,
.confirm-order-no {
  margin: 6px 0 0;
  font-size: 12px;
  color: #86909c;
}
.confirm-channel {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 0;
  padding: 8px 18px;
  border-radius: 18px;
  background: #f6f7f9;
  font-size: 14px;
  color: #1d2129;
}
.channel-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  line-height: 24px;
  text-align: center;
}
.channel-icon.wechat {
  background: #07c160;
}
.channel-icon.alipay {
  background: #1677ff;
}
.confirm-btns {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}
.btn-cancel {
  flex: 1;
  border-color: #c9cdd4;
  color: #4e5969;
}
.btn-ok {
  flex: 2;
  border: none;
}

/* ---- 超时倒计时弹窗 ---- */
.countdown-pop {
  width: 300px;
  padding: 26px 20px 20px;
  text-align: center;
}
.cd-title {
  margin: 12px 0 0;
  font-size: 17px;
  font-weight: 700;
  color: #1d2129;
}
.cd-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: #86909c;
}
.cd-time {
  margin: 4px 0 0;
  font-size: 42px;
  font-weight: 700;
  color: #f53f3f;
  font-variant-numeric: tabular-nums;
}
.cd-note {
  margin: 8px 0 0;
  font-size: 12px;
  color: #86909c;
}
.cd-btns {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

/* 支付宝二维码弹窗 */
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

/* ---- 底部结算栏 ---- */
.checkout-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 10;
  height: 56px;
  padding: 0 12px 0 20px;
  padding-bottom: env(safe-area-inset-bottom);
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}
.bar-total {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.total-label {
  font-size: 13px;
  color: #4e5969;
}
.total-price {
  font-size: 22px;
  font-weight: 700;
  color: #f53f3f;
}
.bar-submit {
  height: 40px;
  padding: 0 34px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  line-height: 40px;
  cursor: pointer;
}
.bar-submit.disabled {
  opacity: 0.5;
  pointer-events: none;
}
</style>
