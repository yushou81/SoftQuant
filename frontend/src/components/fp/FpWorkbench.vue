<script setup>
import { computed, reactive, ref } from 'vue'

const apiBaseUrl = 'http://localhost:8080'

const steps = [
  { key: 'upload', title: '上传与解析', subtitle: '导入 DFD XML 并生成候选' },
  { key: 'candidate', title: '确认组件候选', subtitle: '校正 ILF / EI / EO / EQ' },
  { key: 'matrix', title: '复杂度矩阵', subtitle: '维护 DET / RET / FTR 与复杂度' },
  { key: 'gsc', title: 'GSC 与语言换算', subtitle: '填写 14 项 GSC 和 SLOC/FP' },
  { key: 'result', title: 'FP 结果', subtitle: '查看 UFP、VAF、FP 与 LOC 轨迹' },
]

const componentTypeOptions = [
  { value: 'ILF', label: 'ILF' },
  { value: 'EIF', label: 'EIF' },
  { value: 'EI', label: 'EI' },
  { value: 'EO', label: 'EO' },
  { value: 'EQ', label: 'EQ' },
]

const complexityOptions = [
  { value: 'SIMPLE', label: '低' },
  { value: 'AVERAGE', label: '中' },
  { value: 'COMPLEX', label: '高' },
]

const gscBlueprint = [
  { code: 'G1', label: 'Data communications' },
  { code: 'G2', label: 'Distributed data processing' },
  { code: 'G3', label: 'Performance' },
  { code: 'G4', label: 'Heavily used configuration' },
  { code: 'G5', label: 'Transaction rate' },
  { code: 'G6', label: 'Online data entry' },
  { code: 'G7', label: 'End-user efficiency' },
  { code: 'G8', label: 'Online update' },
  { code: 'G9', label: 'Complex processing' },
  { code: 'G10', label: 'Reusability' },
  { code: 'G11', label: 'Installation ease' },
  { code: 'G12', label: 'Operational ease' },
  { code: 'G13', label: 'Multiple sites' },
  { code: 'G14', label: 'Facilitate change' },
]

const activeStep = ref(0)
const parseState = reactive({
  projectName: '数据流图1',
  sourceName: '',
  xmlContent: '',
  fileName: '',
  loading: false,
  error: '',
  success: '',
})

const parseResult = ref(null)
const componentDrafts = ref([])
const gscDrafts = ref(gscBlueprint.map((item) => ({ ...item, score: 0, dirty: false })))
const languageDraft = reactive({
  code: 'JAVA',
  slocPerFp: 60,
  dirty: false,
})

const calculationState = reactive({
  loading: false,
  error: '',
  success: '',
})
const calculationResult = ref(null)

const parseSummary = computed(() => {
  if (!parseResult.value) {
    return [
      { label: 'Process', value: '0' },
      { label: 'Flow', value: '0' },
      { label: '候选组件', value: '0' },
      { label: '待确认项', value: '0' },
    ]
  }

  return [
    { label: 'Process', value: String(parseResult.value.processes.length) },
    { label: 'Flow', value: String(parseResult.value.flows.length) },
    { label: '候选组件', value: String(parseResult.value.componentCandidates.length) },
    { label: '待确认项', value: String(parseResult.value.pendingFields.length) },
  ]
})

const canMoveToCandidate = computed(() => Boolean(parseResult.value))
const canMoveToMatrix = computed(() => componentDrafts.value.length > 0)
const canMoveToGsc = computed(() => componentDrafts.value.length > 0)
const canMoveToResult = computed(() => componentDrafts.value.length > 0)

const gscTotal = computed(() => gscDrafts.value.reduce((total, item) => total + Number(item.score), 0))

async function handleXmlFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) {
    parseState.fileName = ''
    parseState.sourceName = ''
    parseState.xmlContent = ''
    return
  }

  try {
    parseState.fileName = file.name
    parseState.sourceName = file.name
    parseState.xmlContent = await file.text()
  } catch (_error) {
    parseState.error = '读取 XML 文件失败，请确认文件编码或访问权限。'
  }
}

async function executeParse() {
  if (!parseState.xmlContent.trim()) {
    parseState.error = '请先上传 PowerDesigner DFD XML。'
    return
  }

  parseState.loading = true
  parseState.error = ''
  parseState.success = ''
  calculationState.error = ''
  calculationState.success = ''
  calculationResult.value = null

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/fp/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: parseState.projectName,
        sourceName: parseState.sourceName,
        sourceType: 'POWERDESIGNER_DFD',
        xmlContent: parseState.xmlContent,
      }),
    })

    if (!response.ok) {
      throw new Error(`解析失败: ${response.status}`)
    }

    const data = await response.json()
    parseResult.value = data
    // 同样保持三层状态，避免候选编辑覆盖掉原始解析证据。
    componentDrafts.value = data.componentCandidates.map((item) => ({
      ...item,
      componentType: item.componentType,
      det: item.det ?? 0,
      ret: item.ret ?? 0,
      ftr: item.ftr ?? 0,
      complexityLevel: inferDefaultComplexity(item.componentType),
      dirty: false,
    }))
    gscDrafts.value = gscBlueprint.map((item) => ({ ...item, score: 0, dirty: false }))
    languageDraft.code = 'JAVA'
    languageDraft.slocPerFp = 60
    languageDraft.dirty = false
    activeStep.value = 1
    parseState.success = 'DFD 解析完成，已生成功能组件候选与流向证据。'
  } catch (error) {
    parseState.error = error instanceof Error ? error.message : '解析失败'
  } finally {
    parseState.loading = false
  }
}

function updateComponentType(component, value) {
  component.componentType = value
  component.dirty = true
}

function updateComponentComplexity(component, value) {
  component.complexityLevel = value
  component.dirty = true
}

function markDirty(target) {
  target.dirty = true
}

function updateGscScore(target, rawValue) {
  const value = Number(rawValue)
  target.score = Number.isFinite(value) ? clamp(value, 0, 5) : 0
  target.dirty = true
}

async function executeCalculate() {
  calculationState.loading = true
  calculationState.error = ''
  calculationState.success = ''

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/fp/calculate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: parseState.projectName,
        components: componentDrafts.value.map((item) => ({
          candidateId: item.candidateId,
          componentType: item.componentType,
          name: item.name,
          det: Number(item.det),
          ret: needsRet(item.componentType) ? Number(item.ret) : null,
          ftr: needsRet(item.componentType) ? null : Number(item.ftr),
          complexityLevel: item.complexityLevel,
        })),
        gscScores: gscDrafts.value.map((item) => ({
          code: item.code,
          score: Number(item.score),
        })),
        language: {
          code: languageDraft.code,
          slocPerFp: Number(languageDraft.slocPerFp),
        },
      }),
    })

    if (!response.ok) {
      throw new Error(`计算失败: ${response.status}`)
    }

    calculationResult.value = await response.json()
    calculationState.success = '功能点计算完成，复杂度矩阵、VAF 和 LOC 已刷新。'
    activeStep.value = 4
  } catch (error) {
    calculationState.error = error instanceof Error ? error.message : '计算失败'
  } finally {
    calculationState.loading = false
  }
}

function jumpToStep(index) {
  if (index <= activeStep.value) {
    activeStep.value = index
    return
  }

  if (index === 1 && canMoveToCandidate.value) {
    activeStep.value = index
  }
  if (index === 2 && canMoveToMatrix.value) {
    activeStep.value = index
  }
  if (index === 3 && canMoveToGsc.value) {
    activeStep.value = index
  }
  if (index === 4 && canMoveToResult.value) {
    activeStep.value = index
  }
}

function inferDefaultComplexity(componentType) {
  if (componentType === 'ILF') {
    return 'AVERAGE'
  }
  return 'SIMPLE'
}

function needsRet(componentType) {
  return componentType === 'ILF' || componentType === 'EIF'
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}
</script>

<template>
  <section class="feature-panel workbench-panel">
    <div class="panel-head workbench-head">
      <div>
        <p class="eyebrow">FP Workbench</p>
        <h3>功能点度量五步工作台</h3>
      </div>
      <div class="status-badges">
        <span class="status-pill status-pill-amber">候选识别 + 人工确认双阶段</span>
      </div>
    </div>

    <div class="stepper-shell stepper-shell-five">
      <button
        v-for="(step, index) in steps"
        :key="step.key"
        class="step-chip"
        :class="{
          'step-chip-active': activeStep === index,
          'step-chip-ready': index < activeStep,
        }"
        @click="jumpToStep(index)"
      >
        <span class="step-index">{{ index + 1 }}</span>
        <span>
          <strong>{{ step.title }}</strong>
          <small>{{ step.subtitle }}</small>
        </span>
      </button>
    </div>

    <section class="stats-grid workbench-stats">
      <article v-for="item in parseSummary" :key="item.label" class="stat-card accent-gold">
        <p class="stat-label">{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>解析预览摘要</span>
      </article>
    </section>

    <template v-if="activeStep === 0">
      <section class="workspace-grid">
        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 1</p>
              <h4>上传 PowerDesigner DFD</h4>
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label>项目名称</label>
              <input v-model="parseState.projectName" />
            </div>
            <div class="field">
              <label>源文件名</label>
              <input v-model="parseState.sourceName" placeholder="例如：数据流图1.xml" />
            </div>
          </div>

          <div class="field">
            <label>上传 XML 文件</label>
            <input class="file-input" type="file" accept=".xml" @change="handleXmlFileChange" />
            <p class="helper-text">
              支持直接导入 PowerDesigner DFD XML；当前文件：
              {{ parseState.fileName || '未选择' }}
            </p>
          </div>

          <div class="field">
            <label>XML 预览 / 粘贴区</label>
            <textarea
              v-model="parseState.xmlContent"
              rows="14"
              placeholder="可直接粘贴 PowerDesigner 导出的 BPM_MODEL_XML。"
            />
          </div>

          <div class="actions">
            <button class="primary-button" :disabled="parseState.loading" @click="executeParse">
              {{ parseState.loading ? '解析中...' : '执行 DFD 解析' }}
            </button>
            <button class="secondary-button" :disabled="!parseResult" @click="jumpToStep(1)">
              查看候选结果
            </button>
          </div>

          <p v-if="parseState.error" class="error">{{ parseState.error }}</p>
          <p v-if="parseState.success" class="success">{{ parseState.success }}</p>
        </article>

        <article class="surface-card">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Expect</p>
              <h4>本步输出</h4>
            </div>
          </div>
          <ul class="bullet-list">
            <li>识别 Process、External Entity、Data Store、Flow、ResourceFlow。</li>
            <li>生成 ILF / EI / EO / EQ 候选与证据链。</li>
            <li>为 DET / RET / FTR 提供二阶段人工确认入口。</li>
          </ul>
        </article>
      </section>
    </template>

    <template v-else-if="activeStep === 1">
      <section class="workspace-grid">
        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 2</p>
              <h4>功能组件候选表</h4>
            </div>
            <button class="secondary-button" @click="jumpToStep(2)">继续复杂度判定</button>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>组件类型</th>
                  <th>候选名称</th>
                  <th>DET</th>
                  <th>RET</th>
                  <th>FTR</th>
                  <th>证据</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in componentDrafts" :key="item.candidateId" :class="{ 'row-dirty': item.dirty }">
                  <td>
                    <select :value="item.componentType" @change="updateComponentType(item, $event.target.value)">
                      <option v-for="option in componentTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </td>
                  <td>{{ item.name }}</td>
                  <td><input v-model.number="item.det" min="0" type="number" @input="markDirty(item)" /></td>
                  <td>
                    <input
                      v-model.number="item.ret"
                      :disabled="!needsRet(item.componentType)"
                      min="0"
                      type="number"
                      @input="markDirty(item)"
                    />
                  </td>
                  <td>
                    <input
                      v-model.number="item.ftr"
                      :disabled="needsRet(item.componentType)"
                      min="0"
                      type="number"
                      @input="markDirty(item)"
                    />
                  </td>
                  <td>{{ item.evidenceCodes.join(' / ') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="surface-card">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Evidence</p>
              <h4>流向证据</h4>
            </div>
          </div>
          <ul class="bullet-list compact-list">
            <li v-for="item in parseResult?.flows ?? []" :key="item.flowId">
              <strong>{{ item.flowId }} - {{ item.sourceName }} → {{ item.targetName }}</strong>
              <span>数据项：{{ item.dataNames.join('、') || '无' }}</span>
            </li>
          </ul>
        </article>
      </section>
    </template>

    <template v-else-if="activeStep === 2">
      <section class="workspace-grid">
        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 3</p>
              <h4>复杂度矩阵输入</h4>
            </div>
            <button class="secondary-button" @click="jumpToStep(3)">继续 GSC 评分</button>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>组件</th>
                  <th>类型</th>
                  <th>DET</th>
                  <th>RET</th>
                  <th>FTR</th>
                  <th>复杂度</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in componentDrafts" :key="item.candidateId" :class="{ 'row-dirty': item.dirty }">
                  <td>{{ item.name }}</td>
                  <td>{{ item.componentType }}</td>
                  <td>{{ item.det }}</td>
                  <td>{{ needsRet(item.componentType) ? item.ret : '-' }}</td>
                  <td>{{ needsRet(item.componentType) ? '-' : item.ftr }}</td>
                  <td>
                    <select :value="item.complexityLevel" @change="updateComponentComplexity(item, $event.target.value)">
                      <option v-for="option in complexityOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </template>

    <template v-else-if="activeStep === 3">
      <section class="workspace-grid">
        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 4</p>
              <h4>GSC 评分表</h4>
            </div>
            <p class="score-note">GSC 总分 = {{ gscTotal }}</p>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>特征</th>
                  <th>分值</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in gscDrafts" :key="item.code" :class="{ 'row-dirty': item.dirty }">
                  <td>{{ item.code }}</td>
                  <td>{{ item.label }}</td>
                  <td><input :value="item.score" min="0" max="5" type="number" @input="updateGscScore(item, $event.target.value)" /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="surface-card">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 4</p>
              <h4>语言换算表</h4>
            </div>
          </div>

          <div class="field">
            <label>语言代码</label>
            <input v-model="languageDraft.code" @input="markDirty(languageDraft)" />
          </div>
          <div class="field">
            <label>SLOC / FP</label>
            <input v-model.number="languageDraft.slocPerFp" min="1" type="number" @input="markDirty(languageDraft)" />
          </div>

          <div class="actions">
            <button class="primary-button" :disabled="calculationState.loading" @click="executeCalculate">
              {{ calculationState.loading ? '计算中...' : '执行 FP 计算' }}
            </button>
            <button class="secondary-button" @click="jumpToStep(4)">查看结果页</button>
          </div>

          <p v-if="calculationState.error" class="error">{{ calculationState.error }}</p>
          <p v-if="calculationState.success" class="success">{{ calculationState.success }}</p>
        </article>
      </section>
    </template>

    <template v-else>
      <section class="workspace-grid">
        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 5</p>
              <h4>FP 核心结果</h4>
            </div>
            <button class="secondary-button" :disabled="calculationState.loading" @click="executeCalculate">
              重新计算
            </button>
          </div>

          <template v-if="calculationResult">
            <section class="result-strip">
              <article class="mini-result accent-blue"><p>UFP</p><strong>{{ calculationResult.ufp }}</strong></article>
              <article class="mini-result accent-gold"><p>VAF</p><strong>{{ calculationResult.vaf }}</strong></article>
              <article class="mini-result accent-green"><p>FP</p><strong>{{ calculationResult.fp }}</strong></article>
              <article class="mini-result accent-rose"><p>LOC</p><strong>{{ calculationResult.locEstimate }}</strong></article>
            </section>

            <div class="trace-grid">
              <article class="surface-subcard">
                <h5>UFP / VAF / LOC Trace</h5>
                <ol class="trace-list">
                  <li v-for="item in [...calculationResult.ufpTrace, ...calculationResult.vafTrace, ...calculationResult.locTrace]" :key="`${item.key}-${item.resultField}`">
                    <strong>{{ item.key }}</strong>
                    <span>{{ item.expression }}</span>
                    <code>{{ item.resultField }} = {{ item.result }}</code>
                  </li>
                </ol>
              </article>
              <article class="surface-subcard">
                <h5>组件明细</h5>
                <ul class="bullet-list compact-list">
                  <li v-for="item in calculationResult.componentBreakdown" :key="item.candidateId">
                    <strong>{{ item.componentType }} / {{ item.name }}</strong>
                    <span>{{ item.complexityLevel }}，权重 {{ item.weight }}</span>
                  </li>
                </ul>
              </article>
            </div>

            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>GSC</th>
                    <th>名称</th>
                    <th>分值</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in calculationResult.gscBreakdown" :key="item.code">
                    <td>{{ item.code }}</td>
                    <td>{{ item.label }}</td>
                    <td>{{ item.score }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <p v-else class="empty-state">
            还没有计算结果。请完成复杂度矩阵和 GSC 填写后执行 FP 计算。
          </p>
        </article>
      </section>
    </template>
  </section>
</template>

<style scoped>
.workbench-panel {
  margin-top: 22px;
}

.workbench-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.status-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.24);
  color: #475569;
  font-size: 12px;
}

.status-pill-amber {
  background: #fff7ed;
  color: #b45309;
  border-color: rgba(245, 158, 11, 0.24);
}

.stepper-shell {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.stepper-shell-five {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.step-chip {
  display: flex;
  gap: 12px;
  align-items: center;
  text-align: left;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fbff;
}

.step-chip strong,
.step-chip small {
  display: block;
}

.step-chip small {
  color: #64748b;
  margin-top: 4px;
}

.step-chip-active {
  border-color: rgba(245, 158, 11, 0.28);
  background: #fff8eb;
}

.step-chip-ready {
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, 0.18);
}

.step-index {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  color: #b45309;
  font-weight: 700;
}

.workbench-stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 18px;
  margin-top: 18px;
}

.surface-card,
.surface-subcard {
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.94);
}

.surface-card {
  padding: 22px;
}

.surface-card-large {
  grid-column: 1 / -1;
}

.surface-subcard {
  padding: 18px;
}

.compact-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.compact-head h4,
.surface-subcard h5 {
  margin: 0;
}

.score-note {
  margin: 0;
  color: #475569;
  font-weight: 600;
}

.field {
  margin-bottom: 14px;
}

.field label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
}

.field input,
.field textarea,
.field select {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.field textarea {
  font-family: 'SFMono-Regular', ui-monospace, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
}

.helper-text {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 12px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.success {
  color: #15803d;
  margin: 12px 0 0;
}

.error {
  color: #b91c1c;
  margin: 12px 0 0;
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
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  text-align: left;
  vertical-align: top;
}

th {
  background: #f8fafc;
}

.row-dirty {
  background: rgba(245, 158, 11, 0.08);
}

.bullet-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
}

.compact-list li + li {
  margin-top: 10px;
}

.compact-list strong,
.compact-list span {
  display: block;
}

.empty-state {
  margin: 0;
  color: #64748b;
}

.result-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.mini-result {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #f8fbff;
  padding: 16px;
}

.mini-result p,
.mini-result strong {
  margin: 0;
}

.mini-result p {
  color: #64748b;
  margin-bottom: 6px;
}

.mini-result strong {
  font-size: 1.2rem;
}

.trace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.trace-list {
  margin: 0;
  padding-left: 18px;
  color: #475569;
}

.trace-list li + li {
  margin-top: 12px;
}

.trace-list strong,
.trace-list span,
.trace-list code {
  display: block;
}

.trace-list code {
  margin-top: 4px;
  color: #b45309;
}

@media (max-width: 1180px) {
  .stepper-shell-five,
  .workbench-stats,
  .result-strip,
  .trace-grid,
  .workspace-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .workbench-head,
  .compact-head,
  .stepper-shell-five,
  .workbench-stats,
  .result-strip,
  .trace-grid,
  .workspace-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
