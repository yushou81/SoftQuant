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

const hasFiles = computed(() => selectedFiles.value.length > 0)
const hasResult = computed(() => Boolean(result.value))
const methods = computed(() => result.value?.methods ?? [])

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

async function handleFilesChange(event) {
  const files = Array.from(event.target.files ?? [])
  await loadFiles(files)
  event.target.value = ''
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
  return `${method.className}.${method.methodName} (${method.complexity})`
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
</script>

<template>
  <section class="feature-panel">
    <div class="panel-head">
      <p class="eyebrow">CFG & Complexity</p>
      <h3>控制流与圈复杂度模块</h3>
    </div>
    <p class="module-intro">
      基于 JavaParser 识别类、方法和分支节点，计算 McCabe 圈复杂度，并生成方法级教学控制流图。
    </p>

    <div class="module-tags">
      <span class="tag tag-active">McCabe V(G)</span>
      <span class="tag">方法级风险</span>
      <span class="tag">决策点追踪</span>
      <span class="tag">控制流图</span>
      <span class="tag">Java AST</span>
    </div>

    <div class="form-grid cfg-form-grid">
      <div class="field">
        <label>项目名称</label>
        <input v-model="projectName" />
      </div>
      <div class="field">
        <label>单文件/多文件上传</label>
        <input type="file" class="file-input" accept=".java" multiple @change="handleFilesChange" />
      </div>
      <div class="field">
        <label>项目目录上传</label>
        <input type="file" class="file-input" accept=".java" multiple webkitdirectory @change="handleFilesChange" />
      </div>
    </div>

    <div class="actions">
      <button class="primary-button" :disabled="loading" @click="executeAnalyze">
        {{ loading ? '分析中...' : '执行复杂度分析' }}
      </button>
      <button class="secondary-button" type="button" @click="loadSampleFiles">载入示例</button>
      <button class="secondary-button" type="button" :disabled="!hasFiles" @click="clearFiles">清空文件</button>
      <span class="helper-inline">当前 {{ selectedFiles.length }} 个 Java 文件</span>
    </div>

    <p v-if="successMessage" class="success">{{ successMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div v-if="skippedFiles.length > 0" class="notice-wrap">
      <p class="panel-label">已跳过文件</p>
      <p>{{ skippedFiles.slice(0, 6).join(', ') }}<span v-if="skippedFiles.length > 6"> 等 {{ skippedFiles.length }} 个</span></p>
    </div>

    <div v-if="hasFiles" class="selected-wrap">
      <p class="panel-label">待分析文件</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文件</th>
              <th>大小</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="file in selectedFiles" :key="file.fileName">
              <td class="file-cell">{{ file.fileName }}</td>
              <td>{{ file.content.length }} chars</td>
              <td>
                <button class="text-button" type="button" @click="removeFile(file.fileName)">移除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>

  <section v-if="hasResult" class="feature-panel cfg-result-panel">
    <div class="panel-head">
      <p class="eyebrow">Result</p>
      <h3>复杂度分析结果</h3>
    </div>

    <div class="stats-grid cfg-stats-grid">
      <article class="stat-card accent-blue">
        <p class="stat-label">方法总数</p>
        <strong>{{ result.methodCount }}</strong>
        <span>{{ result.classCount }} 个类</span>
      </article>
      <article class="stat-card accent-green">
        <p class="stat-label">平均复杂度</p>
        <strong>{{ Number(result.avgComplexity ?? 0).toFixed(2) }}</strong>
        <span>方法级平均 V(G)</span>
      </article>
      <article class="stat-card accent-gold">
        <p class="stat-label">最高复杂度</p>
        <strong>{{ result.maxComplexity }}</strong>
        <span>Top 方法见下方</span>
      </article>
      <article class="stat-card accent-rose">
        <p class="stat-label">高风险方法</p>
        <strong>{{ result.highRiskMethodCount + result.veryHighRiskMethodCount }}</strong>
        <span>复杂度 11 以上</span>
      </article>
    </div>

    <div v-if="result.parseIssues.length > 0" class="notice-wrap">
      <p class="panel-label">解析问题</p>
      <ul>
        <li v-for="issue in result.parseIssues" :key="issue">{{ issue }}</li>
      </ul>
    </div>

    <div class="content-grid cfg-content-grid">
      <article class="metric-panel">
        <p class="panel-label">风险分布</p>
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

        <div class="formula-list">
          <p class="panel-label">公式轨迹</p>
          <ul>
            <li v-for="item in result.formulaTrace" :key="item.label">
              <strong>{{ item.label }}</strong>
              <span>{{ item.expression }} = {{ item.result }}</span>
            </li>
          </ul>
        </div>
      </article>

      <article class="metric-panel">
        <p class="panel-label">复杂度 Top 方法</p>
        <div class="bars">
          <div v-for="method in topMethods" :key="methodKey(method)" class="bar-item">
            <span class="bar-label" :title="method.signature">{{ method.className }}.{{ method.methodName }}</span>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ width: `${percentOfTotal(method.complexity, result.maxComplexity)}%` }"
              ></div>
            </div>
            <span class="bar-count">{{ method.complexity }}</span>
          </div>
        </div>
      </article>
    </div>

    <div class="content-grid cfg-content-grid">
      <article class="metric-panel">
        <p class="panel-label">方法控制流图</p>
        <select v-model="selectedMethodKey" class="method-select">
          <option v-for="method in methods" :key="methodKey(method)" :value="methodKey(method)">
            {{ methodLabel(method) }}
          </option>
        </select>

        <div v-if="selectedMethod" class="graph-wrap">
          <div class="graph-nodes">
            <template v-for="(node, index) in selectedMethod.graph.nodes" :key="node.id">
              <div class="graph-node" :class="`node-${node.type.toLowerCase()}`">
                <strong>{{ node.label }}</strong>
                <span>{{ node.type }}<template v-if="node.line"> · L{{ node.line }}</template></span>
              </div>
              <span v-if="index < selectedMethod.graph.nodes.length - 1" class="graph-arrow">→</span>
            </template>
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

    <div class="content-grid cfg-content-grid">
      <article class="metric-panel">
        <p class="panel-label">类级汇总</p>
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
      </article>

      <article class="metric-panel">
        <p class="panel-label">解释建议</p>
        <ul class="insight-list">
          <li>复杂度 6-10 的方法适合优先补充分支测试用例。</li>
          <li>复杂度 11 以上的方法建议拆分条件判断或提取策略对象。</li>
          <li>包含多层循环和 switch 的方法，可在报告中配合控制流图解释风险路径。</li>
        </ul>
      </article>
    </div>

    <div class="metric-panel method-detail-panel">
      <p class="panel-label">方法明细</p>
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
            <tr v-for="method in methods" :key="methodKey(method)">
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
    </div>
  </section>
</template>

<style scoped>
.module-intro {
  margin: 0 0 12px;
  color: #64748b;
}

.module-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.tag {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1e40af;
  border: 1px solid #bfdbfe;
  font-size: 12px;
}

.tag-active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.cfg-form-grid {
  grid-template-columns: 1.1fr 1fr 1fr;
  gap: 12px;
}

.field {
  margin-bottom: 12px;
}

label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
}

input,
select {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 10px;
  padding: 10px;
}

.file-input {
  background: #fff;
}

.actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.helper-inline,
.success {
  color: #64748b;
  font-size: 12px;
}

.success,
.error {
  margin: 10px 0 0;
}

.error {
  color: #b91c1c;
}

.notice-wrap,
.selected-wrap,
.metric-panel {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px 14px;
}

.notice-wrap,
.selected-wrap {
  margin-top: 14px;
}

.notice-wrap p:last-child,
.notice-wrap ul {
  margin: 0;
  color: #64748b;
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
  border: 1px solid rgba(148, 163, 184, 0.3);
  padding: 8px;
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

.text-button {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1d4ed8;
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

.formula-list {
  margin-top: 18px;
}

.formula-list ul,
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

.method-select {
  margin-bottom: 12px;
  background: #fff;
}

.graph-wrap {
  display: grid;
  gap: 12px;
}

.graph-nodes {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.graph-node {
  min-width: 118px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 10px;
  background: #fff;
  padding: 9px 10px;
}

.graph-node strong,
.graph-node span {
  display: block;
}

.graph-node strong {
  color: #334155;
  font-size: 13px;
}

.graph-node span {
  color: #64748b;
  font-size: 11px;
}

.node-entry,
.node-exit {
  border-color: #93c5fd;
  background: #eff6ff;
}

.node-condition,
.node-loop,
.node-case {
  border-color: #fbbf24;
  background: #fffbeb;
}

.node-catch {
  border-color: #fca5a5;
  background: #fef2f2;
}

.node-merge {
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.graph-arrow {
  color: #94a3b8;
  font-weight: 700;
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

.method-detail-panel {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .cfg-form-grid,
  .cfg-content-grid {
    grid-template-columns: 1fr;
  }

  .cfg-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .cfg-stats-grid {
    grid-template-columns: 1fr;
  }

  .bar-item {
    grid-template-columns: 1fr 42px;
  }

  .bar-track {
    grid-column: 1 / -1;
  }
}
</style>
