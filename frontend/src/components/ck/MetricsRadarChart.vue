<script setup>
import { computed } from 'vue'

const props = defineProps({
  metrics: { type: Object, required: true },
  mode: { type: String, default: 'ck' },
})

const ckAxes = [
  { key: 'avgWmc', label: 'WMC', desc: '方法复杂度' },
  { key: 'avgDit', label: 'DIT', desc: '继承深度' },
  { key: 'avgNoc', label: 'NOC', desc: '子类数' },
  { key: 'avgCbo', label: 'CBO', desc: '耦合度' },
  { key: 'avgRfc', label: 'RFC', desc: '响应集' },
  { key: 'avgLcom', label: 'LCOM', desc: '内聚缺失' },
]

const lkAxes = [
  { key: 'avgCs', label: 'CS', desc: '类规模' },
  { key: 'avgNpa', label: 'NPA', desc: '公有属性' },
  { key: 'avgNoo', label: 'NOO', desc: '重写方法' },
  { key: 'avgNoa', label: 'NOA', desc: '新增方法' },
]

const axes = computed(() => props.mode === 'ck' ? ckAxes : lkAxes)
const title = computed(() => props.mode === 'ck' ? 'CK 六维雷达图' : 'LK 四维雷达图')

const center = 110
const radius = 82

const maxValue = computed(() => {
  const values = axes.value.map((a) => Number(props.metrics[a.key] ?? 0))
  return Math.ceil(Math.max(...values, 1) * 1.2)
})

function pointAt(index, ratio) {
  const angle = -Math.PI / 2 + (Math.PI * 2 / axes.value.length) * index
  return {
    x: center + radius * ratio * Math.cos(angle),
    y: center + radius * ratio * Math.sin(angle),
  }
}

const levelPolygons = computed(() =>
  [0.25, 0.5, 0.75, 1].map((ratio) =>
    axes.value.map((_, i) => {
      const p = pointAt(i, ratio)
      return `${p.x},${p.y}`
    }).join(' ')
  )
)

const dataPolygon = computed(() =>
  axes.value.map((axis, i) => {
    const ratio = Math.min(Number(props.metrics[axis.key] ?? 0) / maxValue.value, 1)
    const p = pointAt(i, ratio)
    return `${p.x},${p.y}`
  }).join(' ')
)

const axisLines = computed(() =>
  axes.value.map((_, i) => {
    const p = pointAt(i, 1)
    return { x1: center, y1: center, x2: p.x, y2: p.y }
  })
)

const labels = computed(() =>
  axes.value.map((axis, i) => {
    const p = pointAt(i, 1.22)
    return { ...axis, x: p.x, y: p.y }
  })
)
</script>

<template>
  <div class="radar-wrap">
    <p class="radar-title">{{ title }}</p>
    <svg viewBox="0 0 220 220" class="radar-svg" role="img" :aria-label="title">
      <polygon
        v-for="(pts, idx) in levelPolygons"
        :key="idx"
        :points="pts"
        class="radar-level"
      />
      <line
        v-for="(ln, idx) in axisLines"
        :key="idx"
        :x1="ln.x1" :y1="ln.y1" :x2="ln.x2" :y2="ln.y2"
        class="radar-axis"
      />
      <polygon :points="dataPolygon" class="radar-data" />
      <circle :cx="center" :cy="center" r="2.5" class="radar-center" />
      <text
        v-for="label in labels"
        :key="label.key"
        :x="label.x" :y="label.y"
        class="radar-label"
      >{{ label.label }}</text>
    </svg>
    <div class="radar-legend">
      <span v-for="axis in axes" :key="axis.key" class="legend-item">
        <b>{{ axis.label }}</b> {{ axis.desc }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.radar-wrap {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  padding: 16px;
  background: #f8fbff;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.radar-title {
  font-weight: 600;
  font-size: 13px;
  color: #334155;
  margin: 0 0 8px;
}

.radar-svg {
  width: 100%;
  max-width: 260px;
  display: block;
}

.radar-level {
  fill: rgba(59, 130, 246, 0.04);
  stroke: rgba(100, 116, 139, 0.3);
  stroke-width: 0.7;
}

.radar-axis {
  stroke: rgba(100, 116, 139, 0.35);
  stroke-width: 0.7;
}

.radar-data {
  fill: rgba(37, 99, 235, 0.22);
  stroke: #2563eb;
  stroke-width: 1.8;
}

.radar-center {
  fill: #1d4ed8;
}

.radar-label {
  font-size: 8.5px;
  fill: #334155;
  text-anchor: middle;
  dominant-baseline: middle;
}

.radar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  justify-content: center;
  margin-top: 10px;
}

.legend-item {
  font-size: 11px;
  color: #64748b;
}

.legend-item b {
  color: #1e40af;
  font-weight: 600;
}
</style>
