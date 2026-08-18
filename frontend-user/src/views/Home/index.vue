<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  listCategoriesApi,
  listMerchantsApi,
  listProductsApi,
  type CategoryVO,
  type HomeMerchant,
} from '@/apis/home'
import { listAddressesApi, type AddressVO } from '@/apis/address'
import { extractDistrict } from '@/utils/district'
import HomeHeader from '@/components/home/HomeHeader.vue'
import CategorySlider from '@/components/home/CategorySlider.vue'
// 商家列表（公共组件：首页与搜索页复用，含商品懒加载）
import MerchantList from '@/components/MerchantList.vue'

const router = useRouter()
const authStore = useAuthStore()

/** 默认地址区县 */
const district = ref('')
/** 分类列表 */
const categories = ref<CategoryVO[]>([])
/** 商家列表（含在售商品预览，商品懒加载） */
const merchants = ref<HomeMerchant[]>([])
/** 商家列表加载中 */
const loading = ref(false)
/** 当前选中的主营品类 ID（null=全部） */
const selectedCategoryId = ref<number | null>(null)

/** 默认收货地址 */
const defaultAddress = ref<AddressVO | null>(null)

/** 拉取默认地址并提取区县（未登录 / 无地址则留空） */
const fetchAddress = async () => {
  if (!authStore.isLoggedIn) return
  try {
    const res = await listAddressesApi()
    const addrs = res.data || []
    defaultAddress.value = addrs.find((a) => a.isDefault) || addrs[0] || null
    district.value = extractDistrict(defaultAddress.value?.detail)
  } catch {
    /* 未登录或无地址，忽略 */
  }
}

/** 拉取分类（匿名公开） */
const fetchCategories = async () => {
  try {
    const res = await listCategoriesApi()
    categories.value = res.data || []
  } catch {
    /* 拦截器已提示 */
  }
}

/** 拉取商家列表（可选按主营品类筛选）；商品懒加载：卡片进入视口时才请求 */
const fetchMerchants = async () => {
  loading.value = true
  try {
    const res = await listMerchantsApi({
      page: 1,
      size: 20,
      categoryId: selectedCategoryId.value ?? undefined,
    })
    const list = res.data?.list || []
    merchants.value = list.map((m) => ({ ...m, products: [], productsLoaded: false }))
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

/** 商品懒加载防重：正在请求中的商家 ID */
const loadingProducts = new Set<number>()

/**
 * 懒加载单个商家的在售商品（前 6 个）：
 * 由 MerchantList 的 IntersectionObserver 在卡片进入视口时触发；
 * productsLoaded 与 in-flight 双重防重，失败也置已加载避免反复重试。
 */
const loadProducts = async (m: HomeMerchant) => {
  if (m.productsLoaded || loadingProducts.has(m.id)) return
  loadingProducts.add(m.id)
  try {
    const pr = await listProductsApi(m.id, { page: 1, size: 6 })
    m.products = pr.data?.list || []
  } catch {
    /* 单个商家商品失败不影响整体，保留空预览 */
  } finally {
    m.productsLoaded = true
    loadingProducts.delete(m.id)
  }
}

const onGoAddress = () => router.push('/address')
/** 点击分类：按主营品类筛选商家（id=0 为「全部」，清除筛选） */
const onSelectCategory = (c: CategoryVO) => {
  selectedCategoryId.value = c.id === 0 ? null : c.id
  fetchMerchants()
}
const onMerchantClick = (m: HomeMerchant) => {
  // TODO: 跳转商家详情页（后续实现）
  void m
}

onMounted(async () => {
  fetchCategories()
  await fetchAddress()
  await fetchMerchants()
})
</script>

<template>
  <div class="home-page">
    <!-- 顶部（头部 + 分类滑条）吸顶 -->
    <div class="home-top">
      <HomeHeader :district="district" @go-address="onGoAddress" />

      <CategorySlider
        :categories="categories"
        :active-id="selectedCategoryId ?? 0"
        @select="onSelectCategory"
      />
    </div>

    <MerchantList
      :merchants="merchants"
      :loading="loading"
      @click="onMerchantClick"
      @visible="loadProducts"
    />
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f6f7f9;
}
.home-top {
  position: sticky;
  top: 0;
  z-index: 10;
}
</style>
