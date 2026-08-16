<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listApplications, listAllEnabledCategories,
  batchApproveApplications, batchRejectApplications,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import StatsStrip from '@/components/StatsStrip.vue'
import ApplicationDetailDrawer from './ApplicationDetailDrawer.vue'

interface AppItem {
  id: number; shopName: string; applicantUsername: string; ownerName: string
  phone: string; logoUrl: string; categoryName: string
  status: string; rejectReason: string; createdAt: string; reviewedAt: string
}

const apps = ref<AppItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const categoryId = ref('')
const categoryOptions = ref<{ label: string; value: string }[]>([])
const dateRange = ref<string[]>(['', ''])
const loading = ref(false)

const appStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
]

// 统计（独立请求全量数据，不受筛选影响）
const stats = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })

const fetchStats = async () => {
  try {
    const res = await listApplications({ page: 1, size: 999 })
    const list = res.data.list || []
    stats.value = {
      total: res.data.total || list.length,
      pending: list.filter((a: AppItem) => a.status === 'PENDING').length,
      approved: list.filter((a: AppItem) => a.status === 'APPROVED').length,
      rejected: list.filter((a: AppItem) => a.status === 'REJECTED').length,
    }
  } catch { /* ignore */ }
}

const fetchCategories = async () => {
  try {
    const res = await listAllEnabledCategories()
    categoryOptions.value = (res.data || []).map((c: any) => ({ label: `${c.icon || ''} ${c.name}`.trim(), value: String(c.id) }))
  } catch { /* ignore */ }
}

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'info' },
}

// ---- 数据获取（服务端筛选，与 MerchantTable 逻辑一致） ----
const fetchApps = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (categoryId.value) params.categoryId = categoryId.value
    if (dateRange.value?.[0]) params.dateStart = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1]
    const res = await listApplications(params)
    apps.value = res.data.list || []
    total.value = res.data.total || (res.data.list || []).length
  } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; fetchApps() }
const handleReset = () => { page.value = 1; fetchApps() }
const handleSizeChange = (val: number) => { size.value = val; fetchApps() }
const handlePageChange = (val: number) => { page.value = val; fetchApps() }

// ---- 详情抽屉（通过/拒绝操作移入抽屉内） ----
const detailVisible = ref(false)
const detailAppId = ref<number | null>(null)

const openDetail = (row: AppItem) => {
  detailAppId.value = row.id
  detailVisible.value = true
}

// 抽屉内完成审核后刷新列表
const handleAuditDone = () => {
  detailVisible.value = false
  fetchApps()
  fetchStats()
}

// ---- 批量通过 / 批量拒绝 ----
const selectedIds = ref<number[]>([])
const batchLoading = ref(false)

const handleBatchApprove = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选申请'); return }
  await ElMessageBox.confirm(
    `确定通过选中的 ${selectedIds.value.length} 条入驻申请？将通过并创建商家（默认营业中，配送半径由商家自行设置）。`,
    '批量通过', { type: 'warning', confirmButtonText: '确认通过' })
  batchLoading.value = true
  try {
    const res = await batchApproveApplications([...selectedIds.value])
    ElMessage.success(res.data.message || '批量通过完成')
    selectedIds.value = []
    fetchApps()
    fetchStats()
  } catch { /* 拦截器已提示 */ }
  finally { batchLoading.value = false }
}

const handleBatchReject = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选申请'); return }
  const { value: reason } = await ElMessageBox.prompt(
    `将拒绝选中的 ${selectedIds.value.length} 条入驻申请，请输入统一的拒绝原因（10-200 字，将展示给商家）：`,
    '批量拒绝', { confirmButtonText: '确认拒绝', inputType: 'textarea', inputValidator: (v: string) => (v && v.length >= 10 && v.length <= 200) || '拒绝原因需 10-200 字' })
  if (!reason) return
  batchLoading.value = true
  try {
    const res = await batchRejectApplications([...selectedIds.value], reason)
    ElMessage.success(res.data.message || '批量拒绝完成')
    selectedIds.value = []
    fetchApps()
    fetchStats()
  } catch { /* 拦截器已提示 */ }
  finally { batchLoading.value = false }
}

// ---- 格式化 ----
const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '' }

onMounted(() => { fetchStats(); fetchApps(); fetchCategories() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条（公共组件） ====== -->
    <StatsStrip :items="[
      { value: stats.total, label: '申请总数' },
      { value: stats.pending, label: '待审核', color: 'warning' },
      { value: stats.approved, label: '已通过', color: 'success' },
      { value: stats.rejected, label: '已拒绝', color: 'danger' },
    ]" />

    <!-- ====== 搜索 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:category-value="categoryId"
      v-model:date-range="dateRange"
      placeholder="搜索店铺名/申请人"
      :filter-options="appStatusOptions"
      filter-placeholder="状态筛选"
      :category-options="categoryOptions"
      category-placeholder="品类筛选"
      date-label="申请"
      @search="handleSearch"
      @reset="handleReset"
    >
      <!-- 批量操作（与商家管理页一致：搜索栏右侧，选中后显示） -->
      <template #extra>
        <template v-if="selectedIds.length">
          <el-button type="success" plain :loading="batchLoading" @click="handleBatchApprove">
            批量通过 ({{ selectedIds.length }})
          </el-button>
          <el-button type="danger" plain :loading="batchLoading" @click="handleBatchReject">
            批量拒绝 ({{ selectedIds.length }})
          </el-button>
        </template>
      </template>
    </SearchFilterBar>

    <!-- ====== 表格 ====== -->
    <div class="table-scroll">
    <el-table :data="apps" stripe v-loading="loading" height="100%" style="width: 100%"
      @selection-change="(rows: AppItem[]) => selectedIds = rows.map(r => r.id)">
      <el-table-column type="selection" width="42" fixed="left" />
      <el-table-column type="index" label="编号" width="56" align="center" />
      <el-table-column label="LOGO" width="72" align="center">
        <template #default="{ row }">
          <el-image v-if="row.logoUrl" :src="row.logoUrl" :preview-src-list="[row.logoUrl]" fit="cover"
            style="width:36px;height:36px;border-radius:4px;vertical-align:middle" :preview-teleported="true" />
          <el-avatar v-else :size="36" shape="square" style="background:#EEF3FD;color:#5B8DEF;">
            {{ row.shopName?.charAt(0) || '店' }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="shopName" label="申请店铺名" min-width="140" show-overflow-tooltip
        header-align="center" />
      <el-table-column prop="categoryName" label="品类" width="110" align="center" />
      <el-table-column prop="ownerName" label="申请人" min-width="150" align="center" show-overflow-tooltip />
      <el-table-column prop="phone" label="联系电话" width="130" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'warning'" size="small" effect="plain">
            {{ statusMap[row.status]?.label || '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.reviewedAt) || '' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" background
        @size-change="handleSizeChange" @current-change="handlePageChange"
      />
    </div>

    <!-- ====== 详情抽屉（含通过/拒绝操作，子组件） ====== -->
    <ApplicationDetailDrawer
      :visible="detailVisible"
      :application-id="detailAppId"
      @update:visible="detailVisible = $event"
      @audit-done="handleAuditDone"
    />

  </div>
</template>

<style scoped>
</style>
