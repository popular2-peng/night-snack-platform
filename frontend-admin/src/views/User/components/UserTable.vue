<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  listUsers, getUserDetail, banUser, unbanUser, resetPassword, getUserLogs, deleteUser,
  batchUnbanUsers, batchDeleteUsers,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  MoreFilled, Male, Female, Phone, Calendar, ShoppingBag, Coin, User, WarningFilled,
} from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import StatsStrip from '@/components/StatsStrip.vue'

// ---- 类型 ----
interface UserItem {
  id: number; username: string; phone: string
  avatarUrl: string; gender: number; status: string
  totalOrders: number; totalSpent: number
  lastLoginAt: string; createdAt: string; updatedAt: string
}
interface UserDetail {
  id: number; username: string; phone: string
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

// 批量操作
const selectedIds = ref<number[]>([])
const batchLoading = ref(false)

const runBatch = async (fn: (ids: number[]) => Promise<any>, action: string) => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选用户'); return }
  batchLoading.value = true
  try {
    const res = await fn([...selectedIds.value])
    ElMessage.success(res.data.message || `${action}完成`)
    selectedIds.value = []
    fetchUsers()
    fetchStats()
  } catch { /* 拦截器已提示 */ }
  finally { batchLoading.value = false }
}

// ---- 状态映射 ----
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '正常', value: 'ACTIVE' },
  { label: '已封禁', value: 'DISABLED' },
]

const genderMap = ['', '男', '女']

// ---- 数据获取 ----
const fetchUsers = async () => {
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

const fetchStats = async () => {
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

const handleSearch = () => { page.value = 1; fetchUsers() }
const handleReset = () => { page.value = 1; fetchUsers() }
const handleRefresh = () => { fetchUsers(); fetchStats() }
const handleSizeChange = (val: number) => { size.value = val; fetchUsers() }
const handlePageChange = (val: number) => { page.value = val; fetchUsers() }

// ---- 格式化 ----
const formatMoney = (n: number) => { return n != null ? '¥' + Number(n).toFixed(2) : '¥0.00' }
const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '-' }

// ---- 详情 Drawer ----
const openDetail = async (row: UserItem) => {
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
const openBan = (row: UserItem) => {
  banTarget.value = row
  banReason.value = ''
  banDuration.value = null
  banDurationType.value = 'forever'
  banVisible.value = true
}
const confirmBan = async () => {
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
const handleUnban = async (row: UserItem) => {
  await ElMessageBox.confirm(`确定解封用户「${row.username}」？`, '提示', { type: 'warning' })
  await unbanUser(row.id)
  ElMessage.success('解封成功')
  fetchUsers()
  fetchStats()
}

// ---- 删除（物理删除 + 级联清理订单/地址） ----
const handleDelete = async (row: UserItem) => {
  await ElMessageBox.confirm(
    `确定删除用户「${row.username}」？删除后将同时清理其订单、收货地址等关联数据，且不可恢复！`,
    '危险操作', { type: 'error', confirmButtonText: '确认删除' })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  fetchUsers()
  fetchStats()
}

// ---- 重置密码 ----
const openResetPwd = (row: UserItem) => {
  resetPwdTarget.value = row
  newPassword.value = ''
  resetPwdVisible.value = true
}
const confirmResetPwd = async () => {
  const res = await resetPassword(resetPwdTarget.value!.id)
  newPassword.value = res.data
  ElMessage.success('密码已重置')
}
const copyPassword = () => {
  navigator.clipboard.writeText(newPassword.value)
  ElMessage.success('已复制到剪贴板')
}

// ---- 编辑（骨架） ----
const openEdit = (row: UserItem) => {
  editTarget.value = row
  editVisible.value = true
}

onMounted(() => { fetchUsers(); fetchStats() })
</script>

<template>
  <div class="page-card">

    <!-- ====== 统计条（公共组件） ====== -->
    <StatsStrip :items="[
      { value: stats.total, label: '用户总数' },
      { value: stats.todayNew, label: '今日新增', color: 'primary' },
      { value: stats.active, label: '活跃用户', color: 'success' },
      { value: stats.disabled, label: '已封禁', color: 'danger' },
      { value: stats.todayOrders, label: '今日订单数', color: 'warning' },
    ]" />

    <!-- ====== 搜索 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:date-range="dateRange"
      placeholder="搜索用户名/手机号"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      date-label="注册"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="handleRefresh"
    >
      <!-- 批量操作（与商家管理页一致：搜索栏右侧，选中后显示；封禁需选时长走单个封禁） -->
      <template #extra>
        <template v-if="selectedIds.length">
          <el-button type="success" plain :loading="batchLoading" @click="runBatch(batchUnbanUsers, '批量启用')">
            批量启用 ({{ selectedIds.length }})
          </el-button>
          <el-button type="danger" plain :loading="batchLoading" @click="runBatch(batchDeleteUsers, '批量删除')">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </template>
      </template>
    </SearchFilterBar>

    <!-- ====== 表格 ====== -->
    <el-table :data="users" stripe v-loading="loading" style="width: 100%"
      @selection-change="(rows: UserItem[]) => selectedIds = rows.map(r => r.id)">
      <el-table-column type="selection" width="42" fixed="left" />
      <el-table-column type="index" label="编号" width="60" align="center" />

      <!-- 头像 -->
      <el-table-column label="头像" width="64" align="center">
        <template #default="{ row }">
          <el-avatar v-if="row.avatarUrl" :src="row.avatarUrl" :size="32" shape="circle" />
          <el-avatar v-else :size="32" shape="circle" :style="{ background: '#EEF3FD', color: '#5B8DEF' }">
            {{ row.username?.charAt(0) || 'U' }}
          </el-avatar>
        </template>
      </el-table-column>

      <!-- 用户名 -->
      <el-table-column prop="username" label="用户名" min-width="130" align="left" show-overflow-tooltip />

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
      <el-table-column label="注册时间" min-width="165" align="center" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>

      <!-- 最后登录 -->
      <el-table-column label="最后登录" min-width="165" align="center" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTime(row.lastLoginAt) || '-' }}</template>
      </el-table-column>

      <!-- 更新时间 -->
      <el-table-column label="更新时间" min-width="165" align="center" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTime(row.updatedAt) || '-' }}</template>
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
                  <el-dropdown-item divided @click="handleDelete(row)" style="color:#F56C6C">删除</el-dropdown-item>
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

    <!-- ====== 详情 Drawer（与商家详情同风格） ====== -->
    <el-drawer v-model="detailVisible" title="用户详情" size="520px" direction="rtl">
      <template v-if="detailUser" v-loading="detailLoading">
        <!-- 头部卡片 -->
        <div class="drawer-hero">
          <div class="hero-bg"></div>
          <div class="hero-content">
            <el-avatar
              v-if="detailUser.avatarUrl"
              :src="detailUser.avatarUrl"
              :size="72"
              shape="square"
              class="hero-logo"
            />
            <el-avatar v-else :size="72" shape="square" class="hero-logo-placeholder">
              {{ detailUser.username?.charAt(0) || 'U' }}
            </el-avatar>
            <div class="hero-info">
              <div class="hero-name-row">
                <h2>{{ detailUser.username }}</h2>
                <el-tag
                  :type="detailUser.status === 'ACTIVE' ? 'success' : 'danger'"
                  size="small"
                  effect="dark"
                  round
                >
                  {{ detailUser.status === 'ACTIVE' ? '正常' : '已封禁' }}
                </el-tag>
              </div>
              <p class="hero-sub">{{ detailUser.phone || '-' }}</p>
            </div>
          </div>
        </div>

        <!-- 数据卡片 -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-icon" style="background:#EEF4FF;color:#5B8DEF"><el-icon><ShoppingBag /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num">{{ detailUser.totalOrders || 0 }}</span>
              <span class="stat-label">总订单数</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#F0FAF0;color:#67C23A"><el-icon><Coin /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num">{{ formatMoney(detailUser.totalSpent) }}</span>
              <span class="stat-label">累计消费</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#FFF8ED;color:#E6A23C"><el-icon><User /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num">{{ genderMap[detailUser.gender] || '未知' }}</span>
              <span class="stat-label">性别</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background:#FDF2F2;color:#F56C6C"><el-icon><Calendar /></el-icon></div>
            <div class="stat-body">
              <span class="stat-num stat-num-sm">{{ detailUser.birthday || '-' }}</span>
              <span class="stat-label">生日</span>
            </div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="section">
          <div class="section-title">基本信息</div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label"><el-icon><Phone /></el-icon>手机号</span>
              <span class="info-value">{{ detailUser.phone }}</span>
            </div>
            <div class="info-row" v-if="detailUser.remark">
              <span class="info-label">备注</span>
              <span class="info-value">{{ detailUser.remark }}</span>
            </div>
          </div>
        </div>

        <!-- 封禁信息 -->
        <div v-if="detailUser.bannedReason" class="section banned-section">
          <div class="section-title danger-title">
            <el-icon><WarningFilled /></el-icon>封禁信息
          </div>
          <p class="banned-text">原因：{{ detailUser.bannedReason }}</p>
          <p v-if="detailUser.bannedAt" class="banned-text">时间：{{ formatTime(detailUser.bannedAt) }}</p>
          <p v-if="detailUser.unbannedAt" class="banned-text">解封：{{ formatTime(detailUser.unbannedAt) }}</p>
        </div>

        <!-- 时间信息 -->
        <div class="section">
          <div class="section-title">时间信息</div>
          <div class="info-list">
            <div class="info-row">
              <span class="info-label">注册时间</span>
              <span class="info-value">{{ formatTime(detailUser.createdAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">最后登录</span>
              <span class="info-value">{{ formatTime(detailUser.lastLoginAt) || '-' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">修改时间</span>
              <span class="info-value">{{ formatTime(detailUser.updatedAt) || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 操作日志 -->
        <div class="section">
          <div class="section-title">操作日志</div>
          <div v-if="detailLogs.length" class="timeline-wrap">
            <el-timeline>
              <el-timeline-item
                v-for="(l, i) in detailLogs"
                :key="i"
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
          <p v-else style="color:var(--text-secondary);font-size:13px">暂无操作日志</p>
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
/* ---- Drawer：头部卡片 ---- */
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
.stat-num-sm { font-size: 14px; }
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
.banned-text { margin: 0 0 4px; font-size: 13px; color: #F56C6C; line-height: 1.6; }

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
:deep(.el-timeline-item__timestamp) {
  font-size: 12px; color: var(--text-secondary, #909399);
}

/* ---- 分页 ---- */
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
