<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { regionData, codeToText } from 'element-china-area-data'
import { Location, CollectionTag, Document } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { registerApi } from '@/apis/auth'
import { ElMessage } from 'element-plus'

/** 共享表单状态（父组件 Register/index.vue 持有） */
interface RegisterForm {
  phone: string
  ownerName: string
  password: string
  confirmPassword: string
  shopName: string
  categoryId: number | null
  province: string
  city: string
  district: string
  address: string
}

interface CategoryOption {
  id: number
  name: string
  icon: string
}

const props = defineProps<{
  form: RegisterForm
  categoryOptions: CategoryOption[]
}>()

const emit = defineEmits<{ prev: [] }>()

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

/** 省市区选中的区域码数组（如 ['110000', '110100', '110105']） */
const areaCode = ref<string[]>([])

const rules: FormRules = {
  address: [
    { required: true, message: '请输入详细地址', trigger: 'blur' },
    { max: 200, message: '详细地址最多 200 字', trigger: 'blur' },
  ],
}

/** 已选品类展示（第二步选择结果） */
const selectedCategory = computed(() => {
  return props.categoryOptions.find((c) => c.id === props.form.categoryId) || null
})

/** 省市区选择变化：区域码 → 汉字（直辖市"市辖区"合并为省名） */
const onAreaChange = (codes: string[]) => {
  const textOf = (code?: string) => (code ? (codeToText as Record<string, string>)[code] || '' : '')
  props.form.province = textOf(codes[0])
  props.form.city = textOf(codes[1]) === '市辖区' ? textOf(codes[0]) : textOf(codes[1])
  props.form.district = textOf(codes[2])
}

/** 自动生成登录账号：m + 10 位随机（字母开头，符合后端账号校验） */
const generateUsername = () => {
  return 'm' + Math.random().toString(36).slice(2, 12)
}

const handleSubmit = async () => {
  if (areaCode.value.length < 3) {
    ElMessage.warning('请选择省市区')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await registerApi({
      username: generateUsername(),
      password: props.form.password,
      phone: props.form.phone,
      ownerName: props.form.ownerName,
      shopName: props.form.shopName,
      categoryId: props.form.categoryId!,
      province: props.form.province,
      city: props.form.city,
      district: props.form.district,
      address: props.form.address,
      logoUrl: props.form.logoUrl || undefined,
    })
    ElMessage.success('入驻申请已提交，请等待平台审核')
    router.push('/login')
  } catch {
    // 错误已由拦截器统一提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
    <div class="form-section-title">店铺地址</div>
    <el-form-item label="省市区" required>
      <el-cascader
        v-model="areaCode"
        :options="regionData"
        placeholder="请选择省 / 市 / 区县"
        style="width: 100%"
        @change="onAreaChange"
      />
    </el-form-item>
    <el-form-item label="详细地址" prop="address">
      <el-input
        v-model="form.address"
        type="textarea"
        :rows="3"
        placeholder="街道、门牌号等（如：望京SOHO T1-1201）"
        maxlength="200" show-word-limit
      >
        <template #prefix><el-icon><Location /></el-icon></template>
      </el-input>
    </el-form-item>
    <p class="form-tip">提交后将调用百度地图地理编码，将地址转换为经纬度用于配送范围校验</p>

    <div class="form-section-title">申请信息确认</div>
    <div class="summary-card">
      <div class="summary-row">
        <span class="summary-label"><el-icon><CollectionTag /></el-icon>主营品类</span>
        <span class="summary-value">{{ selectedCategory ? `${selectedCategory.icon} ${selectedCategory.name}` : '-' }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label"><el-icon><Document /></el-icon>店铺名称</span>
        <span class="summary-value">{{ form.shopName }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label"><el-icon><Location /></el-icon>地址</span>
        <span class="summary-value">{{ form.province }}{{ form.city }}{{ form.district }}{{ form.address }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">联系人</span>
        <span class="summary-value">{{ form.ownerName }}（{{ form.phone }}）</span>
      </div>
    </div>

    <div class="step-actions">
      <el-button size="large" class="back-btn" @click="emit('prev')">上一步</el-button>
      <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleSubmit">
        提交入驻申请
      </el-button>
    </div>
  </el-form>
</template>

<style scoped>
.form-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid var(--primary);
  line-height: 1.4;
}
.form-tip {
  font-size: 12px;
  color: var(--text-secondary);
  margin: -4px 0 8px;
}
.summary-card {
  background: #FAFAFA;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  padding: 8px 16px;
  margin-bottom: 16px;
}
.summary-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}
.summary-row:last-child { border-bottom: none; }
.summary-label {
  flex-shrink: 0;
  width: 90px;
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}
.summary-value {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  word-break: break-all;
}
.step-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.step-actions .back-btn {
  flex: 1;
  height: 46px;
  font-size: 15px;
  border-radius: 10px;
}
.step-actions .submit-btn {
  flex: 2;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.step-actions .submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}
.step-actions .submit-btn:active { transform: translateY(0); }
</style>
