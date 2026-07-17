<template>
  <el-dialog
    v-model="visible"
    title="AI 案情分析"
    width="840px"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    :align-center="true"
    class="case-analysis-dialog"
    @close="handleClose"
  >
    <div v-if="loading" class="analysis-progress">
      <div class="progress-icon">
        <el-icon :size="56" :class="{ pulse: !error, shake: error }">
          <component :is="error ? 'WarningFilled' : 'DataAnalysis'" />
        </el-icon>
      </div>
      <div class="progress-title">{{ error ? '分析遇到问题' : 'AI 正在分析案情' }}</div>
      <el-progress
        :percentage="progress"
        :stroke-width="10"
        :color="progressColor"
        :show-text="true"
        class="progress-bar"
      />
      <div class="progress-message">{{ progressMessage || '准备中...' }}</div>
      <div class="phase-steps">
        <div
          v-for="(phase, index) in phases"
          :key="phase.name"
          class="phase-step"
          :class="{
            active: currentPhase === phase.name,
            done: isPhaseDone(phase.name)
          }"
        >
          <span class="phase-dot">
            <el-icon v-if="isPhaseDone(phase.name)" :size="14"><Check /></el-icon>
            <span v-else class="dot-num">{{ index + 1 }}</span>
          </span>
          <span class="phase-label">{{ phase.label }}</span>
        </div>
      </div>
    </div>

    <div v-else-if="analysis" class="analysis-result">
      <div class="result-header">
        <div class="result-title">
          <h3>{{ analysis.title || '案情分析报告' }}</h3>
          <div class="result-meta">
            <el-tag v-if="analysis.caseNo" size="small" type="info" effect="plain">
              {{ analysis.caseNo }}
            </el-tag>
            <span class="meta-time">分析耗时 {{ Math.round(analysis.tookMs / 100) / 10 }}s</span>
          </div>
        </div>
        <div class="result-actions">
          <el-button size="small" @click="copyReport">
            <el-icon><CopyDocument /></el-icon>
            复制全文
          </el-button>
          <el-button size="small" type="primary" @click="reanalyze">
            <el-icon><Refresh /></el-icon>
            重新分析
          </el-button>
        </div>
      </div>

      <el-scrollbar class="result-scroll">
        <div class="result-content">
          <div
            v-for="section in analysis.sections"
            :key="section.id"
            class="section-card"
          >
            <div class="section-header">
              <div class="section-title">
                <span class="section-icon">{{ section.icon || '📌' }}</span>
                <h4>{{ section.title }}</h4>
                <el-tag
                  v-if="section.level"
                  :type="getLevelType(section.level)"
                  size="small"
                  effect="dark"
                  round
                >
                  {{ getLevelLabel(section.level) }}
                </el-tag>
              </div>
              <el-button type="primary" link size="small" @click="copySection(section)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>

            <div v-if="section.keyPoints?.length" class="key-points">
              <div
                v-for="(point, idx) in section.keyPoints"
                :key="idx"
                class="key-point"
              >
                <span class="point-bullet">·</span>
                <span>{{ point }}</span>
              </div>
            </div>

            <div class="section-content" v-html="sanitize(formatContent(section.content))"></div>
          </div>

          <div v-if="analysis.relatedLaws?.length" class="related-block">
            <h4><el-icon><Collection /></el-icon> 相关法律依据</h4>
            <div class="related-list">
              <el-tag
                v-for="(law, idx) in analysis.relatedLaws"
                :key="idx"
                effect="plain"
                type="info"
                class="related-tag"
              >
                {{ law }}
              </el-tag>
            </div>
          </div>

          <div v-if="analysis.relatedCases?.length" class="related-block">
            <h4><el-icon><Document /></el-icon> 类似案件参考</h4>
            <div class="related-list">
              <el-tag
                v-for="(c, idx) in analysis.relatedCases"
                :key="idx"
                effect="plain"
                class="related-tag"
              >
                {{ c }}
              </el-tag>
            </div>
          </div>

          <div v-if="analysis.disclaimer" class="disclaimer">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ analysis.disclaimer }}</span>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

 <script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis,
  WarningFilled,
  Check,
  CopyDocument,
  Refresh,
  Collection,
  Document,
  InfoFilled
} from '@element-plus/icons-vue'
import { sanitizeHTML as sanitize } from '@/utils/sanitize'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  caseUuid: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const loading = ref(false)
const error = ref(false)
const progress = ref(0)
const progressMessage = ref('')
const currentPhase = ref('')
const analysis = ref(null)
let abortController = null

const phases = [
  { name: 'load', label: '加载案件信息', order: 10 },
  { name: 'facts', label: '解析案件事实', order: 25 },
  { name: 'dispute', label: '识别争议焦点', order: 45 },
  { name: 'reasoning', label: '分析裁判逻辑', order: 65 },
  { name: 'advice', label: '生成应对建议', order: 85 },
  { name: 'complete', label: '分析完成', order: 100 }
]

const progressColor = computed(() => {
  if (error.value) return '#ef4444'
  return [
    { color: '#667eea', percentage: 50 },
    { color: '#764ba2', percentage: 100 }
  ]
})

const isPhaseDone = (name) => {
  if (error.value) return false
  const phase = phases.find(p => p.name === name)
  if (!phase) return false
  return progress.value >= phase.order
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.caseUuid) {
    startAnalysis()
  }
})

watch(() => props.caseUuid, (val, oldVal) => {
  if (val && val !== oldVal && visible.value) {
    startAnalysis()
  }
})

const startAnalysis = async () => {
  if (!props.caseUuid) {
    ElMessage.error('案例ID不能为空')
    return
  }

  cleanup()
  loading.value = true
  error.value = false
  progress.value = 0
  progressMessage.value = '准备开始分析...'
  currentPhase.value = ''
  analysis.value = null
  abortController = new AbortController()

  try {
    const token = localStorage.getItem('token') || ''
    const response = await fetch(`/api/v1/case-search/cases/${props.caseUuid}/analysis/stream`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'text/event-stream'
      },
      signal: abortController.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const eventBlock of events) {
        const line = eventBlock.split('\n').find(l => l.startsWith('data:'))
        if (!line) continue
        const data = line.slice(5).trim()
        if (!data) continue
        try {
          const event = JSON.parse(data)
          handleEvent(event)
        } catch (e) {
          console.warn('SSE parse error', e, data)
        }
      }
    }
  } catch (e) {
    if (e.name === 'AbortError') return
    console.error('SSE error', e)
    error.value = true
    progressMessage.value = '分析失败：' + (e.message || '网络错误')
  } finally {
    loading.value = false
  }
}

const handleEvent = (event) => {
  if (event.type === 'progress') {
    progress.value = Math.max(progress.value, event.progress || 0)
    progressMessage.value = event.message || ''
    currentPhase.value = event.phase || ''
  } else if (event.type === 'analysis') {
    analysis.value = event.data
    progress.value = 100
    currentPhase.value = 'complete'
    progressMessage.value = '分析完成'
  } else if (event.type === 'error') {
    error.value = true
    progressMessage.value = event.message || '分析失败'
  }
}

const cleanup = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

const reanalyze = () => {
  startAnalysis()
}

const handleClose = () => {
  cleanup()
  visible.value = false
  emit('update:modelValue', false)
}

const formatContent = (text) => {
  if (!text) return ''
  return text
    .replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const getLevelType = (level) => {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  if (level === 'LOW') return 'success'
  return 'info'
}

const getLevelLabel = (level) => {
  if (level === 'HIGH') return '高'
  if (level === 'MEDIUM') return '中'
  if (level === 'LOW') return '低'
  return ''
}

const copyReport = () => {
  if (!analysis.value) return
  const text = analysis.value.sections.map(s => {
    let t = `${s.title}\n${s.content.replace(/<[^>]+>/g, '')}`
    if (s.keyPoints?.length) {
      t += '\n要点：\n' + s.keyPoints.map(p => `- ${p}`).join('\n')
    }
    return t
  }).join('\n\n')
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制全文到剪贴板')
}

const copySection = (section) => {
  let text = section.title + '\n' + section.content.replace(/<[^>]+>/g, '')
  if (section.keyPoints?.length) {
    text += '\n要点：\n' + section.keyPoints.map(p => `- ${p}`).join('\n')
  }
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制本节内容')
}

onBeforeUnmount(() => {
  cleanup()
})
</script>

<style lang="scss" scoped>
.case-analysis-dialog {
  :deep(.el-dialog__body) {
    padding: 24px 28px;
  }
}

.analysis-progress {
  text-align: center;
  padding: 32px 0 16px;

  .progress-icon {
    margin-bottom: 16px;
    color: #667eea;

    .pulse {
      animation: pulse-anim 1.6s ease-in-out infinite;
    }
    .shake {
      color: #ef4444;
      animation: shake-anim 0.4s ease-in-out;
    }
  }

  .progress-title {
    font-size: 18px;
    font-weight: 600;
    color: #1f2937;
    margin-bottom: 24px;
  }

  .progress-bar {
    margin: 0 32px 16px;
  }

  .progress-message {
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 24px;
  }

  .phase-steps {
    display: flex;
    justify-content: space-between;
    align-items: center;
    max-width: 600px;
    margin: 0 auto;
    padding: 0 16px;
  }

  .phase-step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    flex: 1;
    opacity: 0.4;
    transition: opacity 0.3s ease;

    &.active {
      opacity: 1;
      .phase-dot {
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
      }
      .phase-label {
        color: #667eea;
        font-weight: 500;
      }
    }

    &.done {
      opacity: 0.85;
      .phase-dot {
        background: #10b981;
        color: #fff;
      }
      .phase-label {
        color: #10b981;
      }
    }

    .phase-dot {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #e5e7eb;
      color: #9ca3af;
      font-size: 12px;
      font-weight: 600;
      transition: all 0.3s ease;
    }

    .phase-label {
      font-size: 12px;
      color: #9ca3af;
      transition: color 0.3s ease;
    }
  }
}

.analysis-result {
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e5e7eb;

    .result-title {
      h3 {
        margin: 0 0 6px 0;
        font-size: 17px;
        font-weight: 600;
        color: #1f2937;
      }

      .result-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 12px;
        color: #6b7280;

        .meta-time {
          font-size: 12px;
          color: #9ca3af;
        }
      }
    }

    .result-actions {
      display: flex;
      gap: 6px;
    }
  }

  .result-scroll {
    max-height: 60vh;
  }

  .result-content {
    padding-right: 8px;
  }

  .section-card {
    padding: 16px 18px;
    margin-bottom: 14px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.04), rgba(118, 75, 162, 0.04));
    border-left: 3px solid #667eea;
    border-radius: 10px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .section-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .section-icon {
          font-size: 18px;
        }

        h4 {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
          color: #1f2937;
        }
      }
    }

    .key-points {
      margin-bottom: 10px;
      padding: 10px 12px;
      background: rgba(255, 255, 255, 0.6);
      border-radius: 6px;

      .key-point {
        display: flex;
        gap: 6px;
        font-size: 13px;
        line-height: 1.7;
        color: #4b5563;

        .point-bullet {
          color: #667eea;
          font-weight: 700;
          flex-shrink: 0;
        }
      }
    }

    .section-content {
      font-size: 13px;
      line-height: 1.8;
      color: #374151;

      :deep(strong) {
        color: #1f2937;
        font-weight: 600;
      }

      :deep(pre) {
        background: #1f2937;
        color: #e5e7eb;
        padding: 10px 12px;
        border-radius: 6px;
        overflow-x: auto;
        font-size: 12px;
      }
    }
  }

  .related-block {
    margin-top: 16px;
    padding: 14px 16px;
    background: #f9fafb;
    border-radius: 8px;

    h4 {
      margin: 0 0 10px 0;
      font-size: 14px;
      font-weight: 600;
      color: #1f2937;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .related-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .related-tag {
        font-size: 12px;
      }
    }
  }

  .disclaimer {
    margin-top: 16px;
    padding: 10px 14px;
    background: #fffbeb;
    border-left: 3px solid #f59e0b;
    border-radius: 6px;
    font-size: 12px;
    color: #92400e;
    display: flex;
    align-items: flex-start;
    gap: 8px;

    .el-icon {
      flex-shrink: 0;
      margin-top: 1px;
    }
  }
}

@keyframes pulse-anim {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
}

@keyframes shake-anim {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}
</style>
