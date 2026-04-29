<script setup>
import { computed, ref } from 'vue'

const apiBaseUrl = 'http://localhost:8080'

const projectName = ref('SoftQuant CFG Demo')
const selectedFiles = ref([])
const skippedFiles = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const result = ref(null)
const selectedMethodKey = ref('')
const fileInputRef = ref(null)
const folderInputRef = ref(null)
const dropzoneActive = ref(false)
const resultTab = ref('methods')
const methodSortKey = ref('complexity')
const methodSearch = ref('')

const hasFiles = computed(() => selectedFiles.value.length > 0)
const hasResult = computed(() => Boolean(result.value))
const fileCountLabel = computed(() => `${selectedFiles.value.length} 个 Java 文件`)
const skippedCountLabel = computed(() => (skippedFiles.value.length > 0 ? `跳过 ${skippedFiles.value.length}` : ''))
const methods = computed(() => result.value?.methods ?? [])

const sortedSelectedFiles = computed(() =>
  [...selectedFiles.value].sort((a, b) => a.fileName.localeCompare(b.fileName)),
)

const topMethods = computed(() =>
  [...methods.value]
    .sort((a, b) => b.complexity - a.complexity)
    .slice(0, 8),
)

const selectedMethod = computed(() => {
  if (methods.value.length === 0) {
    return null
  }
  return methods.value.find((method) => methodKey(method) === selectedMethodKey.value) ?? topMethods.value[0]
})
const selectedGraphView = computed(() => buildGraphView(selectedMethod.value?.graph))

const filteredMethods = computed(() => {
  const keyword = methodSearch.value.trim().toLowerCase()
  const pool = keyword
    ? methods.value.filter((method) => {
        const text = `${method.className}.${method.methodName} ${method.signature} ${method.fileName}`.toLowerCase()
        return text.includes(keyword)
      })
    : [...methods.value]

  const compareMap = {
    complexity: (a, b) => b.complexity - a.complexity,
    decisionPointCount: (a, b) => b.decisionPointCount - a.decisionPointCount,
    fileName: (a, b) => a.fileName.localeCompare(b.fileName),
    methodName: (a, b) => `${a.className}.${a.methodName}`.localeCompare(`${b.className}.${b.methodName}`),
  }

  return pool.sort(compareMap[methodSortKey.value] ?? compareMap.complexity)
})

const riskSegments = computed(() => {
  if (!result.value) {
    return []
  }
  const total = Math.max(result.value.methodCount, 1)
  return [
    { key: 'low', label: '低', value: result.value.lowRiskMethodCount ?? 0, color: '#22c55e' },
    { key: 'medium', label: '中', value: result.value.mediumRiskMethodCount ?? 0, color: '#f59e0b' },
    { key: 'high', label: '高', value: result.value.highRiskMethodCount ?? 0, color: '#ef4444' },
    { key: 'veryHigh', label: '极高', value: result.value.veryHighRiskMethodCount ?? 0, color: '#7f1d1d' },
  ].map((item) => ({
    ...item,
    width: Math.max((item.value / total) * 100, item.value > 0 ? 4 : 0),
  }))
})

const overviewStats = computed(() => {
  if (!result.value) {
    return []
  }

  return [
    {
      label: '方法总数',
      value: result.value.methodCount,
      meta: `${result.value.classCount} 个类`,
      accent: 'blue',
    },
    {
      label: '平均复杂度',
      value: Number(result.value.avgComplexity ?? 0).toFixed(2),
      meta: '方法级平均 V(G)',
      accent: 'green',
    },
    {
      label: '最高复杂度',
      value: result.value.maxComplexity,
      meta: 'Top 方法见下方',
      accent: 'gold',
    },
    {
      label: '高风险方法',
      value: (result.value.highRiskMethodCount ?? 0) + (result.value.veryHighRiskMethodCount ?? 0),
      meta: '复杂度 11 以上',
      accent: 'rose',
    },
  ]
})

const insightCards = computed(() => {
  if (!result.value || methods.value.length === 0) {
    return []
  }

  const riskiest = [...methods.value].sort((a, b) => b.complexity - a.complexity)[0]
  const densest = [...methods.value].sort((a, b) => b.decisionPointCount - a.decisionPointCount)[0]

  return [
    {
      label: '最高复杂度',
      value: riskiest ? `${riskiest.className}.${riskiest.methodName}` : '无',
      meta: riskiest ? `V(G) ${riskiest.complexity}` : '',
    },
    {
      label: '决策点最多',
      value: densest ? `${densest.className}.${densest.methodName}` : '无',
      meta: densest ? `${densest.decisionPointCount} 个决策点` : '',
    },
  ]
})

async function handleFilesChange(event) {
  const files = Array.from(event.target.files ?? [])
  await loadFiles(files)
  event.target.value = ''
}

async function handleDrop(event) {
  dropzoneActive.value = false
  const files = Array.from(event.dataTransfer?.files ?? [])
  await loadFiles(files)
}

function openFilePicker() {
  fileInputRef.value?.click()
}

function openFolderPicker() {
  folderInputRef.value?.click()
}

async function loadFiles(files) {
  errorMessage.value = ''
  successMessage.value = ''
  result.value = null
  selectedMethodKey.value = ''

  const accepted = files.filter((file) => file.name.toLowerCase().endsWith('.java'))
  skippedFiles.value = files
    .filter((file) => !file.name.toLowerCase().endsWith('.java'))
    .map((file) => file.webkitRelativePath || file.name)

  const loaded = await Promise.all(
    accepted.map(async (file) => ({
      fileName: file.webkitRelativePath || file.name,
      content: await file.text(),
    })),
  )

  selectedFiles.value = mergeFiles(selectedFiles.value, loaded)
  if (loaded.length > 0) {
    successMessage.value = `已载入 ${loaded.length} 个 Java 文件。`
  }
  if (files.length > 0 && loaded.length === 0) {
    errorMessage.value = '没有识别到可分析的 Java 源码文件。'
  }
}

async function executeAnalyze() {
  if (selectedFiles.value.length === 0) {
    errorMessage.value = '请先选择 Java 文件后再分析。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/cfg/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: projectName.value,
        sources: selectedFiles.value.map((file) => ({
          fileName: file.fileName,
          content: file.content,
        })),
      }),
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }

    result.value = await response.json()
    const firstMethod = [...(result.value.methods ?? [])].sort((a, b) => b.complexity - a.complexity)[0]
    selectedMethodKey.value = firstMethod ? methodKey(firstMethod) : ''
    successMessage.value = '控制流与圈复杂度分析完成。'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分析失败'
  } finally {
    loading.value = false
  }
}

function loadSampleFiles() {
  selectedFiles.value = [
    {
      fileName: 'sample/ComplexDemo.java',
      content: [
        'package sample;',
        '',
        'public class ComplexDemo {',
        '    public int risky(int x) {',
        '        if (x > 0 && x < 10) {',
        '            x++;',
        '        } else if (x == 0) {',
        '            x = 1;',
        '        }',
        '        for (int i = 0; i < x; i++) {',
        '            while (x > i) {',
        '                break;',
        '            }',
        '        }',
        '        switch (x) {',
        '            case 1: return 1;',
        '            case 2: return 2;',
        '            default: return 0;',
        '        }',
        '    }',
        '',
        '    public int guard(String value) {',
        '        try {',
        '            return value == null || value.isBlank() ? 0 : 1;',
        '        } catch (RuntimeException ex) {',
        '            return -1;',
        '        }',
        '    }',
        '}',
      ].join('\n'),
    },
  ]
  skippedFiles.value = []
  result.value = null
  selectedMethodKey.value = ''
  errorMessage.value = ''
  successMessage.value = '已载入复杂度分析示例文件。'
}

function clearFiles() {
  selectedFiles.value = []
  skippedFiles.value = []
  result.value = null
  selectedMethodKey.value = ''
  errorMessage.value = ''
  successMessage.value = ''
}

function removeFile(fileName) {
  selectedFiles.value = selectedFiles.value.filter((file) => file.fileName !== fileName)
  result.value = null
  selectedMethodKey.value = ''
}

function mergeFiles(current, incoming) {
  const fileMap = new Map(current.map((file) => [file.fileName, file]))
  for (const file of incoming) {
    fileMap.set(file.fileName, file)
  }
  return [...fileMap.values()].sort((a, b) => a.fileName.localeCompare(b.fileName))
}

function methodKey(method) {
  return `${method.fileName}|${method.className}|${method.methodName}|${method.startLine}`
}

function methodLabel(method) {
  return `${method.className}.${method.methodName} · V(G) ${method.complexity}`
}

function riskLabel(riskLevel) {
  const labels = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险',
    VERY_HIGH: '极高风险',
  }
  return labels[riskLevel] ?? riskLevel
}

function riskClass(riskLevel) {
  return `risk-${String(riskLevel ?? '').toLowerCase().replace('_', '-')}`
}

function percentOfTotal(value, total) {
  if (!total) {
    return 0
  }
  return Math.max((value / total) * 100, value > 0 ? 6 : 0)
}

function buildGraphView(graph) {
  if (!graph?.nodes?.length) {
    return { width: 0, height: 0, nodes: [], edges: [] }
  }

  const outgoingMap = new Map(graph.nodes.map((node) => [node.id, []]))
  const incomingMap = new Map(graph.nodes.map((node) => [node.id, []]))

  for (const edge of graph.edges ?? []) {
    const outgoing = outgoingMap.get(edge.from) ?? []
    outgoing.push(edge)
    outgoingMap.set(edge.from, outgoing)

    const incoming = incomingMap.get(edge.to) ?? []
    incoming.push(edge)
    incomingMap.set(edge.to, incoming)
  }

  const laneGap = 185
  const verticalGap = 104
  const startY = 66
  const laneByNode = new Map(
    graph.nodes.map((node) => [node.id, nodeLane(node, incomingMap.get(node.id) ?? [], outgoingMap)]),
  )
  const lanes = [...laneByNode.values()]
  const minLane = Math.min(...lanes, 0)
  const maxLane = Math.max(...lanes, 0)
  const centerX = 150 + Math.max(0, -minLane) * laneGap
  const leftRailX = 54

  const nodes = graph.nodes.map((sourceNode, index) => {
    const displayLabel = truncateGraphLabel(sourceNode.label, sourceNode.type)
    const size = nodeSize(sourceNode.type, displayLabel)
    return {
      ...sourceNode,
      displayLabel,
      shape: nodeShape(sourceNode.type),
      cx: centerX + (laneByNode.get(sourceNode.id) ?? 0) * laneGap,
      cy: startY + index * verticalGap,
      width: size.width,
      height: size.height,
    }
  })

  const nodeMap = new Map(nodes.map((node) => [node.id, node]))
  const edges = (graph.edges ?? []).map((edge) => buildGraphEdgeView(edge, nodeMap, leftRailX))
  const width = Math.max(720, 300 + (maxLane - minLane) * laneGap)
  const height = Math.max(420, startY + graph.nodes.length * verticalGap + 40)

  return { width, height, nodes, edges }
}

function buildGraphEdgeView(edge, nodeMap, leftRailX) {
  const from = nodeMap.get(edge.from)
  const to = nodeMap.get(edge.to)

  if (!from || !to) {
    return { ...edge, path: '', labelX: 0, labelY: 0 }
  }

  if (edge.type === 'LOOP_BACK' || edge.type === 'CONTINUE' || to.cy <= from.cy) {
    const startX = from.cx - from.width / 2
    const startY = from.cy
    const endX = to.cx - to.width / 2
    const endY = to.cy
    return {
      ...edge,
      path: `M ${startX} ${startY} L ${leftRailX} ${startY} L ${leftRailX} ${endY} L ${endX} ${endY}`,
      labelX: leftRailX + 26,
      labelY: (startY + endY) / 2 - 8,
    }
  }

  if (Math.abs(from.cx - to.cx) < 8) {
    const startX = from.cx
    const startY = from.cy + from.height / 2
    const endX = to.cx
    const endY = to.cy - to.height / 2
    return {
      ...edge,
      path: `M ${startX} ${startY} L ${endX} ${endY}`,
      labelX: startX + 34,
      labelY: (startY + endY) / 2 - 8,
    }
  }

  const startX = from.cx
  const startY = from.cy + from.height / 2
  const bendY = startY + 26
  const endX = to.cx
  const endY = to.cy - to.height / 2
  return {
    ...edge,
    path: `M ${startX} ${startY} L ${startX} ${bendY} L ${endX} ${bendY} L ${endX} ${endY}`,
    labelX: (startX + endX) / 2,
    labelY: bendY - 8,
  }
}

function nodeLane(node, incomingEdges, outgoingMap) {
  if (['ENTRY', 'EXIT', 'MERGE'].includes(node.type)) {
    return 0
  }

  const caseEdge = incomingEdges.find((edge) => edge.type === 'CASE_BRANCH')
  if (caseEdge) {
    const caseEdges = (outgoingMap.get(caseEdge.from) ?? []).filter((edge) => edge.type === 'CASE_BRANCH')
    const index = Math.max(caseEdges.findIndex((edge) => edge.id === caseEdge.id), 0)
    return index - (caseEdges.length - 1) / 2
  }

  if (incomingEdges.some((edge) => edge.type === 'TRUE')) {
    return -1
  }
  if (incomingEdges.some((edge) => edge.type === 'FALSE' || edge.type === 'EXCEPTION')) {
    return 1
  }
  return 0
}

function truncateGraphLabel(label, type) {
  const text = String(label ?? '')
  const limit = ['CONDITION', 'LOOP', 'CASE', 'CATCH'].includes(type) ? 20 : 26
  return text.length > limit ? `${text.slice(0, limit - 3)}...` : text
}

function nodeShape(type) {
  if (type === 'ENTRY' || type === 'EXIT' || type === 'MERGE') {
    return 'ellipse'
  }
  if (['CONDITION', 'LOOP', 'CASE', 'CATCH'].includes(type)) {
    return 'diamond'
  }
  return 'rect'
}

function nodeSize(type, label = '') {
  if (type === 'ENTRY' || type === 'EXIT') {
    return { width: 124, height: 70 }
  }
  if (type === 'MERGE') {
    return { width: Math.max(104, label.length * 8 + 38), height: 54 }
  }
  if (['CONDITION', 'LOOP', 'CASE', 'CATCH'].includes(type)) {
    return { width: Math.max(136, label.length * 8 + 54), height: 76 }
  }
  return { width: Math.max(150, label.length * 7 + 44), height: 58 }
}
</script>

<template>
  <section class="feature-panel cfg-shell">
    <div class="panel-head cfg-head">
      <div>
        <p class="eyebrow">CFG & Complexity</p>
        <h3>控制流与复杂度工作台</h3>
      </div>
      <div class="head-tools">
        <span class="status-pill status-pill-strong">方法级分析</span>
        <span class="status-pill">风险分层</span>
        <span class="status-pill">标准 CFG</span>
      </div>
    </div>

    <div class="toolbar-row">
      <div class="project-box">
        <label>项目名称</label>
        <input v-model="projectName" />
      </div>
      <div class="toolbar-actions">
        <button class="secondary-button icon-button" type="button" @click="loadSampleFiles">载入示例</button>
        <button class="secondary-button icon-button" type="button" :disabled="!hasFiles" @click="clearFiles">清空</button>
        <button class="primary-button" :disabled="loading || !hasFiles" @click="executeAnalyze">
          {{ loading ? '分析中...' : '开始分析' }}
        </button>
      </div>
    </div>

    <div
      class="dropzone"
      :class="{ 'dropzone-active': dropzoneActive }"
      @dragenter.prevent="dropzoneActive = true"
      @dragover.prevent="dropzoneActive = true"
      @dragleave.prevent="dropzoneActive = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInputRef"
        hidden
        aria-hidden="true"
        type="file"
        class="hidden-file-input"
        accept=".java"
        multiple
        @change="handleFilesChange"
      />
      <input
        ref="folderInputRef"
        hidden
        aria-hidden="true"
        type="file"
        class="hidden-file-input"
        accept=".java"
        multiple
        webkitdirectory
        @change="handleFilesChange"
      />

      <div class="dropzone-copy">
        <p class="dropzone-title">拖拽 Java 源码到这里</p>
        <p class="dropzone-subtitle">支持多文件和项目目录导入，自动提取类、方法、决策点与复杂度信息</p>
      </div>
      <div class="dropzone-actions">
        <button class="secondary-button" type="button" @click="openFilePicker">添加文件</button>
        <button class="secondary-button" type="button" @click="openFolderPicker">导入文件夹</button>
      </div>
      <div class="upload-meta">
        <span class="upload-pill upload-pill-strong">{{ fileCountLabel }}</span>
        <span v-if="skippedFiles.length > 0" class="upload-pill">{{ skippedCountLabel }}</span>
      </div>
    </div>

    <div class="feedback-row">
      <p v-if="successMessage" class="success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>

    <div v-if="hasFiles" class="selected-wrap compact-wrap">
      <div class="subhead-row">
        <p class="panel-label">待分析文件</p>
        <span class="helper-inline">{{ fileCountLabel }}</span>
      </div>
      <div class="file-chip-grid">
        <div v-for="file in sortedSelectedFiles" :key="file.fileName" class="file-chip">
          <div class="file-chip-main">
            <span class="file-chip-name" :title="file.fileName">{{ file.fileName }}</span>
            <span class="file-chip-meta">Java · {{ file.content.length }} chars</span>
          </div>
          <button class="chip-action" type="button" @click="removeFile(file.fileName)">移除</button>
        </div>
      </div>
    </div>
  </section>

  <section v-if="hasResult" class="feature-panel cfg-result-panel">
    <div class="panel-head">
      <p class="eyebrow">Result</p>
      <h3>复杂度分析结果</h3>
    </div>

    <div class="stats-grid cfg-stats-grid">
      <article
        v-for="item in overviewStats"
        :key="item.label"
        class="stat-card"
        :class="`accent-${item.accent}`"
      >
        <p class="stat-label">{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.meta }}</span>
      </article>
    </div>

    <div v-if="result.parseIssues.length > 0" class="notice-wrap">
      <div class="subhead-row">
        <p class="panel-label">解析问题</p>
        <span class="helper-inline">{{ result.parseIssues.length }} 项</span>
      </div>
      <ul class="issue-list">
        <li v-for="issue in result.parseIssues" :key="issue">{{ issue }}</li>
      </ul>
    </div>

    <div class="content-grid cfg-content-grid">
      <article class="metric-panel">
        <div class="subhead-row">
          <p class="panel-label">风险分布</p>
          <span class="helper-inline">{{ result.projectName }}</span>
        </div>
        <div class="stacked-bar">
          <span
            v-for="segment in riskSegments"
            :key="segment.key"
            :style="{ width: `${segment.width}%`, backgroundColor: segment.color }"
          ></span>
        </div>
        <div class="legend-grid">
          <span v-for="segment in riskSegments" :key="segment.key">
            <i :style="{ backgroundColor: segment.color }"></i>
            {{ segment.label }} {{ segment.value }}
          </span>
        </div>
      </article>

      <article class="metric-panel">
        <p class="panel-label">复杂度 Top 方法</p>
        <div class="bars">
          <div v-for="method in topMethods" :key="methodKey(method)" class="bar-item">
            <span class="bar-label" :title="method.signature">{{ method.className }}.{{ method.methodName }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: `${percentOfTotal(method.complexity, result.maxComplexity)}%` }"></div>
            </div>
            <span class="bar-count">{{ method.complexity }}</span>
          </div>
        </div>
        <div class="insight-grid">
          <div v-for="item in insightCards" :key="item.label" class="insight-card">
            <span>{{ item.label }}</span>
            <strong :title="item.value">{{ item.value }}</strong>
            <small>{{ item.meta }}</small>
          </div>
        </div>
      </article>
    </div>

    <div class="content-grid cfg-content-grid">
      <article class="metric-panel">
        <div class="subhead-row">
          <p class="panel-label">方法控制流图</p>
          <span v-if="selectedMethod" class="helper-inline">{{ riskLabel(selectedMethod.riskLevel) }}</span>
        </div>
        <select v-model="selectedMethodKey" class="method-select">
          <option v-for="method in methods" :key="methodKey(method)" :value="methodKey(method)">
            {{ methodLabel(method) }}
          </option>
        </select>

        <div v-if="selectedMethod" class="graph-wrap">
          <div class="method-summary">
            <div class="summary-block">
              <span>签名</span>
              <strong :title="selectedMethod.signature">{{ selectedMethod.signature }}</strong>
            </div>
            <div class="summary-block">
              <span>区间</span>
              <strong>L{{ selectedMethod.startLine }}-{{ selectedMethod.endLine }}</strong>
            </div>
            <div class="summary-block">
              <span>复杂度</span>
              <strong>V(G) {{ selectedMethod.complexity }}</strong>
            </div>
          </div>

          <div class="graph-canvas-wrap">
            <svg
              class="graph-canvas"
              :viewBox="`0 0 ${selectedGraphView.width} ${selectedGraphView.height}`"
              :style="{ width: `${selectedGraphView.width}px`, height: `${selectedGraphView.height}px` }"
            >
              <defs>
                <marker id="cfg-arrow" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto">
                  <path d="M 0 0 L 10 5 L 0 10 z" fill="#94a3b8" />
                </marker>
              </defs>

              <path
                v-for="edge in selectedGraphView.edges"
                :key="edge.id"
                :d="edge.path"
                class="graph-edge-path"
                marker-end="url(#cfg-arrow)"
              />
              <text
                v-for="edge in selectedGraphView.edges"
                :key="`${edge.id}-label`"
                :x="edge.labelX"
                :y="edge.labelY"
                class="graph-edge-label"
                text-anchor="middle"
              >
                {{ edge.label }}
              </text>

              <g v-for="node in selectedGraphView.nodes" :key="node.id">
                <ellipse
                  v-if="node.shape === 'ellipse'"
                  :class="['graph-node-shape', `node-${node.type.toLowerCase()}`]"
                  :cx="node.cx"
                  :cy="node.cy"
                  :rx="node.width / 2"
                  :ry="node.height / 2"
                />
                <rect
                  v-else-if="node.shape === 'rect'"
                  :class="['graph-node-shape', `node-${node.type.toLowerCase()}`]"
                  :x="node.cx - node.width / 2"
                  :y="node.cy - node.height / 2"
                  :width="node.width"
                  :height="node.height"
                  rx="12"
                />
                <polygon
                  v-else
                  :class="['graph-node-shape', `node-${node.type.toLowerCase()}`]"
                  :points="[
                    `${node.cx},${node.cy - node.height / 2}`,
                    `${node.cx + node.width / 2},${node.cy}`,
                    `${node.cx},${node.cy + node.height / 2}`,
                    `${node.cx - node.width / 2},${node.cy}`,
                  ].join(' ')"
                />
                <text :x="node.cx" :y="node.cy - 6" class="graph-node-title" text-anchor="middle">{{ node.displayLabel }}</text>
                <text :x="node.cx" :y="node.cy + 14" class="graph-node-subtitle" text-anchor="middle">
                  {{ node.type }}<tspan v-if="node.line"> · L{{ node.line }}</tspan>
                </text>
              </g>
            </svg>
          </div>
          <div class="edge-list">
            <span v-for="edge in selectedMethod.graph.edges" :key="edge.id">
              {{ edge.from }} → {{ edge.to }} · {{ edge.label }}
            </span>
          </div>
        </div>
      </article>

      <article class="metric-panel">
        <p class="panel-label">决策点</p>
        <div v-if="selectedMethod" class="decision-list">
          <div v-for="point in selectedMethod.decisionPoints" :key="`${point.kind}-${point.line}-${point.label}`">
            <strong>{{ point.kind }}</strong>
            <span>{{ point.label }} · L{{ point.line }} · +{{ point.increment }}</span>
          </div>
          <p v-if="selectedMethod.decisionPoints.length === 0" class="helper-inline">该方法没有分支决策点。</p>
        </div>
      </article>
    </div>

    <div class="metric-panel method-detail-panel">
      <div class="result-tabs">
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'methods' }"
          type="button"
          @click="resultTab = 'methods'"
        >
          方法明细
        </button>
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'classes' }"
          type="button"
          @click="resultTab = 'classes'"
        >
          类级汇总
        </button>
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'rules' }"
          type="button"
          @click="resultTab = 'rules'"
        >
          阅读建议
        </button>
      </div>

      <template v-if="resultTab === 'methods'">
        <div class="result-toolbar">
          <input v-model="methodSearch" class="search-input" placeholder="搜索类名、方法名或文件名" />
          <select v-model="methodSortKey" class="sort-select">
            <option value="complexity">按复杂度</option>
            <option value="decisionPointCount">按决策点</option>
            <option value="methodName">按方法名</option>
            <option value="fileName">按文件名</option>
          </select>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>类</th>
                <th>方法</th>
                <th>签名</th>
                <th>行号</th>
                <th>复杂度</th>
                <th>风险</th>
                <th>决策点</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="method in filteredMethods" :key="methodKey(method)">
                <td>{{ method.className }}</td>
                <td>{{ method.methodName }}</td>
                <td class="file-cell">{{ method.signature }}</td>
                <td>{{ method.startLine }}-{{ method.endLine }}</td>
                <td>{{ method.complexity }}</td>
                <td>
                  <span class="risk-pill" :class="riskClass(method.riskLevel)">{{ riskLabel(method.riskLevel) }}</span>
                </td>
                <td>{{ method.decisionPointCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template v-else-if="resultTab === 'classes'">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>类</th>
                <th>方法</th>
                <th>平均复杂度</th>
                <th>最高复杂度</th>
                <th>高风险</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in result.classSummaries" :key="`${item.fileName}-${item.className}`">
                <td>{{ item.className }}</td>
                <td>{{ item.methodCount }}</td>
                <td>{{ Number(item.avgComplexity ?? 0).toFixed(2) }}</td>
                <td>{{ item.maxComplexity }}</td>
                <td>{{ item.highRiskMethodCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <template v-else>
        <div class="rules-grid">
          <article class="rule-card">
            <p class="panel-label">公式轨迹</p>
            <ul class="formula-list">
              <li v-for="item in result.formulaTrace" :key="item.label">
                <strong>{{ item.label }}</strong>
                <span>{{ item.expression }} = {{ item.result }}</span>
              </li>
            </ul>
          </article>
          <article class="rule-card">
            <p class="panel-label">解释建议</p>
            <ul class="insight-list">
              <li>复杂度 6-10 的方法适合优先补充分支测试用例。</li>
              <li>复杂度 11 以上的方法建议拆分条件判断或提取策略对象。</li>
              <li>包含多层循环和 switch 的方法，可在报告中配合控制流图解释风险路径。</li>
            </ul>
          </article>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.cfg-shell {
  display: grid;
  gap: 18px;
}

.cfg-head {
  align-items: flex-start;
}

.head-tools,
.upload-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-pill,
.upload-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 999px;
  background: #fff;
  color: #475569;
  font-size: 12px;
}

.status-pill-strong,
.upload-pill-strong {
  background: #eff6ff;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.toolbar-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.project-box label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
}

.project-box input,
.search-input,
.sort-select,
.method-select {
  width: 100%;
  min-height: 44px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 12px;
  padding: 0 14px;
  background: #fff;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.icon-button {
  min-width: 104px;
}

.dropzone {
  display: grid;
  gap: 16px;
  padding: 28px;
  border: 1px dashed rgba(37, 99, 235, 0.32);
  border-radius: 18px;
  background:
    radial-gradient(circle at top left, rgba(96, 165, 250, 0.12), transparent 32%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.95), #fff);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.dropzone-active {
  border-color: #2563eb;
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.hidden-file-input {
  display: none;
}

.dropzone-copy {
  display: grid;
  gap: 6px;
}

.dropzone-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.dropzone-subtitle {
  margin: 0;
  color: #64748b;
}

.dropzone-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feedback-row {
  min-height: 18px;
}

.helper-inline,
.success {
  color: #64748b;
  font-size: 12px;
}

.success,
.error {
  margin: 0;
}

.error {
  color: #b91c1c;
}

.notice-wrap,
.selected-wrap,
.metric-panel,
.rule-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 16px;
  background: #f8fafc;
  padding: 16px;
}

.notice-wrap {
  margin-top: 16px;
}

.compact-wrap {
  background: #fff;
}

.subhead-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.file-chip-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.file-chip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 14px;
  background: #f8fafc;
}

.file-chip-main {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.file-chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-weight: 600;
}

.file-chip-meta {
  color: #64748b;
  font-size: 12px;
}

.chip-action {
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1d4ed8;
}

.issue-list {
  margin: 10px 0 0;
  padding-left: 18px;
  color: #475569;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid rgba(148, 163, 184, 0.22);
  padding: 10px 12px;
  text-align: center;
  white-space: nowrap;
}

th {
  background: #f8fafc;
}

.file-cell {
  max-width: 360px;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
}

.cfg-result-panel {
  margin-top: 22px;
}

.cfg-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.cfg-content-grid {
  margin-top: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stacked-bar {
  display: flex;
  width: 100%;
  height: 14px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.legend-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 12px;
  color: #475569;
  font-size: 13px;
}

.legend-grid span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-grid i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.bars {
  display: grid;
  gap: 10px;
}

.bar-item {
  display: grid;
  grid-template-columns: minmax(110px, 0.9fr) minmax(120px, 1.4fr) 42px;
  align-items: center;
  gap: 8px;
}

.bar-label,
.bar-count {
  font-size: 12px;
  color: #475569;
}

.bar-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-count {
  text-align: right;
}

.bar-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.insight-card {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.insight-card span,
.insight-card small {
  color: #64748b;
  font-size: 12px;
}

.insight-card strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
}

.graph-wrap {
  display: grid;
  gap: 14px;
  margin-top: 12px;
}

.method-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-block {
  display: grid;
  gap: 4px;
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.summary-block span {
  color: #64748b;
  font-size: 12px;
}

.summary-block strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
}

.graph-canvas-wrap {
  overflow-x: auto;
  padding-bottom: 4px;
}

.graph-canvas {
  display: block;
}

.graph-edge-path {
  fill: none;
  stroke: #94a3b8;
  stroke-width: 2;
}

.graph-edge-label {
  fill: #64748b;
  font-size: 11px;
}

.graph-node-shape {
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 1.5;
  fill: #fff;
}

.graph-node-title {
  fill: #334155;
  font-size: 12px;
  font-weight: 600;
}

.graph-node-subtitle {
  fill: #64748b;
  font-size: 10px;
}

.node-entry,
.node-exit {
  stroke: #93c5fd;
  fill: #eff6ff;
}

.node-condition,
.node-loop,
.node-case {
  stroke: #fbbf24;
  fill: #fffbeb;
}

.node-catch {
  stroke: #fca5a5;
  fill: #fef2f2;
}

.node-merge {
  stroke: #a7f3d0;
  fill: #ecfdf5;
}

.edge-list,
.decision-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edge-list span,
.decision-list div {
  border-radius: 999px;
  background: #eef6ff;
  color: #475569;
  padding: 6px 10px;
  font-size: 12px;
}

.decision-list div {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.decision-list strong {
  color: #1d4ed8;
}

.method-detail-panel {
  margin-top: 16px;
}

.result-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.result-tab {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 999px;
  background: #fff;
  color: #475569;
}

.result-tab-active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #1d4ed8;
}

.result-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 180px;
  gap: 12px;
  margin-bottom: 14px;
}

.rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.formula-list,
.insight-list {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
}

.formula-list li {
  margin-bottom: 10px;
}

.formula-list strong {
  display: block;
  color: #334155;
}

.formula-list span {
  color: #64748b;
  font-size: 13px;
}

.risk-pill {
  display: inline-flex;
  justify-content: center;
  min-width: 64px;
  border-radius: 999px;
  padding: 5px 8px;
  font-size: 12px;
}

.risk-low {
  background: #dcfce7;
  color: #166534;
}

.risk-medium {
  background: #fef3c7;
  color: #92400e;
}

.risk-high {
  background: #fee2e2;
  color: #991b1b;
}

.risk-very-high {
  background: #7f1d1d;
  color: #fff;
}

@media (max-width: 1180px) {
  .toolbar-row,
  .cfg-content-grid,
  .rules-grid {
    grid-template-columns: 1fr;
  }

  .cfg-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .method-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .cfg-stats-grid,
  .insight-grid,
  .result-toolbar {
    grid-template-columns: 1fr;
  }

  .dropzone {
    padding: 20px;
  }

  .dropzone-title {
    font-size: 20px;
  }

  .bar-item {
    grid-template-columns: 1fr 42px;
  }

  .bar-track {
    grid-column: 1 / -1;
  }

  .file-chip,
  .subhead-row {
    grid-template-columns: 1fr;
  }
}
</style>
