<script setup lang="ts">
import { ref, onMounted } from 'vue'
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

function handleSearch() {
  page.value = 1
  fetchUsers()
}

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

onMounted(fetchUsers)
</script>

<template>
  <div class="page-card">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索用户名/手机号" clearable style="width: 260px" @clear="handleSearch" @keyup.enter="handleSearch" />
      <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="users" stripe v-loading="loading" style="width: 100%" :header-cell-style="{ background: '#fafafa', fontWeight: 600, color: '#303133' }">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="username" label="用户名" />
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
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
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
          <span v-else class="text-disabled">—</span>
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
.text-disabled {
  color: #c0c4cc;
}
</style>
