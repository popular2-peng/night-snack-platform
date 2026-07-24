<script setup lang="ts">
import { ref, watch } from 'vue'
import { getMerchantDetail, getMerchantLogs } from '@/apis/admin'
import { ElMessage } from 'element-plus'
import { Clock, Location, Phone, Star, ShoppingBag, Coin, Timer, Van, WarningFilled } from '@element-plus/icons-vue'

interface LogItem { time: string; action: string; detail: string }

const props = defineProps<{
  visible: boolean
  merchantId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  OPEN: { label: '营业中', type: 'success' },
  CLOSED: { label: '已打烊', type: 'info' },
  SUSPENDED: { label: '已封禁', type: 'danger' },
  PENDING: { label: '审核中', type: 'warning' },
}

const detail = ref<any>({})
const logs = ref<LogItem[]>([])
const loading = ref(false)

watch(() => props.visible, async (v) => {
  if (v && props.merchantId) {
    loading.value = true
    try {
      const [dRes, lRes] = await Promise.all([
        getMerchantDetail(props.merchantId),
        getMerchantLogs(props.merchantId),
      ])
      detail.value = dRes.data
      logs.value = lRes.data || []
    } catch { ElMessage.error('获取详情失败') }
    finally { loading.value = false }
  }
})

function formatMoney(n: number) { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '' }
</script>

<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="商家详情"
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
            {{ detail.name?.charAt(0) || '店' }}
          </el-avatar>
          <div class="hero-info">
            <div class="hero-name-row">
              <h2>{{ detail.name }}</h2>
              <el-tag :type="statusMap[detail.status]?.type || 'info'" size="small" effect="dark" round>
                {{ statusMap[detail.status]?.label || detail.status }}
              </el-tag>
            </div>
            <p class="hero-sub">{{ detail.username || '-' }} · {{ detail.categoryName || '-' }}</p>
          </div>
        </div>
      </div>

      <!-- ====== 经营数据卡片 ====== -->
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon" style="background:#EEF4FF;color:#5B8DEF"><el-icon><Star /></el-icon></div>
          <div class="stat-body">
            <span class="stat-num">{{ detail.avgRating ?? '-' }}</span>
            <span class="stat-label">评分</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#F0FAF0;color:#67C23A"><el-icon><ShoppingBag /></el-icon></div>
          <div class="stat-body">
            <span class="stat-num">{{ detail.totalOrders ?? 0 }}</span>
            <span class="stat-label">历史订单</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#FFF8ED;color:#E6A23C"><el-icon><Coin /></el-icon></div>
          <div class="stat-body">
            <span class="stat-num">{{ detail.todayOrders ?? 0 }}</span>
            <span class="stat-label">今日订单</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background:#FDF2F2;color:#F56C6C"><el-icon><Timer /></el-icon></div>
          <div class="stat-body">
            <span class="stat-num">{{ formatMoney(detail.todayRevenue) }}</span>
            <span class="stat-label">今日营收</span>
          </div>
        </div>
      </div>

      <!-- ====== 基本信息 ====== -->
      <div class="section">
        <div class="section-title">基本信息</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label"><el-icon><Phone /></el-icon>电话</span>
            <span class="info-value">{{ detail.phone }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Location /></el-icon>地址</span>
            <span class="info-value">{{ detail.address || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Clock /></el-icon>营业时间</span>
            <span class="info-value">{{ detail.businessHoursStart }} ~ {{ detail.businessHoursEnd }}</span>
          </div>
          <div class="info-row">
            <span class="info-label"><el-icon><Van /></el-icon>配送半径</span>
            <span class="info-value">{{ detail.deliveryRadius ?? '-' }} 米</span>
          </div>
          <div class="info-row" v-if="detail.description">
            <span class="info-label">简介</span>
            <span class="info-value">{{ detail.description }}</span>
          </div>
          <div class="info-row" v-if="detail.notice">
            <span class="info-label">公告</span>
            <span class="info-value">{{ detail.notice }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 封禁原因 ====== -->
      <div v-if="detail.bannedReason" class="section banned-section">
        <div class="section-title danger-title">
          <el-icon><WarningFilled /></el-icon>封禁原因
        </div>
        <p class="banned-text">{{ detail.bannedReason }}</p>
      </div>

      <!-- ====== 时间信息 ====== -->
      <div class="section">
        <div class="section-title">时间信息</div>
        <div class="info-list">
          <div class="info-row">
            <span class="info-label">入驻时间</span>
            <span class="info-value">{{ formatTime(detail.createdAt) }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">最后登录</span>
            <span class="info-value">{{ formatTime(detail.lastLoginAt) || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- ====== 操作记录 ====== -->
      <div v-if="logs.length" class="section">
        <div class="section-title">操作记录</div>
        <div class="timeline-wrap">
          <el-timeline>
            <el-timeline-item
              v-for="(l, i) in logs" :key="i"
              :timestamp="formatTime(l.time)"
              placement="top"
              :color="i === 0 ? '#5B8DEF' : '#C0C4CC'"
              :hollow="i !== 0"
            >
              <div class="timeline-card">
                <span class="timeline-action">{{ l.action }}</span>
                <p v-if="l.detail" class="timeline-detail">{{ l.detail }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
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

/* ---- 数据卡片 ---- */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px;
  margin-bottom: 20px;
}
.stat-card {
  background: var(--bg, #f8f9fc);
  border-radius: 10px; padding: 14px 10px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  text-align: center;
  transition: transform .15s, box-shadow .15s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-sm, 0 2px 8px rgba(0,0,0,.06)); }
.stat-icon {
  width: 36px; height: 36px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px;
}
.stat-body { display: flex; flex-direction: column; align-items: center; }
.stat-num { font-size: 18px; font-weight: 700; color: var(--text-primary, #303133); line-height: 1.3; font-variant-numeric: tabular-nums; }
.stat-label { font-size: 11px; color: var(--text-secondary, #909399); margin-top: 2px; }

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

/* ---- 封禁区域 ---- */
.banned-section { background: #FEF0F0; border-radius: 10px; padding: 16px; }
.banned-section .section-title { margin-bottom: 8px; }
.banned-text { margin: 0; font-size: 13px; color: #F56C6C; line-height: 1.6; }

/* ---- 时间线 ---- */
.timeline-wrap { padding: 0 4px; }
.timeline-card { margin-top: -4px; }
.timeline-action {
  font-size: 14px; font-weight: 600; color: var(--text-primary, #303133);
}
.timeline-detail {
  margin: 4px 0 0 0; font-size: 12px; color: var(--text-secondary, #909399);
  line-height: 1.5;
}

/* ---- Element Plus Timeline 微调 ---- */
:deep(.el-timeline-item__timestamp) {
  font-size: 12px; color: var(--text-secondary, #909399);
}
:deep(.el-timeline-item__node) { transition: all .3s; }

@media (max-width: 480px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .hero-content { flex-direction: column; text-align: center; }
  .hero-name-row { justify-content: center; }
}
</style>
