<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import {
  getMerchantDetailApi, listProductsApi,
  type MerchantVO, type ProductVO,
} from '@/apis/home'
import { addToCartApi } from '@/apis/cart'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const merchantId = Number(route.params.id)

const shop = ref<MerchantVO | null>(null)
const products = ref<ProductVO[]>([])
const loading = ref(false)

/** 按商品分类分组的商品（分类顺序 = 商品出现顺序） */
const groups = computed(() => {
  const map = new Map<string, ProductVO[]>()
  for (const p of products.value) {
    const key = p.categoryName || '未分类'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(p)
  }
  return [...map.entries()].map(([name, list]) => ({ name, list }))
})

const activeIndex = ref(0)

// ---- 滚动联动（scrollspy） ----
const goodsCol = ref<HTMLElement | null>(null)
const catCol = ref<HTMLElement | null>(null)
/** 遮罩条高度（sticky 分类头） */
const MASK_HEIGHT = 36
/** 手动点击左分类滚动中：忽略 scroll 事件避免回跳 */
let scrollingByClick = false

/** 各分类区块顶部偏移（相对右列滚动容器） */
const blockTops = ref<number[]>([])

const measureBlocks = () => {
  nextTick(() => {
    const blocks = goodsCol.value?.querySelectorAll<HTMLElement>('.cat-block')
    blockTops.value = blocks ? [...blocks].map((b) => b.offsetTop) : []
  })
}

/** 右列滚动：计算当前激活分类（最后一个顶部 <= scrollTop + 遮罩高度 的分类） */
const onGoodsScroll = () => {
  if (scrollingByClick) return
  const el = goodsCol.value
  if (!el || !blockTops.value.length) return
  const pos = el.scrollTop + MASK_HEIGHT
  let idx = 0
  blockTops.value.forEach((top, i) => {
    if (top <= pos) idx = i
  })
  if (idx !== activeIndex.value) {
    activeIndex.value = idx
    // 左栏自动滚动到对应分类
    const catItems = catCol.value?.querySelectorAll<HTMLElement>('.cat-item')
    const target = catItems?.[idx]
    if (target && catCol.value) {
      const targetTop = target.offsetTop - catCol.value.clientHeight / 2 + target.clientHeight / 2
      catCol.value.scrollTo({ top: Math.max(0, targetTop), behavior: 'smooth' })
    }
  }
}

/** 点击左分类：右列滚动到对应区块（scroll-margin-top 自动避开遮罩高度，商品不会被盖住） */
const onSelectCat = (idx: number) => {
  activeIndex.value = idx
  scrollingByClick = true
  const blocks = goodsCol.value?.querySelectorAll<HTMLElement>('.cat-block')
  blocks?.[idx]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  window.setTimeout(() => { scrollingByClick = false }, 400)
}

// ---- 规格弹窗与加购 ----
const specVisible = ref(false)
const specProduct = ref<ProductVO | null>(null)
const specSelected = ref<number | null>(null)
const specQty = ref(1)

/** 有规格的商品：点击「选规格」弹窗；无规格：直接 + 加购提示 */
const onAddClick = (p: ProductVO) => {
  if (!p.specs?.length) {
    showToast('该商品暂未配置规格，暂不可加购')
    return
  }
  specProduct.value = p
  specSelected.value = p.specs[0]?.id ?? null
  specQty.value = 1
  specVisible.value = true
}

const addLoading = ref(false)
const confirmAdd = async () => {
  if (!authStore.isLoggedIn) {
    specVisible.value = false
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (specSelected.value == null) {
    showToast('请选择规格')
    return
  }
  addLoading.value = true
  try {
    await addToCartApi(merchantId, specSelected.value, specQty.value)
    showToast('已加入购物车')
    specVisible.value = false
  } catch { /* 拦截器已提示（跨店冲突/未登录） */ }
  finally { addLoading.value = false }
}

/** 商品价格展示：基础价 + 第一个规格加价后最低价 */
const minPrice = (p: ProductVO) => {
  const deltas = (p.specs || []).map((s) => Number(s.priceDelta || 0))
  const min = deltas.length ? Math.min(...deltas) : 0
  return '¥' + (Number(p.price) + min).toFixed(2)
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
  finally {
    loading.value = false
    measureBlocks()
  }
})
</script>

<template>
  <div class="merchant-page">
    <van-nav-bar
      :title="shop?.name || '店铺'"
      left-arrow fixed placeholder
      @click-left="router.back()"
    />

    <!-- ====== 头部卡片（内边距 20px） ====== -->
    <div class="shop-head" v-loading="loading">
      <template v-if="shop">
        <van-image :src="shop.logoUrl" fit="cover" class="head-logo">
          <template #error>🏪</template>
        </van-image>
        <div class="head-info">
          <h2 class="head-name">{{ shop.name }}</h2>
          <div class="head-sub">
            <span class="head-rating">
              <van-icon name="star" color="#f59a23" size="12" />
              {{ shop.avgRating != null ? Number(shop.avgRating).toFixed(1) : '暂无' }}
            </span>
            <!-- 公告：一行显示，超出屏幕横向滚动（marquee） -->
            <div class="notice-wrap">
              <div class="notice-text" :class="{ marquee: (shop.notice || '').length > 12 }">
                {{ shop.notice || '本店暂无公告' }}
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- ====== 下部卡片：左分类 + 右商品 ====== -->
    <div class="shop-body">
      <!-- 左：分类列表（区域内可滑动） -->
      <div ref="catCol" class="cat-col">
        <div
          v-for="(g, i) in groups"
          :key="g.name"
          class="cat-item"
          :class="{ active: activeIndex === i }"
          @click="onSelectCat(i)"
        >
          {{ g.name }}
        </div>
      </div>

      <!-- 右：商品列表（滚动容器，scrollspy 联动左分类） -->
      <div ref="goodsCol" class="goods-col" @scroll.passive="onGoodsScroll">
        <!-- 固定分类标题：始终吸顶（含最后一个分类），随滚动切换文案 -->
        <div v-if="groups.length" class="goods-title">{{ groups[activeIndex]?.name }}</div>
        <template v-if="groups.length">
          <div v-for="g in groups" :key="g.name" class="cat-block">
            <div v-for="p in g.list" :key="p.id" class="goods-row">
              <van-image :src="p.imageUrl" fit="cover" class="goods-img">
                <template #error>🍽️</template>
              </van-image>
              <div class="goods-info">
                <span class="goods-name">{{ p.name }}</span>
                <div class="goods-bottom">
                  <span class="goods-price">{{ minPrice(p) }}</span>
                  <span v-if="p.specs?.length" class="spec-btn" @click="onAddClick(p)">选规格</span>
                  <span v-else class="plus-btn" @click="onAddClick(p)">+</span>
                </div>
              </div>
            </div>
          </div>
          <!-- 底部提示 + 留白（最后一个分类遮罩固定后，留白只需让最后商品完整可见） -->
          <div class="goods-end">
            <span class="end-line" />
            <span class="end-text">已经到底啦 🍜</span>
            <span class="end-line" />
          </div>
        </template>
        <van-empty v-else-if="!loading" description="暂无在售商品" />
      </div>
    </div>

    <!-- ====== 规格选择弹窗 ====== -->
    <van-popup v-model:show="specVisible" position="bottom" round safe-area-inset-bottom>
      <div class="spec-pop">
        <div class="spec-head">
          <van-image :src="specProduct?.imageUrl" fit="cover" class="spec-img">
            <template #error>🍽️</template>
          </van-image>
          <div class="spec-title">
            <p class="spec-name">{{ specProduct?.name }}</p>
            <p class="spec-price">{{ specProduct ? minPrice(specProduct) : '' }}</p>
          </div>
          <van-icon name="cross" class="spec-close" @click="specVisible = false" />
        </div>

        <p class="spec-label">规格</p>
        <div class="spec-list">
          <span
            v-for="s in specProduct?.specs || []"
            :key="s.id"
            class="spec-chip"
            :class="{ active: specSelected === s.id }"
            @click="specSelected = s.id!"
          >
            {{ s.name }}{{ Number(s.priceDelta) > 0 ? ` +¥${s.priceDelta}` : '' }}
          </span>
        </div>

        <div class="spec-qty">
          <span class="spec-label">数量</span>
          <van-stepper v-model="specQty" :min="1" :max="99" theme="round" button-size="24" />
        </div>

        <van-button type="primary" block round class="spec-confirm" :loading="addLoading" @click="confirmAdd">
          加入购物车
        </van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.merchant-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f6f7f9;
}

/* ---- 头部卡片（内边距 20px） ---- */
.shop-head {
  display: flex;
  gap: 14px;
  margin: 0 10px;
  padding: 20px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
}
.head-logo {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 34px;
}
.head-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}
.head-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.head-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.head-rating {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #f59a23;
  font-weight: 600;
}

/* 公告：一行显示，超出屏幕横向滚动 */
.notice-wrap {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  position: relative;
}
.notice-text {
  display: inline-block;
  font-size: 12px;
  color: #86909c;
  white-space: nowrap;
}
.notice-text.marquee {
  animation: notice-scroll 10s linear infinite;
  padding-right: 40px;
}
@keyframes notice-scroll {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

/* ---- 下部卡片：左分类 + 右商品 ---- */
.shop-body {
  flex: 1;
  min-height: 0;
  display: flex;
  margin: 10px 10px 0;
  background: #fff;
  border-radius: 14px 14px 0 0;
  overflow: hidden;
}

/* 左分类列（区域内可滑动） */
.cat-col {
  width: 88px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #f6f7f9;
  padding: 4px 0 20px;
}
.cat-col::-webkit-scrollbar {
  display: none;
}
.cat-item {
  position: relative;
  padding: 14px 8px;
  font-size: 13px;
  color: #4e5969;
  text-align: center;
  cursor: pointer;
  word-break: break-all;
}
.cat-item.active {
  background: #fff;
  color: #f0810d;
  font-weight: 700;
}
.cat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 0 3px 3px 0;
  background: linear-gradient(180deg, #f59a23, #ff6f3c);
}

/* 右商品列 */
.goods-col {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
.goods-col::-webkit-scrollbar {
  display: none;
}

/* 固定分类标题：始终吸顶（含最后一个分类，不会被划走），文案随滚动切换 */
.goods-title {
  position: sticky;
  top: 0;
  z-index: 3;
  height: 36px;
  line-height: 36px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 700;
  color: #4e5969;
  background: #fafbfc;
  border-bottom: 1px solid #f2f3f5;
}

/* 底部提示 + 固定 500px 留白（试效果） */
.goods-end {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 20px 700px;
  color: #c9cdd4;
  font-size: 12px;
}
.end-line {
  flex: 1;
  height: 1px;
  background: #f2f3f5;
}
.end-text {
  flex-shrink: 0;
}

/* 商品行：左图右（上名称 / 下价格+按钮） */
.goods-row {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
}
.goods-img {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  flex-shrink: 0;
  background: #f2f3f5;
  overflow: hidden;
  font-size: 30px;
}
.goods-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.goods-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.goods-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.goods-price {
  font-size: 16px;
  font-weight: 700;
  color: #f53f3f;
}
/* 选规格按钮 */
.spec-btn {
  flex-shrink: 0;
  padding: 5px 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(245, 154, 35, 0.3);
}
/* + 按钮 */
.plus-btn {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  color: #fff;
  font-size: 18px;
  line-height: 26px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(245, 154, 35, 0.3);
}

/* ---- 规格弹窗 ---- */
.spec-pop {
  padding: 20px 20px calc(16px + env(safe-area-inset-bottom));
}
.spec-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.spec-img {
  width: 72px;
  height: 72px;
  border-radius: 12px;
  flex-shrink: 0;
  background: #f2f3f5;
  font-size: 30px;
  overflow: hidden;
}
.spec-title {
  flex: 1;
  min-width: 0;
}
.spec-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #1d2129;
}
.spec-price {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #f53f3f;
}
.spec-close {
  font-size: 18px;
  color: #a9aeb8;
  cursor: pointer;
}
.spec-label {
  margin: 16px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
}
.spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.spec-chip {
  padding: 7px 14px;
  border-radius: 16px;
  background: #f6f7f9;
  color: #4e5969;
  font-size: 13px;
  cursor: pointer;
}
.spec-chip.active {
  background: #fff4e6;
  color: #f0810d;
  font-weight: 600;
  border: 1px solid #f59a23;
}
.spec-qty {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.spec-qty .spec-label {
  margin: 16px 0 0;
}
.spec-confirm {
  margin-top: 18px;
  background: linear-gradient(135deg, #f59a23, #ff6f3c);
  border: none;
}
</style>
