<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listApplications, approveApplication, rejectApplication } from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

interface AppItem { id: number; shopName: string; applicantUsername: string; reason: string; status: string; createdAt: string }

const apps = ref<AppItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

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
  await ElMessageBox.confirm(`确定通过 "${row.shopName}" 的入驻申请？`, '确认', { type: 'success' })
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

onMounted(fetchApps)
</script>

<template>
  <div class="page-card">
    <el-table :data="apps" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="shopName" label="申请店铺名" width="160" />
      <el-table-column prop="applicantUsername" label="申请人" width="120" />
      <el-table-column prop="reason" label="申请理由" min-width="200" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="warning" size="small">待审核</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="180" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
          <el-button size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; display: flex; justify-content: flex-end">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>
