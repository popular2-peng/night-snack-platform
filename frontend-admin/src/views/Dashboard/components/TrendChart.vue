<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { TrendPoint } from '@/apis/admin'

use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  data: TrendPoint[]
  color?: string
  unit?: string
}>()

const option = computed(() => ({
  tooltip: {
    trigger: 'axis' as const,
    formatter: (params: any) => {
      const p = Array.isArray(params) ? params[0] : params
      return `${p.name}：${p.value} ${props.unit || ''}`
    },
  },
  grid: { left: 40, right: 20, top: 24, bottom: 28 },
  xAxis: {
    type: 'category' as const,
    data: props.data.map((d) => d.date),
    boundaryGap: false,
    axisLine: { lineStyle: { color: '#E5E6EB' } },
    axisLabel: { color: '#86909C', fontSize: 11 },
  },
  yAxis: {
    type: 'value' as const,
    minInterval: 1,
    splitLine: { lineStyle: { color: '#F2F3F5' } },
    axisLabel: { color: '#86909C', fontSize: 11 },
  },
  series: [
    {
      type: 'line' as const,
      smooth: true,
      data: props.data.map((d) => d.count),
      symbol: 'circle',
      symbolSize: 5,
      itemStyle: { color: props.color || '#5B8DEF' },
      lineStyle: { width: 2.5, color: props.color || '#5B8DEF' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: (props.color || '#5B8DEF') + '33' },
            { offset: 1, color: (props.color || '#5B8DEF') + '00' },
          ],
        },
      },
    },
  ],
}))
</script>

<template>
  <v-chart :option="option" style="height: 100%" autoresize />
</template>
