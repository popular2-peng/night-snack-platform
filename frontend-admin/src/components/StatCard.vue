<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  title: string
  value: string | number
  color?: string
  icon?: Component
}>()
</script>

<template>
  <el-card shadow="hover" class="stat-card">
    <div class="stat-body">
      <div v-if="icon" class="stat-icon" :style="{ background: (color || '#409EFF') + '18', color: color || '#409EFF' }">
        <el-icon :size="22">
          <component :is="icon" />
        </el-icon>
      </div>
      <div class="stat-info">
        <div class="stat-value" :style="{ color: color || '#409EFF' }">{{ value }}</div>
        <div class="stat-label">{{ title }}</div>
      </div>
    </div>
    <div class="stat-border" :style="{ background: color || '#409EFF' }" />
  </el-card>
</template>

<style scoped>
.stat-card {
  position: relative;
  overflow: hidden;
  cursor: default;
  border: 1px solid var(--border-light);
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}
.stat-card :deep(.el-card__body) {
  padding: 24px;
}
.stat-border {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
}
.stat-body {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform var(--transition);
}
.stat-card:hover .stat-icon {
  transform: scale(1.08);
}
.stat-info {
  flex: 1;
  min-width: 0;
}
.stat-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -1px;
  font-variant-numeric: tabular-nums;
}
.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
  font-weight: 500;
}
</style>
