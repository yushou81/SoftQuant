<template>
  <div class="dashboard-shell">
    <aside class="sidebar">
      <div class="brand-block">
        <p class="brand-kicker">SoftQuant Lab</p>
        <h1>软件度量操作台</h1>
        <p class="brand-copy">
          面向课程实验的自动化质量分析平台，覆盖设计、编码与评估阶段的多类软件实体。
        </p>
      </div>

      <nav class="sidebar-nav">
        <a class="nav-item nav-item-active" href="#">总览仪表盘</a>
        <a class="nav-item" href="#">代码度量</a>
        <a class="nav-item" href="#">设计图分析</a>
        <a class="nav-item" href="#">工作量评估</a>
        <a class="nav-item" href="#">报告中心</a>
        <a class="nav-item" href="#">系统设置</a>
      </nav>

      <section class="sidebar-panel">
        <p class="panel-label">实验目标</p>
        <p class="panel-text">
          优先支持 CK、LK、LoC、圈复杂度，并逐步扩展到用例、功能点和成本估算。
        </p>
      </section>
    </aside>

    <main class="workspace">
      <header class="hero-panel">
        <div>
          <p class="eyebrow">Course Experiment Dashboard</p>
          <h2>把度量任务拆成清晰的输入、分析与报告流程</h2>
          <p class="lead">
            先搭好前端操作台，后续我们可以把 Java 源码分析、类图解析、用例度量、工时预测等能力逐个接入。
          </p>
        </div>

        <div class="hero-actions">
          <button class="primary-button">新建分析任务</button>
          <button class="secondary-button">导入实验样例</button>
        </div>
      </header>

      <section class="stats-grid">
        <article class="stat-card accent-blue">
          <p class="stat-label">待接入核心模型</p>
          <strong>CK / LK / LoC / CC</strong>
          <span>类耦合、继承深度、代码规模、圈复杂度</span>
        </article>
        <article class="stat-card accent-gold">
          <p class="stat-label">支持输入源</p>
          <strong>4 类</strong>
          <span>代码、类图、流程图、用例图</span>
        </article>
        <article class="stat-card accent-green">
          <p class="stat-label">目标软件实体</p>
          <strong>规模 / 成本 / 工时</strong>
          <span>支持质量评估与项目预测</span>
        </article>
        <article class="stat-card accent-rose">
          <p class="stat-label">当前阶段</p>
          <strong>前端框架初始化</strong>
          <span>准备逐步接入后端分析能力</span>
        </article>
      </section>

      <section class="content-grid">
        <article class="feature-panel">
          <div class="panel-head">
            <p class="eyebrow">Metric Modules</p>
            <h3>度量能力入口</h3>
          </div>
          <div class="module-grid">
            <div class="module-card">
              <span class="module-badge">OO</span>
              <h4>面向对象度量</h4>
              <p>CK 模型、LK 模型、类关系与结构质量分析。</p>
            </div>
            <div class="module-card">
              <span class="module-badge">CODE</span>
              <h4>传统代码指标</h4>
              <p>LoC、圈复杂度、方法数量、注释率等基础统计。</p>
            </div>
            <div class="module-card">
              <span class="module-badge">DESIGN</span>
              <h4>设计阶段输入</h4>
              <p>面向类图、流程图、用例图的结构化录入与解析。</p>
            </div>
            <div class="module-card">
              <span class="module-badge">PLAN</span>
              <h4>成本与工时估算</h4>
              <p>结合规模、人员与阶段输入生成实验报告依据。</p>
            </div>
          </div>
        </article>

        <article class="feature-panel">
          <div class="panel-head">
            <p class="eyebrow">Input Sources</p>
            <h3>输入数据面板</h3>
          </div>
          <ul class="source-list">
            <li>
              <strong>程序代码</strong>
              <span>上传 Java 项目或粘贴代码片段，后续接 AST 分析。</span>
            </li>
            <li>
              <strong>类图 / UML</strong>
              <span>支持手动录入或文件导入，作为结构度量依据。</span>
            </li>
            <li>
              <strong>流程图 / 用例图</strong>
              <span>用于功能规模、复杂性与场景覆盖分析。</span>
            </li>
            <li>
              <strong>人员与工期参数</strong>
              <span>为工作量、成本、开发时间估算提供输入。</span>
            </li>
          </ul>
        </article>

        <article class="feature-panel">
          <div class="panel-head">
            <p class="eyebrow">Workflow</p>
            <h3>实验执行流程</h3>
          </div>
          <ol class="flow-list">
            <li>创建一次分析任务，选择度量目标和输入类型。</li>
            <li>上传代码或设计图，补充项目规模与人员信息。</li>
            <li>调用后端分析器，生成面向课程要求的度量结果。</li>
            <li>在报告中心查看图表、评分和结论摘要。</li>
          </ol>
        </article>

        <article class="feature-panel">
          <div class="panel-head">
            <p class="eyebrow">Task Queue</p>
            <h3>任务与报告</h3>
          </div>
          <div class="report-card">
            <p class="report-title">Java 项目静态分析实验</p>
            <p class="report-meta">状态：待实现后端接入</p>
            <div class="progress-track">
              <div class="progress-bar"></div>
            </div>
            <div class="report-tags">
              <span>CK</span>
              <span>LK</span>
              <span>LoC</span>
              <span>Complexity</span>
            </div>
          </div>

          <div class="note-card">
            <p class="panel-label">下一步建议</p>
            <p class="panel-text">
              优先做“上传 Java 项目 + 返回基础指标卡片”这一条链路，最容易形成可演示成果。
            </p>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>
