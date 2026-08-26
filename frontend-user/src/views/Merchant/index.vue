<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getMerchantDetailApi, listProductsApi,
  type MerchantVO, type ProductVO,
} from '@/apis/home'
import { addToCartApi } from '@/apis/cart'
import { useAuthStore } from '@/stores/auth'
import ShopHead from './components/ShopHead.vue'
import CategorySidebar from './components/CategorySidebar.vue'
import GoodsList, { type GoodsGroup } from './components/GoodsList.vue'
import SpecPopup from './components/SpecPopup.vue'

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

// ---- 规格弹窗与加购 ----
const specVisible = ref(false)
const specProduct = ref<ProductVO | null>(null)
const addLoading = ref(false)

/** 有规格的商品：点击「选规格」弹窗；无规格：直接 + 加购提示 */
const onAddClick = (p: ProductVO) => {
  if (!p.specs?.length) {
    showToast('该商品暂未配置规格，暂不可加购')
    return
  }
  specProduct.value = p
  specVisible.value = true
}

/** 加购：未登录先跳登录；已登录调购物车接口 */
const confirmAdd = async (specId: number, qty: number) => {
  if (!authStore.isLoggedIn) {
    specVisible.value = false
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  addLoading.value = true
  try {
    await addToCartApi(merchantId, specId, qty)
    showToast('已加入购物车')
    specVisible.value = false
  } catch { /* 拦截器已提示（跨店冲突/未登录） */ }
  finally { addLoading.value = false }
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
        @active-change="onActiveChange"
        @add="onAddClick"
      />
    </div>

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
