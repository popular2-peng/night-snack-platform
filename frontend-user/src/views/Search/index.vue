<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  listMerchantsApi, searchProductsApi, listProductsApi,
  type HomeMerchant, type ProductVO,
} from '@/apis/home'
import MerchantList from '@/components/MerchantList.vue'

const router = useRouter()

const keyword = ref('')
/** 防抖计时器 */
let debounceTimer: number | null = null
/** 当前生效的搜索词（防抖后触发请求） */
const activeKeyword = ref('')

/** 当前 tab：merchant 商家 / product 商品 */
const activeTab = ref<'merchant' | 'product'>('merchant')

// ---- 商家结果（复用公共 MerchantList：含商品懒加载） ----
const merchants = ref<HomeMerchant[]>([])
const merchantLoading = ref(false)
const merchantSearched = ref(false)

const fetchMerchants = async () => {
  merchantLoading.value = true
  try {
    const res = await listMerchantsApi({ page: 1, size: 20, keyword: activeKeyword.value })
    merchants.value = (res.data?.list || []).map((m) => ({
      ...m, products: [], productsLoaded: false,
    }))
    merchantSearched.value = true
  } catch { showToast('搜索商家失败') }
  finally { merchantLoading.value = false }
}

/** 商品懒加载（复用首页逻辑：卡片进入视口才请求该商家商品） */
const loadingProducts = new Set<number>()
const loadProducts = async (m: HomeMerchant) => {
  if (m.productsLoaded || loadingProducts.has(m.id)) return
  loadingProducts.add(m.id)
  try {
    const pr = await listProductsApi(m.id, { page: 1, size: 6 })
    m.products = pr.data?.list || []
  } catch { /* 忽略 */ }
  finally {
    m.productsLoaded = true
    loadingProducts.delete(m.id)
  }
}

// ---- 商品结果（跨商家搜索 + 滚动分页） ----
const products = ref<ProductVO[]>([])
const productPage = ref(1)
const productTotal = ref(0)
const productLoading = ref(false)
const productFinished = ref(false)
const productSearched = ref(false)

const fetchProducts = async () => {
  if (productLoading.value || productFinished.value) return
  productLoading.value = true
  try {
    const res = await searchProductsApi({
      keyword: activeKeyword.value, page: productPage.value, size: 10,
    })
    const list = res.data?.list || []
    products.value = productPage.value === 1 ? list : [...products.value, ...list]
    productTotal.value = res.data?.total || 0
    productSearched.value = true
    if (products.value.length >= productTotal.value) {
      productFinished.value = true
    } else {
      productPage.value += 1
    }
  } catch { showToast('搜索商品失败'); productFinished.value = true }
  finally { productLoading.value = false }
}

const hasKeyword = computed(() => activeKeyword.value.trim().length > 0)

/** 输入防抖：300ms 后触发两类搜索（重新发起接口请求） */
const onKeywordChange = (kw: string) => {
  if (debounceTimer !== null) clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => {
    activeKeyword.value = kw.trim()
    if (!hasKeyword.value) {
      merchants.value = []
      products.value = []
      merchantSearched.value = false
      productSearched.value = false
      return
    }
    // 重置分页状态
    productPage.value = 1
    productFinished.value = false
    productSearched.value = false
    products.value = []
    fetchMerchants()
    fetchProducts()
  }, 300)
}

const onMerchantClick = () => {
  // TODO: 跳转商家详情页（后续实现）
}
const onProductClick = (p: ProductVO) => {
  // TODO: 跳转商品/商家详情页（后续实现）
  void p
}
</script>

<template>
  <div class="search-page">
    <!-- 顶部搜索栏（自动聚焦，实时防抖搜索） -->
    <van-search
      v-model="keyword"
      placeholder="搜索店铺 / 美食"
      shape="round"
      autofocus
      show-action
      @update:model-value="onKeywordChange"
    >
      <template #action>
        <span class="cancel-btn" @click="router.back()">取消</span>
      </template>
    </van-search>

    <!-- 结果 Tab -->
    <van-tabs v-model:active="activeTab" class="result-tabs">
      <!-- ====== 商家结果 ====== -->
      <van-tab title="商家" name="merchant">
        <template v-if="hasKeyword">
          <MerchantList
            :merchants="merchants"
            :loading="merchantLoading"
            @click="onMerchantClick"
            @visible="loadProducts"
          />
        </template>
        <div v-else class="guide">
          <span class="guide-icon">🔍</span>
          <p>输入关键词搜索店铺或美食</p>
        </div>
      </van-tab>

      <!-- ====== 商品结果 ====== -->
      <van-tab title="商品" name="product">
        <template v-if="hasKeyword">
          <van-list
            v-model:loading="productLoading"
            :finished="productFinished"
            finished-text="没有更多了"
            @load="fetchProducts"
          >
            <div v-for="p in products" :key="p.id" class="product-row" @click="onProductClick(p)">
              <van-image :src="p.imageUrl" fit="cover" class="product-img">
                <template #error>🍽️</template>
              </van-image>
              <div class="product-info">
                <span class="product-name">{{ p.name }}</span>
                <span class="product-shop">{{ p.merchantName || '-' }}</span>
                <span class="product-price">¥{{ p.price }}</span>
              </div>
            </div>
          </van-list>
          <van-empty v-if="productSearched && !products.length && !productLoading" description="没有找到相关商品" />
        </template>
        <div v-else class="guide">
          <span class="guide-icon">🍜</span>
          <p>输入关键词搜索店铺或美食</p>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<style scoped>
.search-page {
  min-height: 100vh;
  background: #f6f7f9;
  padding-bottom: 20px;
}
.cancel-btn {
  color: #f59a23;
  font-size: 14px;
}
.result-tabs {
  --van-tabs-bottom-bar-color: #f59a23;
}

/* 引导态 */
.guide {
  padding: 80px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #86909c;
  font-size: 14px;
}
.guide-icon {
  font-size: 48px;
}

/* 商品行 */
.product-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 0 12px 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
}
.product-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #f2f3f5;
  flex-shrink: 0;
  overflow: hidden;
  font-size: 28px;
}
.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.product-shop {
  font-size: 12px;
  color: #86909c;
}
.product-price {
  font-size: 15px;
  font-weight: 700;
  color: #f53f3f;
}
</style>
