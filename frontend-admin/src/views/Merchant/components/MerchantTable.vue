<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listMerchants, getMerchantStats,
  banMerchant, unbanMerchant, pauseMerchant, resumeMerchant, deleteMerchant,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled } from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import MerchantDetailDrawer from './MerchantDetailDrawer.vue'

// ---- 类型 ----
interface MerchantItem {
  id: number; name: string; ownerName: string; phone: string
  logoUrl: string; categoryName: string; todayOrders: number; todayRevenue: number
  avgRating: number; status: string; lastLoginAt: string; createdAt: string
}
interface Stats { total: number; openCount: number; pendingCount: number; suspendedCount: number; closedCount: number }


// ---- 数据 ----
const merchants = ref<MerchantItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const stats = ref<Stats>({ total: 0, openCount: 0, pendingCount: 0, suspendedCount: 0, closedCount: 0 })

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
  PENDING: { label: '审核中', type: 'warning' },
}

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '营业中', value: 'OPEN' },
  { label: '已打烊', value: 'CLOSED' },
  { label: '已封禁', value: 'SUSPENDED' },
  { label: '审核中', value: 'PENDING' },
]

// ---- 数据获取 ----
async function fetchMerchants() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await listMerchants(params)
    merchants.value = res.data.list || []
    total.value = res.data.total || (res.data.list || []).length
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    const res = await getMerchantStats()
    stats.value = res.data
  } catch { /* ignore */ }
}

function handleSearch() { page.value = 1; fetchMerchants() }
function handleSizeChange(val: number) { size.value = val; fetchMerchants() }
function handlePageChange(val: number) { page.value = val; fetchMerchants() }

// ---- 选择 ----
function handleSelectionChange(rows: MerchantItem[]) {
  selectedIds.value = rows.map(r => r.id)
}

// ---- 详情抽屉 ----
function openDetail(row: MerchantItem) {
  detailMerchantId.value = row.id
  drawerVisible.value = true
}

// ---- 暂停/恢复 ----
async function handlePause(row: MerchantItem) {
  await ElMessageBox.confirm(`确定暂停「${row.name}」营业？`, '提示', { type: 'warning' })
  await pauseMerchant(row.id)
  ElMessage.success('已暂停营业')
  refreshAll()
}
async function handleResume(row: MerchantItem) {
  await resumeMerchant(row.id)
  ElMessage.success('已恢复营业')
  refreshAll()
}

// ---- 封禁/解封 ----
function openBan(row: MerchantItem) {
  banTarget.value = row
  banReason.value = ''
  banVisible.value = true
}
async function confirmBan() {
  if (!banReason.value.trim()) { ElMessage.warning('请输入封禁原因'); return }
  await banMerchant(banTarget.value!.id, banReason.value)
  ElMessage.success('封禁成功')
  banVisible.value = false
  refreshAll()
}
async function handleUnban(row: MerchantItem) {
  await unbanMerchant(row.id)
  ElMessage.success('解封成功')
  refreshAll()
}

// ---- 删除 ----
async function handleDelete(row: MerchantItem) {
  await ElMessageBox.confirm(`确定删除「${row.name}」？此操作不可恢复。`, '危险操作', { type: 'error', confirmButtonText: '确认删除' })
  await deleteMerchant(row.id)
  ElMessage.success('已删除')
  refreshAll()
}

// ---- 批量操作 ----
async function batchBan() {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选商家'); return }
  const { value: reason } = await ElMessageBox.prompt('请输入封禁原因', '批量封禁', { confirmButtonText: '确定', inputType: 'textarea' })
  if (!reason) return
  for (const id of selectedIds.value) {
    try { await banMerchant(id, reason) } catch { /* continue */ }
  }
  ElMessage.success('批量封禁完成')
  refreshAll()
}
async function batchDelete() {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选商家'); return }
  await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个商家？`, '危险操作', { type: 'error' })
  for (const id of selectedIds.value) {
    try { await deleteMerchant(id) } catch { /* continue */ }
  }
  ElMessage.success('批量删除完成')
  refreshAll()
}

function refreshAll() {
  fetchMerchants()
  fetchStats()
}

// ---- 格式化 ----
function formatMoney(n: number) { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '' }

onMounted(refreshAll)
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条 ====== -->
    <div class="stats-strip">
      <div class="stat-item">
        <span class="stat-item-value">{{ stats.total }}</span>
        <span class="stat-item-label">商家总数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-success">{{ stats.openCount }}</span>
        <span class="stat-item-label">营业中</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-warning">{{ stats.pendingCount }}</span>
        <span class="stat-item-label">待审核</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-danger">{{ stats.suspendedCount }}</span>
        <span class="stat-item-label">已封禁</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-info">{{ stats.closedCount }}</span>
        <span class="stat-item-label">打烊中</span>
      </div>
    </div>

    <!-- ====== 搜索 + 批量操作 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      placeholder="搜索店铺名/店主/电话"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      @search="handleSearch"
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

      <el-table-column prop="name" label="店铺名称" min-width="130" show-overflow-tooltip sortable="custom" align="left" header-align="center" />
      <el-table-column prop="categoryName" label="品类" width="110" align="center" />
      <el-table-column prop="ownerName" label="店主" width="105" align="center" />
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
      <el-table-column label="最后登录" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.lastLoginAt) || '-' }}</template>
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
/* ---- 统计条 ---- */
.stats-strip {
  display: flex; align-items: center; gap: 24px;
  margin-bottom: 20px; padding: 16px 20px;
  background: var(--bg); border-radius: var(--border-radius-sm);
  flex-wrap: wrap;
}
.stat-item { display: flex; flex-direction: column; }
.stat-item-value {
  font-size: 22px; font-weight: 700; color: var(--text-primary);
  line-height: 1.2; font-variant-numeric: tabular-nums;
}
.stat-item-value.text-success { color: var(--success); }
.stat-item-value.text-warning { color: var(--warning); }
.stat-item-value.text-danger  { color: var(--danger); }
.stat-item-value.text-info    { color: var(--info); }
.stat-item-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.stat-divider { width: 1px; height: 32px; background: var(--border-color); }

/* ---- 订单营收 ---- */
.order-revenue-cell { font-size: 13px; white-space: nowrap; }

/* ---- 详情 ---- */
/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .stats-strip { gap: 16px; padding: 12px; }
  .stat-item-value { font-size: 18px; }
}
</style>
