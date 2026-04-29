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
const fileInputRef = ref(null)
const folderInputRef = ref(null)
const dropzoneActive = ref(false)
const resultTab = ref('files')
const fileSortKey = ref('codeLines')
const fileSearch = ref('')

const hasFiles = computed(() => selectedFiles.value.length > 0)
const hasResult = computed(() => Boolean(result.value))
const fileCountLabel = computed(() => `${selectedFiles.value.length} 个文件`)
const skippedCountLabel = computed(() => (skippedFiles.value.length > 0 ? `跳过 ${skippedFiles.value.length}` : ''))

const uploadLanguageSummary = computed(() => {
  const summary = new Map()
  for (const file of selectedFiles.value) {
    const label = languageLabel(file.language)
    summary.set(label, (summary.get(label) ?? 0) + 1)
  }
  return [...summary.entries()].map(([language, count]) => ({ language, count }))
})

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

const sortedSelectedFiles = computed(() =>
  [...selectedFiles.value].sort((a, b) => a.fileName.localeCompare(b.fileName)),
)

const filteredResultFiles = computed(() => {
  if (!result.value?.files) {
    return []
  }

  const keyword = fileSearch.value.trim().toLowerCase()
  const files = keyword
    ? result.value.files.filter((file) => file.fileName.toLowerCase().includes(keyword))
    : [...result.value.files]

  const compareMap = {
    codeLines: (a, b) => b.codeLines - a.codeLines,
    logicalLines: (a, b) => b.logicalLines - a.logicalLines,
    commentRate: (a, b) => (b.commentRate ?? 0) - (a.commentRate ?? 0),
    fileName: (a, b) => a.fileName.localeCompare(b.fileName),
  }

  return files.sort(compareMap[fileSortKey.value] ?? compareMap.codeLines)
})

const overviewStats = computed(() => {
  if (!result.value) {
    return []
  }

  return [
    {
      label: '物理总行',
      value: result.value.physicalLines,
      meta: `${result.value.fileCount} 个文件`,
      accent: 'blue',
    },
    {
      label: '有效代码行',
      value: result.value.codeLines,
      meta: `混合行 ${result.value.mixedLines}`,
      accent: 'green',
    },
    {
      label: '注释率',
      value: formatRate(result.value.commentRate),
      meta: `纯注释 ${result.value.commentLines}`,
      accent: 'gold',
    },
    {
      label: '逻辑代码行',
      value: result.value.logicalLines,
      meta: 'Java AST / 其他文本统计',
      accent: 'rose',
    },
  ]
})

const insightCards = computed(() => {
  if (!result.value) {
    return []
  }

  const mostCodeFile = [...result.value.files].sort((a, b) => b.codeLines - a.codeLines)[0]
  const highestCommentFile = [...result.value.files].sort((a, b) => b.commentRate - a.commentRate)[0]

  return [
    {
      label: '主导文件',
      value: mostCodeFile ? mostCodeFile.fileName : '无',
      meta: mostCodeFile ? `${mostCodeFile.codeLines} 行代码` : '',
    },
    {
      label: '最高注释率',
      value: highestCommentFile ? formatRate(highestCommentFile.commentRate) : '0.00%',
      meta: highestCommentFile ? highestCommentFile.fileName : '',
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
  <section class="feature-panel loc-shell">
    <div class="panel-head loc-head">
      <div>
        <p class="eyebrow">LOC Metrics</p>
        <h3>代码行分析工作台</h3>
      </div>
      <div class="head-tools">
        <span class="status-pill status-pill-strong">项目级统计</span>
        <span class="status-pill">多语言</span>
        <span class="status-pill">Java AST</span>
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
        type="file"
        class="hidden-file-input"
        accept=".java,.py,.cpp,.cc,.cxx,.c,.h,.hpp,.hh,.hxx,.cs,.js,.jsx,.ts,.tsx"
        multiple
        @change="handleFilesChange"
      />
      <input
        ref="folderInputRef"
        type="file"
        class="hidden-file-input"
        accept=".java,.py,.cpp,.cc,.cxx,.c,.h,.hpp,.hh,.hxx,.cs,.js,.jsx,.ts,.tsx"
        multiple
        webkitdirectory
        @change="handleFilesChange"
      />

      <div class="dropzone-copy">
        <p class="dropzone-title">拖拽源码文件到这里</p>
        <p class="dropzone-subtitle">或使用下方快捷入口导入文件、文件夹与演示样例</p>
      </div>
      <div class="dropzone-actions">
        <button class="secondary-button" type="button" @click="openFilePicker">添加文件</button>
        <button class="secondary-button" type="button" @click="openFolderPicker">导入文件夹</button>
      </div>
      <div class="upload-meta">
        <span class="upload-pill upload-pill-strong">{{ fileCountLabel }}</span>
        <span v-if="skippedFiles.length > 0" class="upload-pill">{{ skippedCountLabel }}</span>
        <span
          v-for="item in uploadLanguageSummary"
          :key="item.language"
          class="upload-pill"
        >
          {{ item.language }} {{ item.count }}
        </span>
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
            <span class="file-chip-meta">{{ languageLabel(file.language) }} · {{ file.content.length }} chars</span>
          </div>
          <button class="chip-action" type="button" @click="removeFile(file.fileName)">移除</button>
        </div>
      </div>
    </div>
  </section>

  <section v-if="hasResult" class="feature-panel loc-result-panel">
    <div class="panel-head">
      <p class="eyebrow">Result</p>
      <h3>代码行度量结果</h3>
    </div>

    <div class="stats-grid loc-stats-grid">
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

    <div class="content-grid loc-content-grid">
      <article class="metric-panel">
        <div class="subhead-row">
          <p class="panel-label">行组成</p>
          <span class="helper-inline">{{ result.projectName }}</span>
        </div>
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
        <div class="insight-grid">
          <div v-for="item in insightCards" :key="item.label" class="insight-card">
            <span>{{ item.label }}</span>
            <strong :title="item.value">{{ item.value }}</strong>
            <small>{{ item.meta }}</small>
          </div>
        </div>
      </article>
    </div>

    <div class="metric-panel file-detail-panel">
      <div class="result-tabs">
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'files' }"
          type="button"
          @click="resultTab = 'files'"
        >
          文件明细
        </button>
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'languages' }"
          type="button"
          @click="resultTab = 'languages'"
        >
          语言汇总
        </button>
        <button
          class="result-tab"
          :class="{ 'result-tab-active': resultTab === 'rules' }"
          type="button"
          @click="resultTab = 'rules'"
        >
          统计规则
        </button>
      </div>

      <template v-if="resultTab === 'files'">
        <div class="result-toolbar">
          <input v-model="fileSearch" class="search-input" placeholder="搜索文件名" />
          <select v-model="fileSortKey" class="sort-select">
            <option value="codeLines">按代码行</option>
            <option value="logicalLines">按逻辑行</option>
            <option value="commentRate">按注释率</option>
            <option value="fileName">按文件名</option>
          </select>
        </div>
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
              <tr v-for="file in filteredResultFiles" :key="file.fileName">
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
      </template>

      <template v-else-if="resultTab === 'languages'">
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
            <p class="panel-label">阅读建议</p>
            <ul class="insight-list">
              <li>代码行偏高的文件适合优先检查职责边界和重复逻辑。</li>
              <li>注释率偏低时，优先补充复杂算法、边界条件和外部接口约束。</li>
              <li>混合行偏多通常说明行尾注释较多，报告里可单独说明统计口径。</li>
            </ul>
          </article>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.loc-shell {
  display: grid;
  gap: 18px;
}

.loc-head {
  align-items: flex-start;
}

.head-tools,
.upload-meta,
.module-tags {
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
.sort-select {
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

.chip-action,
.text-button {
  padding: 8px 12px;
  border-radius: 999px;
  background: #eef6ff;
  color: #1d4ed8;
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
  max-width: 320px;
  overflow: hidden;
  text-align: left;
  text-overflow: ellipsis;
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

.file-detail-panel {
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

@media (max-width: 1180px) {
  .toolbar-row,
  .loc-content-grid,
  .rules-grid {
    grid-template-columns: 1fr;
  }

  .loc-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .loc-stats-grid,
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
