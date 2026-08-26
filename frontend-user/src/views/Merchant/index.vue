<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getMerchantDetailApi, listProductsApi,
  type MerchantVO, type ProductVO,
} from '@/apis/home'
import {
  addToCartApi, getCartApi, updateCartQtyApi, removeCartItemApi, clearCartApi,
  type CartItem,
} from '@/apis/cart'
import { useAuthStore } from '@/stores/auth'
import ShopHead from './components/ShopHead.vue'
import CategorySidebar from './components/CategorySidebar.vue'
import GoodsList, { type GoodsGroup } from './components/GoodsList.vue'
import SpecPopup from './components/SpecPopup.vue'
import CartBar from './components/CartBar.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const merchantId = Number(route.params.id)

// ---- 数据 ----
const shop = ref<MerchantVO | null>(null)
const products = ref<ProductVO[]>([])
const loading = ref(false)

/** 按商品分类分组的商品（分类顺序 = 商品出现顺序） */
const groups = computed<GoodsGroup[]>(() => {
  const map = new Map<string, ProductVO[]>()
  for (const p of products.value) {
    const key = p.categoryName || '未分类'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(p)
  }
  return [...map.entries()].map(([name, list]) => ({ name, list }))
})

/** 左栏分类名列表（传给 CategorySidebar） */
const cats = computed(() => groups.value.map((g) => g.name))

// ---- 分类联动协调（scrollspy 在 GoodsList 内部，状态提升到此处） ----
const activeIndex = ref(0)
const goodsListRef = ref<InstanceType<typeof GoodsList> | null>(null)

/** 右列滚动时左栏高亮切换 */
const onActiveChange = (idx: number) => { activeIndex.value = idx }

/** 点击左分类：委托 GoodsList 滚动到对应区块 */
const onSelectCat = (idx: number) => {
  activeIndex.value = idx
  goodsListRef.value?.scrollToBlock(idx)
}

// ---- 购物车状态 ----
const cartItems = ref<CartItem[]>([])

/** 刷新购物车（登录后才请求） */
const refreshCart = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const res = await getCartApi()
    cartItems.value = res.data?.items || []
  } catch { /* 静默：购物车失败不阻塞点餐页 */ }
}

/** 商品 ID → 购物车总数量（>0 时商品行显示步进器） */
const qtyMap = computed<Record<number, number>>(() => {
  const m: Record<number, number> = {}
  for (const it of cartItems.value) m[it.productId] = (m[it.productId] || 0) + it.quantity
  return m
})

/** 有规格商品记住本会话最后选的规格（+ 按钮直接加该规格） */
const lastSpec = new Map<number, number>()

/** 未登录统一处理：跳登录并带上回跳地址 */
const requireLogin = (): boolean => {
  if (authStore.isLoggedIn) return true
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

// ---- 规格弹窗与加购 ----
const specVisible = ref(false)
const specProduct = ref<ProductVO | null>(null)
const addLoading = ref(false)

/** 商品点击：已在购物车 → 由步进器处理；未加购 → 有规格弹窗 / 无规格直接 +1 */
const onAddClick = (p: ProductVO) => {
  if (qtyMap.value[p.id]) {
    plusItem(p)
    return
  }
  if (!p.specs?.length) {
    directAdd(p)
    return
  }
  specProduct.value = p
  specVisible.value = true
}

/** 无规格商品：直接加购（按商品基础价） */
const directAdd = async (p: ProductVO) => {
  if (!requireLogin()) return
  try {
    await addToCartApi(merchantId, p.id, null, 1)
    refreshCart()
  } catch { /* 拦截器已提示（跨店冲突/未登录） */ }
}

/** 加购：未登录先跳登录；已登录调购物车接口 */
const confirmAdd = async (specId: number, qty: number) => {
  if (!requireLogin()) {
    specVisible.value = false
    return
  }
  addLoading.value = true
  try {
    await addToCartApi(merchantId, specProduct.value!.id, specId, qty)
    lastSpec.set(specProduct.value!.id, specId) // 记住规格：之后 + 直接加该规格
    specVisible.value = false
    refreshCart()
  } catch { /* 拦截器已提示（跨店冲突/未登录） */ }
  finally { addLoading.value = false }
}

/** 步进器 +：有规格商品用已记住的规格（未选过则弹规格窗），无规格 +1 */
const plusItem = async (p: ProductVO) => {
  if (!requireLogin()) return
  if (p.specs?.length) {
    const specId = lastSpec.get(p.id)
    if (specId == null) {
      onAddClick(p) // 弹窗选规格
      return
    }
    await addToCartApi(merchantId, p.id, specId, 1)
  } else {
    await addToCartApi(merchantId, p.id, null, 1)
  }
  refreshCart()
}

/** 步进器 -：找到该商品当前条目减 1，减到 0 移除 */
const minusItem = async (p: ProductVO) => {
  if (!requireLogin()) return
  const key = p.specs?.length && lastSpec.has(p.id)
    ? String(lastSpec.get(p.id))
    : 'p' + p.id
  const item = cartItems.value.find((i) => i.cartKey === key)
  if (!item) return
  const newQty = item.quantity - 1
  try {
    if (newQty <= 0) await removeCartItemApi(key)
    else await updateCartQtyApi(key, newQty)
    refreshCart()
  } catch { /* 拦截器已提示 */ }
}

/** 购物车面板内加减 */
const onCartUpdate = async (cartKey: string, newQty: number) => {
  try {
    if (newQty <= 0) await removeCartItemApi(cartKey)
    else await updateCartQtyApi(cartKey, newQty)
    refreshCart()
  } catch { /* 拦截器已提示 */ }
}

/** 清空购物车 */
const onCartClear = async () => {
  try {
    await clearCartApi()
    refreshCart()
  } catch { /* 拦截器已提示 */ }
}

/** 去结算（下单确认页待开发，先提示） */
const onCheckout = () => {
  showToast('结算功能开发中，敬请期待')
}

onMounted(async () => {
  loading.value = true
  try {
    const [shopRes, prodRes] = await Promise.all([
      getMerchantDetailApi(merchantId),
      listProductsApi(merchantId, { page: 1, size: 100 }),
    ])
    shop.value = shopRes.data
    products.value = prodRes.data?.list || []
  } catch { showToast('获取店铺信息失败') }
  finally { loading.value = false }
  refreshCart()
})
</script>

<template>
  <div class="merchant-page">
    <van-nav-bar
      :title="shop?.name || '店铺'"
      left-arrow fixed placeholder
      @click-left="router.back()"
    />

    <!-- 头部卡片 -->
    <ShopHead :shop="shop" :loading="loading" />

    <!-- 下部卡片：左分类 + 右商品 -->
    <div class="shop-body">
      <!-- 左：分类列表（区域内可滑动，联动高亮） -->
      <CategorySidebar :cats="cats" :active-index="activeIndex" @select="onSelectCat" />

      <!-- 右：商品列表（滚动容器，scrollspy 联动左分类） -->
      <GoodsList
        ref="goodsListRef"
        :groups="groups"
        :loading="loading"
        :qty-map="qtyMap"
        @active-change="onActiveChange"
        @add="onAddClick"
        @minus="minusItem"
        @plus="plusItem"
      />
    </div>

    <!-- 底部购物车栏（当前商家有购物车商品时显示） -->
    <CartBar
      :items="cartItems"
      :merchant-name="shop?.name || ''"
      @update="onCartUpdate"
      @clear="onCartClear"
      @checkout="onCheckout"
    />

    <!-- 规格选择弹窗 -->
    <SpecPopup
      v-model:visible="specVisible"
      :product="specProduct"
      :add-loading="addLoading"
      @confirm="confirmAdd"
    />
  </div>
</template>

<style scoped>
.merchant-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f7f9;
}

/* 下部卡片：左分类 + 右商品 */
.shop-body {
  flex: 1;
  min-height: 0;
  display: flex;
  margin: 10px 10px 0;
  background: #fff;
  border-radius: 14px 14px 0 0;
  overflow: hidden;
}
</style>
