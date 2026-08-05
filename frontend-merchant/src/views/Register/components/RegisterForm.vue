<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { registerApi, getCategoryList } from '@/apis/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Phone, Shop, Postcard, Document, CollectionTag } from '@element-plus/icons-vue'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const categoryOptions = ref<{ id: number; name: string; icon: string }[]>([])

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  shopName: '',
  categoryId: null as number | null,
  reason: '',
  description: '',
})

/** 账号：4-20位，字母开头，字母/数字/下划线 */
const ACCOUNT_REGEX = /^[a-zA-Z][a-zA-Z0-9_]{3,19}$/
/** 密码：6-20位，必须包含字母和数字 */
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*()_+]{6,20}$/
/** 手机号：11位数字 */
const PHONE_REGEX = /^1[3-9]\d{9}$/

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { pattern: ACCOUNT_REGEX, message: '4-20位，字母开头，支持字母/数字/下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: PASSWORD_REGEX, message: '6-20位，必须包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_REGEX, message: '请输入正确的11位手机号', trigger: 'blur' },
  ],
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 20, message: '店铺名称 2-20 字', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择主营品类', trigger: 'change' },
  ],
}

async function fetchCategories() {
  try {
    const res = await getCategoryList()
    categoryOptions.value = (res.data || []).map((c: any) => ({
      id: c.id, name: c.name, icon: c.icon || '',
    }))
  } catch { /* ignore */ }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await registerApi({
      username: form.username,
      password: form.password,
      phone: form.phone,
      shopName: form.shopName,
      categoryId: form.categoryId!,
      reason: form.reason || undefined,
      description: form.description || undefined,
    })
    ElMessage.success('入驻申请已提交，请等待平台审核')
    router.push('/login')
  } catch {
    // 错误已由拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(fetchCategories)
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
    <!-- 登录账号 -->
    <div class="form-section-title">账号信息</div>
    <div class="form-row">
      <el-form-item label="登录账号" prop="username" class="half">
        <el-input v-model="form.username" placeholder="字母开头，4-20位" maxlength="20">
          <template #prefix><el-icon><User /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="手机号" prop="phone" class="half">
        <el-input v-model="form.phone" placeholder="用于接收审核通知" maxlength="11">
          <template #prefix><el-icon><Phone /></el-icon></template>
        </el-input>
      </el-form-item>
    </div>
    <div class="form-row">
      <el-form-item label="登录密码" prop="password" class="half">
        <el-input v-model="form.password" type="password" placeholder="6-20位，含字母和数字" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword" class="half">
        <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password>
          <template #prefix><el-icon><Lock /></el-icon></template>
        </el-input>
      </el-form-item>
    </div>

    <!-- 店铺信息 -->
    <div class="form-section-title">店铺信息</div>
    <el-form-item label="店铺名称" prop="shopName">
      <el-input v-model="form.shopName" placeholder="如：老王小吃店" maxlength="20">
        <template #prefix><el-icon><Shop /></el-icon></template>
      </el-input>
    </el-form-item>
    <div class="form-row">
      <el-form-item label="主营品类" prop="categoryId" class="half">
        <el-select v-model="form.categoryId" placeholder="选择主营品类" style="width: 100%">
          <template #prefix><el-icon><CollectionTag /></el-icon></template>
          <el-option
            v-for="c in categoryOptions" :key="c.id"
            :label="`${c.icon} ${c.name}`" :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="店铺简介" class="half">
        <el-input v-model="form.description" placeholder="一句话介绍您的店（可选）" maxlength="200">
          <template #prefix><el-icon><Postcard /></el-icon></template>
        </el-input>
      </el-form-item>
    </div>
    <el-form-item label="申请理由" prop="reason">
      <el-input
        v-model="form.reason" type="textarea" :rows="3"
        placeholder="说说您的餐饮经验、开店计划等（可选，审核参考）"
        maxlength="500" show-word-limit
      >
        <template #prefix><el-icon><Document /></el-icon></template>
      </el-input>
    </el-form-item>

    <el-form-item class="submit-item">
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        class="submit-btn"
        @click="handleRegister"
      >
        提交入驻申请
      </el-button>
    </el-form-item>
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

.form-row {
  display: flex;
  gap: 16px;
}
.form-row .half {
  flex: 1;
}

.submit-item {
  margin-top: 8px;
  margin-bottom: 0;
}
.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border: none;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}
.submit-btn:active { transform: translateY(0); }
</style>
