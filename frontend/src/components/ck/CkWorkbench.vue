<script setup>
import { computed, reactive, ref } from 'vue'
import MetricsRadarChart from './MetricsRadarChart.vue'

const apiBaseUrl = 'http://localhost:8080'

const projectName = ref('')
const classDiagramText = ref('')
const umlFileName = ref('')
const selectedJavaFiles = ref([])
const loading = ref(false)
const errorMessage = ref('')
const metricSet = ref('ck')

const result = reactive({
  classCount: 0,
  avgWmc: 0, avgDit: 0, avgNoc: 0, avgCbo: 0, avgRfc: 0, avgLcom: 0,
  avgCs: 0, avgNpa: 0, avgNoo: 0, avgNoa: 0,
  classCoverage: 0, methodDriftRate: 0, inheritanceConsistency: 0,
  consistencyIssues: [],
  classes: [],
})

async function analyze() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (selectedJavaFiles.value.length === 0 && !classDiagramText.value.trim()) {
      throw new Error('请上传 Java 源文件或类图文件后再分析。')
    }
    const sources = selectedJavaFiles.value.map((f) => ({ fileName: f.fileName, content: f.content }))
    const response = await fetch(`${apiBaseUrl}/api/metrics/ck/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: projectName.value || '未命名项目',
        metricSet: metricSet.value,
        classDiagramText: classDiagramText.value,
        flowDiagramText: '',
        useCaseText: '',
        sources,
      }),
    })
    if (!response.ok) throw new Error(`请求失败: ${response.status}`)
    const data = await response.json()
    Object.assign(result, {
      classCount: data.classCount ?? 0,
      avgWmc: data.avgWmc ?? 0, avgDit: data.avgDit ?? 0,
      avgNoc: data.avgNoc ?? 0, avgCbo: data.avgCbo ?? 0,
      avgRfc: data.avgRfc ?? 0, avgLcom: data.avgLcom ?? 0,
      avgCs: data.avgCs ?? 0, avgNpa: data.avgNpa ?? 0,
      avgNoo: data.avgNoo ?? 0, avgNoa: data.avgNoa ?? 0,
      classCoverage: data.classCoverage ?? 0,
      methodDriftRate: data.methodDriftRate ?? 0,
      inheritanceConsistency: data.inheritanceConsistency ?? 0,
      consistencyIssues: data.consistencyIssues ?? [],
      classes: data.classes ?? [],
    })
  } catch (e) {
    errorMessage.value = e instanceof Error ? e.message : '分析失败'
  } finally {
    loading.value = false
  }
}

const hasResult = computed(() => result.classes.length > 0)
const isCk = computed(() => metricSet.value === 'ck')

const wmcDistribution = computed(() => {
  const buckets = [
    { label: '0–5', min: 0, max: 5, count: 0 },
    { label: '6–10', min: 6, max: 10, count: 0 },
    { label: '11–20', min: 11, max: 20, count: 0 },
    { label: '21+', min: 21, max: Infinity, count: 0 },
  ]
  for (const item of result.classes) {
    const b = buckets.find((b) => item.wmc >= b.min && item.wmc <= b.max)
    if (b) b.count++
  }
  return buckets
})

// 排序
const sortKey = ref('')
const sortDesc = ref(true)
function setSort(key) {
  if (sortKey.value === key) sortDesc.value = !sortDesc.value
  else { sortKey.value = key; sortDesc.value = true }
}
const sortedClasses = computed(() => {
  if (!sortKey.value) return result.classes
  return [...result.classes].sort((a, b) => {
    const v = (a[sortKey.value] ?? 0) - (b[sortKey.value] ?? 0)
    return sortDesc.value ? -v : v
  })
})

async function handleUmlFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) { umlFileName.value = ''; return }
  try {
    classDiagramText.value = await file.text()
    umlFileName.value = file.name
  } catch {
    errorMessage.value = '读取文件失败，请确认文件可读。'
  }
}

async function handleJavaFilesChange(event) {
  const files = Array.from(event.target.files ?? []).filter((f) => f.name.endsWith('.java'))
  selectedJavaFiles.value = await Promise.all(
    files.map(async (f) => ({ fileName: f.name, content: await f.text() }))
  )
}
</script>

<template>
  <section class="panel">
    <!-- 头部 -->
    <div class="panel-head">
      <div>
        <h3 class="panel-title">面向对象度量分析</h3>
        <p class="panel-sub">基于源代码或类图，自动计算 CK / LK 度量指标，识别设计风险。</p>
      </div>
      <div class="mode-switch">
        <button :class="['mode-btn', metricSet === 'ck' && 'active']" @click="metricSet = 'ck'">CK 度量</button>
        <button :class="['mode-btn', metricSet === 'lk' && 'active']" @click="metricSet = 'lk'">LK 度量</button>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="input-grid">
      <div class="input-card">
        <p class="input-label">
          <span class="input-icon">☕</span> Java 源文件
        </p>
        <label class="file-btn">
          选择文件（支持多选）
          <input type="file" accept=".java" multiple hidden @change="handleJavaFilesChange" />
        </label>
        <p class="file-hint" v-if="selectedJavaFiles.length > 0">
          已选 {{ selectedJavaFiles.length }} 个文件：{{ selectedJavaFiles.map((f) => f.fileName).join('、') }}
        </p>
        <p class="file-hint muted" v-else>支持同时上传多个 .java 文件</p>
      </div>

      <div class="input-card">
        <p class="input-label">
          <span class="input-icon">📐</span> 类图文件
        </p>
        <label class="file-btn">
          选择文件
          <input type="file" accept=".puml,.plantuml,.uml,.xmi,.xml,.txt" hidden @change="handleUmlFileChange" />
        </label>
        <p class="file-hint" v-if="umlFileName">已选：{{ umlFileName }}</p>
        <p class="file-hint muted" v-else>支持 PlantUML / XMI / XML</p>
        <textarea
          v-model="classDiagramText"
          rows="3"
          placeholder="或直接粘贴类图文本…"
          class="diagram-input"
        />
      </div>

      <div class="input-card">
        <p class="input-label">
          <span class="input-icon">🏷️</span> 项目信息
        </p>
        <input v-model="projectName" placeholder="项目名称（可选）" class="text-input" />
      </div>
    </div>

    <div class="action-row">
      <button class="primary-btn" :disabled="loading" @click="analyze">
        {{ loading ? '分析中…' : '开始分析' }}
      </button>
      <p v-if="errorMessage" class="error-msg">{{ errorMessage }}</p>
    </div>
  </section>

  <!-- 结果区 -->
  <section v-if="hasResult" class="panel result-panel">
    <div class="result-head">
      <h3 class="panel-title">分析结果 · {{ result.classCount }} 个类</h3>
      <span class="mode-badge">{{ isCk ? 'CK 度量集' : 'LK 度量集' }}</span>
    </div>

    <!-- 核心指标卡片 -->
    <div class="metrics-row" v-if="isCk">
      <div class="metric-card">
        <p class="metric-name">WMC</p>
        <p class="metric-val">{{ result.avgWmc }}</p>
        <p class="metric-desc">方法加权复杂度</p>
      </div>
      <div class="metric-card">
        <p class="metric-name">DIT</p>
        <p class="metric-val">{{ result.avgDit }}</p>
        <p class="metric-desc">继承树深度</p>
      </div>
      <div class="metric-card">
        <p class="metric-name">NOC</p>
        <p class="metric-val">{{ result.avgNoc }}</p>
        <p class="metric-desc">直接子类数</p>
      </div>
      <div class="metric-card highlight">
        <p class="metric-name">CBO</p>
        <p class="metric-val">{{ result.avgCbo }}</p>
        <p class="metric-desc">类间耦合度</p>
      </div>
      <div class="metric-card highlight">
        <p class="metric-name">RFC</p>
        <p class="metric-val">{{ result.avgRfc }}</p>
        <p class="metric-desc">响应方法集</p>
      </div>
      <div class="metric-card highlight">
        <p class="metric-name">LCOM</p>
        <p class="metric-val">{{ result.avgLcom }}</p>
        <p class="metric-desc">内聚缺失度</p>
      </div>
    </div>
    <div class="metrics-row" v-else>
      <div class="metric-card highlight">
        <p class="metric-name">CS</p>
        <p class="metric-val">{{ result.avgCs }}</p>
        <p class="metric-desc">类规模</p>
      </div>
      <div class="metric-card">
        <p class="metric-name">NPA</p>
        <p class="metric-val">{{ result.avgNpa }}</p>
        <p class="metric-desc">公有属性数</p>
      </div>
      <div class="metric-card">
        <p class="metric-name">NOO</p>
        <p class="metric-val">{{ result.avgNoo }}</p>
        <p class="metric-desc">重写方法数</p>
      </div>
      <div class="metric-card">
        <p class="metric-name">NOA</p>
        <p class="metric-val">{{ result.avgNoa }}</p>
        <p class="metric-desc">新增方法数</p>
      </div>
    </div>

    <!-- 图表 + 明细 -->
    <div class="content-grid">
      <div class="left-col">
        <!-- 类明细表 -->
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th class="th-name">类名</th>
                <template v-if="isCk">
                  <th v-for="col in ['wmc','dit','noc','cbo','rfc','lcom']" :key="col"
                      class="th-metric sortable" @click="setSort(col)">
                    {{ col.toUpperCase() }}
                    <span class="sort-icon">{{ sortKey === col ? (sortDesc ? '↓' : '↑') : '↕' }}</span>
                  </th>
                </template>
                <template v-else>
                  <th v-for="col in ['cs','npa','noo','noa']" :key="col"
                      class="th-metric sortable" @click="setSort(col)">
                    {{ col.toUpperCase() }}
                    <span class="sort-icon">{{ sortKey === col ? (sortDesc ? '↓' : '↑') : '↕' }}</span>
                  </th>
                </template>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in sortedClasses" :key="item.className">
                <td class="td-name" :title="item.className">{{ item.className }}</td>
                <template v-if="isCk">
                  <td :class="item.wmc > 10 ? 'warn' : ''">{{ item.wmc }}</td>
                  <td :class="item.dit > 4 ? 'warn' : ''">{{ item.dit }}</td>
                  <td>{{ item.noc }}</td>
                  <td :class="item.cbo > 10 ? 'warn' : ''">{{ item.cbo }}</td>
                  <td :class="item.rfc > 20 ? 'warn' : ''">{{ item.rfc }}</td>
                  <td :class="item.lcom > 10 ? 'warn' : ''">{{ item.lcom }}</td>
                </template>
                <template v-else>
                  <td :class="item.cs > 20 ? 'warn' : ''">{{ item.cs }}</td>
                  <td :class="item.npa > 0 ? 'warn' : ''">{{ item.npa }}</td>
                  <td>{{ item.noo }}</td>
                  <td>{{ item.noa }}</td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- WMC 分布直方图（仅 CK） -->
        <div v-if="isCk" class="histogram">
          <p class="section-label">WMC 分布</p>
          <div class="bars">
            <div v-for="b in wmcDistribution" :key="b.label" class="bar-row">
              <span class="bar-label">{{ b.label }}</span>
              <div class="bar-track">
                <div class="bar-fill"
                  :style="{ width: `${Math.max(b.count / Math.max(result.classCount, 1) * 100, b.count > 0 ? 6 : 0)}%` }" />
              </div>
              <span class="bar-count">{{ b.count }}</span>
            </div>
          </div>
        </div>

        <!-- 一致性问题 -->
        <div v-if="result.consistencyIssues.length > 0" class="issues-wrap">
          <p class="section-label">设计一致性问题</p>
          <div class="issue-badges">
            <span class="issue-badge">类覆盖率 {{ result.classCoverage }}%</span>
            <span class="issue-badge">方法漂移 {{ result.methodDriftRate }}%</span>
            <span class="issue-badge">继承一致性 {{ result.inheritanceConsistency }}%</span>
          </div>
          <ul class="issue-list">
            <li v-for="issue in result.consistencyIssues" :key="issue">{{ issue }}</li>
          </ul>
        </div>
      </div>

      <!-- 雷达图 -->
      <div class="right-col">
        <MetricsRadarChart :metrics="result" :mode="metricSet" />

        <!-- 风险提示 -->
        <div class="tips-card">
          <p class="section-label">风险解读</p>
          <ul v-if="isCk">
            <li>WMC &gt; 10：方法过多，类职责可能过重</li>
            <li>CBO &gt; 10：耦合偏高，模块边界需关注</li>
            <li>LCOM &gt; 10：内聚不足，建议拆分类</li>
            <li>DIT &gt; 4：继承链过深，维护成本高</li>
          </ul>
          <ul v-else>
            <li>CS &gt; 20：类体量过大，建议按职责拆分</li>
            <li>NPA &gt; 0：存在公有属性，封装性不足</li>
            <li>NOO / NOA 比值低：继承复用率偏低</li>
          </ul>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px;
}

.panel-sub {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.mode-switch {
  display: flex;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
}

.mode-btn {
  padding: 7px 18px;
  font-size: 13px;
  font-weight: 500;
  background: #fff;
  color: #64748b;
  border: none;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.mode-btn.active {
  background: #2563eb;
  color: #fff;
}

/* 输入区 */
.input-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 14px;
  margin-bottom: 16px;
}

.input-card {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 12px;
  padding: 14px;
  background: #f8fafc;
}

.input-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.input-icon {
  font-size: 15px;
}

.file-btn {
  display: inline-block;
  padding: 7px 14px;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  cursor: pointer;
  transition: border-color 0.15s;
}

.file-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

.file-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #334155;
  word-break: break-all;
}

.file-hint.muted {
  color: #94a3b8;
}

.diagram-input {
  width: 100%;
  margin-top: 10px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 8px;
  padding: 8px;
  font-size: 12px;
  font-family: ui-monospace, monospace;
  resize: vertical;
  box-sizing: border-box;
  background: #fff;
}

.text-input {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 8px;
  padding: 9px 10px;
  font-size: 13px;
  box-sizing: border-box;
  background: #fff;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.primary-btn {
  padding: 10px 28px;
  background: #2563eb;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.primary-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-msg {
  color: #dc2626;
  font-size: 13px;
  margin: 0;
}

/* 结果区 */
.result-panel {
  margin-top: 0;
}

.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.mode-badge {
  padding: 3px 10px;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

/* 指标卡片 */
.metrics-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.metric-card {
  flex: 1;
  min-width: 80px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px 10px;
  text-align: center;
  background: #f8fafc;
}

.metric-card.highlight {
  border-color: rgba(37, 99, 235, 0.25);
  background: #eff6ff;
}

.metric-name {
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin: 0 0 4px;
  letter-spacing: 0.05em;
}

.metric-val {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 2px;
  line-height: 1;
}

.metric-card.highlight .metric-val {
  color: #1d4ed8;
}

.metric-desc {
  font-size: 11px;
  color: #94a3b8;
  margin: 0;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 20px;
  align-items: start;
}

.left-col {
  min-width: 0;
}

/* 表格 */
.table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.th-name {
  text-align: left;
  padding: 10px 12px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  white-space: nowrap;
}

.th-metric {
  padding: 10px 10px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  white-space: nowrap;
  text-align: center;
}

.th-metric.sortable {
  cursor: pointer;
  user-select: none;
}

.th-metric.sortable:hover {
  background: #f1f5f9;
  color: #2563eb;
}

.sort-icon {
  font-size: 10px;
  margin-left: 3px;
  opacity: 0.5;
}

td {
  padding: 9px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  text-align: center;
  color: #334155;
}

.td-name {
  text-align: left;
  padding-left: 12px;
  font-weight: 500;
  color: #0f172a;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

td.warn {
  color: #b45309;
  font-weight: 600;
  background: #fffbeb;
}

tbody tr:last-child td {
  border-bottom: none;
}

tbody tr:hover td {
  background: #f8fafc;
}

/* 直方图 */
.histogram {
  margin-top: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 14px;
  background: #f8fafc;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin: 0 0 10px;
}

.bars {
  display: grid;
  gap: 7px;
}

.bar-row {
  display: grid;
  grid-template-columns: 44px 1fr 24px;
  align-items: center;
  gap: 8px;
}

.bar-label, .bar-count {
  font-size: 12px;
  color: #64748b;
}

.bar-track {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
  border-radius: 999px;
  transition: width 0.3s;
}

/* 一致性问题 */
.issues-wrap {
  margin-top: 14px;
  border: 1px solid rgba(234, 179, 8, 0.3);
  border-radius: 12px;
  padding: 14px;
  background: #fefce8;
}

.issue-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.issue-badge {
  padding: 3px 10px;
  background: #fff;
  border: 1px solid rgba(234, 179, 8, 0.4);
  border-radius: 999px;
  font-size: 12px;
  color: #92400e;
  font-weight: 500;
}

.issue-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #78350f;
}

.issue-list li {
  margin-bottom: 4px;
}

/* 右侧 */
.right-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tips-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 14px;
  background: #f8fafc;
}

.tips-card ul {
  margin: 0;
  padding-left: 16px;
  font-size: 13px;
  color: #475569;
}

.tips-card li {
  margin-bottom: 6px;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  .right-col {
    flex-direction: row;
    flex-wrap: wrap;
  }
  .right-col > * {
    flex: 1;
    min-width: 240px;
  }
}

@media (max-width: 800px) {
  .input-grid {
    grid-template-columns: 1fr;
  }
  .panel-head {
    flex-direction: column;
  }
  .metrics-row {
    gap: 8px;
  }
  .metric-card {
    min-width: 70px;
  }
}
</style>
