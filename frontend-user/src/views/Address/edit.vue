<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import {
  getAddressApi, createAddressApi, updateAddressApi, deleteAddressApi,
  type AddressForm,
} from '@/apis/address'

const route = useRoute()
const router = useRouter()

const editingId = computed(() => {
  const id = Number(route.query.id)
  return Number.isFinite(id) && id > 0 ? id : null
})
const isEdit = computed(() => editingId.value !== null)

const form = reactive<AddressForm>({
  receiver: '',
  phone: '',
  detail: '',
  isDefault: false,
})

const saving = ref(false)

const PHONE_REGEX = /^1[3-9]\d{9}$/

const validate = () => {
  if (!form.receiver.trim()) { showToast('请输入收货人姓名'); return false }
  if (!PHONE_REGEX.test(form.phone)) { showToast('请输入正确的 11 位手机号'); return false }
  if (!form.detail.trim()) { showToast('请输入详细地址'); return false }
  return true
}

const handleSave = async () => {
  if (!validate()) return
  saving.value = true
  try {
    const payload: AddressForm = {
      receiver: form.receiver.trim(),
      phone: form.phone,
      detail: form.detail.trim(),
      isDefault: form.isDefault,
    }
    if (isEdit.value) {
      await updateAddressApi(editingId.value!, payload)
      showToast('地址已更新')
    } else {
      await createAddressApi(payload)
      showToast('地址已添加')
    }
    router.back()
  } catch { /* 拦截器已提示 */ }
  finally { saving.value = false }
}

const handleDelete = async () => {
  try {
    await showConfirmDialog({
      title: '删除地址',
      message: '确定删除该收货地址？',
      confirmButtonText: '删除',
      confirmButtonColor: '#FF4D4F',
    })
  } catch { return }
  try {
    await deleteAddressApi(editingId.value!)
    showToast('已删除')
    router.back()
  } catch { /* 拦截器已提示 */ }
}

onMounted(async () => {
  if (!isEdit.value) return
  try {
    const res = await getAddressApi(editingId.value!)
    form.receiver = res.data.receiver
    form.phone = res.data.phone
    form.detail = res.data.detail
    form.isDefault = res.data.isDefault
  } catch { showToast('获取地址失败'); router.back() }
})
</script>

<template>
  <div class="address-edit-page">
    <!-- 顶部导航 -->
    <van-nav-bar
      :title="isEdit ? '编辑收货地址' : '新增收货地址'"
      left-arrow fixed placeholder
      @click-left="router.back()"
    />

    <div class="edit-body">
      <van-cell-group inset class="form-group">
        <van-field
          v-model="form.receiver"
          label="收货人"
          placeholder="请输入收货人姓名"
          maxlength="20"
          clearable
        />
        <van-field
          v-model="form.phone"
          type="tel"
          label="手机号"
          placeholder="请输入 11 位手机号"
          maxlength="11"
          clearable
        />
        <van-field
          v-model="form.detail"
          type="textarea"
          label="详细地址"
          placeholder="街道、小区、楼栋、门牌号等"
          :autosize="{ minHeight: 60 }"
          maxlength="200"
          show-word-limit
        />
      </van-cell-group>

      <van-cell-group inset style="margin-top: 12px">
        <van-cell title="设为默认地址" center>
          <template #right-icon>
            <van-switch v-model="form.isDefault" size="22px" active-color="#F59A23" />
          </template>
        </van-cell>
      </van-cell-group>

      <p class="form-tip">设为默认后，下单时优先使用该地址配送</p>

      <van-button v-if="isEdit" round block plain type="danger" class="del-btn" @click="handleDelete">
        删除地址
      </van-button>
    </div>

    <!-- 底部保存 -->
    <div class="bottom-bar">
      <van-button type="primary" round block :loading="saving" @click="handleSave">
        保 存
      </van-button>
    </div>
  </div>
</template>

<style scoped>
.address-edit-page {
  min-height: 100vh;
  background: #F6F7F9;
}
.edit-body {
  padding: 12px 0 90px;
}
.form-group :deep(.van-field__label) {
  width: 72px;
  color: #4E5969;
  font-weight: 600;
}
.form-tip {
  margin: 10px 20px 0;
  font-size: 12px;
  color: #86909C;
}
.del-btn {
  margin: 20px 16px 0;
}
.bottom-bar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
}
</style>
