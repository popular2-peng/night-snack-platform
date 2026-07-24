<script setup lang="ts">
import { ref, watch } from 'vue'
import { getApplicationDetail } from '@/apis/admin'
import { ElMessage } from 'element-plus'

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

function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '' }
</script>

<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    title="申请详情"
    size="480px"
    v-loading="loading"
  >
    <template v-if="detail.id">
      <div class="detail-logo">
        <el-image v-if="detail.logoUrl" :src="detail.logoUrl" :preview-src-list="[detail.logoUrl]" fit="cover"
          style="width:72px;height:72px;border-radius:6px;flex-shrink:0" :preview-teleported="true" />
        <el-avatar v-else :size="72" shape="square" style="font-size:28px;background:#EEF3FD;color:#5B8DEF;">
          {{ detail.shopName?.charAt(0) || '店' }}
        </el-avatar>
        <div>
          <h3>{{ detail.shopName }}</h3>
          <p>申请人: {{ detail.applicantUsername }}</p>
        </div>
      </div>
      <el-descriptions :column="1" border size="small" style="margin-top:16px">
        <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="品类">{{ detail.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请理由">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="拒绝原因">
          <span style="color:var(--danger)">{{ detail.rejectReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[detail.status]?.type || 'warning'" size="small" effect="plain">
            {{ statusMap[detail.status]?.label || detail.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
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
</style>
