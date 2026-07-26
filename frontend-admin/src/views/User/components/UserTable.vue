<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listUsers, getUserDetail, banUser, unbanUser, resetPassword, getUserLogs,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, Male, Female } from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'

// ---- 类型 ----
interface UserItem {
  id: number; username: string; nickname: string; phone: string
  avatarUrl: string; gender: number; status: string
  totalOrders: number; totalSpent: number; lastLoginAt: string; createdAt: string
}
interface UserDetail {
  id: number; username: string; nickname: string; phone: string
  avatarUrl: string; gender: number; birthday: string; status: string
  totalOrders: number; totalSpent: number
  lastLoginAt: string
  bannedReason: string; bannedAt: string; unbannedAt: string; remark: string
  createdAt: string; updatedAt: string
}
interface LogItem { time: string; action: string; detail: string }

// ---- 数据 ----
const users = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const dateRange = ref<string[]>(['', ''])
const loading = ref(false)

// 统计
const stats = ref({ total: 0, todayNew: 0, active: 0, disabled: 0, todayOrders: 0 })

// 详情 Drawer
const detailVisible = ref(false)
const detailUser = ref<UserDetail | null>(null)
const detailLogs = ref<LogItem[]>([])
const detailLoading = ref(false)

// 封禁弹窗
const banVisible = ref(false)
const banTarget = ref<UserItem | null>(null)
const banReason = ref('')
const banDuration = ref<number | null>(null)
const banDurationType = ref<'forever' | 'days' | 'custom'>('forever')

// 编辑弹窗（骨架）
const editVisible = ref(false)
const editTarget = ref<UserItem | null>(null)

// 重置密码弹窗
const resetPwdVisible = ref(false)
const resetPwdTarget = ref<UserItem | null>(null)
const newPassword = ref('')

// ---- 状态映射 ----
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '已封禁', value: 'DISABLED' },
]

const genderMap = ['', '男', '女']

// ---- 数据获取 ----
async function fetchUsers() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (dateRange.value?.[0]) params.dateStart = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1]
    const res = await listUsers(params)
    users.value = res.data.list || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

async function fetchStats() {
  try {
    // 拉全量数据计算统计
    const [allRes, disabledRes] = await Promise.all([
      listUsers({ page: 1, size: 1 }),
      listUsers({ page: 1, size: 1, status: 'DISABLED' }),
    ])
    stats.value.total = allRes.data.total || 0
    stats.value.disabled = disabledRes.data.total || 0
    stats.value.active = stats.value.total - stats.value.disabled
    // 今日新增和今日订单数在后端未提供的情况下暂设 0
  } catch { /* ignore */ }
}

function handleSearch() { page.value = 1; fetchUsers() }
function handleReset() { page.value = 1; fetchUsers() }
function handleRefresh() { fetchUsers(); fetchStats() }
function handleSizeChange(val: number) { size.value = val; fetchUsers() }
function handlePageChange(val: number) { page.value = val; fetchUsers() }

// ---- 格式化 ----
function formatMoney(n: number) { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
function formatTime(s: string) { return s ? s.replace('T', ' ').substring(0, 16) : '-' }

// ---- 详情 Drawer ----
async function openDetail(row: UserItem) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [dRes, lRes] = await Promise.all([
      getUserDetail(row.id),
      getUserLogs(row.id),
    ])
    detailUser.value = dRes.data
    detailLogs.value = lRes.data || []
  } finally { detailLoading.value = false }
}

// ---- 封禁 ----
function openBan(row: UserItem) {
  banTarget.value = row
  banReason.value = ''
  banDuration.value = null
  banDurationType.value = 'forever'
  banVisible.value = true
}
async function confirmBan() {
  if (!banReason.value.trim()) { ElMessage.warning('请输入封禁原因'); return }
  if (banDurationType.value === 'days' && !banDuration.value) { ElMessage.warning('请选择封禁天数'); return }
  if (banDurationType.value === 'custom' && (!banDuration.value || banDuration.value <= 0)) { ElMessage.warning('请输入有效的封禁天数'); return }

  let durationDays: number | null = null
  if (banDurationType.value === 'days') durationDays = banDuration.value
  else if (banDurationType.value === 'custom') durationDays = banDuration.value

  await banUser(banTarget.value!.id, { reason: banReason.value, durationDays })
  ElMessage.success('封禁成功')
  banVisible.value = false
  fetchUsers()
  fetchStats()
}

// ---- 解封 ----
async function handleUnban(row: UserItem) {
  await ElMessageBox.confirm(`确定解封用户「${row.username}」？`, '提示', { type: 'warning' })
  await unbanUser(row.id)
  ElMessage.success('解封成功')
  fetchUsers()
  fetchStats()
}

// ---- 重置密码 ----
function openResetPwd(row: UserItem) {
  resetPwdTarget.value = row
  newPassword.value = ''
  resetPwdVisible.value = true
}
async function confirmResetPwd() {
  const res = await resetPassword(resetPwdTarget.value!.id)
  newPassword.value = res.data
  ElMessage.success('密码已重置')
}
function copyPassword() {
  navigator.clipboard.writeText(newPassword.value)
  ElMessage.success('已复制到剪贴板')
}

// ---- 编辑（骨架） ----
function openEdit(row: UserItem) {
  editTarget.value = row
  editVisible.value = true
}

onMounted(() => { fetchUsers(); fetchStats() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条 ====== -->
    <div class="stats-strip">
      <div class="stat-item">
        <span class="stat-item-value">{{ stats.total }}</span>
        <span class="stat-item-label">用户总数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-primary">{{ stats.todayNew }}</span>
        <span class="stat-item-label">今日新增</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-success">{{ stats.active }}</span>
        <span class="stat-item-label">活跃用户</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-danger">{{ stats.disabled }}</span>
        <span class="stat-item-label">已封禁</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-warning">{{ stats.todayOrders }}</span>
        <span class="stat-item-label">今日订单数</span>
      </div>
    </div>

    <!-- ====== 搜索 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:date-range="dateRange"
      placeholder="搜索用户名/手机号/昵称"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="handleRefresh"
    />

    <!-- ====== 表格 ====== -->
    <el-table :data="users" stripe v-loading="loading" style="width: 100%">
      <el-table-column type="index" label="编号" width="60" align="center" />

      <!-- 头像 -->
      <el-table-column label="头像" width="64" align="center">
        <template #default="{ row }">
          <el-avatar v-if="row.avatarUrl" :src="row.avatarUrl" :size="32" shape="circle" />
          <el-avatar v-else :size="32" shape="circle" :style="{ background: '#EEF3FD', color: '#5B8DEF' }">
            {{ row.nickname?.charAt(0) || row.username?.charAt(0) || 'U' }}
          </el-avatar>
        </template>
      </el-table-column>

      <!-- 用户名 -->
      <el-table-column prop="username" label="用户名" min-width="110" align="left" show-overflow-tooltip />

      <!-- 昵称 -->
      <el-table-column prop="nickname" label="昵称" min-width="100" align="left" show-overflow-tooltip>
        <template #default="{ row }">
          <span style="color:var(--text-secondary)">{{ row.nickname || '-' }}</span>
        </template>
      </el-table-column>

      <!-- 手机号 -->
      <el-table-column prop="phone" label="手机号" width="140" align="center" />

      <!-- 性别 -->
      <el-table-column label="性别" width="70" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.gender === 1" color="#409EFF" :size="18"><Male /></el-icon>
          <el-icon v-else-if="row.gender === 2" color="#F56C6C" :size="18"><Female /></el-icon>
          <span v-else style="color:var(--text-placeholder)">-</span>
        </template>
      </el-table-column>

      <!-- 订单数 -->
      <el-table-column prop="totalOrders" label="订单数" width="90" align="center" sortable="custom" />

      <!-- 累计消费 -->
      <el-table-column label="累计消费" width="120" align="center" sortable="custom">
        <template #default="{ row }">{{ formatMoney(row.totalSpent) }}</template>
      </el-table-column>

      <!-- 状态 -->
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small" effect="plain">
            {{ row.status === 'ACTIVE' ? '正常' : '已封禁' }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 注册时间 -->
      <el-table-column label="注册时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>

      <!-- 最后登录 -->
      <el-table-column label="最后登录" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.lastLoginAt) || '-' }}</template>
      </el-table-column>

      <!-- 操作：下拉菜单 -->
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-dropdown trigger="click">
            <el-button size="small" :icon="MoreFilled" circle />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openDetail(row)">查看详情</el-dropdown-item>
                <el-dropdown-item @click="openEdit(row)">编辑</el-dropdown-item>
                <el-dropdown-item @click="openResetPwd(row)">重置密码</el-dropdown-item>
                <template v-if="row.status === 'ACTIVE'">
                  <el-dropdown-item divided @click="openBan(row)" style="color:#F56C6C">封禁</el-dropdown-item>
                </template>
                <template v-else>
                  <el-dropdown-item divided @click="handleUnban(row)" style="color:#67C23A">解封</el-dropdown-item>
                </template>
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

    <!-- ====== 详情 Drawer ====== -->
    <el-drawer v-model="detailVisible" title="用户详情" size="460px" direction="rtl">
      <template v-if="detailUser" v-loading="detailLoading">
        <div class="drawer-section">
          <el-avatar :src="detailUser.avatarUrl" :size="64" shape="circle" style="margin-bottom:12px">
            {{ detailUser.nickname?.charAt(0) || detailUser.username?.charAt(0) || 'U' }}
          </el-avatar>
          <p><strong>用户名：</strong>{{ detailUser.username }}</p>
          <p><strong>昵称：</strong>{{ detailUser.nickname || '-' }}</p>
          <p><strong>手机号：</strong>{{ detailUser.phone }}</p>
          <p><strong>性别：</strong>{{ genderMap[detailUser.gender] || '未知' }}</p>
          <p><strong>生日：</strong>{{ detailUser.birthday || '-' }}</p>
          <p><strong>状态：</strong>
            <el-tag :type="detailUser.status === 'ACTIVE' ? 'success' : 'danger'" size="small" effect="plain">
              {{ detailUser.status === 'ACTIVE' ? '正常' : '已封禁' }}
            </el-tag>
          </p>
          <p><strong>注册时间：</strong>{{ formatTime(detailUser.createdAt) }}</p>
          <p><strong>最后登录：</strong>{{ formatTime(detailUser.lastLoginAt) || '-' }}</p>
          <p><strong>总订单数：</strong>{{ detailUser.totalOrders || 0 }}</p>
          <p><strong>累计消费：</strong>{{ formatMoney(detailUser.totalSpent) }}</p>
          <p v-if="detailUser.bannedReason"><strong>封禁原因：</strong>{{ detailUser.bannedReason }}</p>
          <p v-if="detailUser.remark"><strong>备注：</strong>{{ detailUser.remark }}</p>
        </div>

        <el-divider>操作日志</el-divider>
        <div class="drawer-section">
          <el-timeline v-if="detailLogs.length">
            <el-timeline-item
              v-for="(l, i) in detailLogs"
              :key="i"
              :timestamp="l.time"
              placement="top"
            >
              <strong>{{ l.action }}</strong>：{{ l.detail }}
            </el-timeline-item>
          </el-timeline>
          <p v-else style="color:var(--text-secondary)">暂无操作日志</p>
        </div>
      </template>
      <el-skeleton v-else :rows="8" animated />
    </el-drawer>

    <!-- ====== 封禁弹窗 ====== -->
    <el-dialog v-model="banVisible" title="封禁用户" width="460px">
      <p style="margin-bottom:12px">封禁「<strong>{{ banTarget?.username }}</strong>」</p>
      <p style="color:var(--text-secondary);margin-bottom:16px">
        当前状态：<el-tag :type="banTarget?.status === 'ACTIVE' ? 'success' : 'danger'" size="small" effect="plain">
          {{ banTarget?.status === 'ACTIVE' ? '正常' : '已封禁' }}
        </el-tag>
      </p>

      <el-form label-position="top">
        <el-form-item label="封禁原因（必填）" required>
          <el-input v-model="banReason" type="textarea" :rows="3" placeholder="请输入封禁原因" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="封禁时长">
          <el-radio-group v-model="banDurationType">
            <el-radio value="forever">永久</el-radio>
            <el-radio value="days">7 天</el-radio>
            <el-radio value="custom">自定义</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="banDurationType === 'days'">
          <el-radio-group v-model="banDuration">
            <el-radio-button :value="7">7 天</el-radio-button>
            <el-radio-button :value="30">30 天</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="banDurationType === 'custom'" label="自定义天数">
          <el-input-number v-model="banDuration" :min="1" :max="365" placeholder="请输入天数" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" :disabled="!banReason.trim()" @click="confirmBan">确认封禁</el-button>
      </template>
    </el-dialog>

    <!-- ====== 重置密码弹窗 ====== -->
    <el-dialog v-model="resetPwdVisible" title="重置密码" width="420px">
      <p>为用户「<strong>{{ resetPwdTarget?.username }}</strong>」重置密码</p>
      <p v-if="newPassword" style="margin:16px 0">
        新密码：<el-tag type="success" size="large" style="font-family:monospace;font-size:16px;cursor:pointer" @click="copyPassword">{{ newPassword }}</el-tag>
        <el-button size="small" type="primary" plain style="margin-left:8px" @click="copyPassword">复制</el-button>
      </p>
      <p v-if="!newPassword" style="color:var(--text-secondary);margin:16px 0">确认后将随机生成一个新密码。</p>
      <template #footer>
        <el-button @click="resetPwdVisible = false">关闭</el-button>
        <el-button v-if="!newPassword" type="primary" @click="confirmResetPwd">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- ====== 编辑弹窗（骨架） ====== -->
    <el-dialog v-model="editVisible" title="编辑用户" width="500px">
      <p style="color:var(--text-secondary)">编辑功能将在后续版本实现。</p>
      <p>当前用户: <strong>{{ editTarget?.username }}</strong></p>
      <template #footer>
        <el-button @click="editVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
/* ---- 统计条 ---- */
.stats-strip {
  display: flex; align-items: center; gap: 20px;
  margin-bottom: 20px; padding: 16px 20px;
  background: var(--bg); border-radius: var(--border-radius-sm);
  flex-wrap: wrap;
}
.stat-item { display: flex; flex-direction: column; }
.stat-item-value {
  font-size: 22px; font-weight: 700; color: var(--text-primary);
  line-height: 1.2; font-variant-numeric: tabular-nums;
}
.stat-item-value.text-success  { color: var(--success); }
.stat-item-value.text-danger   { color: var(--danger); }
.stat-item-value.text-warning  { color: var(--warning); }
.stat-item-value.text-primary  { color: var(--color-primary, #5B8DEF); }
.stat-item-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.stat-divider { width: 1px; height: 32px; background: var(--border-color); }

/* ---- Drawer ---- */
.drawer-section { padding: 0 4px; }
.drawer-section p { margin: 8px 0; line-height: 1.6; }

/* ---- 分页 ---- */
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
