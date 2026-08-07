<script setup lang="ts">
import { ref, watch } from 'vue'
import { getApplicationDetail } from '@/apis/admin'
import { ElMessage } from 'element-plus'
import { User, Phone, ShoppingCart, Clock } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  applicationId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'info' },
}

const detail = ref<any>({})
const loading = ref(false)

watch(() => props.visible, async (v) => {
  if (v && props.applicationId) {
    loading.value = true
    try {
      const res = await getApplicationDetail(props.applicationId)
      detail.value = res.data
    } catch { ElMessage.error('获取详情失败') }
    finally { loading.value = false }
  }
})

const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '' }
</script>

<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="申请详情"
    size="520px"
    v-loading="loading"
  >
    <template v-if="detail.id">
      <!-- ====== 头部卡片 ====== -->
      <div class="drawer-hero">
        <div class="hero-bg"></div>
        <div class="hero-content">
          <el-image v-if="detail.logoUrl" :src="detail.logoUrl" :preview-src-list="[detail.logoUrl]" fit="cover"
            class="hero-logo" :preview-teleported="true" />
          <el-avatar v-else :size="72" shape="square" class="hero-logo-placeholder">
            {{ detail.shopName?.charAt(0) || '店' }}
          </el-avatar>
          <div class="hero-info">
            <div class="hero-name-row">
              <h2>{{ detail.shopName }}</h2>
              <el-tag :type="statusMap[detail.status]?.type || 'warning'" size="small" effect="dark" round>
                {{ statusMap[detail.status]?.label || detail.status }}
              </el-tag>
            </div>
            <p class="hero-sub">申请人: {{ detail.applicantUsername }}</p>
          </div>
        </div>
      </div>

      <!-- ====== 申请信息 ====== -->
      <div class="section">
        <div class="section-title">申请信息</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label"><el-icon><User /></el-icon>申请人</span>
            <span class="info-value">{{ detail.applicantUsername }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Phone /></el-icon>联系电话</span>
            <span class="info-value">{{ detail.phone }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><ShoppingCart /></el-icon>品类</span>
            <span class="info-value">{{ detail.categoryName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">申请理由</span>
            <span class="info-value">{{ detail.reason }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Clock /></el-icon>申请时间</span>
            <span class="info-value">{{ formatTime(detail.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 审核备注 ====== -->
      <div v-if="detail.reviewNote" class="section">
        <div class="section-title">审核备注</div>
        <div class="note-card">
          {{ detail.reviewNote }}
        </div>
      </div>

      <!-- ====== 拒绝原因 ====== -->
      <div v-if="detail.rejectReason" class="section rejected-section">
        <div class="section-title danger-title">拒绝原因</div>
        <p class="danger-text">{{ detail.rejectReason }}</p>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
/* ---- 头部卡片 ---- */
.drawer-hero {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}
.hero-bg {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, #5B8DEF 0%, #7FA9F5 40%, #A3C4FA 100%);
}
.hero-content {
  position: relative;
  display: flex; align-items: center; gap: 16px;
  padding: 24px 20px;
}
.hero-logo {
  width: 72px; height: 72px; border-radius: 12px;
  border: 3px solid rgba(255,255,255,.4);
  flex-shrink: 0;
}
.hero-logo-placeholder {
  font-size: 28px; font-weight: 700;
  background: rgba(255,255,255,.25); color: #fff;
  border: 3px solid rgba(255,255,255,.4);
  border-radius: 12px; flex-shrink: 0;
}
.hero-info { flex: 1; min-width: 0; }
.hero-name-row {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 6px;
}
.hero-name-row h2 {
  margin: 0; font-size: 20px; font-weight: 700; color: #fff;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hero-sub {
  margin: 0; font-size: 13px; color: rgba(255,255,255,.8);
}

/* ---- 分区 ---- */
.section { margin-bottom: 20px; }
.section-title {
  font-size: 14px; font-weight: 600; color: var(--text-primary, #303133);
  margin-bottom: 12px; padding-left: 12px;
  border-left: 3px solid #5B8DEF;
  display: flex; align-items: center; gap: 6px;
}
.danger-title { border-left-color: #F56C6C; color: #F56C6C; }

/* ---- 信息行 ---- */
.info-list {
  background: var(--bg, #f8f9fc);
  border-radius: 10px; padding: 4px 16px;
}
.info-row {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color, #EBEEF5);
}
.info-row:last-child { border-bottom: none; }
.info-label {
  flex-shrink: 0; width: 80px;
  font-size: 13px; color: var(--text-secondary, #909399);
  display: flex; align-items: center; gap: 4px;
}
.info-value { flex: 1; font-size: 13px; color: var(--text-primary, #303133); word-break: break-all; }

/* ---- 审核备注 ---- */
.note-card {
  background: #EEF4FF;
  border-radius: 10px; padding: 14px 16px;
  font-size: 13px; color: #5B8DEF; line-height: 1.6;
}

/* ---- 拒绝原因 ---- */
.rejected-section { background: #FEF0F0; border-radius: 10px; padding: 16px; }
.rejected-section .section-title { margin-bottom: 8px; }
.danger-text { margin: 0; font-size: 13px; color: #F56C6C; line-height: 1.6; }

@media (max-width: 480px) {
  .hero-content { flex-direction: column; text-align: center; }
  .hero-name-row { justify-content: center; }
}
</style>
