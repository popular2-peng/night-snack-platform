<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listMerchants, suspendMerchant, activateMerchant } from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

interface MerchantItem { id: number; name: string; ownerName: string; phone: string; status: string; createdAt: string }

const merchants = ref<MerchantItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const loading = ref(false)

async function fetchMerchants() {
  loading.value = true
  const params: any = { page: page.value, size: size.value }
  if (keyword.value) params.keyword = keyword.value
  if (statusFilter.value) params.status = statusFilter.value
  const res = await listMerchants(params)
  merchants.value = res.data.list
  total.value = res.data.total
  loading.value = false
}

function handleSearch() { page.value = 1; fetchMerchants() }
function handleSizeChange(val: number) { size.value = val; fetchMerchants() }
function handlePageChange(val: number) { page.value = val; fetchMerchants() }

async function handleSuspend(row: MerchantItem) {
  await ElMessageBox.confirm(`确定封禁商家 "${row.name}"？`, '提示', { type: 'warning' })
  await suspendMerchant(row.id)
  ElMessage.success('封禁成功')
  fetchMerchants()
}

async function handleActivate(row: MerchantItem) {
  await activateMerchant(row.id)
  ElMessage.success('解封成功')
  fetchMerchants()
}

const statusOptions = [
  { label: '全部', value: '' },
  { label: '营业中', value: 'OPEN' },
  { label: '已打烊', value: 'CLOSED' },
  { label: '已封禁', value: 'SUSPENDED' },
]

onMounted(fetchMerchants)
</script>

<template>
  <div class="page-card">
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索店铺名" clearable style="width: 220px" @keyup.enter="handleSearch" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="merchants" border stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="name" label="店铺名称" />
      <el-table-column prop="ownerName" label="店主" width="120" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 'OPEN' ? 'success' : row.status === 'CLOSED' ? 'info' : row.status === 'SUSPENDED' ? 'danger' : 'warning'"
            size="small"
          >
            {{ row.status === 'OPEN' ? '营业中' : row.status === 'CLOSED' ? '已打烊' : row.status === 'SUSPENDED' ? '已封禁' : row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="入驻时间" width="180" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'SUSPENDED'" size="small" type="success" @click="handleActivate(row)">解封</el-button>
          <el-button v-else size="small" type="danger" @click="handleSuspend(row)">封禁</el-button>
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
