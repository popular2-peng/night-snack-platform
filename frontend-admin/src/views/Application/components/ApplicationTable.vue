<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listApplications, approveApplication, rejectApplication } from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

interface AppItem { id: number; shopName: string; applicantUsername: string; reason: string; status: string; createdAt: string }

const apps = ref<AppItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'info' },
}

async function fetchApps() {
  loading.value = true
  const res = await listApplications({ page: page.value, size: size.value })
  apps.value = res.data.list
  total.value = res.data.total
  loading.value = false
}

function handleSizeChange(val: number) { size.value = val; fetchApps() }
function handlePageChange(val: number) { page.value = val; fetchApps() }

async function handleApprove(row: AppItem) {
  await ElMessageBox.confirm(`确定通过 "${row.shopName}" 的入驻申请？`, '确认通过', { type: 'success' })
  await approveApplication(row.id)
  ElMessage.success('已通过')
  fetchApps()
}

async function handleReject(row: AppItem) {
  const { value: reason } = await ElMessageBox.prompt('请输入拒绝理由', '拒绝申请', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
  if (reason !== null) {
    await rejectApplication(row.id, reason)
    ElMessage.success('已拒绝')
    fetchApps()
  }
}

const pendingCount = computed(() => apps.value.filter(a => a.status === 'PENDING').length)

onMounted(fetchApps)
</script>

<template>
  <div class="page-card">
    <!-- 数据统计条 -->
    <div class="stats-strip">
      <div class="stat-item">
        <span class="stat-item-value">{{ total }}</span>
        <span class="stat-item-label">申请总数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-warning">{{ pendingCount }}</span>
        <span class="stat-item-label">待审核</span>
      </div>
    </div>

    <el-table :data="apps" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="shopName" label="申请店铺名" width="160" />
      <el-table-column prop="applicantUsername" label="申请人" width="120" />
      <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'warning'" size="small" effect="plain">
            {{ statusMap[row.status]?.label || '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="180" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
          </template>
          <span v-else class="text-disabled">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.stats-strip {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--bg);
  border-radius: var(--border-radius-sm);
}
.stat-item {
  display: flex;
  flex-direction: column;
}
.stat-item-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat-item-value.text-warning { color: var(--warning); }
.stat-item-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--border-color);
}
.text-disabled { color: var(--text-placeholder); }
</style>
