<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import Step1AccountForm from './components/Step1AccountForm.vue'
import Step2CategoryForm from './components/Step2CategoryForm.vue'
import Step3AddressForm from './components/Step3AddressForm.vue'
import { getCategoryList } from '@/apis/auth'

/** 入驻申请步骤 */
const step = ref(1)
const stepTitles = ['联系人信息', '经营品类', '店铺地址']

/** 三步共享的表单状态 */
const form = reactive({
  phone: '',
  ownerName: '',
  password: '',
  confirmPassword: '',
  shopName: '',
  categoryId: null as number | null,
  province: '',
  city: '',
  district: '',
  address: '',
  logoUrl: '',
})

/** 平台品类（第二步选择、第三步展示名称） */
const categoryOptions = ref<{ id: number; name: string; icon: string }[]>([])

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    categoryOptions.value = (res.data || []).map((c: any) => ({
      id: c.id, name: c.name, icon: c.icon || '',
    }))
  } catch { /* ignore */ }
}

onMounted(fetchCategories)
</script>

<template>
  <div class="register-page">
    <!-- 背景装饰 -->
    <div class="bg-circle bg-circle-1" />
    <div class="bg-circle bg-circle-2" />
    <div class="bg-grid" />

    <div class="register-card fade-in-scale">
      <div class="register-header">
        <div class="register-logo">
          <span class="logo-emoji">🏪</span>
        </div>
        <h1 class="register-title">商家入驻申请</h1>
        <p class="register-subtitle">加入夜宵外卖平台，开启深夜生意</p>
      </div>

      <!-- 步骤条 -->
      <div class="register-steps">
        <div
          v-for="(title, i) in stepTitles" :key="i"
          class="step-item"
          :class="{ active: step === i + 1, done: step > i + 1 }"
        >
          <div class="step-dot">
            <span v-if="step > i + 1" class="step-done">✓</span>
            <span v-else>{{ i + 1 }}</span>
          </div>
          <span class="step-label">{{ title }}</span>
          <div v-if="i < stepTitles.length - 1" class="step-line" :class="{ filled: step > i + 1 }" />
        </div>
      </div>

      <div class="step-body">
        <Step1AccountForm v-show="step === 1" :form="form" @next="step = 2" />
        <Step2CategoryForm
          v-show="step === 2" :form="form" :category-options="categoryOptions"
          @next="step = 3" @prev="step = 1"
        />
        <Step3AddressForm
          v-show="step === 3" :form="form" :category-options="categoryOptions"
          @prev="step = 2"
        />
      </div>

      <p class="register-login-tip">
        已有店铺账号？
        <router-link to="/login" class="login-link">直接登录</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: linear-gradient(140deg, #191510 0%, #2A1F12 40%, #3D2B12 100%);
  position: relative;
  overflow: hidden;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245, 154, 35, 0.16), transparent 70%);
}
.bg-circle-1 {
  width: 640px; height: 640px;
  top: -220px; right: -160px;
}
.bg-circle-2 {
  width: 460px; height: 460px;
  bottom: -140px; left: -120px;
  background: radial-gradient(circle, rgba(255, 180, 74, 0.1), transparent 70%);
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 75%);
}

.register-card {
  width: 560px;
  max-width: 100%;
  background: #fff;
  border-radius: 16px;
  padding: 40px 44px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.45);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 20px;
}
.register-logo { display: flex; justify-content: center; margin-bottom: 12px; }
.logo-emoji {
  font-size: 36px;
  width: 64px; height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(245, 154, 35, 0.4);
}
.register-title { font-size: 22px; color: #303133; margin: 0 0 4px; font-weight: 600; }
.register-subtitle { color: #909399; font-size: 13px; margin: 0; letter-spacing: 1px; }

/* ---- 步骤条 ---- */
.register-steps {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 0 8px;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  flex: 1;
  min-width: 0;
}
.step-dot {
  width: 28px; height: 28px;
  border-radius: 50%;
  background: #F0F1F4;
  color: #909399;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all .3s;
  z-index: 1;
}
.step-item.active .step-dot {
  background: linear-gradient(135deg, #F59A23 0%, #E07F0E 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(245, 154, 35, 0.4);
}
.step-item.done .step-dot {
  background: #52C41A;
  color: #fff;
}
.step-label {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  white-space: nowrap;
}
.step-item.active .step-label { color: #E07F0E; font-weight: 600; }
.step-item.done .step-label { color: #52C41A; }
.step-line {
  position: absolute;
  top: 14px;
  left: calc(50% + 20px);
  right: calc(-50% + 20px);
  height: 2px;
  background: #F0F1F4;
  transition: background .3s;
}
.step-line.filled { background: #52C41A; }

.step-body { min-height: 320px; }

.register-login-tip {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 18px;
}
.login-link {
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
.login-link:hover { text-decoration: underline; }
</style>
