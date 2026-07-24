<script setup lang="ts">
import { ref, watch } from 'vue'
import { getMerchantDetail, getMerchantLogs } from '@/apis/admin'
import { ElMessage } from 'element-plus'

interface LogItem { time: string; action: string; detail: string }

const props = defineProps<{
  visible: boolean
  merchantId: number | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

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

function close() { emit('update:visible', false) }

function formatMoney(n: number) { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '' }
</script>

<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="商家详情"
    size="480px"
    v-loading="loading"
  >
    <template v-if="detail.id">
      <!-- LOGO + 基本信息 -->
      <div class="detail-logo">
        <el-image v-if="detail.logoUrl" :src="detail.logoUrl" :preview-src-list="[detail.logoUrl]" fit="cover"
          style="width:72px;height:72px;border-radius:6px;flex-shrink:0" :preview-teleported="true" />
        <el-avatar v-else :size="72" shape="square" style="font-size:28px;background:#EEF3FD;color:#5B8DEF;">
          {{ detail.name?.charAt(0) || '店' }}
        </el-avatar>
        <div>
          <h3>{{ detail.name }}</h3>
          <p>店主: {{ detail.username }}</p>
        </div>
      </div>

      <!-- 详情字段 -->
      <el-descriptions :column="1" border size="small" style="margin-top:16px">
        <el-descriptions-item label="电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address }}</el-descriptions-item>
        <el-descriptions-item label="品类">{{ detail.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="简介">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公告">{{ detail.notice || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评分">{{ detail.avgRating ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="历史订单">{{ detail.totalOrders ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="今日订单">{{ detail.todayOrders ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="今日营收">{{ formatMoney(detail.todayRevenue) }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ detail.businessHoursStart }} ~ {{ detail.businessHoursEnd }}</el-descriptions-item>
        <el-descriptions-item label="配送半径">{{ detail.deliveryRadius }} 米</el-descriptions-item>
        <el-descriptions-item v-if="detail.bannedReason" label="封禁原因">{{ detail.bannedReason }}</el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ formatTime(detail.lastLoginAt) || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作记录 -->
      <div v-if="logs.length" style="margin-top:20px">
        <h4>操作记录</h4>
        <el-timeline style="margin-top:12px">
          <el-timeline-item v-for="(l, i) in logs" :key="i" :timestamp="formatTime(l.time)" placement="top">
            <strong>{{ l.action }}</strong>
            <p v-if="l.detail">{{ l.detail }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped>
.detail-logo {
  display: flex;
  align-items: center;
  gap: 16px;
}
.detail-logo h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
}
.detail-logo p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}
h4 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}
</style>
