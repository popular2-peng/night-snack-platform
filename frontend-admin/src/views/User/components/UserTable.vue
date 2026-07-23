<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listUsers, disableUser, enableUser } from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

interface UserItem { id: number; username: string; phone: string; role: string; status: string; createdAt: string }

const users = ref<UserItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const loading = ref(false)

const roleMap: Record<string, { label: string; type: '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  ADMIN: { label: '管理员', type: 'danger' },
  MERCHANT: { label: '商家', type: 'warning' },
  USER: { label: '用户', type: '' },
}

async function fetchUsers() {
  loading.value = true
  const res = await listUsers({ page: page.value, size: size.value, keyword: keyword.value })
  users.value = res.data.list
  total.value = res.data.total
  loading.value = false
}

function handleSearch() { page.value = 1; fetchUsers() }
function handleSizeChange(val: number) { size.value = val; fetchUsers() }
function handlePageChange(val: number) { page.value = val; fetchUsers() }

async function handleDisable(row: UserItem) {
  await ElMessageBox.confirm(`确定封禁用户 "${row.username}"？`, '提示', { type: 'warning' })
  await disableUser(row.id)
  ElMessage.success('封禁成功')
  fetchUsers()
}

async function handleEnable(row: UserItem) {
  await enableUser(row.id)
  ElMessage.success('解封成功')
  fetchUsers()
}

const activeCount = computed(() => users.value.filter(u => u.status === 'ACTIVE').length)

onMounted(fetchUsers)
</script>

<template>
  <div class="page-card">
    <!-- 数据统计条 -->
    <div class="stats-strip">
      <div class="stat-item">
        <span class="stat-item-value">{{ total }}</span>
        <span class="stat-item-label">用户总数</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value text-success">{{ activeCount }}</span>
        <span class="stat-item-label">活跃用户</span>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/手机号" clearable style="width: 280px" @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="users" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="role" label="角色" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="roleMap[row.role]?.type || 'info'" size="small">
            {{ roleMap[row.role]?.label || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small" effect="plain">
            {{ row.status === 'ACTIVE' ? '正常' : '已封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="180" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.role !== 'ADMIN'">
            <el-button v-if="row.status === 'ACTIVE'" size="small" type="danger" plain @click="handleDisable(row)">封禁</el-button>
            <el-button v-else size="small" type="success" plain @click="handleEnable(row)">解封</el-button>
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
.stat-item-value.text-success { color: var(--success); }
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
