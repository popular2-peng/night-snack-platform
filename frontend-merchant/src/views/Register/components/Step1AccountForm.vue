<script setup lang="ts">
import { ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Phone, User, Lock } from '@element-plus/icons-vue'

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

const props = defineProps<{ form: RegisterForm }>()

const emit = defineEmits<{ next: [] }>()

const formRef = ref<FormInstance>()

/** 手机号：11 位数字 */
const PHONE_REGEX = /^1[3-9]\d{9}$/
/** 联系人姓名：2-10 个中文字符 */
const OWNER_NAME_REGEX = /^[一-龥]{2,10}$/
/** 密码：6-20 位，必须包含字母和数字 */
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d!@#$%^&*()_+]{6,20}$/

const rules: FormRules = {
  phone: [
    { required: true, message: '请输入联系人手机号', trigger: 'blur' },
    { pattern: PHONE_REGEX, message: '请输入正确的11位手机号', trigger: 'blur' },
  ],
  ownerName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { pattern: OWNER_NAME_REGEX, message: '联系人姓名需为 2-10 个中文字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: PASSWORD_REGEX, message: '6-20位，必须包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== props.form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
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
    <div class="form-section-title">联系人信息</div>
    <el-form-item label="联系人手机号" prop="phone">
      <el-input v-model="form.phone" placeholder="用于接收审核通知" maxlength="11">
        <template #prefix><el-icon><Phone /></el-icon></template>
      </el-input>
    </el-form-item>
    <el-form-item label="联系人姓名" prop="ownerName">
      <el-input v-model="form.ownerName" placeholder="申请人/店主姓名（中文字符）" maxlength="10">
        <template #prefix><el-icon><User /></el-icon></template>
      </el-input>
    </el-form-item>

    <div class="form-section-title">设置登录密码</div>
    <el-form-item label="登录密码" prop="password">
      <el-input v-model="form.password" type="password" placeholder="6-20位，含字母和数字" show-password>
        <template #prefix><el-icon><Lock /></el-icon></template>
      </el-input>
    </el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword">
      <el-input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" show-password>
        <template #prefix><el-icon><Lock /></el-icon></template>
      </el-input>
    </el-form-item>

    <el-form-item class="submit-item">
      <el-button type="primary" size="large" class="submit-btn" @click="handleNext">
        下一步
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
