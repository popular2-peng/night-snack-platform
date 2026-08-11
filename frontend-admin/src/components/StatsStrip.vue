<script setup lang="ts">
// 统计条数据项
interface StatsItem {
  /** 数值（数字或已格式化好的字符串，如 "¥1,234"） */
  value: number | string
  /** 标签文字 */
  label: string
  /** 颜色类型，不传则为主文字色 */
  color?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}

defineProps<{ items: StatsItem[] }>()
</script>

<template>
  <div class="stats-strip">
    <template v-for="(item, i) in items" :key="i">
      <div v-if="i > 0" class="stat-divider" />
      <div class="stat-item">
        <span class="stat-item-value" :class="item.color ? `text-${item.color}` : ''">{{ item.value }}</span>
        <span class="stat-item-label">{{ item.label }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.stats-strip {
  display: flex; align-items: center; gap: 24px;
  margin-bottom: 20px; padding: 16px 20px;
  background: var(--bg); border-radius: var(--border-radius-sm);
  flex-wrap: wrap;
}
.stat-item { display: flex; flex-direction: column; }
.stat-item-value {
  font-size: 22px; font-weight: 700; color: var(--text-primary);
  line-height: 1.2; font-variant-numeric: tabular-nums;
}
.stat-item-value.text-success { color: var(--success); }
.stat-item-value.text-warning { color: var(--warning); }
.stat-item-value.text-danger  { color: var(--danger); }
.stat-item-value.text-primary { color: var(--color-primary, #5B8DEF); }
.stat-item-value.text-info    { color: var(--info); }
.stat-item-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
.stat-divider { width: 1px; height: 32px; background: var(--border-color); }
@media (max-width: 768px) {
  .stats-strip { gap: 16px; padding: 12px; }
  .stat-item-value { font-size: 18px; }
}
</style>
