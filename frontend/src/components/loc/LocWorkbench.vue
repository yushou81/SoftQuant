<script setup>
import { computed, ref } from 'vue'

const apiBaseUrl = 'http://localhost:8080'

const supportedExtensions = [
  '.java',
  '.py',
  '.cpp',
  '.cc',
  '.cxx',
  '.c',
  '.h',
  '.hpp',
  '.hh',
  '.hxx',
  '.cs',
  '.js',
  '.jsx',
  '.ts',
  '.tsx',
]

const projectName = ref('SoftQuant LOC Demo')
const selectedFiles = ref([])
const skippedFiles = ref([])
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const result = ref(null)

const hasFiles = computed(() => selectedFiles.value.length > 0)
const hasResult = computed(() => Boolean(result.value))

const topCodeFiles = computed(() => {
  if (!result.value?.files) {
    return []
  }
  return [...result.value.files]
    .sort((a, b) => b.codeLines - a.codeLines)
    .slice(0, 8)
})

const composition = computed(() => {
  const total = Math.max(result.value?.physicalLines ?? 0, 1)
  return [
    { key: 'code', label: '代码', value: result.value?.codeLines ?? 0, color: '#2563eb' },
    { key: 'comment', label: '纯注释', value: result.value?.commentLines ?? 0, color: '#f59e0b' },
    { key: 'blank', label: '空白', value: result.value?.blankLines ?? 0, color: '#94a3b8' },
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

  const accepted = files.filter((file) => isSupportedFile(file.name))
  skippedFiles.value = files
    .filter((file) => !isSupportedFile(file.name))
    .map((file) => file.webkitRelativePath || file.name)

  const loaded = await Promise.all(
    accepted.map(async (file) => ({
      fileName: file.webkitRelativePath || file.name,
      language: inferLanguage(file.name),
      content: await file.text(),
    })),
  )

  selectedFiles.value = mergeFiles(selectedFiles.value, loaded)
  if (loaded.length > 0) {
    successMessage.value = `已载入 ${loaded.length} 个源码文件。`
  }
  if (files.length > 0 && loaded.length === 0) {
    errorMessage.value = '没有识别到可分析的源码文件。'
  }
}

async function executeAnalyze() {
  if (selectedFiles.value.length === 0) {
    errorMessage.value = '请先选择源码文件后再分析。'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/loc/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: projectName.value,
        sources: selectedFiles.value.map((file) => ({
          fileName: file.fileName,
          language: file.language,
          content: file.content,
        })),
      }),
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }

    result.value = await response.json()
    successMessage.value = '代码行度量完成，统计结果已刷新。'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '分析失败'
  } finally {
    loading.value = false
  }
}

function loadSampleFiles() {
  selectedFiles.value = [
    {
      fileName: 'sample/Demo.java',
      language: 'JAVA',
      content: [
        'package sample;',
        '',
        'public class Demo {',
        '    private String text = "not // comment"; // inline note',
        '    /*',
        '     * block comment',
        '     */',
        '    public int add(int a, int b) {',
        '        int total = a + b;',
        '        return total;',
        '    }',
        '}',
      ].join('\n'),
    },
    {
      fileName: 'sample/script.py',
      language: 'PYTHON',
      content: [
        '"""module doc"""',
        'import os  # inline note',
        '',
        'def run():',
        '    text = "# not comment"',
        '    return text',
      ].join('\n'),
    },
  ]
  skippedFiles.value = []
  result.value = null
  errorMessage.value = ''
  successMessage.value = '已载入 Java/Python 示例文件。'
}

function clearFiles() {
  selectedFiles.value = []
  skippedFiles.value = []
  result.value = null
  errorMessage.value = ''
  successMessage.value = ''
}

function removeFile(fileName) {
  selectedFiles.value = selectedFiles.value.filter((file) => file.fileName !== fileName)
  result.value = null
}

function mergeFiles(current, incoming) {
  const fileMap = new Map(current.map((file) => [file.fileName, file]))
  for (const file of incoming) {
    fileMap.set(file.fileName, file)
  }
  return [...fileMap.values()].sort((a, b) => a.fileName.localeCompare(b.fileName))
}

function isSupportedFile(fileName) {
  const lower = fileName.toLowerCase()
  return supportedExtensions.some((extension) => lower.endsWith(extension))
}

function inferLanguage(fileName) {
  const lower = fileName.toLowerCase()
  if (lower.endsWith('.java')) return 'JAVA'
  if (lower.endsWith('.py')) return 'PYTHON'
  if (['.cpp', '.cc', '.cxx', '.hpp', '.hh', '.hxx'].some((extension) => lower.endsWith(extension))) return 'CPP'
  if (lower.endsWith('.c') || lower.endsWith('.h')) return 'C'
  if (lower.endsWith('.cs')) return 'CSHARP'
  if (lower.endsWith('.js') || lower.endsWith('.jsx')) return 'JAVASCRIPT'
  if (lower.endsWith('.ts') || lower.endsWith('.tsx')) return 'TYPESCRIPT'
  return 'GENERIC'
}

function languageLabel(language) {
  const labels = {
    JAVA: 'Java',
    PYTHON: 'Python',
    CPP: 'C++',
    C: 'C',
    CSHARP: 'C#',
    JAVASCRIPT: 'JavaScript',
    TYPESCRIPT: 'TypeScript',
    GENERIC: 'Generic',
  }
  return labels[language] ?? language
}

function formatRate(value) {
  return `${Number(value ?? 0).toFixed(2)}%`
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
      <p class="eyebrow">LOC Metrics</p>
      <h3>代码行度量模块</h3>
    </div>
    <p class="module-intro">
      支持多文件上传，按项目、语言和单文件统计物理行、有效代码行、注释行、空白行、混合行与逻辑行。
    </p>

    <div class="module-tags">
      <span class="tag tag-active">物理 LOC</span>
      <span class="tag">逻辑 LOC</span>
      <span class="tag">注释率</span>
      <span class="tag">Java AST</span>
      <span class="tag">多语言扫描</span>
    </div>

    <div class="form-grid loc-form-grid">
      <div class="field">
        <label>项目名称</label>
        <input v-model="projectName" />
      </div>
      <div class="field">
        <label>单文件/多文件上传</label>
        <input
          type="file"
          class="file-input"
          accept=".java,.py,.cpp,.cc,.cxx,.c,.h,.hpp,.hh,.hxx,.cs,.js,.jsx,.ts,.tsx"
          multiple
          @change="handleFilesChange"
        />
      </div>
      <div class="field">
        <label>项目目录上传</label>
        <input
          type="file"
          class="file-input"
          accept=".java,.py,.cpp,.cc,.cxx,.c,.h,.hpp,.hh,.hxx,.cs,.js,.jsx,.ts,.tsx"
          multiple
          webkitdirectory
          @change="handleFilesChange"
        />
      </div>
    </div>

    <div class="actions">
      <button class="primary-button" :disabled="loading" @click="executeAnalyze">
        {{ loading ? '分析中...' : '执行代码行度量' }}
      </button>
      <button class="secondary-button" type="button" @click="loadSampleFiles">载入示例</button>
      <button class="secondary-button" type="button" :disabled="!hasFiles" @click="clearFiles">清空文件</button>
      <span class="helper-inline">当前 {{ selectedFiles.length }} 个文件</span>
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
              <th>语言</th>
              <th>大小</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="file in selectedFiles" :key="file.fileName">
              <td class="file-cell">{{ file.fileName }}</td>
              <td>{{ languageLabel(file.language) }}</td>
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

  <section v-if="hasResult" class="feature-panel loc-result-panel">
    <div class="panel-head">
      <p class="eyebrow">Result</p>
      <h3>代码行度量结果</h3>
    </div>

    <div class="stats-grid loc-stats-grid">
      <article class="stat-card accent-blue">
        <p class="stat-label">物理总行</p>
        <strong>{{ result.physicalLines }}</strong>
        <span>{{ result.fileCount }} 个文件</span>
      </article>
      <article class="stat-card accent-green">
        <p class="stat-label">有效代码行</p>
        <strong>{{ result.codeLines }}</strong>
        <span>含 {{ result.mixedLines }} 行行尾注释</span>
      </article>
      <article class="stat-card accent-gold">
        <p class="stat-label">注释率</p>
        <strong>{{ formatRate(result.commentRate) }}</strong>
        <span>{{ result.commentLines }} 行纯注释</span>
      </article>
      <article class="stat-card accent-rose">
        <p class="stat-label">逻辑代码行</p>
        <strong>{{ result.logicalLines }}</strong>
        <span>Java 使用 AST 统计</span>
      </article>
    </div>

    <div class="content-grid loc-content-grid">
      <article class="metric-panel">
        <p class="panel-label">行组成</p>
        <div class="stacked-bar" aria-label="LOC composition">
          <span
            v-for="segment in composition"
            :key="segment.key"
            :style="{ width: `${segment.width}%`, backgroundColor: segment.color }"
          ></span>
        </div>
        <div class="legend-grid">
          <span v-for="segment in composition" :key="segment.key">
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
        <p class="panel-label">代码行 Top 文件</p>
        <div class="bars">
          <div v-for="file in topCodeFiles" :key="file.fileName" class="bar-item">
            <span class="bar-label" :title="file.fileName">{{ file.fileName }}</span>
            <div class="bar-track">
              <div
                class="bar-fill"
                :style="{ width: `${percentOfTotal(file.codeLines, result.codeLines)}%` }"
              ></div>
            </div>
            <span class="bar-count">{{ file.codeLines }}</span>
          </div>
        </div>
      </article>
    </div>

    <div class="content-grid loc-content-grid">
      <article class="metric-panel">
        <p class="panel-label">语言汇总</p>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>语言</th>
                <th>文件</th>
                <th>物理行</th>
                <th>代码行</th>
                <th>注释行</th>
                <th>注释率</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in result.languageSummaries" :key="item.language">
                <td>{{ languageLabel(item.language) }}</td>
                <td>{{ item.fileCount }}</td>
                <td>{{ item.physicalLines }}</td>
                <td>{{ item.codeLines }}</td>
                <td>{{ item.commentLines }}</td>
                <td>{{ formatRate(item.commentRate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="metric-panel">
        <p class="panel-label">解释建议</p>
        <ul class="insight-list">
          <li>代码行偏高的文件可优先检查职责边界和重复逻辑。</li>
          <li>注释率过低时，重点补充复杂分支、算法和外部接口约束。</li>
          <li>混合行偏多说明行尾注释较多，报告中可单独解释统计规则。</li>
        </ul>
      </article>
    </div>

    <div class="metric-panel file-detail-panel">
      <p class="panel-label">文件明细</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>文件</th>
              <th>语言</th>
              <th>物理行</th>
              <th>逻辑行</th>
              <th>代码行</th>
              <th>纯注释</th>
              <th>空白</th>
              <th>混合</th>
              <th>注释率</th>
              <th>解析状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="file in result.files" :key="file.fileName">
              <td class="file-cell">{{ file.fileName }}</td>
              <td>{{ languageLabel(file.language) }}</td>
              <td>{{ file.physicalLines }}</td>
              <td>{{ file.logicalLines }}</td>
              <td>{{ file.codeLines }}</td>
              <td>{{ file.commentLines }}</td>
              <td>{{ file.blankLines }}</td>
              <td>{{ file.mixedLines }}</td>
              <td>{{ formatRate(file.commentRate) }}</td>
              <td>{{ file.parseStatus }}</td>
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

.loc-form-grid {
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

input {
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

.notice-wrap p:last-child {
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
  max-width: 320px;
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

.loc-result-panel {
  margin-top: 22px;
}

.loc-stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.loc-content-grid {
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

.stacked-bar span {
  min-width: 0;
  height: 100%;
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
  grid-template-columns: minmax(90px, 0.9fr) minmax(120px, 1.4fr) 42px;
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

.file-detail-panel {
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .loc-form-grid,
  .loc-content-grid {
    grid-template-columns: 1fr;
  }

  .loc-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .loc-stats-grid {
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
