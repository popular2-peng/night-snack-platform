<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  listCategories, getCategoryStats,
  createCategory, updateCategory, enableCategory, disableCategory, deleteCategory,
  batchEnableCategories, batchDisableCategories, batchDeleteCategories,
} from '@/apis/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import SearchFilterBar from '@/components/SearchFilterBar.vue'
import StatsStrip from '@/components/StatsStrip.vue'

// ---- types ----
interface CategoryItem {
  id: number; name: string; icon: string; iconType: string
  sortOrder: number; status: string; remark: string
  createdAt: string; updatedAt: string
}

// ---- data ----
const categories = ref<CategoryItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const dateRange = ref<string[]>(['', ''])
const loading = ref(false)

const stats = ref({ total: 0, enabled: 0, disabled: 0 })

// ---- 批量操作 ----
const selectedIds = ref<number[]>([])
const batchLoading = ref(false)

const runBatch = async (fn: (ids: number[]) => Promise<any>, action: string) => {
  if (!selectedIds.value.length) { ElMessage.warning('请先勾选品类'); return }
  batchLoading.value = true
  try {
    const res = await fn([...selectedIds.value])
    ElMessage.success(res.data.message || `${action}完成`)
    selectedIds.value = []
    fetchCategories()
    fetchStats()
  } catch { /* 拦截器已提示 */ }
  finally { batchLoading.value = false }
}

// dialog visibility
const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editId = ref<number | null>(null)

// form fields
const formName = ref('')
const formSortOrder = ref(1)
const formIcon = ref('🍖')
const formIconType = ref('EMOJI')
const formStatus = ref('ENABLED')
const formRemark = ref('')

// emoji picker
const emojiVisible = ref(false)
const emojiSearch = ref('')
const emojiGroups = [
  { label: '美食', items: '🍖🍗🥩🥓🍔🍟🍕🌭🥪🌮🌯🍜🍝🍣🍱🍤🍙🍚🍘🍥🍢🍡🍧🍨🍦🍩🍰🎂🍪🍫🍬🍭🍮🍯🍲🍳🍵☕🍶🍺🍻🥂' },
  { label: '饮品', items: '🥤🧋🧃🥛🧈🍼🍾🍷🥃🍸🍹🧉🔋💧' },
  { label: '水果', items: '🍇🍈🍉🍊🍋🍌🍍🥭🍎🍏🍐🍑🍒🍓🫐🥝🍅🫒🥥🥑🍆🥔🥕🌽' },
  { label: '符号', items: '🔥⭐✨💫🌟🎯🎉🎊🏆🥇🥈🥉✅❌⚡💡📍📌🔖🏷️💎🔔🎵💯♨️🆕🆒' },
  { label: '交通', items: '🚗🚕🚙🚌🚎🏎️🚓🚑🚒🚐🛻🚚🚛🚜🏍️🛵🚲🛴🛹🏍' },
]

const filteredEmojis = computed(() => {
  if (!emojiSearch.value) return emojiGroups
  return emojiGroups.map(g => ({
    ...g,
    items: [...g.items].filter(c => c.includes(emojiSearch.value)),
  })).filter(g => g.items.length > 0)
})

// ---- status options ----
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '已启用', value: 'ENABLED' },
  { label: '已停用', value: 'DISABLED' },
]

// ---- fetch ----
const fetchCategories = async () => {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value) params.status = statusFilter.value
    if (dateRange.value?.[0]) params.dateStart = dateRange.value[0]
    if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1]
    const res = await listCategories(params)
    categories.value = res.data.list || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

const fetchStats = async () => {
  try {
    const res = await getCategoryStats()
    stats.value = res.data
  } catch { /* ignore */ }
}

const handleSearch = () => { page.value = 1; fetchCategories() }
const handleReset = () => { page.value = 1; fetchCategories() }
const handleRefresh = () => { fetchCategories(); fetchStats() }

// ---- form ----
const openCreate = () => {
  formMode.value = 'create'
  editId.value = null
  formName.value = ''
  formSortOrder.value = 1
  formIcon.value = '🍖'
  formIconType.value = 'EMOJI'
  formStatus.value = 'ENABLED'
  formRemark.value = ''
  formVisible.value = true
}

const openEdit = (row: CategoryItem) => {
  formMode.value = 'edit'
  editId.value = row.id
  formName.value = row.name
  formSortOrder.value = row.sortOrder
  formIcon.value = row.icon || '🍖'
  formIconType.value = row.iconType || 'EMOJI'
  formStatus.value = row.status
  formRemark.value = row.remark || ''
  formVisible.value = true
}

const handleSubmit = async () => {
  if (!formName.value.trim()) { ElMessage.warning('请输入品类名称'); return }
  if (formName.value.trim().length < 2 || formName.value.trim().length > 10) { ElMessage.warning('品类名称长度 2-10 字'); return }
  if (formSortOrder.value == null || formSortOrder.value < 0) { ElMessage.warning('排序权重必须为非负整数'); return }

  const data = {
    name: formName.value.trim(),
    sortOrder: formSortOrder.value,
    icon: formIcon.value,
    iconType: formIconType.value,
    status: formStatus.value,
    remark: formRemark.value || undefined,
  }

  try {
    if (formMode.value === 'create') {
      await createCategory(data)
      ElMessage.success('新增成功')
    } else {
      await updateCategory(editId.value!, data)
      ElMessage.success('修改成功')
    }
    formVisible.value = false
    fetchCategories()
    fetchStats()
  } catch { /* error handled by interceptor */ }
}

// ---- enable / disable ----
const handleEnable = async (row: CategoryItem) => {
  await enableCategory(row.id)
  ElMessage.success('已启用')
  fetchCategories()
  fetchStats()
}

const handleDisable = async (row: CategoryItem) => {
  await ElMessageBox.confirm(
    `停用「${row.name}」后，用户端将不再展示该品类，已关联商家不受影响，可随时重新启用。`,
    '确认停用',
    { type: 'warning', confirmButtonText: '确认停用' },
  )
  await disableCategory(row.id)
  ElMessage.success('已停用')
  fetchCategories()
  fetchStats()
}

// ---- delete ----
const handleDelete = async (row: CategoryItem) => {
  await ElMessageBox.confirm(
    `⚠️ 此操作不可恢复！确认删除「${row.name}」？该品类下不能有已关联的商家。`,
    '确认删除',
    { type: 'error', confirmButtonText: '确认删除' },
  )
  try {
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    fetchCategories()
    fetchStats()
  } catch { /* error handled by interceptor */ }
}

// ---- emoji picker ----
const selectEmoji = (emoji: string) => {
  formIcon.value = emoji
  formIconType.value = 'EMOJI'
  emojiVisible.value = false
}

// ---- format ----
const formatTime = (s: string) => { return s ? s.replace('T', ' ').substring(0, 16) : '-' }

onMounted(() => { fetchCategories(); fetchStats() })
</script>

<template>
  <div class="page-card list-page">

    <!-- ====== 统计条（公共组件） ====== -->
    <StatsStrip :items="[
      { value: stats.total, label: '品类总数' },
      { value: stats.enabled, label: '已启用', color: 'success' },
      { value: stats.disabled, label: '已停用', color: 'info' },
    ]" />

    <!-- ====== 搜索 + 操作 ====== -->
    <SearchFilterBar
      v-model:keyword="keyword"
      v-model:filter-value="statusFilter"
      v-model:date-range="dateRange"
      placeholder="搜索品类名称"
      :filter-options="statusOptions"
      filter-placeholder="状态筛选"
      date-label="创建"
      @search="handleSearch"
      @reset="handleReset"
      @refresh="handleRefresh"
    >
      <template #extra>
        <el-button type="primary" @click="openCreate">+ 新增品类</el-button>
        <!-- 批量操作（与商家管理页一致：搜索栏右侧，选中后显示） -->
        <template v-if="selectedIds.length">
          <el-button type="success" plain :loading="batchLoading" @click="runBatch(batchEnableCategories, '批量启用')">
            批量启用 ({{ selectedIds.length }})
          </el-button>
          <el-button type="warning" plain :loading="batchLoading" @click="runBatch(batchDisableCategories, '批量停用')">
            批量停用 ({{ selectedIds.length }})
          </el-button>
          <el-button type="danger" plain :loading="batchLoading" @click="runBatch(batchDeleteCategories, '批量删除')">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </template>
      </template>
    </SearchFilterBar>

    <!-- ====== 表格 ====== -->
    <div class="table-scroll">
    <el-table :data="categories" stripe v-loading="loading" height="100%" style="width: 100%"
      @selection-change="(rows: CategoryItem[]) => selectedIds = rows.map(r => r.id)">
      <el-table-column type="selection" width="42" fixed="left" />
      <el-table-column type="index" label="编号" width="60" align="center" />
      <el-table-column label="图标" width="70" align="center">
        <template #default="{ row }">
          <span style="font-size:24px;line-height:1">{{ row.icon || '📦' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="品类名称" min-width="130" align="left" show-overflow-tooltip />
      <el-table-column label="排序" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain" type="info">{{ row.sortOrder }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small" effect="plain">
            {{ row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="修改时间" width="150" align="center">
        <template #default="{ row }">{{ formatTime(row.updatedAt) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'ENABLED'"
            size="small"
            type="warning"
            @click="handleDisable(row)"
          >
            停用
          </el-button>
          <el-button v-else size="small" type="success" @click="handleEnable(row)">启用</el-button>
          <el-button
            v-if="row.status === 'DISABLED'"
            size="small"
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
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
        @size-change="fetchCategories" @current-change="fetchCategories"
      />
    </div>

    <!-- ====== 新增/编辑弹窗 ====== -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增品类' : '编辑品类'"
      width="480px"
      destroy-on-close
    >
      <el-form label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="品类名称" required>
          <el-input v-model="formName" placeholder="请输入品类名称（2-10字）" maxlength="10" show-word-limit />
        </el-form-item>

        <el-form-item label="排序权重" required>
          <el-input-number v-model="formSortOrder" :min="0" :max="999" placeholder="值越小越靠前" style="width:100%" />
        </el-form-item>

        <el-form-item label="品类图标">
          <div style="display:flex;align-items:center;gap:12px">
            <span style="font-size:32px;line-height:1;cursor:pointer" @click="emojiVisible = true">{{ formIcon || '📦' }}</span>
            <el-button size="small" @click="emojiVisible = true">选择图标</el-button>
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="formStatus">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注（可选）">
          <el-input v-model="formRemark" type="textarea" :rows="2" placeholder="备注信息（200字内）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">
          {{ formMode === 'create' ? '确认新增' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ====== 图标选择器 ====== -->
    <el-dialog v-model="emojiVisible" title="选择图标" width="520px" destroy-on-close>
      <el-input
        v-model="emojiSearch"
        placeholder="搜索 Emoji..."
        :prefix-icon="Search"
        clearable
        style="margin-bottom:16px"
      />

      <div v-for="group in filteredEmojis" :key="group.label" style="margin-bottom:14px">
        <p style="font-size:12px;color:var(--text-secondary);margin-bottom:6px">{{ group.label }}</p>
        <div style="display:flex;flex-wrap:wrap;gap:6px">
          <span
            v-for="ch in group.items" :key="ch"
            style="font-size:26px;cursor:pointer;padding:4px;border-radius:6px;transition:0.15s;line-height:1"
            :style="formIcon === ch ? { background: 'var(--color-primary)', color: '#fff' } : {}"
            @click="selectEmoji(ch)"
          >{{ ch }}</span>
        </div>
      </div>

      <div v-if="filteredEmojis.length === 0" style="text-align:center;padding:20px;color:var(--text-secondary)">
        没有匹配的图标
      </div>

      <template #footer>
        <el-button @click="emojiVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
/* ---- 分页 ---- */
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
