<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue'
import { computed } from 'vue'

export interface FilterOption {
  label: string
  value: string
}

defineProps<{
  placeholder?: string
  inputWidth?: number | string
  filterPlaceholder?: string
  filterWidth?: number | string
  filterOptions?: FilterOption[]
  /** 支付方式下拉选项（可选，如订单管理页；不传则不显示，显示在状态筛选右侧） */
  payChannelOptions?: FilterOption[]
  /** 支付方式下拉占位符 */
  payChannelPlaceholder?: string
  /** 分类筛选下拉选项（可选，如平台品类；不传则不显示） */
  categoryOptions?: FilterOption[]
  /** 分类下拉占位符 */
  categoryPlaceholder?: string
  /** 日期范围对应的业务时间字段名，如"注册"/"入驻"/"申请"，用于 placeholder 提示 */
  dateLabel?: string
}>()

const keyword = defineModel<string>('keyword', { default: '' })
const filterValue = defineModel<string>('filterValue', { default: '' })
const payChannelValue = defineModel<string>('payChannelValue', { default: '' })
const categoryValue = defineModel<string>('categoryValue', { default: '' })
const dateRange = defineModel<string[]>('dateRange', { default: () => ['', ''] })

const emit = defineEmits<{
  search: []
  refresh: []
  reset: []
}>()

// 日期使用 v-model 双向绑定（保证输入框回显所选日期），
// 变化时整体赋值 dateRange 以同步父组件，并触发 search
const startDate = computed({
  get: () => dateRange.value?.[0] || '',
  set: (val: string) => { dateRange.value = [val, dateRange.value?.[1] || ''] },
})
const endDate = computed({
  get: () => dateRange.value?.[1] || '',
  set: (val: string) => { dateRange.value = [dateRange.value?.[0] || '', val] },
})

const onDateStart = () => { emit('search') }
const onDateEnd = () => { emit('search') }

const handleReset = () => {
  keyword.value = ''
  filterValue.value = ''
  payChannelValue.value = ''
  categoryValue.value = ''
  dateRange.value = ['', '']
  emit('reset')
}
</script>

<template>
  <div class="search-filter-bar">
    <el-input
      v-model="keyword"
      :placeholder="placeholder || '请输入关键词'"
      clearable
      :style="{ width: inputWidth || '240px' }"
      @keyup.enter="emit('search')"
      @clear="emit('search')"
    >
      <template #prefix><el-icon><Search /></el-icon></template>
    </el-input>

    <el-select
      v-if="filterOptions && filterOptions.length"
      v-model="filterValue"
      :placeholder="filterPlaceholder || '筛选'"
      clearable
      :style="{ width: filterWidth || '130px' }"
      @change="emit('search')"
    >
      <el-option v-for="o in filterOptions" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>

    <!-- 支付方式筛选（可选，显示在状态筛选右侧） -->
    <el-select
      v-if="payChannelOptions && payChannelOptions.length"
      v-model="payChannelValue"
      :placeholder="payChannelPlaceholder || '支付方式'"
      clearable
      :style="{ width: '120px' }"
      @change="emit('search')"
    >
      <el-option v-for="o in payChannelOptions" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>

    <el-select
      v-if="categoryOptions && categoryOptions.length"
      v-model="categoryValue"
      :placeholder="categoryPlaceholder || '分类筛选'"
      clearable
      :style="{ width: '130px' }"
      @change="emit('search')"
    >
      <el-option v-for="o in categoryOptions" :key="o.value" :label="o.label" :value="o.value" />
    </el-select>

    <div class="date-range-inline">
      <el-date-picker
        v-model="startDate"
        type="date"
        :placeholder="(dateLabel || '日期') + '开始'"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-pick"
        @change="onDateStart"
      />
      <span class="date-sep">~</span>
      <el-date-picker
        v-model="endDate"
        type="date"
        :placeholder="(dateLabel || '日期') + '结束'"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-pick"
        @change="onDateEnd"
      />
    </div>

    <el-button type="primary" @click="emit('search')">搜索</el-button>
    <el-button @click="handleReset">重置</el-button>
    <el-button :icon="Refresh" @click="emit('refresh')">刷新</el-button>

    <span style="flex:1" />
    <slot name="extra" />
  </div>
</template>

<style scoped>
.search-filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
}
.date-range-inline {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.date-pick {
  width: 95px;
}
.date-pick :deep(.el-input__wrapper) {
  padding: 0 4px 0 6px;
}
.date-sep {
  color: var(--text-placeholder, #999);
  font-size: 13px;
  flex-shrink: 0;
}
</style>
