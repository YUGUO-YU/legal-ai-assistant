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
            <span class="char-count">{{ query.length }}/500</span>
          </div>
          <el-input
            v-model="query"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
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
 import api from '../api'
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

  phases[0].active = true
  progressMessage.value = '正在解析研究问题...'

  try {
    const response = await fetch('/api/v1/legal-research/generate/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      },
      body: JSON.stringify({
        question: query.value,
        depth: depth.value,
        sources: sources.value
      })
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullReportContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data: ')) {
          const data = line.slice(6)
          if (data === '[DONE]') continue

          try {
            const event = JSON.parse(data)

            if (event.type === 'progress') {
              progress.value = event.progress
              progressMessage.value = event.message

              const phaseMap = {
                'parse': 0,
                'search_laws': 1,
                'search_cases': 2,
                'generate_def': 3,
                'generate_basis': 4,
                'generate_risk': 5,
                'generate_conclusion': 6,
                'complete': 6
              }
              const phaseIndex = phaseMap[event.phase]
              if (phaseIndex !== undefined) {
                phases[phaseIndex].active = false
                phases[phaseIndex].completed = true
                if (phaseIndex < phases.length - 1) {
                  phases[phaseIndex + 1].active = true
                }
              }
            } else if (event.type === 'report') {
              fullReportContent = event.content
              report.value = parseReportContent(fullReportContent)
            }
          } catch (e) {
            console.error('解析SSE事件失败:', e)
          }
        }
      }
    }

    phases[3].active = false
    phases[3].completed = true
    phases[4].completed = true
    phases[5].completed = true
    phases[6].completed = true
    progress.value = 100
    progressMessage.value = '研究完成！'
    progressStatus.value = 'success'

    loading.value = false
    ElMessage.success('研究报告生成完成')
  } catch (e) {
    console.error(e)
    loading.value = false
    progressStatus.value = 'exception'
    progressMessage.value = '生成失败，请稍后重试'
    ElMessage.error('研究报告生成失败')
  }
}

const parseReportContent = (content) => {
  if (!content) return []

  const sections = []
  const sectionTitles = [
    { pattern: /#+\s*一[、、]?问题界定/, title: '一、问题界定' },
    { pattern: /#+\s*二[、、]?法律依据/, title: '二、法律依据' },
    { pattern: /#+\s*三[、、]?学术观点/, title: '三、学术观点' },
    { pattern: /#+\s*四[、、]?实务指引/, title: '四、实务指引' },
    { pattern: /#+\s*五[、、]?风险提示/, title: '五、风险提示' },
    { pattern: /#+\s*六[、、]?结论建议/, title: '六、结论建议' }
  ]

  let currentSection = null
  let currentContent = []

  const lines = content.split('\n')
  for (const line of lines) {
    let matched = false
    for (const st of sectionTitles) {
      if (st.pattern.test(line)) {
        if (currentSection) {
          sections.push({
            id: sections.length + 1,
            title: currentSection,
            content: formatContent(currentContent.join('\n')),
            citations: []
          })
        }
        currentSection = st.title
        currentContent = []
        matched = true
        break
      }
    }
    if (!matched) {
      currentContent.push(line)
    }
  }

  if (currentSection && currentContent.length > 0) {
    sections.push({
      id: sections.length + 1,
      title: currentSection,
      content: formatContent(currentContent.join('\n')),
      citations: []
    })
  }

  if (sections.length === 0) {
    sections.push({
      id: 1,
      title: '研究报告',
      content: formatContent(content),
      citations: []
    })
  }

  return sections
}

const formatContent = (text) => {
  if (!text) return ''
  return text
    .replace(/^#+\s*/gm, '')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
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
