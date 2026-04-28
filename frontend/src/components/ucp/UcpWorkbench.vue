<script setup>
import { computed, reactive, ref } from 'vue'

const apiBaseUrl = 'http://localhost:8080'

const steps = [
  { key: 'upload', title: '上传与解析', subtitle: '导入用例图 XML 并生成预览' },
  { key: 'classify', title: '校正 Actor / UseCase', subtitle: '确认复杂度、权重与缺失项' },
  { key: 'factors', title: '填写 TCF / EF', subtitle: '维护技术因子与环境因子分值' },
  { key: 'result', title: '计算与结果', subtitle: '查看 UCP、工作量与公式轨迹' },
]

const actorOptions = [
  { value: 'SIMPLE', label: '简单', weight: 1 },
  { value: 'AVERAGE', label: '普通', weight: 2 },
  { value: 'COMPLEX', label: '复杂', weight: 3 },
]

const useCaseOptions = [
  { value: 'SIMPLE', label: '简单', weight: 5 },
  { value: 'AVERAGE', label: '普通', weight: 10 },
  { value: 'COMPLEX', label: '复杂', weight: 15 },
]

const technicalFactorBlueprint = [
  { code: 'T1', label: '分布式系统', weight: 2 },
  { code: 'T2', label: '响应或吞吐量性能', weight: 1 },
  { code: 'T3', label: '终端用户效率', weight: 1 },
  { code: 'T4', label: '复杂的内部处理', weight: 1 },
  { code: 'T5', label: '可重用性', weight: 1 },
  { code: 'T6', label: '易安装性', weight: 0.5 },
  { code: 'T7', label: '易用性', weight: 0.5 },
  { code: 'T8', label: '可移植性', weight: 2 },
  { code: 'T9', label: '易更改性', weight: 1 },
  { code: 'T10', label: '并发性', weight: 1 },
  { code: 'T11', label: '特殊的安全性', weight: 1 },
  { code: 'T12', label: '提供第三方接口', weight: 1 },
  { code: 'T13', label: '特别的用户培训', weight: 1 },
]

const environmentalFactorBlueprint = [
  { code: 'E1', label: '熟悉 UML 程度', weight: 1.5 },
  { code: 'E2', label: '开发应用程序经验', weight: 0.5 },
  { code: 'E3', label: '面向对象经验', weight: 1 },
  { code: 'E4', label: '主分析师能力', weight: 0.5 },
  { code: 'E5', label: '激励', weight: 1 },
  { code: 'E6', label: '需求稳定', weight: 2 },
  { code: 'E7', label: '兼职人员', weight: -1 },
  { code: 'E8', label: '不同的编程语言', weight: -1 },
]

const activeStep = ref(0)
const parseState = reactive({
  projectName: '在线教学系统',
  sourceName: '',
  xmlContent: '',
  fileName: '',
  loading: false,
  error: '',
  success: '',
})

const parseResult = ref(null)
const actorDrafts = ref([])
const useCaseDrafts = ref([])
const technicalFactors = ref(buildFactorDrafts(technicalFactorBlueprint))
const environmentalFactors = ref(buildFactorDrafts(environmentalFactorBlueprint))

const calculationState = reactive({
  loading: false,
  error: '',
  success: '',
})
const calculationResult = ref(null)

const canMoveToClassify = computed(() => Boolean(parseResult.value))
const canMoveToFactors = computed(() => actorDrafts.value.length > 0 && useCaseDrafts.value.length > 0)
const canMoveToResult = computed(() => canMoveToFactors.value)

const parseSummary = computed(() => {
  if (!parseResult.value) {
    return [
      { label: 'Actor', value: '0' },
      { label: 'UseCase', value: '0' },
      { label: '关系边', value: '0' },
      { label: '待补录项', value: '0' },
    ]
  }

  return [
    { label: 'Actor', value: String(parseResult.value.actors.length) },
    { label: 'UseCase', value: String(parseResult.value.useCases.length) },
    { label: '关系边', value: String(parseResult.value.relationships.length) },
    { label: '待补录项', value: String(parseResult.value.pendingFields.length) },
  ]
})

const technicalFactorScore = computed(() =>
  technicalFactors.value.reduce((total, item) => total + item.weightedScore, 0),
)
const environmentalFactorScore = computed(() =>
  environmentalFactors.value.reduce((total, item) => total + item.weightedScore, 0),
)

const groupedPendingFields = computed(() => {
  if (!parseResult.value) {
    return []
  }

  const grouped = new Map()
  for (const item of parseResult.value.pendingFields) {
    if (!grouped.has(item.entityId)) {
      grouped.set(item.entityId, { entityName: item.entityName, reasons: [] })
    }
    grouped.get(item.entityId).reasons.push(item.reason)
  }

  return [...grouped.entries()].map(([entityId, value]) => ({
    entityId,
    entityName: value.entityName,
    reasons: value.reasons,
  }))
})

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
    parseState.error = '请先上传 PowerDesigner 用例图 XML。'
    return
  }

  parseState.loading = true
  parseState.error = ''
  parseState.success = ''
  calculationState.error = ''
  calculationState.success = ''
  calculationResult.value = null

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/ucp/parse`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: parseState.projectName,
        sourceName: parseState.sourceName,
        sourceType: 'POWERDESIGNER_USE_CASE',
        xmlContent: parseState.xmlContent,
      }),
    })

    if (!response.ok) {
      throw new Error(`解析失败: ${response.status}`)
    }

    const data = await response.json()
    parseResult.value = data
    // 解析结果、用户覆盖值、最终计算结果三层分离，避免步骤切换时丢状态。
    actorDrafts.value = data.actors.map((actor) => ({
      ...actor,
      selectedComplexity: actor.suggestedComplexity,
      selectedWeight: actor.suggestedWeight,
      dirty: false,
    }))
    useCaseDrafts.value = data.useCases.map((item) => ({
      ...item,
      selectedComplexity: item.suggestedComplexity,
      selectedWeight: item.suggestedWeight,
      entityCount: item.entityCount ?? 0,
      stepCount: item.stepCount ?? 0,
      classCount: item.classCount ?? 0,
      dirty: false,
    }))
    technicalFactors.value = buildFactorDrafts(technicalFactorBlueprint)
    environmentalFactors.value = buildFactorDrafts(environmentalFactorBlueprint)
    activeStep.value = 1
    parseState.success = '解析完成，已生成 Actor / UseCase / 关系图预览。'
  } catch (error) {
    parseState.error = error instanceof Error ? error.message : '解析失败'
  } finally {
    parseState.loading = false
  }
}

function updateActorComplexity(actor, value) {
  actor.selectedComplexity = value
  actor.selectedWeight = actorOptions.find((item) => item.value === value)?.weight ?? actor.selectedWeight
  actor.dirty = true
}

function updateUseCaseComplexity(useCase, value) {
  useCase.selectedComplexity = value
  useCase.selectedWeight = useCaseOptions.find((item) => item.value === value)?.weight ?? useCase.selectedWeight
  useCase.dirty = true
}

function markDirty(target) {
  target.dirty = true
}

function updateFactorScore(target, rawValue) {
  const value = Number(rawValue)
  target.score = Number.isFinite(value) ? clamp(value, 0, 5) : 0
  target.weightedScore = Number((target.score * target.weight).toFixed(2))
  target.dirty = true
}

async function executeCalculate() {
  calculationState.loading = true
  calculationState.error = ''
  calculationState.success = ''

  try {
    const response = await fetch(`${apiBaseUrl}/api/metrics/ucp/calculate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectName: parseState.projectName,
        actors: actorDrafts.value.map((actor) => ({
          actorId: actor.actorId,
          actorName: actor.actorName,
          selectedComplexity: actor.selectedComplexity,
          selectedWeight: Number(actor.selectedWeight),
        })),
        useCases: useCaseDrafts.value.map((item) => ({
          useCaseId: item.useCaseId,
          useCaseName: item.useCaseName,
          selectedComplexity: item.selectedComplexity,
          selectedWeight: Number(item.selectedWeight),
          entityCount: Number(item.entityCount),
          stepCount: Number(item.stepCount),
          classCount: Number(item.classCount),
        })),
        technicalFactors: technicalFactors.value.map((item) => ({
          code: item.code,
          score: Number(item.score),
        })),
        environmentalFactors: environmentalFactors.value.map((item) => ({
          code: item.code,
          score: Number(item.score),
        })),
        productivity: 28,
        productivityUnit: 'person-hours-per-ucp',
      }),
    })

    if (!response.ok) {
      throw new Error(`计算失败: ${response.status}`)
    }

    calculationResult.value = await response.json()
    calculationState.success = 'UCP 计算完成，过程与结果已同步刷新。'
    activeStep.value = 3
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

  if (index === 1 && canMoveToClassify.value) {
    activeStep.value = index
  }
  if (index === 2 && canMoveToFactors.value) {
    activeStep.value = index
  }
  if (index === 3 && canMoveToResult.value) {
    activeStep.value = index
  }
}

function buildFactorDrafts(blueprint) {
  return blueprint.map((item) => ({
    ...item,
    score: 0,
    weightedScore: 0,
    dirty: false,
  }))
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}
</script>

<template>
  <section class="feature-panel workbench-panel">
    <div class="panel-head workbench-head">
      <div>
        <p class="eyebrow">UCP Workbench</p>
        <h3>用例点度量四步工作台</h3>
      </div>
      <div class="status-badges">
        <span class="status-pill status-pill-blue">解析结果 / 用户覆盖值 / 计算结果三层分离</span>
      </div>
    </div>

    <div class="stepper-shell">
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
      <article v-for="item in parseSummary" :key="item.label" class="stat-card accent-blue">
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
              <h4>上传 PowerDesigner 用例图</h4>
            </div>
          </div>

          <div class="form-grid">
            <div class="field">
              <label>项目名称</label>
              <input v-model="parseState.projectName" />
            </div>
            <div class="field">
              <label>源文件名</label>
              <input v-model="parseState.sourceName" placeholder="例如：在线教学系统.xml" />
            </div>
          </div>

          <div class="field">
            <label>上传 XML 文件</label>
            <input class="file-input" type="file" accept=".xml" @change="handleXmlFileChange" />
            <p class="helper-text">
              支持直接导入 PowerDesigner 用例图 XML；当前文件：
              {{ parseState.fileName || '未选择' }}
            </p>
          </div>

          <div class="field">
            <label>XML 预览 / 粘贴区</label>
            <textarea
              v-model="parseState.xmlContent"
              rows="14"
              placeholder="可直接粘贴 PowerDesigner 导出的 CLD_OBJECT_MODEL XML。"
            />
          </div>

          <div class="actions">
            <button class="primary-button" :disabled="parseState.loading" @click="executeParse">
              {{ parseState.loading ? '解析中...' : '执行解析预览' }}
            </button>
            <button class="secondary-button" :disabled="!parseResult" @click="jumpToStep(1)">
              查看解析结果
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
            <li>识别 Actor、UseCase、Association、include / extend。</li>
            <li>生成待补录字段清单，提示实体数 / 步骤数 / 类数。</li>
            <li>保留证据链，方便课堂展示“为什么这么判断”。</li>
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
              <h4>Actor 分类表</h4>
            </div>
            <button class="secondary-button" @click="jumpToStep(2)">继续填写因子</button>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Actor</th>
                  <th>Stereotype</th>
                  <th>复杂度</th>
                  <th>权重</th>
                  <th>证据</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="actor in actorDrafts" :key="actor.actorId" :class="{ 'row-dirty': actor.dirty }">
                  <td>{{ actor.actorName }}</td>
                  <td>{{ actor.stereotype || '未标注' }}</td>
                  <td>
                    <select :value="actor.selectedComplexity" @change="updateActorComplexity(actor, $event.target.value)">
                      <option v-for="option in actorOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </td>
                  <td>
                    <input v-model.number="actor.selectedWeight" min="1" max="3" type="number" @input="markDirty(actor)" />
                  </td>
                  <td>{{ actor.evidenceCodes.join(' / ') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 2</p>
              <h4>UseCase 复杂度表</h4>
            </div>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>UseCase</th>
                  <th>实体数</th>
                  <th>步骤数</th>
                  <th>类数</th>
                  <th>复杂度</th>
                  <th>权重</th>
                  <th>证据</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="useCase in useCaseDrafts" :key="useCase.useCaseId" :class="{ 'row-dirty': useCase.dirty }">
                  <td>{{ useCase.useCaseName }}</td>
                  <td><input v-model.number="useCase.entityCount" min="0" type="number" @input="markDirty(useCase)" /></td>
                  <td><input v-model.number="useCase.stepCount" min="0" type="number" @input="markDirty(useCase)" /></td>
                  <td><input v-model.number="useCase.classCount" min="0" type="number" @input="markDirty(useCase)" /></td>
                  <td>
                    <select :value="useCase.selectedComplexity" @change="updateUseCaseComplexity(useCase, $event.target.value)">
                      <option v-for="option in useCaseOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </td>
                  <td><input v-model.number="useCase.selectedWeight" min="5" max="15" step="5" type="number" @input="markDirty(useCase)" /></td>
                  <td>{{ useCase.evidenceCodes.join(' / ') }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="surface-card">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Pending</p>
              <h4>待补录提示</h4>
            </div>
          </div>
          <ul class="bullet-list compact-list">
            <li v-for="item in groupedPendingFields" :key="item.entityId">
              <strong>{{ item.entityName }}</strong>
              <span>{{ item.reasons.join('；') }}</span>
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
              <h4>TCF 因子表</h4>
            </div>
            <p class="score-note">TFactor = {{ technicalFactorScore.toFixed(2) }}</p>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>因子</th>
                  <th>权重</th>
                  <th>分值</th>
                  <th>加权分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in technicalFactors" :key="item.code" :class="{ 'row-dirty': item.dirty }">
                  <td>{{ item.code }}</td>
                  <td>{{ item.label }}</td>
                  <td>{{ item.weight }}</td>
                  <td><input :value="item.score" min="0" max="5" type="number" @input="updateFactorScore(item, $event.target.value)" /></td>
                  <td>{{ item.weightedScore.toFixed(2) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <article class="surface-card surface-card-large">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Step 3</p>
              <h4>EF 因子表</h4>
            </div>
            <p class="score-note">EFactor = {{ environmentalFactorScore.toFixed(2) }}</p>
          </div>

          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编号</th>
                  <th>因子</th>
                  <th>权重</th>
                  <th>分值</th>
                  <th>加权分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in environmentalFactors" :key="item.code" :class="{ 'row-dirty': item.dirty }">
                  <td>{{ item.code }}</td>
                  <td>{{ item.label }}</td>
                  <td>{{ item.weight }}</td>
                  <td><input :value="item.score" min="0" max="5" type="number" @input="updateFactorScore(item, $event.target.value)" /></td>
                  <td>{{ item.weightedScore.toFixed(2) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="actions">
            <button class="primary-button" :disabled="calculationState.loading" @click="executeCalculate">
              {{ calculationState.loading ? '计算中...' : '执行 UCP 计算' }}
            </button>
            <button class="secondary-button" @click="jumpToStep(3)">查看结果页</button>
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
              <p class="eyebrow">Step 4</p>
              <h4>UCP 核心结果</h4>
            </div>
            <button class="secondary-button" :disabled="calculationState.loading" @click="executeCalculate">
              重新计算
            </button>
          </div>

          <template v-if="calculationResult">
            <section class="result-strip">
              <article class="mini-result accent-blue"><p>UAW</p><strong>{{ calculationResult.uaw }}</strong></article>
              <article class="mini-result accent-gold"><p>UUC</p><strong>{{ calculationResult.uuc }}</strong></article>
              <article class="mini-result accent-green"><p>UUCP</p><strong>{{ calculationResult.uucp }}</strong></article>
              <article class="mini-result accent-blue"><p>TCF</p><strong>{{ calculationResult.tcf }}</strong></article>
              <article class="mini-result accent-gold"><p>EF</p><strong>{{ calculationResult.ef }}</strong></article>
              <article class="mini-result accent-green"><p>UCP</p><strong>{{ calculationResult.ucp }}</strong></article>
              <article class="mini-result accent-rose"><p>工作量</p><strong>{{ calculationResult.effort }}</strong></article>
            </section>

            <div class="panel-head compact-head">
              <div>
                <p class="eyebrow">Breakdown</p>
                <h4>公式轨迹与解释</h4>
              </div>
            </div>

            <div class="trace-grid">
              <article class="surface-subcard">
                <h5>Formula Trace</h5>
                <ol class="trace-list">
                  <li v-for="item in calculationResult.formulaTrace" :key="item.formulaKey">
                    <strong>{{ item.formulaKey }}</strong>
                    <span>{{ item.expression }}</span>
                    <code>{{ item.resultField }} = {{ item.result }}</code>
                  </li>
                </ol>
              </article>
              <article class="surface-subcard">
                <h5>解释文本</h5>
                <ul class="bullet-list compact-list">
                  <li v-for="item in calculationResult.explanations" :key="item.code">
                    <strong>{{ item.code }}</strong>
                    <span>{{ item.text }}</span>
                  </li>
                </ul>
              </article>
            </div>

            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>UseCase</th>
                    <th>复杂度</th>
                    <th>权重</th>
                    <th>小计</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in calculationResult.useCaseWeightBreakdown" :key="item.useCaseId">
                    <td>{{ item.useCaseName }}</td>
                    <td>{{ item.selectedComplexity }}</td>
                    <td>{{ item.selectedWeight }}</td>
                    <td>{{ item.subtotal }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <p v-else class="empty-state">
            还没有计算结果。请回到上一步完成因子填写后执行计算。
          </p>
        </article>

        <article class="surface-card">
          <div class="panel-head compact-head">
            <div>
              <p class="eyebrow">Evidence</p>
              <h4>证据链预览</h4>
            </div>
          </div>
          <ul class="bullet-list compact-list evidence-list">
            <li v-for="item in parseResult?.evidence ?? []" :key="item.code">
              <strong>{{ item.code }}</strong>
              <span>{{ item.summary }}</span>
            </li>
          </ul>
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

.status-pill-blue {
  background: #eff6ff;
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.22);
}

.stepper-shell {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
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
  border-color: rgba(37, 99, 235, 0.28);
  background: #eaf3ff;
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
  color: #1d4ed8;
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
  background: rgba(56, 189, 248, 0.08);
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
  color: #1d4ed8;
}

.evidence-list {
  max-height: 620px;
  overflow-y: auto;
}

@media (max-width: 1180px) {
  .stepper-shell,
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
  .stepper-shell,
  .workbench-stats,
  .result-strip,
  .trace-grid,
  .workspace-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .step-chip {
    align-items: flex-start;
  }
}
</style>
