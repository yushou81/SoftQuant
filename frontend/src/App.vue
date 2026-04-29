<script setup>
import { computed, ref } from 'vue'
import CkWorkbench from './components/ck/CkWorkbench.vue'
import CfgWorkbench from './components/cfg/CfgWorkbench.vue'
import FpWorkbench from './components/fp/FpWorkbench.vue'
import LocWorkbench from './components/loc/LocWorkbench.vue'
import UcpWorkbench from './components/ucp/UcpWorkbench.vue'

const activeNav = ref('ucp')

const modules = [
  {
    key: 'ucp',
    title: '用例点度量',
    subtitle: 'Use Case Points',
    description: '输入参与者、用例复杂度、TCF 和 EF，输出 UUCP/UCP 与工作量估算。',
    inputs: ['参与者数量和复杂度', '用例数量和复杂度', 'TCF 因子', 'EF 因子'],
    outputs: ['UUCP', 'UCP', '工作量(人时/人月)'],
  },
  {
    key: 'loc',
    title: '代码行度量',
    subtitle: 'LOC Metrics',
    description: '面向 Java/Python/C++ 统计物理行、逻辑行、注释率与空白行。',
    inputs: ['项目目录或单文件', '语言类型', '统计粒度'],
    outputs: ['总行数', '逻辑代码行', '注释率'],
  },
  {
    key: 'oo',
    title: '面向对象度量',
    subtitle: 'OO Metrics (CK/LK)',
    description: '基于 AST 解析类结构、继承关系与调用关系，输出 CK/LK 指标。',
    inputs: ['Java 源码', '类图/UML 文件', '项目参数(可选)'],
    outputs: ['WMC/DIT/NOC/CBO/RFC/LCOM', '风险雷达图', '类级明细'],
  },
  {
    key: 'fp',
    title: '功能点度量',
    subtitle: 'Function Points',
    description: '输入五大信息域与 14 个 GSC，输出 UFP/VAF/FP 与 LOC 估算。',
    inputs: ['ILF/EIF/EI/EO/EQ', '复杂度级别', '14 项 GSC 打分'],
    outputs: ['UFP', 'VAF', 'FP 与 LOC 估算'],
  },
  {
    key: 'cfg',
    title: '控制流与复杂度',
    subtitle: 'CFG & Cyclomatic Complexity',
    description: '构建控制流图并计算 McCabe 复杂度 V(G)=E-N+2。',
    inputs: ['代码片段或函数', '流程图描述', '节点和边(可选手工)'],
    outputs: ['控制流图', '圈复杂度', '高风险路径提示'],
  },
]

const activeModule = computed(() => modules.find((item) => item.key === activeNav.value) ?? modules[0])
const implementedModules = new Set(['ucp', 'loc', 'oo', 'fp', 'cfg'])

const activeModuleStatus = computed(() =>
  implementedModules.has(activeNav.value) ? '工作台已接入' : '模块 UI 已规划',
)

function switchNav(target) {
  activeNav.value = target
}
</script>

<template>
  <div class="dashboard-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <p class="brand-kicker">SoftQuant Lab</p>
        <h1>软件度量操作台</h1>
      </div>

      <nav class="sidebar-nav">
        <button
          class="nav-item"
          :class="{ 'nav-item-active': activeNav === 'ucp' }"
          @click="switchNav('ucp')"
        >
          用例点度量
        </button>
        <button
          class="nav-item"
          :class="{ 'nav-item-active': activeNav === 'loc' }"
          @click="switchNav('loc')"
        >
          代码行度量
        </button>
        <button
          class="nav-item"
          :class="{ 'nav-item-active': activeNav === 'oo' }"
          @click="switchNav('oo')"
        >
          面向对象度量
        </button>
        <button
          class="nav-item"
          :class="{ 'nav-item-active': activeNav === 'fp' }"
          @click="switchNav('fp')"
        >
          功能点度量
        </button>
        <button
          class="nav-item"
          :class="{ 'nav-item-active': activeNav === 'cfg' }"
          @click="switchNav('cfg')"
        >
          控制流与复杂度
        </button>
      </nav>
    </aside>

    <main class="workspace">
      <header class="hero-panel">
        <div>
          <p class="eyebrow">Automated Metrics Tool</p>
          <h2>{{ activeModule.title }}</h2>
          <p class="lead">
            {{ activeModule.description }}
          </p>
        </div>
      </header>

      <!-- 已落地模块直接挂接真实工作台，未落地模块仍保留占位信息卡。 -->
      <UcpWorkbench v-if="activeNav === 'ucp'" />
      <LocWorkbench v-else-if="activeNav === 'loc'" />
      <FpWorkbench v-else-if="activeNav === 'fp'" />
      <CkWorkbench v-else-if="activeNav === 'oo'" />
      <CfgWorkbench v-else-if="activeNav === 'cfg'" />

      <template v-else>
        <section class="stats-grid">
          <article class="stat-card accent-blue">
            <p class="stat-label">主要输入</p>
            <strong>{{ activeModule.inputs[0] }}</strong>
            <span>{{ activeModule.inputs[1] }}</span>
          </article>
          <article class="stat-card accent-gold">
            <p class="stat-label">扩展输入</p>
            <strong>{{ activeModule.inputs[2] }}</strong>
            <span>{{ activeModule.inputs[3] ?? '更多参数可配置' }}</span>
          </article>
          <article class="stat-card accent-green">
            <p class="stat-label">输出结果</p>
            <strong>{{ activeModule.outputs[0] }}</strong>
            <span>{{ activeModule.outputs[1] }}</span>
          </article>
          <article class="stat-card accent-rose">
            <p class="stat-label">工程状态</p>
            <strong>{{ activeModuleStatus }}</strong>
            <span>{{ activeModule.outputs[2] }}</span>
          </article>
        </section>

        <section class="content-grid">
          <article class="feature-panel">
            <div class="panel-head">
              <p class="eyebrow">Input Design</p>
              <h3>建议输入表单</h3>
            </div>
            <ul class="source-list">
              <li v-for="item in activeModule.inputs" :key="item">
                <strong>{{ item }}</strong>
                <span>建议提供文本录入 + 文件导入两种方式，便于演示与批量处理。</span>
              </li>
            </ul>
          </article>
          <article class="feature-panel">
            <div class="panel-head">
              <p class="eyebrow">Output Design</p>
              <h3>建议输出展示</h3>
            </div>
            <ul class="source-list">
              <li v-for="item in activeModule.outputs" :key="item">
                <strong>{{ item }}</strong>
                <span>建议同时提供指标卡片、图表可视化和结果明细表，提升可解释性。</span>
              </li>
            </ul>
          </article>
        </section>
      </template>
    </main>
  </div>
</template>
