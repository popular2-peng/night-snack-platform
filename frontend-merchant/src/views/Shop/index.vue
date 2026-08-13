<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Shop, Phone, Location, Clock, Position, Document, Bell, Picture, SwitchButton,
} from '@element-plus/icons-vue'
import { getShopApi, updateShopApi, updateShopStatusApi } from '@/apis/shop'
import { uploadSignApi, uploadFileToOss } from '@/apis/upload'

// ---- 表单状态（与后端 MerchantShopUpdateDTO 对齐） ----
const form = reactive({
  name: '',
  logoUrl: '',
  address: '',
  businessHoursStart: '',
  businessHoursEnd: '',
  deliveryRadius: 5000,
  description: '',
  notice: '',
})

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const shop = ref<any>({})
const originAddress = ref('')

const rules: FormRules = {
  name: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 20, message: '店铺名称 2-20 字', trigger: 'blur' },
  ],
  businessHoursStart: [{ pattern: /^([01]\d|2[0-3]):[0-5]\d$/, message: '时间格式 HH:mm', trigger: 'change' }],
  businessHoursEnd: [{ pattern: /^([01]\d|2[0-3]):[0-5]\d$/, message: '时间格式 HH:mm', trigger: 'change' }],
  address: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 200, message: '详细地址最多 200 字', trigger: 'blur' },
  ],
}

const statusLabel = computed(() => (shop.value.status === 'CLOSED' ? '已打烊' : shop.value.status === 'OPEN' ? '营业中' : shop.value.status))
const statusTagType = computed(() => (shop.value.status === 'OPEN' ? 'success' : 'warning'))

/** 拉取店铺信息并回填表单 */
const fetchShop = async () => {
  loading.value = true
  try {
    const res = await getShopApi()
    shop.value = res.data || {}
    form.name = shop.value.name || ''
    form.logoUrl = shop.value.logoUrl || ''
    form.address = shop.value.address || ''
    originAddress.value = shop.value.address || ''
    form.businessHoursStart = shop.value.businessHoursStart || ''
    form.businessHoursEnd = shop.value.businessHoursEnd || ''
    form.deliveryRadius = shop.value.deliveryRadius ?? 5000
    form.description = shop.value.description || ''
    form.notice = shop.value.notice || ''
  } catch { ElMessage.error('获取店铺信息失败') }
  finally { loading.value = false }
}

// ---- LOGO 上传（OSS 签名直传，与入驻申请同链路） ----
const uploadingLogo = ref(false)
const handleLogoUpload = async (options: UploadRequestOptions) => {
  const file = options.file as File
  uploadingLogo.value = true
  try {
    const signRes = await uploadSignApi('SHOP_LOGO', file.name)
    const url = await uploadFileToOss(signRes.data, file)
    form.logoUrl = url
    ElMessage.success('LOGO 上传成功，保存后生效')
  } catch (e: any) {
    ElMessage.error(e?.message || 'LOGO 上传失败')
  } finally { uploadingLogo.value = false }
}

// ---- 营业状态切换（二次确认） ----
const beforeStatusChange = async (): Promise<boolean> => {
  const target = shop.value.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  const action = target === 'OPEN' ? '开始营业' : '打烊'
  try {
    await ElMessageBox.confirm(
      target === 'OPEN' ? '确定开始营业吗？用户端将展示你的店铺。' : '确定打烊吗？打烊后用户无法下单。',
      action, { type: 'warning', confirmButtonText: action })
  } catch { return false }
  try {
    await updateShopStatusApi(target)
    ElMessage.success(target === 'OPEN' ? '已开始营业' : '已打烊')
    await fetchShop()
    return true
  } catch { return false }
}

/** 重置为后端当前值 */
const handleReset = () => { fetchShop() }

/** 保存修改（全字段提交，后端仅更新非空字段） */
const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if ((form.businessHoursStart && !form.businessHoursEnd) || (!form.businessHoursStart && form.businessHoursEnd)) {
    ElMessage.warning('营业开始与结束时间需同时填写')
    return
  }
  saving.value = true
  try {
    const res = await updateShopApi({
      name: form.name.trim(),
      logoUrl: form.logoUrl || undefined,
      address: form.address.trim(),
      businessHoursStart: form.businessHoursStart || undefined,
      businessHoursEnd: form.businessHoursEnd || undefined,
      deliveryRadius: form.deliveryRadius,
      description: form.description || undefined,
      notice: form.notice || undefined,
    })
    ElMessage.success('店铺信息已保存')
    shop.value = res.data || {}
    originAddress.value = shop.value.address || ''
    form.address = shop.value.address || ''
  } catch { /* 拦截器已提示 */ }
  finally { saving.value = false }
}

/** 地址是否已修改（提示将重新解析坐标） */
const addressChanged = computed(() => form.address.trim() !== originAddress.value)

onMounted(fetchShop)
</script>

<template>
  <div class="shop-page" v-loading="loading">
    <!-- ====== 顶部品牌卡 ====== -->
    <div class="shop-hero fade-in-scale">
      <div class="hero-deco deco-1" />
      <div class="hero-deco deco-2" />
      <div class="hero-content">
        <el-upload
          class="hero-logo-upload"
          accept=".jpg,.jpeg,.png"
          :show-file-list="false"
          :http-request="handleLogoUpload"
        >
          <div class="hero-logo" v-loading="uploadingLogo">
            <img v-if="form.logoUrl" :src="form.logoUrl" alt="店铺LOGO" class="hero-logo-img" />
            <el-icon v-else :size="26"><Shop /></el-icon>
            <span class="hero-logo-mask"><el-icon><Picture /></el-icon> 更换</span>
          </div>
        </el-upload>
        <div class="hero-info">
          <div class="hero-name-row">
            <h2>{{ form.name || '未命名店铺' }}</h2>
            <el-tag :type="statusTagType" effect="dark" round size="small">{{ statusLabel }}</el-tag>
          </div>
          <p class="hero-sub">
            <el-icon><Phone /></el-icon>{{ shop.phone || '-' }}
            <span class="hero-divider">·</span>
            <el-icon><Location /></el-icon>{{ form.address || '未设置地址' }}
          </p>
        </div>
        <div class="hero-switch">
          <span class="switch-label"><el-icon><SwitchButton /></el-icon> 营业状态</span>
          <el-switch
            :model-value="shop.status === 'OPEN'"
            :before-change="beforeStatusChange"
            style="--el-switch-on-color: #52C41A; --el-switch-off-color: #C9CDD4"
          />
        </div>
      </div>
    </div>

    <!-- ====== 表单 ====== -->
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="shop-form">
      <div class="form-grid">
        <!-- 基本信息 -->
        <div class="form-card fade-in">
          <div class="card-title"><el-icon><Shop /></el-icon> 基本信息</div>
          <el-form-item label="店铺名称" prop="name">
            <el-input v-model="form.name" maxlength="20" show-word-limit placeholder="请输入店铺名称（2-20 字）" />
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200"
              show-word-limit placeholder="招牌菜、店铺特色……将展示在用户端店铺页" />
          </el-form-item>
          <el-form-item label="店铺公告">
            <el-input v-model="form.notice" type="textarea" :rows="3" maxlength="200"
              show-word-limit placeholder="如：今日休息、新菜品上线……将展示在用户端店铺页" />
          </el-form-item>
        </div>

        <!-- 经营信息 -->
        <div class="form-card fade-in">
          <div class="card-title"><el-icon><Clock /></el-icon> 经营信息</div>
          <el-form-item label="营业时间" class="hours-item">
            <div class="hours-row">
              <el-time-picker
                v-model="form.businessHoursStart"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="开始时间"
                style="width: 140px"
              />
              <span class="hours-sep">至</span>
              <el-time-picker
                v-model="form.businessHoursEnd"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="结束时间"
                style="width: 140px"
              />
            </div>
            <p class="field-tip">支持跨天营业（如 18:00 至 02:00 次日凌晨）；下单时服务端按营业时间校验</p>
          </el-form-item>
          <el-form-item label="配送半径（米）">
            <el-input-number v-model="form.deliveryRadius" :min="500" :max="10000" :step="100"
              controls-position="right" style="width: 200px" />
            <p class="field-tip">范围 500 - 10000 米，默认 5000 米；超出该范围的用户无法下单</p>
          </el-form-item>
          <el-form-item label="详细地址" prop="address">
            <el-input v-model="form.address" maxlength="200" placeholder="请输入店铺详细地址" />
            <p class="field-tip" :class="{ 'tip-warn': addressChanged }">
              <el-icon><Position /></el-icon>
              {{ addressChanged ? '地址已修改，保存后将自动调用百度地图重新解析坐标（解析失败保留原坐标）' : '修改地址后系统会自动重新解析店铺坐标，用于配送范围校验' }}
            </p>
          </el-form-item>
        </div>
      </div>

      <!-- 底部操作 -->
      <div class="form-actions">
        <span class="actions-note"><el-icon><Document /></el-icon> 管理员仅可查看，以上信息由你自行维护</span>
        <el-button size="large" @click="handleReset">重置</el-button>
        <el-button size="large" type="primary" :loading="saving" @click="handleSave">
          <el-icon style="margin-right: 4px"><Bell /></el-icon>保存修改
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.shop-page { max-width: 1080px; margin: 0 auto; }

/* ---- 品牌卡 ---- */
.shop-hero {
  position: relative;
  border-radius: var(--border-radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #2A2016 0%, #3D2E18 55%, #F59A23 130%);
  box-shadow: var(--shadow-md);
}
.hero-deco { position: absolute; border-radius: 50%; background: radial-gradient(circle, rgba(245, 154, 35, 0.28), transparent 70%); }
.deco-1 { width: 300px; height: 300px; top: -120px; right: -60px; }
.deco-2 { width: 200px; height: 200px; bottom: -100px; left: 30%; }
.hero-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 26px 28px;
}
.hero-logo {
  width: 84px; height: 84px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  border: 2px solid rgba(255, 255, 255, 0.35);
  display: flex; align-items: center; justify-content: center;
  color: rgba(255, 255, 255, 0.85);
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: transform var(--transition-fast);
}
.hero-logo:hover { transform: scale(1.04); }
.hero-logo-img { width: 100%; height: 100%; object-fit: cover; }
.hero-logo-mask {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center; gap: 4px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff; font-size: 12px;
  opacity: 0; transition: opacity var(--transition-fast);
}
.hero-logo:hover .hero-logo-mask { opacity: 1; }
.hero-info { flex: 1; min-width: 0; }
.hero-name-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.hero-name-row h2 {
  margin: 0; font-size: 22px; font-weight: 700; color: #fff;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hero-sub {
  margin: 0; font-size: 13px; color: rgba(255, 255, 255, 0.65);
  display: flex; align-items: center; gap: 5px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hero-divider { margin: 0 4px; opacity: 0.5; }
.hero-switch {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  flex-shrink: 0;
}
.switch-label {
  font-size: 12px; color: rgba(255, 255, 255, 0.75);
  display: flex; align-items: center; gap: 4px;
}

/* ---- 表单布局 ---- */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.form-card {
  background: var(--bg-white);
  border-radius: var(--border-radius-lg);
  padding: 20px 22px;
  box-shadow: var(--shadow-sm);
}
.card-title {
  font-size: 15px; font-weight: 600; color: var(--text-primary);
  display: flex; align-items: center; gap: 8px;
  padding-bottom: 12px; margin-bottom: 18px;
  border-bottom: 1px solid var(--border-light);
}
.card-title .el-icon { color: var(--primary); }

.hours-row { display: flex; align-items: center; gap: 10px; }
.hours-sep { color: var(--text-secondary); font-size: 13px; }
.field-tip {
  margin: 6px 0 0; font-size: 12px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 4px; line-height: 1.5;
}
.tip-warn { color: var(--primary-dark); }

/* ---- 底部操作 ---- */
.form-actions {
  display: flex; align-items: center; justify-content: flex-end; gap: 12px;
  background: var(--bg-white);
  border-radius: var(--border-radius-lg);
  padding: 16px 22px;
  box-shadow: var(--shadow-sm);
}
.actions-note {
  flex: 1; font-size: 12px; color: var(--text-secondary);
  display: flex; align-items: center; gap: 5px;
}

@media (max-width: 900px) {
  .form-grid { grid-template-columns: 1fr; }
  .hero-content { flex-direction: column; text-align: center; }
  .hero-name-row { justify-content: center; }
  .hero-sub { justify-content: center; }
}
</style>
