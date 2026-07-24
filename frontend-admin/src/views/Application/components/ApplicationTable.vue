<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  listApplications, getApplicationDetail,
  approveApplicationV2, rejectApplicationV2,
} from '@/apis/admin'
import { ElMessage } from 'element-plus'
import SearchFilterBar from '@/components/SearchFilterBar.vue'

interface AppItem {
  id: number; shopName: string; applicantUsername: string; phone: string
  reason: string; logoUrl: string; categoryName: string
  status: string; rejectReason: string; createdAt: string
}

const apps = ref<AppItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

const appStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已拒绝', value: 'REJECTED' },
]

// 统计（独立请求全量数据，不受筛选影响）
const stats = ref({ total: 0, pending: 0, approved: 0, rejected: 0 })

async function fetchStats() {
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

const statusMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' }> = {
  PENDING: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'info' },
}

// ---- 数据获取（服务端筛选，与 MerchantTable 逻辑一致） ----
async function fetchApps() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    const res = await listApplications(params)
    apps.value = res.data.list || []
    total.value = res.data.total || (res.data.list || []).length
  } finally { loading.value = false }
}

function handleSearch() { page.value = 1; fetchApps() }
function handleSizeChange(val: number) { size.value = val; fetchApps() }
function handlePageChange(val: number) { page.value = val; fetchApps() }

// ---- 通过弹窗 ----
const approveVisible = ref(false)
const approveTarget = ref<AppItem | null>(null)
const approveForm = ref({ initialStatus: 'OPEN', deliveryRadius: 3000, reviewNote: '' })

function openApprove(row: AppItem) {
  approveTarget.value = row
  approveForm.value = { initialStatus: 'OPEN', deliveryRadius: 3000, reviewNote: '' }
  approveVisible.value = true
}
async function confirmApprove() {
  await approveApplicationV2(approveTarget.value!.id, approveForm.value)
  ElMessage.success('已通过')
  approveVisible.value = false
  fetchApps()
}

// ---- 拒绝弹窗 ----
const rejectVisible = ref(false)
const rejectTarget = ref<AppItem | null>(null)
const rejectReason = ref('')

const rejectCharCount = computed(() => rejectReason.value.length)
const rejectValid = computed(() => rejectCharCount.value >= 10 && rejectCharCount.value <= 200)

function openReject(row: AppItem) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}
async function confirmReject() {
  if (!rejectValid.value) { ElMessage.warning('拒绝原因需 10-200 字'); return }
  await rejectApplicationV2(rejectTarget.value!.id, rejectReason.value)
  ElMessage.success('已拒绝')
  rejectVisible.value = false
  fetchApps()
}

// ---- 详情抽屉 ----
const detailVisible = ref(false)
const detail = ref<any>({})

async function openDetail(row: AppItem) {
  try {
    const res = await getApplicationDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch { ElMessage.error('获取详情失败') }
}

// ---- 格式化 ----
function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '' }

onMounted(() => { fetchStats(); fetchApps() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条 ====== -->
    <div class="stats-strip">
      <div class="stat-item">
        <span class="stat-item-value">{{ stats.total }}</span>
        <span class="stat-item-label">申请总数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-warning">{{ stats.pending }}</span>
        <span class="stat-item-label">待审核</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-success">{{ stats.approved }}</span>
        <span class="stat-item-label">已通过</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-danger">{{ stats.rejected }}</span>
        <span class="stat-item-label">已拒绝</span>
      </div>
    </div>

    <!-- ====== 搜索 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      placeholder="搜索店铺名/申请人"
      :filter-options="appStatusOptions"
      filter-placeholder="状态筛选"
      @search="handleSearch"
    />

    <!-- ====== 表格 ====== -->
    <el-table :data="apps" stripe v-loading="loading" style="width: 100%">
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
      <el-table-column prop="applicantUsername" label="申请人" width="100" align="center" />
      <el-table-column prop="phone" label="联系电话" width="130" align="center" />
      <el-table-column prop="categoryName" label="品类" width="95" align="center" />
      <el-table-column prop="reason" label="申请理由" min-width="180" show-overflow-tooltip header-align="center" />
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
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="openApprove(row)">通过</el-button>
            <el-button size="small" type="danger" @click="openReject(row)">拒绝</el-button>
          </template>
          <el-button size="small" @click="openDetail(row)">详情</el-button>
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

    <!-- ====== 通过弹窗 ====== -->
    <el-dialog v-model="approveVisible" title="通过商家入驻申请" width="480px">
      <p style="margin-bottom:16px">店铺：<strong>{{ approveTarget?.shopName }}</strong> ｜ 申请人：<strong>{{ approveTarget?.applicantUsername }}</strong></p>
      <el-form label-width="100px" label-position="left">
        <el-form-item label="初始营业状态">
          <el-select v-model="approveForm.initialStatus" style="width:100%">
            <el-option label="营业中" value="OPEN" />
            <el-option label="打烊" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="配送半径">
          <el-input-number v-model="approveForm.deliveryRadius" :min="500" :max="20000" :step="500" style="width:100%" />
          <span style="margin-left:8px;font-size:12px;color:var(--text-secondary)">米</span>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="approveForm.reviewNote" type="textarea" :rows="3" placeholder="可选，内部留存"
            maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="success" @click="confirmApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- ====== 拒绝弹窗 ====== -->
    <el-dialog v-model="rejectVisible" title="拒绝商家入驻申请" width="480px">
      <p style="margin-bottom:12px">店铺：<strong>{{ rejectTarget?.shopName }}</strong> ｜ 申请人：<strong>{{ rejectTarget?.applicantUsername }}</strong></p>
      <el-input v-model="rejectReason" type="textarea" :rows="4"
        placeholder="请输入拒绝原因（必填，10-200 字，将展示给商家）"
        maxlength="200" show-word-limit />
      <p style="margin-top:6px;font-size:12px" :style="{ color: rejectValid ? 'var(--success)' : 'var(--danger)' }">
        当前 {{ rejectCharCount }} 字{{ rejectCharCount < 10 ? '（还需 ' + (10 - rejectCharCount) + ' 字）' : '' }}
      </p>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!rejectValid" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- ====== 详情抽屉 ====== -->
    <el-drawer v-model="detailVisible" title="申请详情" size="480px">
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
.stat-item-value.text-warning { color: var(--warning); }
.stat-item-value.text-success { color: var(--success); }
.stat-item-value.text-danger  { color: var(--danger); }
.stat-item-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.stat-divider { width: 1px; height: 32px; background: var(--border-color); }

/* ---- 详情 ---- */
.detail-logo { display: flex; align-items: center; gap: 16px; }
.detail-logo h3 { margin: 0 0 4px 0; font-size: 18px; }
.detail-logo p { margin: 0; color: var(--text-secondary); font-size: 13px; }

@media (max-width: 768px) {
  .stats-strip { gap: 16px; padding: 12px; }
  .stat-item-value { font-size: 18px; }
}
</style>
