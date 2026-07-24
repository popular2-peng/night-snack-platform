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

const emit = defineEmits<{
  search: []
  refresh: []
}>()
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

    <el-button type="primary" @click="emit('search')">搜索</el-button>
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
</style>
