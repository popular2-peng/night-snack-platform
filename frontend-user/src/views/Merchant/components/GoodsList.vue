<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { ProductVO } from '@/apis/home'

/** 分类分组（名称 + 商品列表） */
export interface GoodsGroup {
  name: string
  list: ProductVO[]
}

const props = defineProps<{
  groups: GoodsGroup[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'active-change', idx: number): void
  (e: 'add', product: ProductVO): void
}>()

// ---- 滚动联动（scrollspy） ----
const goodsCol = ref<HTMLElement | null>(null)
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

/** groups 变化（数据加载完成/刷新）后重新测量区块位置 */
watch(
  () => props.groups,
  () => measureBlocks(),
  { deep: false },
)

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
  emit('active-change', idx)
}

/** 点击左分类：滚动到对应区块（scroll-margin-top 自动避开遮罩高度，商品不会被盖住） */
const scrollToBlock = (idx: number) => {
  scrollingByClick = true
  const blocks = goodsCol.value?.querySelectorAll<HTMLElement>('.cat-block')
  blocks?.[idx]?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  window.setTimeout(() => { scrollingByClick = false }, 400)
}

defineExpose({ scrollToBlock })

/** 商品价格展示：基础价 + 第一个规格加价后最低价 */
const minPrice = (p: ProductVO) => {
  const deltas = (p.specs || []).map((s) => Number(s.priceDelta || 0))
  const min = deltas.length ? Math.min(...deltas) : 0
  return '¥' + (Number(p.price) + min).toFixed(2)
}
</script>

<template>
  <!-- 右：商品列表（滚动容器，scrollspy 联动左分类） -->
  <div ref="goodsCol" class="goods-col" @scroll.passive="onGoodsScroll">
    <template v-if="groups.length">
      <!-- 美团式分组：每个分类区块自带吸顶标题，滚动到下一分类时被顶替替换 -->
      <div v-for="g in groups" :key="g.name" class="cat-block">
        <div class="cat-block-title">{{ g.name }}</div>
        <div v-for="p in g.list" :key="p.id" class="goods-row">
          <van-image :src="p.imageUrl" fit="cover" class="goods-img">
            <template #error>🍽️</template>
          </van-image>
          <div class="goods-info">
            <span class="goods-name">{{ p.name }}</span>
            <div class="goods-bottom">
              <span class="goods-price">{{ minPrice(p) }}</span>
              <span v-if="p.specs?.length" class="spec-btn" @click="emit('add', p)">选规格</span>
              <span v-else class="plus-btn" @click="emit('add', p)">+</span>
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
</template>

<style scoped>
.goods-col {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
.goods-col::-webkit-scrollbar {
  display: none;
}

/* 分类区块标题：组内吸顶（美团式）—— 当前分类标题固定在顶部；
   滚到下一分类时，下一块的标题从顶部进入并把上一块标题顶走（sticky 不重叠） */
.cat-block-title {
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

/* 底部提示 + 固定 700px 留白 */
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
  line-clamp: 2;
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
</style>
