<script setup lang="ts">
/** 订单状态展示元信息（美团配色） */
const STATUS_META: Record<string, { label: string; color: string }> = {
  PENDING_PAY: { label: '待支付', color: '#F59A23' },
  PAID: { label: '已支付', color: '#409EFF' },
  ACCEPTED: { label: '已接单', color: '#409EFF' },
  COOKING: { label: '制作中', color: '#409EFF' },
  COMPLETED: { label: '已完成', color: '#67C23A' },
  CANCELLED: { label: '已取消', color: '#909399' },
  REFUNDING: { label: '退款中', color: '#F56C6C' },
  REFUNDED: { label: '已退款', color: '#909399' },
}

defineProps<{ status: string }>()
</script>

<template>
  <span class="status-tag" :style="{ color: STATUS_META[status]?.color || '#666' }">
    {{ STATUS_META[status]?.label || status }}
  </span>
</template>

<style scoped>
.status-tag {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
}
</style>
