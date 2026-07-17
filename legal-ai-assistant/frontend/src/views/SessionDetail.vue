<template>
  <div class="session-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回问答
        </el-button>
      </div>
      <div class="header-content">
        <h2>会话详情</h2>
        <p>查看完整问答历史、引用与时间线</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载会话..." />

    <div v-else-if="messages.length" class="detail-container">
      <el-card class="info-card summary-card">
        <div class="summary-row">
          <div class="avatar">
            <el-icon :size="32"><ChatDotRound /></el-icon>
          </div>
          <div class="meta">
            <h1>{{ title }}</h1>
            <div class="meta-tags">
              <el-tag type="info" effect="plain" round>ID: {{ sessionId }}</el-tag>
              <el-tag size="small" effect="dark" round>共 {{ messages.length }} 条消息</el-tag>
              <el-tag size="small" type="success" effect="plain" round>用户 {{ userCount }} 条</el-tag>
              <el-tag size="small" type="primary" effect="plain" round>助手 {{ assistantCount }} 条</el-tag>
            </div>
          </div>
        </div>
        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ totalQuestions }}</span>
              <span class="stat-label">问题数</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ totalCitations }}</span>
              <span class="stat-label">引用来源</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ avgLength }}</span>
              <span class="stat-label">平均字数</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ duration }}</span>
              <span class="stat-label">会话时长</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="info-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <el-icon><ChatLineSquare /></el-icon>
            <span>对话时间线</span>
            <el-button type="primary" link size="small" @click="exportHistory" style="margin-left: auto">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </div>
        </template>

        <el-timeline>
          <el-timeline-item
            v-for="(m, idx) in messages"
            :key="idx"
            :timestamp="formatTime(m.createdAt)"
            :type="m.role === 'user' ? 'primary' : 'success'"
            :hollow="m.role === 'user'"
            placement="top"
          >
            <div class="msg" :class="['msg-' + m.role]">
              <div class="msg-head">
                <el-tag :type="m.role === 'user' ? 'primary' : 'success'" effect="dark" size="small">
                  {{ m.role === 'user' ? '我' : 'AI' }}
                </el-tag>
                <span class="msg-snippet">{{ snippet(m.content) }}</span>
              </div>
              <div class="msg-content">{{ m.content }}</div>
              <div v-if="m.citations?.length" class="msg-citations">
                <div class="cit-title">参考来源：</div>
                <el-tag
                  v-for="(c, ci) in m.citations"
                  :key="ci"
                  size="small"
                  effect="plain"
                  class="cit-tag"
                >
                  <el-icon><Link /></el-icon>
                  {{ c.title || c.sourceName || '来源' }}
                </el-tag>
              </div>
            </div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card class="info-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <el-icon><Operation /></el-icon>
            <span>操作</span>
          </div>
        </template>
        <div class="action-row">
          <el-button type="primary" @click="continueAsk">
            <el-icon><ChatLineSquare /></el-icon>
            继续提问
          </el-button>
          <el-button @click="copyHistory">
            <el-icon><CopyDocument /></el-icon>
            复制完整记录
          </el-button>
          <el-button type="danger" plain @click="onClear" :disabled="clearing">
            <el-icon><Delete /></el-icon>
            {{ clearing ? '清除中...' : '清除会话' }}
          </el-button>
        </div>
      </el-card>
    </div>

    <empty-state
      v-else
      icon="ChatLineSquare"
      title="未找到会话记录"
      description="该会话可能已过期或被清空"
      action-text="返回"
      @action="goBack"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ChatDotRound,
  ChatLineSquare,
  Link,
  Download,
  Operation,
  CopyDocument,
  Delete
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const messages = ref([])
const sessionId = ref('')
const title = ref('会话详情')
const clearing = ref(false)

const userCount = computed(() => messages.value.filter(m => m.role === 'user').length)
const assistantCount = computed(() => messages.value.filter(m => m.role === 'assistant').length)
const totalQuestions = computed(() => userCount.value)
const totalCitations = computed(() => {
  let n = 0
  for (const m of messages.value) {
    if (m.citations) n += m.citations.length
  }
  return n
})
const avgLength = computed(() => {
  const assistantMsgs = messages.value.filter(m => m.role === 'assistant' && m.content)
  if (!assistantMsgs.length) return 0
  const total = assistantMsgs.reduce((s, m) => s + (m.content || '').length, 0)
  return Math.round(total / assistantMsgs.length)
})
const duration = computed(() => {
  if (messages.value.length < 2) return '-'
  const first = messages.value[0]?.createdAt
  const last = messages.value[messages.value.length - 1]?.createdAt
  if (!first || !last) return '-'
  const t1 = new Date(first).getTime()
  const t2 = new Date(last).getTime()
  if (isNaN(t1) || isNaN(t2)) return '-'
  const sec = Math.round((t2 - t1) / 1000)
  if (sec < 60) return `${sec}s`
  if (sec < 3600) return `${Math.floor(sec / 60)}m${sec % 60}s`
  return `${Math.floor(sec / 3600)}h${Math.floor((sec % 3600) / 60)}m`
})

const loadSession = async () => {
  sessionId.value = route.params.sessionId
  if (!sessionId.value) {
    ElMessage.error('会话ID缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await api.docQa.getSessionHistory(sessionId.value)
    if (res) {
      messages.value = res
      const firstUser = messages.value.find(m => m.role === 'user')
      if (firstUser) {
        title.value = snippet(firstUser.content, 30) || '会话详情'
      } else {
        title.value = `会话 ${sessionId.value.slice(0, 8)}`
      }
    } else {
      ElMessage.error('会话不存在或已过期')
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/doc-qa')
}

const continueAsk = () => {
  router.push({ path: '/doc-qa', query: { sessionId: sessionId.value } })
}

const snippet = (text, len = 24) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}

const formatTime = (t) => {
  if (!t) return ''
  try {
    return new Date(t).toLocaleString('zh-CN', { hour12: false })
  } catch (e) {
    return t
  }
}

const exportHistory = () => {
  const lines = [`# ${title.value}`, `会话ID: ${sessionId.value}`, '']
  for (const m of messages.value) {
    const tag = m.role === 'user' ? '我' : 'AI'
    lines.push(`## [${formatTime(m.createdAt)}] ${tag}`)
    lines.push(m.content || '')
    if (m.citations?.length) {
      lines.push('**引用**:')
      for (const c of m.citations) {
        lines.push(`- ${c.title || c.sourceName || '来源'}`)
      }
    }
    lines.push('')
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `会话记录_${sessionId.value}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出')
}

const copyHistory = async () => {
  const text = messages.value.map(m => `[${m.role === 'user' ? '我' : 'AI'}] ${m.content || ''}`).join('\n\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const onClear = async () => {
  try {
    await ElMessageBox.confirm('确认清除该会话？清除后无法恢复', '清除确认', { type: 'warning' })
    clearing.value = true
    await api.docQa.clearSession(sessionId.value)
    ElMessage.success('已清除')
    router.push('/doc-qa')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('清除失败')
    }
  } finally {
    clearing.value = false
  }
}

onMounted(loadSession)
</script>

<style lang="scss" scoped>
.session-detail { animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.page-header {
  margin-bottom: 24px;
  .header-nav :deep(.el-button) { display: inline-flex; align-items: center; gap: 6px; color: var(--color-text-secondary); }
  .header-content h2 {
    margin: 0 0 8px 0; font-size: 26px; font-weight: 600;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 14px; }
}

.detail-container {
  .info-card {
    border: none; border-radius: 16px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    :deep(.el-card__header) { padding: 16px 20px; }
    :deep(.el-card__body) { padding: 16px 20px 20px; }
  }

  .summary-card :deep(.el-card__body) { padding: 24px; }
  .summary-row { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 20px; }
  .avatar {
    width: 64px; height: 64px; border-radius: 14px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    display: flex; align-items: center; justify-content: center; color: #fff;
  }
  .meta { flex: 1; h1 { margin: 0 0 12px 0; font-size: 22px; color: var(--color-text-primary); } }
  .meta-tags { display: flex; gap: 8px; flex-wrap: wrap; }

  .stat-row .stat-cell {
    background: #f9fafb; border-radius: 10px; padding: 14px;
    display: flex; flex-direction: column; align-items: center; gap: 4px;
    .stat-value { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
    .stat-label { font-size: 12px; color: var(--color-text-secondary); }
  }

  .card-header {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: 600; color: var(--color-text-primary);
    .el-icon { color: #667eea; }
  }

  .msg {
    background: #f9fafb; border-radius: 10px; padding: 12px 14px;
    margin-top: 6px;
    &.msg-user { background: linear-gradient(135deg, rgba(102,126,234,0.06), rgba(118,75,162,0.06)); border-left: 3px solid #667eea; }
    &.msg-assistant { background: #f9fafb; border-left: 3px solid #10b981; }
    .msg-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .msg-snippet { color: var(--color-text-secondary); font-size: 12px; }
    .msg-content { font-size: 14px; line-height: 1.7; color: var(--color-text-primary); white-space: pre-wrap; }
    .msg-citations { margin-top: 8px; }
    .cit-title { font-size: 12px; color: var(--color-text-secondary); margin-bottom: 4px; }
    .cit-tag { display: inline-flex; align-items: center; gap: 4px; margin-right: 6px; margin-bottom: 4px; }
  }

  .action-row { display: flex; gap: 12px; flex-wrap: wrap; }
}
</style>
