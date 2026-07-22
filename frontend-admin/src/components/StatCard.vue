<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  color?: string
}>()
</script>

<template>
  <el-card shadow="hover" class="stat-card">
    <div class="stat-label">{{ title }}</div>
    <div class="stat-value" :style="{ color: color || '#409EFF' }">{{ value }}</div>
  </el-card>
</template>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
}
</style>
