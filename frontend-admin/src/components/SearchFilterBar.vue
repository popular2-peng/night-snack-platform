<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue'

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
}>()

const keyword = defineModel<string>('keyword', { default: '' })
const filterValue = defineModel<string>('filterValue', { default: '' })
const dateRange = defineModel<string[]>('dateRange', { default: () => ['', ''] })

// 两个独立 date 用于双向绑定，变化时同步回数组
function onDateStart(val: string) { dateRange.value = [val, dateRange.value[1]]; emit('search') }
function onDateEnd(val: string)   { dateRange.value = [dateRange.value[0], val]; emit('search') }

const emit = defineEmits<{
  search: []
  refresh: []
  reset: []
}>()

function handleReset() {
  keyword.value = ''
  filterValue.value = ''
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

    <div class="date-range-inline">
      <el-date-picker
        :model-value="dateRange[0]"
        type="date"
        placeholder="开始"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-pick"
        @update:model-value="onDateStart"
      />
      <span class="date-sep">~</span>
      <el-date-picker
        :model-value="dateRange[1]"
        type="date"
        placeholder="结束"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-pick"
        @update:model-value="onDateEnd"
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
