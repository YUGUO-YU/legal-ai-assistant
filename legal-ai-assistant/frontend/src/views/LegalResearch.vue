<template>
  <div class="legal-research">
    <div class="page-header">
      <div class="header-content">
        <h2>AI法律研究</h2>
        <p>对法律问题进行多维度研究，输出结构化分析报告</p>
      </div>
    </div>

    <el-card class="research-card">
      <div class="research-form">
        <div class="form-section">
          <div class="section-label">
            <el-icon><Edit /></el-icon>
            <span>研究问题</span>
          </div>
          <el-input
            v-model="query"
            type="textarea"
            :rows="4"
            placeholder="请输入需要研究的具体法律问题，如：建设工程合同纠纷中的工期延误索赔问题研究"
          />
        </div>

        <div class="form-options">
          <div class="option-group">
            <label>
              <el-icon><DataAnalysis /></el-icon>
              研究深度
            </label>
            <el-radio-group v-model="depth">
              <el-radio-button label="brief">简略</el-radio-button>
              <el-radio-button label="normal">标准</el-radio-button>
              <el-radio-button label="detailed">详细</el-radio-button>
            </el-radio-group>
          </div>
          <div class="option-group">
            <label>
              <el-icon><Connection /></el-icon>
              数据来源
            </label>
            <el-checkbox-group v-model="sources">
              <el-checkbox label="laws">法律法规</el-checkbox>
              <el-checkbox label="cases">司法案例</el-checkbox>
              <el-checkbox label="papers">学术论文</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <div class="form-actions">
          <el-button type="primary" size="large" @click="handleResearch" :loading="loading">
            <el-icon><MagicStick /></el-icon>
            开始研究
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-if="loading" class="progress-panel">
      <el-card class="progress-card">
        <div class="progress-header">
          <div class="progress-info">
            <div class="progress-icon">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div class="progress-text">
              <span class="progress-title">正在生成研究报告</span>
              <span class="progress-percent">{{ progress }}%</span>
            </div>
          </div>
        </div>
        <el-progress :percentage="progress" :status="progressStatus" :stroke-width="8" />
        <div class="progress-phases">
          <div
            v-for="phase in phases"
            :key="phase.name"
            :class="['phase-item', { active: phase.active, completed: phase.completed }]"
          >
            <div class="phase-icon">
              <el-icon v-if="phase.completed"><SuccessFilled /></el-icon>
              <el-icon v-else-if="phase.active" class="is-loading"><Loading /></el-icon>
              <el-icon v-else><Document /></el-icon>
            </div>
            <span>{{ phase.label }}</span>
          </div>
        </div>
        <p class="progress-message">{{ progressMessage }}</p>
      </el-card>
    </div>

    <div v-if="report" class="report-content">
      <el-card class="report-toolbar-card">
        <div class="report-toolbar">
          <div class="toolbar-left">
            <el-icon><Document /></el-icon>
            <span>研究报告</span>
          </div>
          <div class="toolbar-actions">
            <el-button @click="exportPdf">
              <el-icon><Download /></el-icon>
              导出PDF
            </el-button>
            <el-button @click="copyReport">
              <el-icon><CopyDocument /></el-icon>
              复制全文
            </el-button>
            <el-button @click="printReport">
              <el-icon><Printer /></el-icon>
              打印
            </el-button>
          </div>
        </div>
      </el-card>

      <div class="report-container">
        <el-card
          v-for="section in report"
          :key="section.id"
          :id="'section-' + section.id"
          class="report-section"
        >
          <div class="section-header">
            <div class="section-title">
              <h2>{{ section.title }}</h2>
            </div>
            <el-button type="primary" link @click="copySection(section)">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
          <div class="section-content" v-html="section.content"></div>
          <div class="section-citations" v-if="section.citations?.length">
            <div class="citations-header">
              <el-icon><Link /></el-icon>
              <span>参考来源</span>
            </div>
            <div v-for="c in section.citations" :key="c.id" class="citation-item">
              <a :href="c.url" target="_blank">{{ c.title }}</a>
              <el-tag size="small" type="info">{{ c.source }}</el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Edit,
  DataAnalysis,
  Connection,
  MagicStick,
  Loading,
  SuccessFilled,
  Document,
  Download,
  CopyDocument,
  Printer,
  Link
} from '@element-plus/icons-vue'

const query = ref('')
const loading = ref(false)
const progress = ref(0)
const progressStatus = ref()
const progressMessage = ref('')
const report = ref(null)
const depth = ref('normal')
const sources = ref(['laws', 'cases'])

const phases = reactive([
  { name: 'parse', label: '解析问题', active: false, completed: false },
  { name: 'search_laws', label: '检索法规', active: false, completed: false },
  { name: 'search_cases', label: '检索案例', active: false, completed: false },
  { name: 'generate_def', label: '问题界定', active: false, completed: false },
  { name: 'generate_basis', label: '法律依据', active: false, completed: false },
  { name: 'generate_risk', label: '风险提示', active: false, completed: false },
  { name: 'generate_conclusion', label: '结论建议', active: false, completed: false }
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
          <li>加强项目管理，确保设计变更，材料供应等及时到位。</li>
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
.legal-research {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  margin-bottom: 24px;

  .header-content {
    h2 {
      margin: 0 0 8px 0;
      font-size: 26px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea, #764ba2);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    p {
      margin: 0;
      color: #6b7280;
      font-size: 14px;
    }
  }
}

.research-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.research-form {
  .form-section {
    margin-bottom: 24px;

    .section-label {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 14px;
      font-size: 15px;
      font-weight: 500;
      color: #1f2937;

      .el-icon {
        font-size: 18px;
        color: #667eea;
      }
    }

    :deep(.el-textarea__inner) {
      border-radius: 12px;
      padding: 16px;
      font-size: 14px;
      line-height: 1.8;
      resize: none;

      &::placeholder {
        color: #9ca3af;
      }
    }
  }

  .form-options {
    display: flex;
    gap: 32px;
    margin-bottom: 24px;

    .option-group {
      label {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: #4b5563;
        margin-bottom: 12px;

        .el-icon {
          color: #667eea;
        }
      }
    }
  }

  .form-actions {
    :deep(.el-button) {
      height: 48px;
      padding: 0 48px;
      border-radius: 12px;
      font-size: 15px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
      }
    }
  }
}

.progress-panel {
  margin-bottom: 24px;
}

.progress-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);

  :deep(.el-card__body) {
    padding: 28px;
  }

  .progress-header {
    margin-bottom: 24px;

    .progress-info {
      display: flex;
      align-items: center;
      gap: 16px;

      .progress-icon {
        width: 48px;
        height: 48px;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;

        .el-icon {
          font-size: 24px;
          color: #667eea;
        }
      }

      .progress-text {
        flex: 1;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .progress-title {
          font-size: 16px;
          font-weight: 500;
          color: #1f2937;
        }

        .progress-percent {
          font-size: 20px;
          font-weight: 700;
          color: #667eea;
        }
      }
    }
  }

  :deep(.el-progress-bar__outer) {
    background: #f3f4f6;
    border-radius: 6px;
  }

  .progress-phases {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-top: 28px;

    .phase-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: #f9fafb;
      border-radius: 20px;
      font-size: 13px;
      color: #9ca3af;
      transition: all 0.3s;

      .phase-icon {
        font-size: 14px;
      }

      &.active {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
        color: #667eea;
      }

      &.completed {
        background: rgba(16, 185, 129, 0.1);
        color: #10b981;
      }
    }
  }

  .progress-message {
    text-align: center;
    color: #6b7280;
    font-size: 14px;
    margin: 20px 0 0 0;
  }
}

.report-toolbar-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 16px 20px;
  }
}

.report-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 15px;
    font-weight: 600;
    color: #1f2937;

    .el-icon {
      font-size: 20px;
      color: #667eea;
    }
  }

  .toolbar-actions {
    display: flex;
    gap: 10px;

    :deep(.el-button) {
      border-radius: 8px;
    }
  }
}

.report-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.report-section {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  }

  :deep(.el-card__body) {
    padding: 28px;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 2px solid #f3f4f6;

    .section-title {
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #1f2937;
        border-left: 4px solid #667eea;
        padding-left: 16px;
        line-height: 1.4;
      }
    }
  }

  .section-content {
    line-height: 1.9;
    font-size: 14px;
    color: #374151;

    p {
      margin: 0 0 16px 0;
    }

    ul {
      padding-left: 24px;
      margin: 0 0 16px 0;

      li {
        margin-bottom: 8px;
        line-height: 1.7;
      }
    }

    strong {
      color: #1f2937;
    }
  }

  .section-citations {
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px dashed #e5e7eb;

    .citations-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 14px;
      font-size: 14px;
      font-weight: 500;
      color: #6b7280;

      .el-icon {
        color: #667eea;
      }
    }

    .citation-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 0;
      border-bottom: 1px solid #f3f4f6;

      &:last-child {
        border-bottom: none;
      }

      a {
        color: #667eea;
        text-decoration: none;
        font-size: 14px;

        &:hover {
          text-decoration: underline;
        }
      }
    }
  }
}

@media print {
  .report-toolbar-card,
  .progress-panel {
    display: none;
  }

  .report-section {
    box-shadow: none;
    border: 1px solid #e5e7eb;
  }
}
</style>
