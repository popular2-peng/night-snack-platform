<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getApplicationDetail, approveApplicationV2, rejectApplicationV2 } from '@/apis/admin'
import { ElMessage } from 'element-plus'
import { User, Phone, CollectionTag, Clock, Location, Document, Shop } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  applicationId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'audit-done': []
}>()

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'info' },
}

const detail = ref<any>({})
const loading = ref(false)
/** 抽屉操作模式：view 查看 / approve 通过 / reject 拒绝 */
const mode = ref<'view' | 'approve' | 'reject'>('view')
const submitting = ref(false)

// 通过表单（配送半径由商家审核通过后自行设置，此处不配置）
const approveForm = ref({ initialStatus: 'OPEN', reviewNote: '' })
// 拒绝表单
const rejectReason = ref('')
const rejectCharCount = computed(() => rejectReason.value.length)
const rejectValid = computed(() => rejectCharCount.value >= 10 && rejectCharCount.value <= 200)

watch(() => props.visible, async (v) => {
  if (v && props.applicationId) {
    mode.value = 'view'
    loading.value = true
    try {
      const res = await getApplicationDetail(props.applicationId)
      detail.value = res.data
    } catch { ElMessage.error('获取详情失败') }
    finally { loading.value = false }
  }
})

const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '' }

/** 百度地图嵌入：按地址参数搜索（无 AK，直接打开地址搜索结果页） */
const mapSrc = computed(() => {
  if (!detail.value.address) return ''
  return `https://map.baidu.com/search/${encodeURIComponent(detail.value.address)}`
})

// ---- 通过 ----
const confirmApprove = async () => {
  submitting.value = true
  try {
    await approveApplicationV2(props.applicationId!, approveForm.value)
    ElMessage.success('已通过该入驻申请')
    emit('audit-done')
  } catch { /* 拦截器已提示 */ }
  finally { submitting.value = false }
}

// ---- 拒绝 ----
const confirmReject = async () => {
  if (!rejectValid.value) { ElMessage.warning('拒绝原因需 10-200 字'); return }
  submitting.value = true
  try {
    await rejectApplicationV2(props.applicationId!, rejectReason.value)
    ElMessage.success('已拒绝该入驻申请')
    emit('audit-done')
  } catch { /* 拦截器已提示 */ }
  finally { submitting.value = false }
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    :title="mode === 'approve' ? '通过入驻申请' : mode === 'reject' ? '拒绝入驻申请' : '申请详情'"
    size="540px"
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
            <p class="hero-sub">申请人：{{ detail.ownerName || detail.applicantUsername }}</p>
          </div>
        </div>
      </div>

      <!-- ====== 申请信息 ====== -->
      <div class="section">
        <div class="section-title">申请信息</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label"><el-icon><Shop /></el-icon>店铺名称</span>
            <span class="info-value">{{ detail.shopName }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><User /></el-icon>申请人</span>
            <span class="info-value">{{ detail.ownerName || detail.applicantUsername || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Phone /></el-icon>联系电话</span>
            <span class="info-value">{{ detail.phone }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><CollectionTag /></el-icon>品类</span>
            <span class="info-value">{{ detail.categoryName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Clock /></el-icon>申请时间</span>
            <span class="info-value">{{ formatTime(detail.createdAt) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Document /></el-icon>审核时间</span>
            <span class="info-value">{{ formatTime(detail.reviewedAt) || '待审核' }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 店铺地址 + 百度地图 ====== -->
      <div class="section">
        <div class="section-title">店铺地址</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label"><el-icon><Location /></el-icon>详细地址</span>
            <span class="info-value">{{ detail.address || '-' }}</span>
          </div>
        </div>
        <iframe v-if="mapSrc" :src="mapSrc" class="map-frame" loading="lazy" />
        <div v-else class="map-empty">地址坐标缺失（地理编码未成功），请人工核实地址</div>
      </div>

      <!-- ====== 审核备注 ====== -->
      <div v-if="detail.reviewNote" class="section">
        <div class="section-title">审核备注</div>
        <div class="note-card">{{ detail.reviewNote }}</div>
      </div>

      <!-- ====== 拒绝原因 ====== -->
      <div v-if="detail.rejectReason" class="section rejected-section">
        <div class="section-title danger-title">拒绝原因</div>
        <p class="danger-text">{{ detail.rejectReason }}</p>
      </div>

      <!-- ====== 操作表单区：待审核时点击底部按钮展开 ====== -->
      <div v-if="detail.status === 'PENDING' && mode === 'approve'" class="audit-panel">
        <el-form label-width="100px" label-position="left">
          <el-form-item label="初始营业状态">
            <el-select v-model="approveForm.initialStatus" style="width:100%">
              <el-option label="营业中" value="OPEN" />
              <el-option label="打烊" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input v-model="approveForm.reviewNote" type="textarea" :rows="3" placeholder="可选，内部留存"
              maxlength="200" show-word-limit />
          </el-form-item>
        </el-form>
      </div>

      <div v-if="detail.status === 'PENDING' && mode === 'reject'" class="audit-panel">
        <el-input v-model="rejectReason" type="textarea" :rows="4"
          placeholder="请输入拒绝原因（必填，10-200 字，将展示给商家）"
          maxlength="200" show-word-limit />
        <p style="margin-top:6px;font-size:12px" :style="{ color: rejectValid ? 'var(--success)' : 'var(--danger)' }">
          当前 {{ rejectCharCount }} 字{{ rejectCharCount < 10 ? '（还需 ' + (10 - rejectCharCount) + ' 字）' : '' }}
        </p>
      </div>
    </template>

    <!-- ====== 底部操作区（固定，待审核时显示通过/拒绝） ====== -->
    <template #footer>
      <template v-if="detail.status === 'PENDING'">
        <template v-if="mode === 'view'">
          <el-button size="large" class="footer-btn" @click="mode = 'reject'">拒绝申请</el-button>
          <el-button size="large" type="success" class="footer-btn" @click="mode = 'approve'">通过申请</el-button>
        </template>
        <template v-else-if="mode === 'approve'">
          <el-button size="large" @click="mode = 'view'">返回</el-button>
          <el-button size="large" type="success" :loading="submitting" @click="confirmApprove">确认通过</el-button>
        </template>
        <template v-else>
          <el-button size="large" @click="mode = 'view'">返回</el-button>
          <el-button size="large" type="danger" :disabled="!rejectValid" :loading="submitting" @click="confirmReject">确认拒绝</el-button>
        </template>
      </template>
      <span v-else class="footer-tip">该申请已审核完毕</span>
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

/* ---- 百度地图 ---- */
.map-frame {
  width: 100%;
  height: 220px;
  border: none;
  border-radius: 10px;
  margin-top: 12px;
  background: #F0F2F5;
}
.map-empty {
  background: #F0F2F5;
  border-radius: 10px;
  padding: 24px 16px;
  text-align: center;
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 12px;
}

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

/* ---- 操作区 ---- */
.audit-panel {
  background: #FAFAFA;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
}
.audit-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
.footer-btn {
  flex: 1;
}
.footer-tip {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 480px) {
  .hero-content { flex-direction: column; text-align: center; }
  .hero-name-row { justify-content: center; }
}
</style>
