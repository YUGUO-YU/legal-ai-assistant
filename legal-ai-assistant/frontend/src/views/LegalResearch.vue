<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI法律研究</h2>
      <p>对法律问题进行多维度研究，输出结构化分析报告</p>
    </div>

    <div class="research-form">
      <el-input
        v-model="query"
        type="textarea"
        :rows="4"
        placeholder="请输入需要研究的具体法律问题，如：建设工程合同纠纷中的工期延误索赔问题研究"
        size="large"
      />
      <div class="form-row">
        <div class="form-item">
          <label>研究深度：</label>
          <el-radio-group v-model="depth">
            <el-radio label="brief">简略</el-radio>
            <el-radio label="normal">标准</el-radio>
            <el-radio label="detailed">详细</el-radio>
          </el-radio-group>
        </div>
        <div class="form-item">
          <label>数据来源：</label>
          <el-checkbox-group v-model="sources">
            <el-checkbox label="laws">法律法规</el-checkbox>
            <el-checkbox label="cases">司法案例</el-checkbox>
            <el-checkbox label="papers">学术论文</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
      <el-button type="primary" size="large" @click="handleResearch" :loading="loading">
        <el-icon><MagicStick /></el-icon>
        开始研究
      </el-button>
    </div>

    <div v-if="loading" class="progress-panel">
      <div class="progress-header">
        <span>正在生成研究报告...</span>
        <span class="progress-percent">{{ progress }}%</span>
      </div>
      <el-progress :percentage="progress" :status="progressStatus" :stroke-width="10" />
      <div class="progress-phases">
        <div
          v-for="phase in phases"
          :key="phase.name"
          :class="['phase-item', { active: phase.active, completed: phase.completed }]"
        >
          <el-icon v-if="phase.completed"><SuccessFilled /></el-icon>
          <el-icon v-else-if="phase.active" class="is-loading"><Loading /></el-icon>
          <el-icon v-else><Document /></el-icon>
          <span>{{ phase.label }}</span>
        </div>
      </div>
      <p class="progress-message">{{ progressMessage }}</p>
    </div>

    <div v-if="report" class="report-content">
      <div class="report-toolbar">
        <el-button-group>
          <el-button @click="exportPdf">
            <el-icon><Download /></el-icon>导出PDF
          </el-button>
          <el-button @click="copyReport">
            <el-icon><CopyDocument /></el-icon> 复制全文
          </el-button>
          <el-button @click="printReport">
            <el-icon><Printer /></el-icon> 打印
          </el-button>
        </el-button-group>
      </div>

      <div class="report-container">
        <div
          v-for="section in report"
          :key="section.id"
          :id="'section-' + section.id"
          class="report-section"
        >
          <div class="section-header">
            <h2>{{ section.title }}</h2>
            <el-button text size="small" @click="copySection(section)">
              <el-icon><CopyDocument /></el-icon>
           </el-button>
          </div>
          <div class="section-content" v-html="section.content"></div>
          <div class="section-citations" v-if="section.citations?.length">
            <h4>参考来源</h4>
            <div v-for="c in section.citations" :key="c.id" class="citation-item">
              <a :href="c.url" target="_blank">{{ c.title }}</a>
              <span class="citation-source">{{ c.source }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const query = ref('')
const loading = ref(false)
const progress = ref(0)
const progressStatus = ref()
const progressMessage = ref('')
const report = ref(null)
const depth = ref('normal')
const sources = ref(['laws', 'cases'])

const phases = reactive([
  { name: 'parse', label: '解析研究问题', active: false, completed: false },
  { name: 'search_laws', label: '检索法律法规', active: false, completed: false },
  { name: 'search_cases', label: '检索司法案例', active: false, completed: false },
  { name: 'generate_def', label: '生成问题界定', active: false, completed: false },
  { name: 'generate_basis', label: '生成法律依据', active: false, completed: false },
  { name: 'generate_risk', label: '生成风险提示', active: false, completed: false },
  { name: 'generate_conclusion', label: '生成结论建议', active: false, completed: false }
])

const handleResearch = async () => {
  if (!query.value.trim()) {
    ElMessage.warning('请输入研究问题')
    return
  }

  loading.value = true
  progress.value = 0
  report.value = null

  phases.forEach(p => {
    p.active = false
    p.completed = false
  })

  const phaseConfigs = [
    { progress: 10, phase: 'parse', message: '正在解析研究问题...', duration: 800 },
    { progress: 25, phase: 'search_laws', message: '检索法律法规...找到12条相关法规', duration: 1200 },
    { progress: 40, phase: 'search_cases', message: '检索司法案例...找到8个相关判例', duration: 1000 },
    { progress: 55, phase: 'generate_def', message: '正在生成问题界定章节...', duration: 1500 },
    { progress: 70, phase: 'generate_basis', message: '正在生成法律依据章节...', duration: 1500 },
    { progress: 85, phase: 'generate_risk', message: '正在生成风险提示章节...', duration: 1200 },
    { progress: 95, phase: 'generate_conclusion', message: '正在生成结论建议...', duration: 1000 },
    { progress: 100, phase: 'done', message: '研究完成！', duration: 500 }
  ]

  for (const config of phaseConfigs) {
    await new Promise(r => setTimeout(r, config.duration))
    progress.value = config.progress
    progressMessage.value = config.message

    const phase = phases.find(p => p.name === config.phase)
    if (phase) {
      phase.completed = config.progress === 100
      phase.active = config.progress < 100
    }

    if (config.progress === 100) {
      progressStatus.value = 'success'
    }
  }

  report.value = [
    {
      id: 1,
      title: '一、问题界定',
      content: `<p><strong>（一）研究背景</strong></p>
        <p>随着基础设施建设规模的持续扩大，建设工程合同纠纷已成为民商事诉讼中占比最高的案件类型之一。其中，工期延误索赔问题因其涉及因素复杂、证据认定困难等特点，成为司法实践中的难点问题。</p>
        <p><strong>（二）核心问题</strong></p>
        <p>本研究聚焦以下核心问题：承包人主张工期顺延应满足何种条件？工期延误损失赔偿的范围如何确定？发包人反索赔的诉讼时效从何时起算？</p>
        <p><strong>（三）关键术语</strong></p>
        <ul>
          <li><strong>工期延误</strong>：指建设工程合同的实际完工日期超出合同约定的完工日期。</li>
          <li><strong>工期索赔</strong>：承包人基于合同约定或法律规定，向发包人主张延长工期或赔偿损失的权利。</li>
          <li><strong>不可抗力</strong>：不能预见、不能避免并不能克服的客观情况，如自然灾害、社会事件等。</li>
        </ul>`,
      citations: []
    },
    {
      id: 2,
      title: '二、法律依据',
      content: `<p><strong>（一）核心法规</strong></p>
        <p>《民法典》合同编相关规定：</p>
        <ul>
          <li><strong>第577条</strong>：当事人一方不履行合同义务或者履行合同义务不符合约定的，应当承担违约责任。</li>
          <li><strong>第584条</strong>：当事人一方不履行合同义务或者履行合同义务不符合约定的，给对方造成损失的，损失赔偿额应当相当于因违约所造成的损失。</li>
          <li><strong>第590条</strong>：当事人一方因不可抗力不能履行合同的，根据不可抗力的影响，部分或者全部免除责任。</li>
        </ul>
        <p><strong>（二）司法解释</strong></p>
        <ul>
          <li>《最高人民法院关于审理建设工程施工合同纠纷案件适用法律问题的解释（一）》第10条、第11条对工期延误责任认定作出具体规定。</li>
        </ul>`,
      citations: [
        { id: 1, title: '《民法典》', url: 'https://flk.npc.gov.cn/', source: '国家法律法规信息库' }
      ]
    },
    {
      id: 3,
      title: '三、风险提示',
      content: `<p><strong>（一）法律风险识别</strong></p>
        <ul>
          <li><strong>证据风险</strong>：工期延误索赔需提供完整的工期顺延签证、往来函件、监理日志等证据，证据不足将导致索赔失败。</li>
          <li><strong>时效风险</strong>：建设工程款请求权诉讼时效为3年，需在法定期限内主张权利。</li>
          <li><strong>鉴定风险</strong>：工期延误的因果关系认定往往需要专业鉴定，鉴定周期长、费用高。</li>
        </ul>
        <p><strong>（二）风险防控建议</strong></p>
        <ul>
          <li>建议在合同履行过程中建立完善的文档管理制度，确保工期签证及时签认。</li>
          <li>发生工期延误事件后，应在合同约定的期限内向发包人提出书面索赔报告。</li>
          <li>及时委托专业律师介入，固定证据，制定诉讼策略。</li>
        </ul>`,
      citations: []
    },
    {
      id: 4,
      title: '四、结论与建议',
      content: `<p><strong>（一）核心结论</strong></p>
        <p>建设工程工期延误索赔纠纷的处理应遵循"有约定从约定，无约定依法定"的原则。承包人主张工期顺延，必须提供充分证据证明延误原因符合合同约定的顺延条件或存在不可抗力等免责事由。</p>
        <p><strong>（二）行动建议</strong></p>
        <p><strong>对承包人的建议：</strong></p>
        <ul>
          <li>在合同签订阶段，务必明确工期延误的免责条款和顺延条件。</li>
          <li>施工过程中，指派专人负责工期管理，及时办理签证手续。</li>
          <li>发生延误后，按合同约定的时间和程序提交索赔报告。</li>
        </ul>
        <p><strong>对发包人的建议：</strong></p>
        <ul>
          <li>加强项目管理，确保设计变更、材料供应等及时到位。</li>
          <li>建立工期考核机制，定期评估施工进度。</li>
          <li>如遇承包人索赔，及时固定反证材料，准备应诉策略。</li>
        </ul>`,
      citations: []
    }
  ]

  loading.value = false
  ElMessage.success('研究报告生成完成')
}

const exportPdf = () => {
  ElMessage.info('PDF导出功能开发中...')
}

const copyReport = () => {
  if (report.value) {
    const text = report.value.map(s => `${s.title}\n${s.content.replace(/<[^>]+>/g, '')}`).join('\n\n')
    navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  }
}

const printReport = () => {
  window.print()
}

const copySection = (section) => {
  const text = `${section.title}\n${section.content.replace(/<[^>]+>/g, '')}`
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}
</script>

<style lang="scss" scoped>
.research-form {
  .form-row {
    display: flex;
    gap: 24px;
    margin: 16px 0;
    .form-item {
      display: flex;
      align-items: center;
      gap: 8px;
      label {
        color: #666;
        font-size: 14px;
      }
    }
  }
}

.progress-panel {
  margin: 32px 0;
  padding: 24px;
  background: #fafafa;
  border-radius: 8px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    .progress-percent {
      font-weight: bold;
      color: #1890ff;
    }
  }

  .progress-phases {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 24px;
    .phase-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: #fff;
      border-radius: 16px;
      font-size: 13px;
      color: #999;
      &.active {
        color: #1890ff;
        background: #e6f7ff;
      }
      &.completed {
        color: #52c41a;
        background: #f6ffed;
      }
    }
  }

  .progress-message {
    text-align: center;
    color: #666;
    margin-top: 16px;
  }
}

.report-content {
  margin-top: 32px;
}

.report-toolbar {
  margin-bottom: 24px;
}

.report-container {
  background: #fff;
  padding: 48px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.report-section {
  margin-bottom: 48px;
  padding-bottom: 32px;
  border-bottom: 1px solid #f0f0f0;
  &:last-child {
    border-bottom: none;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    h2 {
      margin: 0;
      font-size: 20px;
      border-left: 4px solid #1890ff;
      padding-left: 12px;
    }
  }

  .section-content {
    line-height: 1.8;
    p { margin: 0 0 16px 0; }
    ul {
      padding-left: 24px;
      margin: 0 0 16px 0;
      li { margin-bottom: 8px; }
    }
    strong { color: #333; }
  }

  .section-citations {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px dashed #f0f0f0;
    h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #666;
    }
    .citation-item {
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;
      &:last-child { border-bottom: none; }
      a {
        color: #1890ff;
        margin-right: 12px;
      }
      .citation-source {
        color: #999;
        font-size: 12px;
      }
    }
  }
}

@media print {
  .report-toolbar { display: none; }
  .report-container { padding: 0; border: none; }
}
</style>