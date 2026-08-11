<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Shop, CollectionTag } from '@element-plus/icons-vue'

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

const emit = defineEmits<{ next: []; prev: [] }>()

const formRef = ref<FormInstance>()

const rules: FormRules = {
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 20, message: '店铺名称 2-20 字', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择主营品类', trigger: 'change' },
  ],
}

const handleNext = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('next')
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
    <div class="form-section-title">店铺信息</div>
    <el-form-item label="店铺名称" prop="shopName">
      <el-input v-model="form.shopName" placeholder="如：老王小吃店" maxlength="20">
        <template #prefix><el-icon><Shop /></el-icon></template>
      </el-input>
    </el-form-item>
    <el-form-item label="主营品类" prop="categoryId">
      <el-select v-model="form.categoryId" placeholder="选择经营的品类" style="width: 100%">
        <template #prefix><el-icon><CollectionTag /></el-icon></template>
        <el-option
          v-for="c in props.categoryOptions" :key="c.id"
          :label="`${c.icon} ${c.name}`" :value="c.id"
        />
      </el-select>
    </el-form-item>
    <p class="form-tip">审核通过后该品类将作为店铺的主营品类展示在平台</p>

    <div class="step-actions">
      <el-button size="large" class="back-btn" @click="emit('prev')">上一步</el-button>
      <el-button type="primary" size="large" class="submit-btn" @click="handleNext">
        下一步
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
