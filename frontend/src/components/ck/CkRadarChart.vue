<script setup>
import { computed } from 'vue'

const props = defineProps({
  metrics: {
    type: Object,
    required: true,
  },
})

const axes = [
  { key: 'avgWmc', label: 'WMC' },
  { key: 'avgDit', label: 'DIT' },
  { key: 'avgNoc', label: 'NOC' },
  { key: 'avgCbo', label: 'CBO' },
  { key: 'avgRfc', label: 'RFC' },
  { key: 'avgLcom', label: 'LCOM' },
]

const center = 110
const radius = 86

const maxValue = computed(() => {
  const values = axes.map((axis) => Number(props.metrics[axis.key] ?? 0))
  const max = Math.max(...values, 1)
  return Math.ceil(max * 1.15)
})

function pointBy(index, ratio) {
  const angle = (-Math.PI / 2) + ((Math.PI * 2) / axes.length) * index
  return {
    x: center + radius * ratio * Math.cos(angle),
    y: center + radius * ratio * Math.sin(angle),
  }
}

const levelPolygons = computed(() => {
  const levels = [0.25, 0.5, 0.75, 1]
  return levels.map((ratio) =>
    axes
      .map((_, index) => {
        const point = pointBy(index, ratio)
        return `${point.x},${point.y}`
      })
      .join(' ')
  )
})

const dataPolygon = computed(() =>
  axes
    .map((axis, index) => {
      const value = Number(props.metrics[axis.key] ?? 0)
      const ratio = Math.min(value / maxValue.value, 1)
      const point = pointBy(index, ratio)
      return `${point.x},${point.y}`
    })
    .join(' ')
)

const axisLines = computed(() =>
  axes.map((_, index) => {
    const point = pointBy(index, 1)
    return { x1: center, y1: center, x2: point.x, y2: point.y }
  })
)

const labels = computed(() =>
  axes.map((axis, index) => {
    const point = pointBy(index, 1.18)
    return { ...axis, x: point.x, y: point.y }
  })
)
</script>

<template>
  <section class="radar-wrap">
    <h3>CK 六维雷达图</h3>
    <svg viewBox="0 0 220 220" class="radar-svg" role="img" aria-label="CK radar chart">
      <polygon
        v-for="(polygon, idx) in levelPolygons"
        :key="idx"
        :points="polygon"
        class="radar-level"
      />
      <line
        v-for="(line, idx) in axisLines"
        :key="idx"
        :x1="line.x1"
        :y1="line.y1"
        :x2="line.x2"
        :y2="line.y2"
        class="radar-axis"
      />
      <polygon :points="dataPolygon" class="radar-data" />
      <circle cx="110" cy="110" r="2.5" class="radar-center" />
      <text v-for="label in labels" :key="label.key" :x="label.x" :y="label.y" class="radar-label">
        {{ label.label }}
      </text>
    </svg>
    <p class="radar-note">量纲自动按当前分析结果归一化，适合比较同一项目内不同批次结果。</p>
  </section>
</template>

<style scoped>
.radar-wrap {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 16px;
  padding: 14px 16px;
  background: #f8fbff;
}

h3 {
  margin: 0 0 6px;
}

.radar-svg {
  width: 100%;
  max-width: 320px;
  display: block;
  margin: 0 auto;
}

.radar-level {
  fill: rgba(59, 130, 246, 0.03);
  stroke: rgba(100, 116, 139, 0.35);
  stroke-width: 0.8;
}

.radar-axis {
  stroke: rgba(100, 116, 139, 0.4);
  stroke-width: 0.8;
}

.radar-data {
  fill: rgba(37, 99, 235, 0.26);
  stroke: #2563eb;
  stroke-width: 1.7;
}

.radar-center {
  fill: #1d4ed8;
}

.radar-label {
  font-size: 8px;
  fill: #334155;
  text-anchor: middle;
}

.radar-note {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}
</style>
