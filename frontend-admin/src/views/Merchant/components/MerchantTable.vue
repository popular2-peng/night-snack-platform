<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listMerchants, getMerchantStats, listAllEnabledCategories,
  banMerchant, unbanMerchant, pauseMerchant, resumeMerchant, deleteMerchant,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import StatsStrip from '@/components/StatsStrip.vue'
import MerchantDetailDrawer from './MerchantDetailDrawer.vue'

// ---- 类型 ----
interface MerchantItem {
  id: number; name: string; ownerName: string; phone: string
  logoUrl: string; categoryName: string; todayOrders: number; todayRevenue: number
  avgRating: number; status: string
  lastLoginAt: string; createdAt: string; updatedAt: string
}
interface Stats { total: number; openCount: number; suspendedCount: number; closedCount: number }


// ---- 数据 ----
const merchants = ref<MerchantItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const categoryId = ref('')
const categoryOptions = ref<{ label: string; value: string }[]>([])
const dateRange = ref<string[]>(['', ''])
const loading = ref(false)

const stats = ref<Stats>({ total: 0, openCount: 0, suspendedCount: 0, closedCount: 0 })

// 选中行
const selectedIds = ref<number[]>([])

// 详情抽屉
const drawerVisible = ref(false)
const detailMerchantId = ref<number | null>(null)

// 封禁弹窗
const banVisible = ref(false)
const banTarget = ref<MerchantItem | null>(null)
const banReason = ref('')

// 编辑弹窗（占位）
const editVisible = ref(false)
const editTarget = ref<MerchantItem | null>(null)

// ---- 状态映射 ----
const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  OPEN: { label: '营业中', type: 'success' },
  CLOSED: { label: '已打烊', type: 'info' },
  SUSPENDED: { label: '已封禁', type: 'danger' },
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '营业中', value: 'OPEN' },
  { label: '已打烊', value: 'CLOSED' },
  { label: '已封禁', value: 'SUSPENDED' },
]

// ---- 数据获取 ----
const fetchMerchants = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (categoryId.value) params.categoryId = categoryId.value
    if (dateRange.value?.[0]) params.dateStart = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1]
    const res = await listMerchants(params)
    merchants.value = res.data.list || []
    total.value = res.data.total || (res.data.list || []).length
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getMerchantStats()
    stats.value = res.data
  } catch { /* ignore */ }
}

const fetchCategories = async () => {
  try {
    const res = await listAllEnabledCategories()
    categoryOptions.value = (res.data || []).map((c: any) => ({ label: `${c.icon || ''} ${c.name}`.trim(), value: String(c.id) }))
  } catch { /* ignore */ }
}

const handleSearch = () => { page.value = 1; fetchMerchants() }
const handleReset = () => { page.value = 1; fetchMerchants() }
const handleSizeChange = (val: number) => { size.value = val; fetchMerchants() }
const handlePageChange = (val: number) => { page.value = val; fetchMerchants() }

// ---- 选择 ----
const handleSelectionChange = (rows: MerchantItem[]) => {
  selectedIds.value = rows.map(r => r.id)
}

// ---- 详情抽屉 ----
const openDetail = (row: MerchantItem) => {
  detailMerchantId.value = row.id
  drawerVisible.value = true
}

// ---- 暂停/恢复 ----
const handlePause = async (row: MerchantItem) => {
  await ElMessageBox.confirm(`确定暂停「${row.name}」营业？`, '提示', { type: 'warning' })
  await pauseMerchant(row.id)
  ElMessage.success('已暂停营业')
  refreshAll()
}
const handleResume = async (row: MerchantItem) => {
  await resumeMerchant(row.id)
  ElMessage.success('已恢复营业')
  refreshAll()
}

// ---- 封禁/解封 ----
const openBan = (row: MerchantItem) => {
  banTarget.value = row
  banReason.value = ''
  banVisible.value = true
}
const confirmBan = async () => {
  if (!banReason.value.trim()) { ElMessage.warning('请输入封禁原因'); return }
  await banMerchant(banTarget.value!.id, banReason.value)
  ElMessage.success('封禁成功')
  banVisible.value = false
  refreshAll()
}
const handleUnban = async (row: MerchantItem) => {
  await unbanMerchant(row.id)
  ElMessage.success('解封成功')
  refreshAll()
}

// ---- 删除 ----
const handleDelete = async (row: MerchantItem) => {
  await ElMessageBox.confirm(`确定删除「${row.name}」？此操作不可恢复。`, '危险操作', { type: 'error', confirmButtonText: '确认删除' })
  await deleteMerchant(row.id)
  ElMessage.success('已删除')
  refreshAll()
}

// ---- 批量操作 ----
const batchBan = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选商家'); return }
  const { value: reason } = await ElMessageBox.prompt('请输入封禁原因', '批量封禁', { confirmButtonText: '确定', inputType: 'textarea' })
  if (!reason) return
  for (const id of selectedIds.value) {
    try { await banMerchant(id, reason) } catch { /* continue */ }
  }
  ElMessage.success('批量封禁完成')
  refreshAll()
}
const batchDelete = async () => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选商家'); return }
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个商家？`, '危险操作', { type: 'error' })
  for (const id of selectedIds.value) {
    try { await deleteMerchant(id) } catch { /* continue */ }
  }
  ElMessage.success('批量删除完成')
  refreshAll()
}

const refreshAll = () => {
  fetchMerchants()
  fetchStats()
}

// ---- 格式化 ----
const formatMoney = (n: number) => { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '' }

onMounted(() => { refreshAll(); fetchCategories() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条（公共组件） ====== -->
    <StatsStrip :items="[
      { value: stats.total, label: '商家总数' },
      { value: stats.openCount, label: '营业中', color: 'success' },
      { value: stats.suspendedCount, label: '已封禁', color: 'danger' },
      { value: stats.closedCount, label: '打烊中', color: 'info' },
    ]" />

    <!-- ====== 搜索 + 批量操作 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:category-value="categoryId"
      v-model:date-range="dateRange"
      placeholder="搜索店铺名/店主/电话"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      :category-options="categoryOptions"
      category-placeholder="品类筛选"
      date-label="入驻"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="refreshAll"
    >
      <template #extra>
        <template v-if="selectedIds.length">
          <el-button type="warning" plain @click="batchBan">批量封禁 ({{ selectedIds.length }})</el-button>
          <el-button type="danger" plain @click="batchDelete">批量删除 ({{ selectedIds.length }})</el-button>
        </template>
      </template>
    </SearchFilterBar>

    <!-- ====== 表格 ====== -->
    <el-table :data="merchants" stripe v-loading="loading" style="width: 100%"
      @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="42" fixed="left" />
      <el-table-column type="index" label="编号" width="56" align="center" />

      <el-table-column label="LOGO" width="72" align="center">
        <template #default="{ row }">
          <el-image v-if="row.logoUrl" :src="row.logoUrl" :preview-src-list="[row.logoUrl]" fit="cover"
            style="width:36px;height:36px;border-radius:4px;vertical-align:middle" :preview-teleported="true" />
          <el-avatar v-else :size="36" shape="square" style="background:#EEF3FD;color:#5B8DEF;">
            {{ row.name?.charAt(0) || '店' }}
          </el-avatar>
        </template>
      </el-table-column>

      <el-table-column prop="name" label="店铺名称" min-width="100" show-overflow-tooltip sortable="custom" align="left" header-align="center" />
      <el-table-column prop="categoryName" label="品类" width="110" align="center" />
      <el-table-column prop="ownerName" label="店主" width="125" align="center" />
      <el-table-column prop="phone" label="联系电话" width="130" align="center" />
      <el-table-column label="今日订单/营收" width="155" align="center">
        <template #default="{ row }">
          <span class="order-revenue-cell">
            <strong>{{ row.todayOrders ?? 0 }}</strong> 单 / {{ formatMoney(row.todayRevenue) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="avgRating" label="评分" width="80" align="center" sortable="custom" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'" size="small" effect="plain">
            {{ statusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="入驻时间" width="150" sortable="custom" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="修改时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.updatedAt) || '-' }}</template>
      </el-table-column>

      <!-- 操作列：下拉菜单 -->
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-dropdown trigger="click">
            <el-button size="small" :icon="MoreFilled" circle />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openDetail(row)">查看详情</el-dropdown-item>
                <el-dropdown-item @click="editTarget = row; editVisible = true">编辑</el-dropdown-item>
                <template v-if="row.status === 'SUSPENDED'">
                  <el-dropdown-item @click="handleUnban(row)" style="color:#67C23A">解封</el-dropdown-item>
                </template>
                <template v-else>
                  <el-dropdown-item v-if="row.status === 'OPEN'" @click="handlePause(row)">暂停营业</el-dropdown-item>
                  <el-dropdown-item v-if="row.status === 'CLOSED'" @click="handleResume(row)">恢复营业</el-dropdown-item>
                  <el-dropdown-item divided @click="openBan(row)" style="color:#F56C6C">封禁</el-dropdown-item>
                </template>
                <el-dropdown-item divided @click="handleDelete(row)" style="color:#F56C6C">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page" v-model:page-size="size"
        :total="total" :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next" background
        @size-change="handleSizeChange" @current-change="handlePageChange"
      />
    </div>

    <!-- ====== 详情抽屉（子组件） ====== -->
    <MerchantDetailDrawer
      :visible="drawerVisible"
      :merchant-id="detailMerchantId"
      @update:visible="drawerVisible = $event"
    />

    <!-- ====== 封禁弹窗 ====== -->
    <el-dialog v-model="banVisible" title="封禁商家" width="440px">
      <p style="margin-bottom:12px">封禁「<strong>{{ banTarget?.name }}</strong>」</p>
      <el-input v-model="banReason" type="textarea" :rows="3" placeholder="请输入封禁原因（必填）" maxlength="500" show-word-limit />
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!banReason.trim()" @click="confirmBan">确认封禁</el-button>
      </template>
    </el-dialog>

    <!-- ====== 编辑弹窗（骨架） ====== -->
    <el-dialog v-model="editVisible" title="编辑商家" width="500px">
      <p style="color:var(--text-secondary)">编辑功能将在后续版本实现。</p>
      <p>当前商家: <strong>{{ editTarget?.name }}</strong></p>
      <template #footer>
        <el-button @click="editVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
/* ---- 订单营收 ---- */
.order-revenue-cell { font-size: 13px; white-space: nowrap; }

/* ---- 详情 ---- */
</style>
