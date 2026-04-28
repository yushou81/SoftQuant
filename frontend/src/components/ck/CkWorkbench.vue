<script setup>
import { computed, reactive, ref } from 'vue'
import CkRadarChart from './CkRadarChart.vue'

const apiBaseUrl = 'http://localhost:8080'

const projectName = ref('SoftQuant Demo')
const classDiagramText = ref('')
const umlFileName = ref('')
const selectedJavaFiles = ref([])
const teamSize = ref(3)
const estimateWeeks = ref(4)
const loading = ref(false)
const errorMessage = ref('')
const metricSet = ref('ck')

const result = reactive({
  classCount: 0,
  avgWmc: 0,
  avgDit: 0,
  avgNoc: 0,
  avgCbo: 0,
  avgRfc: 0,
  avgLcom: 0,
  avgCs: 0,
  avgNpa: 0,
  avgNoo: 0,
  avgNoa: 0,
  classCoverage: 0,
  methodDriftRate: 0,
  inheritanceConsistency: 0,
  consistencyIssues: [],
  classes: [],
})

async function analyzeCk() {
  loading.value = true
  errorMessage.value = ''
  try {
    if (selectedJavaFiles.value.length === 0) {
      throw new Error('请至少选择一个 Java 文件后再分析。')
    }

    const sources = selectedJavaFiles.value.map((file) => ({
      fileName: file.fileName,
      content: file.content,
    }))

    const response = await fetch(`${apiBaseUrl}/api/metrics/ck/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: projectName.value,
        metricSet: metricSet.value,
        classDiagramText: classDiagramText.value,
        flowDiagramText: '',
        useCaseText: '',
        teamSize: Number(teamSize.value),
        estimateWeeks: Number(estimateWeeks.value),
        sources,
      }),
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }

    const data = await response.json()
    result.classCount = data.classCount ?? 0
    result.avgWmc = data.avgWmc ?? 0
    result.avgDit = data.avgDit ?? 0
    result.avgNoc = data.avgNoc ?? 0
    result.avgCbo = data.avgCbo ?? 0
    result.avgRfc = data.avgRfc ?? 0
    result.avgLcom = data.avgLcom ?? 0
    result.avgCs = data.avgCs ?? 0
    result.avgNpa = data.avgNpa ?? 0
    result.avgNoo = data.avgNoo ?? 0
    result.avgNoa = data.avgNoa ?? 0
    result.classCoverage = data.classCoverage ?? 0
    result.methodDriftRate = data.methodDriftRate ?? 0
    result.inheritanceConsistency = data.inheritanceConsistency ?? 0
    result.consistencyIssues = data.consistencyIssues ?? []
    result.classes = data.classes ?? []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分析失败'
  } finally {
    loading.value = false
  }
}

const hasResult = computed(() => result.classes.length > 0)
const isCk = computed(() => metricSet.value === 'ck')
const wmcDistribution = computed(() => {
  const buckets = [
    { label: '0-5', min: 0, max: 5, count: 0 },
    { label: '6-10', min: 6, max: 10, count: 0 },
    { label: '11-20', min: 11, max: 20, count: 0 },
    { label: '21+', min: 21, max: Number.MAX_SAFE_INTEGER, count: 0 },
  ]
  for (const item of result.classes) {
    const hit = buckets.find((bucket) => item.wmc >= bucket.min && item.wmc <= bucket.max)
    if (hit) {
      hit.count += 1
    }
  }
  return buckets
})

async function handleUmlFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    umlFileName.value = ''
    return
  }
  try {
    const text = await file.text()
    classDiagramText.value = text
    umlFileName.value = file.name
  } catch (_error) {
    errorMessage.value = '读取 UML 文件失败，请确认文件可读。'
  }
}

async function handleJavaFilesChange(event) {
  const files = Array.from(event.target.files ?? [])
    .filter((file) => file.name.endsWith('.java'))
  const loaded = await Promise.all(
    files.map(async (file) => ({
      fileName: file.name,
      content: await file.text(),
    }))
  )
  selectedJavaFiles.value = loaded
}
</script>

<template>
  <section class="feature-panel">
    <div class="panel-head">
      <p class="eyebrow">OO Metrics</p>
      <h3>面向对象度量模块（CK / LK）</h3>
    </div>
    <p class="module-intro">
      当前先落地 CK 六项核心指标（WMC/DIT/NOC/CBO/RFC/LCOM），并预留 LK 指标与复杂度分布分析入口。
    </p>

    <div class="module-tags">
      <span class="tag tag-active">CK 核心指标</span>
      <span class="tag">LK 扩展指标（下一步）</span>
      <span class="tag">JavaParser + AST</span>
      <span class="tag">多源输入</span>
    </div>

    <div class="architecture-grid">
      <article class="arch-card">
        <p class="panel-label">后端技术路线</p>
        <ul>
          <li>解析引擎：JavaParser 构建 AST，替代正则匹配。</li>
          <li>遍历机制：Visitor 模式拦截类、方法、调用、变量节点。</li>
          <li>算法扩展：Strategy 模式封装 CK/LK 度量策略。</li>
          <li>多趟统计：先建继承关系，再汇总 DIT/NOC/CBO。</li>
        </ul>
      </article>
      <article class="arch-card">
        <p class="panel-label">前端交互目标</p>
        <ul>
          <li>左侧项目资源树（包/类勾选）与文件过滤。</li>
          <li>右上源码预览，支持语法高亮和定位。</li>
          <li>右下结果网格，可按指标排序定位风险类。</li>
          <li>图表面板支持单类雷达图 + 项目分布图。</li>
        </ul>
      </article>
    </div>

    <h4 class="block-title">1) 项目上下文参数</h4>
    <div class="form-grid">
      <div class="field">
        <label>项目名称</label>
        <input v-model="projectName" />
      </div>
      <div class="field">
        <label>团队人数</label>
        <input v-model.number="teamSize" min="1" type="number" />
      </div>
      <div class="field">
        <label>估算工期(周)</label>
        <input v-model.number="estimateWeeks" min="1" type="number" />
      </div>
      <div class="field">
        <label>度量策略</label>
        <select v-model="metricSet">
          <option value="ck">CK 度量集（当前可用）</option>
          <option value="lk">LK 度量集（CS/NPA/NOO/NOA）</option>
        </select>
      </div>
    </div>

    <h4 class="block-title">2) 输入方式（仅两种）</h4>
    <div class="field">
      <label>选取 Java 文件（支持多选）</label>
      <input
        type="file"
        class="file-input"
        accept=".java"
        multiple
        @change="handleJavaFilesChange"
      />
      <p class="helper-text" v-if="selectedJavaFiles.length > 0">
        已选择 {{ selectedJavaFiles.length }} 个 Java 文件：{{ selectedJavaFiles.map((f) => f.fileName).join(', ') }}
      </p>
      <p class="helper-text" v-else>尚未选择 Java 文件。</p>
    </div>
    <div class="field">
      <label>类图描述（文本/UML 摘要）</label>
      <input
        type="file"
        class="file-input"
        accept=".puml,.plantuml,.uml,.xmi,.xml,.txt"
        @change="handleUmlFileChange"
      />
      <p class="helper-text">
        支持上传 UML 文本文件（PlantUML/XMI/XML/TXT），内容会自动填充到下方输入框。
        <span v-if="umlFileName">当前文件：{{ umlFileName }}</span>
      </p>
      <textarea v-model="classDiagramText" rows="4" placeholder="例如：User -> Order(1..n), Order -> Item(n..n)" />
    </div>

    <div class="actions">
      <button class="primary-button" :disabled="loading" @click="analyzeCk">
        {{ loading ? '分析中...' : '执行 OO 度量分析' }}
      </button>
      <span v-if="metricSet === 'lk'" class="helper-inline">LK 已接入，重点评估类规模与封装性。</span>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>
  </section>

  <section v-if="hasResult" class="feature-panel ck-result-panel">
    <div class="panel-head">
      <p class="eyebrow">Result</p>
      <h3>OO 度量可视化与明细</h3>
    </div>

    <div class="result-grid">
      <div>
        <div class="stats-grid stats-grid-ck">
          <article class="stat-card accent-blue"><p class="stat-label">类总数</p><strong>{{ result.classCount }}</strong></article>
          <article class="stat-card accent-gold"><p class="stat-label">{{ isCk ? '平均 WMC' : '平均 CS' }}</p><strong>{{ isCk ? result.avgWmc : result.avgCs }}</strong></article>
          <article class="stat-card accent-green"><p class="stat-label">{{ isCk ? '平均 CBO' : '平均 NPA' }}</p><strong>{{ isCk ? result.avgCbo : result.avgNpa }}</strong></article>
          <article class="stat-card accent-rose"><p class="stat-label">{{ isCk ? '平均 RFC' : '平均 NOO' }}</p><strong>{{ isCk ? result.avgRfc : result.avgNoo }}</strong></article>
        </div>

        <div class="stats-grid stats-grid-ck">
          <article class="stat-card accent-blue"><p class="stat-label">类覆盖率</p><strong>{{ result.classCoverage }}%</strong></article>
          <article class="stat-card accent-gold"><p class="stat-label">方法漂移度</p><strong>{{ result.methodDriftRate }}%</strong></article>
          <article class="stat-card accent-green"><p class="stat-label">继承一致性</p><strong>{{ result.inheritanceConsistency }}%</strong></article>
          <article class="stat-card accent-rose"><p class="stat-label">一致性问题</p><strong>{{ result.consistencyIssues.length }}</strong></article>
        </div>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Class</th>
                <template v-if="isCk">
                  <th>WMC</th>
                  <th>DIT</th>
                  <th>NOC</th>
                  <th>CBO</th>
                  <th>RFC</th>
                  <th>LCOM</th>
                </template>
                <template v-else>
                  <th>CS</th>
                  <th>NPA</th>
                  <th>NOO</th>
                  <th>NOA</th>
                </template>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in result.classes" :key="item.className">
                <td>{{ item.className }}</td>
                <template v-if="isCk">
                  <td>{{ item.wmc }}</td>
                  <td>{{ item.dit }}</td>
                  <td>{{ item.noc }}</td>
                  <td>{{ item.cbo }}</td>
                  <td>{{ item.rfc }}</td>
                  <td>{{ item.lcom }}</td>
                </template>
                <template v-else>
                  <td>{{ item.cs }}</td>
                  <td>{{ item.npa }}</td>
                  <td>{{ item.noo }}</td>
                  <td>{{ item.noa }}</td>
                </template>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="consistency-wrap" v-if="result.consistencyIssues.length > 0">
          <p class="panel-label">设计-实现不一致清单</p>
          <ul>
            <li v-for="issue in result.consistencyIssues" :key="issue">{{ issue }}</li>
          </ul>
        </div>

        <div v-if="isCk" class="histogram-wrap">
          <p class="panel-label">WMC 项目分布（直方图）</p>
          <div class="bars">
            <div v-for="bucket in wmcDistribution" :key="bucket.label" class="bar-item">
              <span class="bar-label">{{ bucket.label }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: `${Math.max((bucket.count / Math.max(result.classCount, 1)) * 100, bucket.count > 0 ? 8 : 0)}%` }"
                ></div>
              </div>
              <span class="bar-count">{{ bucket.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <CkRadarChart v-if="isCk" :metrics="result" />
    </div>

    <div class="insight-grid">
      <article class="insight-card">
        <p class="panel-label">指标解读建议</p>
        <ul>
          <template v-if="isCk">
            <li>WMC/RFC 偏高：优先检查类职责是否过重。</li>
            <li>CBO 偏高：关注依赖扩散与模块边界清晰度。</li>
            <li>LCOM 偏高：考虑按职责拆分类结构。</li>
          </template>
          <template v-else>
            <li>CS 偏高：类体量过大，建议按职责拆分。</li>
            <li>NPA 大于 0：说明封装性不足，建议属性私有化。</li>
            <li>NOO 与 NOA 对比：可观察继承复用与新增职责平衡。</li>
          </template>
        </ul>
      </article>
      <article class="insight-card">
        <p class="panel-label">课程报告建议</p>
        <ul>
          <li>附类级 Top 风险清单（按 CBO 或 RFC 排序）。</li>
          <li>结合雷达图展示迭代前后优化效果。</li>
          <li>说明阈值依据与改进动作闭环。</li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

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

.architecture-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.arch-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px 14px;
}

.arch-card ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
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

.block-title {
  margin: 18px 0 10px;
  color: #334155;
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
select,
textarea {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 10px;
  padding: 10px;
}

.helper-inline {
  color: #64748b;
  font-size: 12px;
}

.file-input {
  margin-bottom: 8px;
  background: #fff;
}

.helper-text {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 12px;
}

textarea {
  font-family: "SFMono-Regular", ui-monospace, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.actions {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.error {
  color: #b91c1c;
  margin: 0;
}

.result-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
}

.stats-grid-ck {
  margin-top: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.table-wrap {
  margin-top: 16px;
  overflow-x: auto;
}

.histogram-wrap {
  margin-top: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.consistency-wrap {
  margin-top: 14px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.consistency-wrap ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
}

.bars {
  display: grid;
  gap: 8px;
}

.bar-item {
  display: grid;
  grid-template-columns: 48px 1fr 24px;
  align-items: center;
  gap: 8px;
}

.bar-label,
.bar-count {
  font-size: 12px;
  color: #475569;
}

.bar-track {
  height: 10px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
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
}

th {
  background: #f8fafc;
}

.insight-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.insight-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  background: #f8fafc;
  padding: 12px 14px;
}

.insight-card ul {
  margin: 8px 0 0;
  padding-left: 18px;
  color: #475569;
}

@media (max-width: 1180px) {
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-grid {
    grid-template-columns: 1fr;
  }

  .architecture-grid {
    grid-template-columns: 1fr;
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .form-grid,
  .stats-grid-ck {
    grid-template-columns: 1fr;
  }
}
</style>
