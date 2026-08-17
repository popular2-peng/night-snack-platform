<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  listCategoriesApi,
  listMerchantsApi,
  listProductsApi,
  type CategoryVO,
  type HomeMerchant,
  type ProductVO,
} from '@/apis/home'
import { listAddressesApi, type AddressVO } from '@/apis/address'
import { extractDistrict } from '@/utils/district'
import HomeHeader from '@/components/home/HomeHeader.vue'
import CategorySlider from '@/components/home/CategorySlider.vue'
import MerchantList from '@/components/home/MerchantList.vue'

const router = useRouter()
const authStore = useAuthStore()

/** 默认地址区县 */
const district = ref('')
/** 分类列表 */
const categories = ref<CategoryVO[]>([])
/** 商家列表（含在售商品预览） */
const merchants = ref<HomeMerchant[]>([])
/** 商家列表加载中 */
const loading = ref(false)
/** 搜索关键词 */
const keyword = ref('')

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

/** 拉取附近商家，并为每个商家并发拉取前 3 个在售商品 */
const fetchMerchants = async () => {
  loading.value = true
  try {
    const res = await listMerchantsApi({ page: 1, size: 20 })
    const list = res.data?.list || []

    const withProducts = await Promise.all(
      list.map(async (m): Promise<HomeMerchant> => {
        let products: ProductVO[] = []
        try {
          const pr = await listProductsApi(m.id, { page: 1, size: 6 })
          products = pr.data?.list || []
        } catch {
          /* 单个商家商品失败不影响整体 */
        }
        return { ...m, products }
      }),
    )
    merchants.value = withProducts
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

/** 搜索过滤：按店铺名 / 在售商品名 */
const filteredMerchants = computed(() => {
  if (!keyword.value) return merchants.value
  const kw = keyword.value.toLowerCase()
  return merchants.value.filter(
    (m) =>
      m.name.toLowerCase().includes(kw) ||
      m.products.some((p) => p.name.toLowerCase().includes(kw)),
  )
})

const onSearch = (kw: string) => {
  keyword.value = kw
}
const onGoAddress = () => router.push('/address')
const onSelectCategory = (c: CategoryVO) => {
  // MVP：分类筛选商家需后端配合，暂仅记录
  void c
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
    <HomeHeader :district="district" @search="onSearch" @go-address="onGoAddress" />

    <CategorySlider :categories="categories" @select="onSelectCategory" />

    <MerchantList
      :merchants="filteredMerchants"
      :loading="loading"
      @click="onMerchantClick"
    />
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f6f7f9;
}
</style>
