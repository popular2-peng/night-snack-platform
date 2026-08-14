<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import {
  listAddressesApi, deleteAddressApi, setDefaultAddressApi, maskPhone,
  type AddressVO,
} from '@/apis/address'

const router = useRouter()

const addrs = ref<AddressVO[]>([])
const loading = ref(false)

/** 默认地址（头部卡片展示，is_default 字段驱动） */
const defaultAddr = computed(() => addrs.value.find((a) => a.isDefault) || null)
/** 列表：除默认外的其余地址 */
const otherAddrs = computed(() => addrs.value.filter((a) => !a.isDefault))

const fetchAddrs = async () => {
  loading.value = true
  try {
    const res = await listAddressesApi()
    addrs.value = res.data || []
  } catch { showToast('获取地址列表失败') }
  finally { loading.value = false }
}

const editAddr = (a?: AddressVO) => {
  router.push({ path: '/address/edit', query: a ? { id: String(a.id) } : {} })
}

/** 设为默认（非默认地址行内操作） */
const setDefault = async (a: AddressVO) => {
  try {
    await setDefaultAddressApi(a.id)
    showToast('已设为默认地址')
    fetchAddrs()
  } catch { /* 拦截器已提示 */ }
}

/** 左滑删除 */
const removeAddr = async (a: AddressVO) => {
  try {
    await showConfirmDialog({
      title: '删除地址',
      message: `确定删除「${a.detail}」？`,
      confirmButtonText: '删除',
      confirmButtonColor: '#FF4D4F',
    })
  } catch { return }
  try {
    await deleteAddressApi(a.id)
    showToast('已删除')
    fetchAddrs()
  } catch { /* 拦截器已提示 */ }
}

onMounted(fetchAddrs)
</script>

<template>
  <div class="address-page">
    <!-- 顶部导航 -->
    <van-nav-bar title="我的收货地址" left-arrow fixed placeholder @click-left="router.back()" />

    <div class="page-body" v-loading="loading">
      <!-- ====== 默认地址头部卡（is_default） ====== -->
      <div v-if="defaultAddr" class="default-card" @click="editAddr(defaultAddr)">
        <div class="card-head">
          <span class="default-badge">默认</span>
          <span class="loc-icon">📍</span>
          <span class="edit-text">修改 ></span>
        </div>
        <p class="detail-line">{{ defaultAddr.detail }}</p>
        <p class="people-line">{{ defaultAddr.receiver }}&nbsp;&nbsp;{{ maskPhone(defaultAddr.phone) }}</p>
      </div>
      <div v-else-if="!loading" class="default-card empty" @click="router.push('/address/edit')">
        <span class="loc-icon">📍</span>
        <p class="empty-text">暂无默认地址，点击新增</p>
        <span class="edit-text">新增 ></span>
      </div>

      <!-- ====== 地址列表（除默认外） ====== -->
      <div class="addr-list">
        <van-swipe-cell v-for="a in otherAddrs" :key="a.id">
          <div class="addr-item" @click="editAddr(a)">
            <p class="detail-line">{{ a.detail }}</p>
            <p class="people-line">{{ a.receiver }}&nbsp;&nbsp;{{ maskPhone(a.phone) }}</p>
            <div class="item-foot">
              <van-button size="mini" plain type="primary" @click.stop="setDefault(a)">设为默认</van-button>
              <span class="edit-link">编辑 ></span>
            </div>
          </div>
          <template #right>
            <van-button square type="danger" text="删除" class="swipe-del" @click="removeAddr(a)" />
          </template>
        </van-swipe-cell>

        <van-empty v-if="!loading && !addrs.length" description="还没有收货地址，快来添加吧">
          <template #image><span style="font-size: 52px">📍</span></template>
        </van-empty>
      </div>
    </div>

    <!-- 底部新增按钮 -->
    <div class="bottom-bar">
      <van-button type="primary" round block @click="router.push('/address/edit')">
        <template #icon><van-icon name="plus" style="margin-right: 4px" /></template>
        新增收货地址
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.address-page {
  min-height: 100vh;
  background: #F6F7F9;
}
.page-body {
  padding: 12px 12px 90px;
}

/* ---- 默认地址卡 ---- */
.default-card {
  position: relative;
  background: linear-gradient(135deg, #FFF8EE 0%, #FFEFD8 100%);
  border: 1px solid #FFE0B2;
  border-radius: 14px;
  padding: 14px 16px 16px;
  margin-bottom: 14px;
  box-shadow: 0 4px 14px rgba(245, 154, 35, 0.12);
  cursor: pointer;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.default-badge {
  background: linear-gradient(135deg, #F59A23, #E07F0E);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}
.loc-icon { font-size: 15px; }
.edit-text {
  margin-left: auto;
  font-size: 13px;
  color: #F59A23;
  font-weight: 600;
}
.detail-line {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #1D2129;
  line-height: 1.4;
  word-break: break-all;
}
.people-line {
  margin: 0;
  font-size: 13px;
  color: #4E5969;
}
.default-card.empty {
  display: flex;
  align-items: center;
  gap: 10px;
}
.empty-text {
  margin: 0;
  font-size: 14px;
  color: #86909C;
}

/* ---- 地址列表 ---- */
.addr-item {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}
.addr-item .detail-line { font-size: 15px; }
.addr-item .people-line { margin-bottom: 8px; }
.item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
  border-top: 1px dashed #F2F3F5;
}
.edit-link {
  font-size: 13px;
  color: #86909C;
}
.swipe-del {
  height: 100%;
  border-radius: 0 12px 12px 0;
}

/* ---- 底部新增 ---- */
.bottom-bar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
}
</style>
