<script setup>
import { computed, reactive, ref } from 'vue'
import CkRadarChart from './CkRadarChart.vue'

const apiBaseUrl = 'http://localhost:8080'

const projectName = ref('SoftQuant Demo')
const sourceName = ref('DemoClass.java')
const sourceCode = ref(`class BaseAnalyzer {
    protected int score;
}

class DemoAnalyzer extends BaseAnalyzer {
    private String name;

    public void parse() {
        score = score + 1;
        Helper.log(name);
    }

    public int calculate() {
        return score;
    }
}

class Helper {
    public static void log(String message) {
        System.out.println(message);
    }
}`)
const classDiagramText = ref('')
const flowDiagramText = ref('')
const useCaseText = ref('')
const umlFileName = ref('')
const teamSize = ref(3)
const estimateWeeks = ref(4)
const loading = ref(false)
const errorMessage = ref('')

const result = reactive({
  classCount: 0,
  avgWmc: 0,
  avgDit: 0,
  avgNoc: 0,
  avgCbo: 0,
  avgRfc: 0,
  avgLcom: 0,
  classes: [],
})

async function analyzeCk() {
  loading.value = true
  errorMessage.value = ''
  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/ck/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: projectName.value,
        classDiagramText: classDiagramText.value,
        flowDiagramText: flowDiagramText.value,
        useCaseText: useCaseText.value,
        teamSize: Number(teamSize.value),
        estimateWeeks: Number(estimateWeeks.value),
        sources: [{ fileName: sourceName.value, content: sourceCode.value }],
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
    result.classes = data.classes ?? []
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分析失败'
  } finally {
    loading.value = false
  }
}

const hasResult = computed(() => result.classes.length > 0)

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
</script>

<template>
  <section class="feature-panel">
    <div class="panel-head">
      <p class="eyebrow">OO Metrics</p>
      <h3>面向对象度量模块（CK / LK）</h3>
    </div>
    <p class="module-intro">
      当前先落地 CK 六项核心指标（WMC/DIT/NOC/CBO/RFC/LCOM），页面已预留 LK 与传统复杂度融合入口。
    </p>

    <div class="module-tags">
      <span class="tag tag-active">CK 核心指标</span>
      <span class="tag">LK 扩展指标（预留）</span>
      <span class="tag">AST 解析（规划中）</span>
      <span class="tag">多源输入</span>
    </div>

    <h4 class="block-title">1) 项目上下文参数</h4>
    <div class="form-grid">
      <div class="field">
        <label>项目名称</label>
        <input v-model="projectName" />
      </div>
      <div class="field">
        <label>源码文件名</label>
        <input v-model="sourceName" />
      </div>
      <div class="field">
        <label>团队人数</label>
        <input v-model.number="teamSize" min="1" type="number" />
      </div>
      <div class="field">
        <label>估算工期(周)</label>
        <input v-model.number="estimateWeeks" min="1" type="number" />
      </div>
    </div>

    <h4 class="block-title">2) 多源输入（代码 + 设计文本）</h4>
    <div class="field">
      <label>程序代码（当前 CK 计算核心输入）</label>
      <textarea v-model="sourceCode" rows="12"></textarea>
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
    <div class="field">
      <label>流程图描述（文本）</label>
      <textarea v-model="flowDiagramText" rows="4" placeholder="例如：登录 -> 校验 -> 拉取任务 -> 生成报告" />
    </div>
    <div class="field">
      <label>用例描述（文本）</label>
      <textarea v-model="useCaseText" rows="4" placeholder="例如：分析员上传代码并生成质量评估报告" />
    </div>

    <div class="actions">
      <button class="primary-button" :disabled="loading" @click="analyzeCk">
        {{ loading ? '分析中...' : '执行 OO 度量分析' }}
      </button>
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
          <article class="stat-card accent-gold"><p class="stat-label">平均 WMC</p><strong>{{ result.avgWmc }}</strong></article>
          <article class="stat-card accent-green"><p class="stat-label">平均 CBO</p><strong>{{ result.avgCbo }}</strong></article>
          <article class="stat-card accent-rose"><p class="stat-label">平均 RFC</p><strong>{{ result.avgRfc }}</strong></article>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Class</th>
                <th>WMC</th>
                <th>DIT</th>
                <th>NOC</th>
                <th>CBO</th>
                <th>RFC</th>
                <th>LCOM</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in result.classes" :key="item.className">
                <td>{{ item.className }}</td>
                <td>{{ item.wmc }}</td>
                <td>{{ item.dit }}</td>
                <td>{{ item.noc }}</td>
                <td>{{ item.cbo }}</td>
                <td>{{ item.rfc }}</td>
                <td>{{ item.lcom }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <CkRadarChart :metrics="result" />
    </div>

    <div class="insight-grid">
      <article class="insight-card">
        <p class="panel-label">指标解读建议</p>
        <ul>
          <li>WMC/RFC 偏高：优先检查类职责是否过重。</li>
          <li>CBO 偏高：关注依赖扩散与模块边界清晰度。</li>
          <li>LCOM 偏高：考虑按职责拆分类结构。</li>
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
textarea {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 10px;
  padding: 10px;
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
