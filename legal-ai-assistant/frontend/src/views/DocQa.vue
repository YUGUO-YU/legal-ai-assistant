<template>
  <div class="doc-qa">
    <div class="page-header">
      <div class="header-content">
        <h2>AI文件问答</h2>
        <p>基于上传的文档进行智能问答，支持多轮对话</p>
      </div>
    </div>

    <el-row :gutter="24" class="main-content">
      <el-col :span="16">
        <el-card class="chat-card">
          <div class="chat-header">
            <div class="header-left">
              <div class="ai-avatar">
                <el-icon :size="20"><MagicStick /></el-icon>
              </div>
              <div class="header-info">
                <span class="ai-name">法律AI助手</span>
                <span class="ai-model">MiniMax M3</span>
              </div>
            </div>
            <el-button type="primary" link @click="clearHistory">
              <el-icon><Delete /></el-icon>
              清空对话
            </el-button>
          </div>

          <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="true" @close="errorMsg = ''" style="margin-bottom: 16px;">
            <template #default>
              <el-button size="small" type="primary" @click="errorMsg = ''; handleAsk()">重试</el-button>
            </template>
          </el-alert>

          <div class="chat-messages" ref="chatContainer">
            <div v-if="messages.length === 0" class="empty-state">
              <div class="empty-icon">
                <el-icon><ChatDotRound /></el-icon>
              </div>
              <h3>开始对话吧</h3>
              <p>向我提问关于文档的问题，我会基于已上传的文档给出准确答案</p>
              <div class="empty-suggestions">
                <el-tag
                  v-for="q in quickQuestions"
                  :key="q"
                  class="suggestion-tag"
                  @click="question = q; handleAsk()"
                >
                  {{ q }}
                </el-tag>
              </div>
            </div>

            <div v-else class="messages-list">
              <div
                v-for="(msg, index) in messages"
                :key="index"
                :class="['message', msg.role]"
              >
                <div class="message-avatar">
                  <el-avatar v-if="msg.role === 'user'" :size="36" class="user-avatar">
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <el-avatar v-else :size="36" class="ai-avatar">
                    <el-icon><MagicStick /></el-icon>
                  </el-avatar>
                </div>
                <div class="message-content">
                  <div class="message-bubble">
                    <div class="message-text" v-html="formatContent(msg.content)"></div>
                    <div v-if="msg.citations?.length" class="message-citations">
                      <div class="citation-header">
                        <el-icon><Document /></el-icon>
                        <span>参考来源</span>
                      </div>
                      <div
                        v-for="(c, ci) in msg.citations"
                        :key="ci"
                        class="citation-item"
                        @click="scrollToCitation(c)"
                      >
                        <el-progress
                          type="circle"
                          :percentage="c.score > 1 ? c.score : (c.score * 100)"
                          :width="28"
                          :stroke-width="3"
                          :show-text="false"
                        />
                        <span class="citation-text">{{ c.content?.substring(0, 60) }}...</span>
                      </div>
                    </div>
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
            </div>

            <div v-if="suggestedQuestions.length > 0 && lastAssistantMsg" class="suggested-questions">
              <div class="suggested-label">追问建议</div>
              <el-tag v-for="q in suggestedQuestions" :key="q" @click="fillQuestion(q)" class="suggested-tag">{{ q }}</el-tag>
            </div>

            <div v-if="loading" class="message assistant">
              <div class="message-avatar">
                <el-avatar :size="36" class="ai-avatar">
                  <el-icon><MagicStick /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-bubble loading">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span>正在思考...</span>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <div class="input-wrapper">
              <el-input
                v-model="question"
                placeholder="输入问题 (按 / 聚焦, Ctrl+Enter 发送)"
                :disabled="loading"
              />
              <el-button
                type="primary"
                class="send-btn"
                :disabled="!question.trim() || loading"
                @click="handleAsk"
              >
                <el-icon><Promotion /></el-icon>
              </el-button>
            </div>
            <div class="input-hints">
              <el-icon><InfoFilled /></el-icon>
              <span>支持追问和上下文理解</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="context-card">
          <div class="panel-section">
            <div class="section-header">
              <el-icon><Collection /></el-icon>
              <h3>当前知识库</h3>
            </div>
            <div class="current-kb">
              <el-tag v-if="currentKb" type="success" effect="dark" round>
                {{ currentKb }}
              </el-tag>
              <el-tag v-else type="info" round>暂未选择</el-tag>
            </div>
            <el-button class="switch-btn" @click="showKbSelector = true">
              <el-icon><Switch /></el-icon>
              切换知识库
            </el-button>
          </div>

          <el-divider />

          <div class="panel-section">
            <div class="section-header">
              <el-icon><ChatLineSquare /></el-icon>
              <h3>会话历史</h3>
            </div>
            <div class="session-list">
              <div
                v-for="s in sessions"
                :key="s.id"
                :class="['session-item', { active: s.id === currentSession }]"
                @click="switchSession(s.id)"
              >
                <div class="session-icon">
                  <el-icon><ChatDotRound /></el-icon>
                </div>
                <div class="session-info">
                  <span class="session-title">{{ s.title }}</span>
                  <span class="session-date">{{ s.date }}</span>
                </div>
                <el-icon class="session-continue-btn" @click.stop="restoreSession(s)" title="继续对话">
                  <VideoPlay />
                </el-icon>
                <el-icon class="session-detail-btn" @click.stop="goSessionDetail(s)" title="查看详情">
                  <View />
                </el-icon>
                <el-icon class="session-arrow"><Right /></el-icon>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="panel-section">
            <div class="section-header">
              <el-icon><QuestionFilled /></el-icon>
              <h3>快捷问题</h3>
            </div>
            <div class="quick-questions">
              <div
                v-for="q in quickQuestions"
                :key="q"
                class="quick-question"
                @click="question = q; handleAsk()"
              >
                <el-icon><Key /></el-icon>
                <span>{{ q }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showKbSelector" title="选择知识库" width="480px" class="kb-dialog">
      <el-select v-model="selectedKb" placeholder="请选择知识库" style="width: 100%">
        <el-option
          v-for="kb in kbList"
          :key="kb.id"
          :label="kb.name"
          :value="kb.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="showKbSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmKb">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  ChatDotRound,
  ChatLineRound,
  User,
  Promotion,
  Document,
  Delete,
  Loading,
  CopyDocument,
  Refresh,
  Download,
  Collection,
  Link,
  ChatLineSquare,
  Right,
  QuestionFilled,
  InfoFilled,
  Switch,
  Key,
  View,
  VideoPlay
} from '@element-plus/icons-vue'
import api from '../api'
import { useKeyboardShortcuts, matchShortcut, isInputFocused } from '@/composables/useKeyboardShortcuts'
import { useStats } from '@/composables/useStats'

const router = useRouter()
const route = useRoute()
const { increment } = useStats()
const question = ref('')
const messages = ref([])
const chatContainer = ref(null)
const currentSession = ref(null)
const sessionId = ref(null)
const currentKb = ref('')
const showKbSelector = ref(false)
const selectedKb = ref('')
const loading = ref(false)
const kbList = ref([])
const errorMsg = ref('')

const sessions = ref([])
const suggestedQuestions = ref([])

const lastAssistantMsg = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i].role === 'assistant') return messages.value[i]
  }
  return null
})

const loadSessions = async () => {
  try {
    const res = await api.docQa.getSessionList()
    if (res && res.length > 0) {
      sessions.value = res.map(s => ({
        id: s.sessionId,
        sessionUuid: s.sessionId,
        title: s.title || '新会话',
        date: s.date || ''
      }))
    }
  } catch (e) {
    console.error('加载会话列表失败:', e)
  }
}

const loadKbList = async () => {
  try {
    const res = await api.knowledgeBase.list()
    if (res && res.length > 0) {
      kbList.value = res.map(kb => ({
        id: kb.id,
        name: kb.name
      }))
      if (kbList.value.length > 0 && !selectedKb.value) {
        selectedKb.value = kbList.value[0].id
        currentKb.value = kbList.value[0].name
      }
    }
  } catch (e) {
    console.error('加载知识库列表失败:', e)
  }
}

const quickQuestions = [
  '合同欺诈的构成要件是什么？',
  '用人单位可以单方解除劳动合同的情形？',
  '建设工程合同纠纷的管辖法院如何确定？',
  '借款合同利息的法律保护上限是多少？'
]

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

const generateSuggestions = (q) => {
  if (q.includes('合同')) {
    suggestedQuestions.value = ['合同审查的标准是什么？', '如何避免合同纠纷？', '合同违约怎么处理？']
  } else if (q.includes('劳动')) {
    suggestedQuestions.value = ['劳动仲裁流程是什么？', '如何申请劳动仲裁？', '劳动合同注意事项有哪些？']
  } else {
    suggestedQuestions.value = ['请详细说明相关法律规定？', '有哪些类似案例可参考？', '我还需要注意哪些关键点？']
  }
}

const fillQuestion = (q) => {
  question.value = q
}

const handleAsk = async () => {
  errorMsg.value = ''
  if (!question.value.trim() || loading.value) return

  const userMsg = {
    id: Date.now(),
    role: 'user',
    content: question.value,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  messages.value.push(userMsg)
  const q = question.value
  question.value = ''

  loading.value = true

  await nextTick()
  chatContainer.value.scrollTop = chatContainer.value.scrollHeight

  const aiMsg = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
    citations: [],
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  messages.value.push(aiMsg)

  try {
    const response = await fetch('/api/v1/doc-qa/ask/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      },
      body: JSON.stringify({
        question: q,
        sessionId: sessionId.value,
        kbId: selectedKb.value
      })
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const isNewSession = !sessionId.value
    sessionId.value = sessionId.value || `session-${Date.now()}`
    if (isNewSession) increment('sessionCount')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let pendingUpdate = false

    const flushUpdate = async () => {
      if (pendingUpdate) {
        pendingUpdate = false
        messages.value[messages.value.length - 1] = { ...aiMsg }
        await nextTick()
        chatContainer.value.scrollTop = chatContainer.value.scrollHeight
      }
    }

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

          aiMsg.content += data

          if (!pendingUpdate) {
            pendingUpdate = true
            requestAnimationFrame(flushUpdate)
          }
        }
      }
    }

    await flushUpdate()
    generateSuggestions(q)
    await loadSessions()
  } catch (e) {
    aiMsg.content = '回答生成失败，请稍后重试'
    ElMessage.error('回答生成失败，请稍后重试')
    errorMsg.value = '网络错误，请检查网络连接或稍后重试'
  } finally {
    loading.value = false
    await nextTick()
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const clearHistory = async () => {
  if (sessionId.value) {
    try {
      await api.docQa.clearSession(sessionId.value)
    } catch (e) {
    }
  }
  messages.value = []
  sessionId.value = null
  ElMessage.success('对话已清空')
}

const switchSession = async (session) => {
  currentSession.value = session.id
  sessionId.value = session.sessionUuid || session.id
  messages.value = []

  try {
    const res = await api.docQa.getSessionHistory(session.sessionUuid || session.id)
    if (res && res.length > 0) {
      messages.value = res.map((msg, idx) => ({
        id: idx,
        role: msg.role,
        content: msg.content,
        time: msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
      }))
    }
  } catch (e) {
    console.error('获取会话历史失败:', e)
  }

  ElMessage.info('已切换会话')
}

const confirmKb = async () => {
  if (selectedKb.value !== currentKb.value) {
    messages.value = []
    currentKb.value = selectedKb.value
    sessionId.value = null
    currentSession.value = null
    await loadSessions()
  }
  showKbSelector.value = false
  ElMessage.success('已切换知识库')
}

const scrollToCitation = (c) => {
  if (c?.documentId) {
    const kb = kbList.value.find(k => k.id === c.documentId)
    const kbName = kb ? kb.name : c.documentId
    ElMessage.info(`正在定位文档: ${kbName}`)
  }
}

const goSessionDetail = (s) => {
  const id = s.sessionUuid || s.id
  if (!id) {
    ElMessage.warning('该会话没有可用的ID')
    return
  }
  router.push(`/qa-session/${id}`)
}

const restoreSession = async (session) => {
  currentSession.value = session.id
  sessionId.value = session.sessionUuid || session.id
  messages.value = []

  try {
    const res = await api.docQa.getSessionHistory(session.sessionUuid || session.id)
    if (res && res.length > 0) {
      messages.value = res.map((msg, idx) => ({
        id: idx,
        role: msg.role,
        content: msg.content,
        time: msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
      }))
    }
  } catch (e) {
    console.error('加载会话消息失败:', e)
    ElMessage.error('加载会话消息失败')
  }

  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
  ElMessage.success('已恢复会话')
}

onMounted(() => {
  loadKbList()
  if (route.query.kbId) {
    selectedKb.value = route.query.kbId
    currentKb.value = '已加载知识库'
  }
  loadSessions()
})

useKeyboardShortcuts([
  {
    match: (e) => matchShortcut(e, { ctrl: true, key: 'enter' }) && !loading.value,
    handler: () => handleAsk()
  },
  {
    match: (e) => e.key === '/' && !isInputFocused(),
    handler: () => {
      const input = document.querySelector('.chat-input input')
      if (input) input.focus()
    }
  }
])
</script>

<style lang="scss" scoped>
.doc-qa {
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
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }
}

.chat-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0;
    display: flex;
    flex-direction: column;
    height: 680px;
  }
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;

    .ai-avatar {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .header-info {
      display: flex;
      flex-direction: column;

      .ai-name {
        font-size: 15px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .ai-model {
        font-size: 12px;
        color: #10b981;
      }
    }
  }
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #fafbfc;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #e5e7eb;
    border-radius: 3px;
  }
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;

  .empty-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;

    .el-icon {
      font-size: 36px;
      color: #667eea;
    }
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  p {
    margin: 0 0 24px 0;
    color: var(--color-text-secondary);
    font-size: 14px;
    max-width: 360px;
  }

  .empty-suggestions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    justify-content: center;

    .suggestion-tag {
      cursor: pointer;
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 13px;
      background: rgba(102, 126, 234, 0.08);
      border: 1px solid rgba(102, 126, 234, 0.2);
      color: var(--color-text-secondary);
      transition: all 0.3s;

      &:hover {
        background: linear-gradient(135deg, #667eea, #764ba2);
        border-color: transparent;
        color: #fff;
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
      }
    }
  }
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message {
  display: flex;
  gap: 14px;
  animation: slideIn 0.3s ease;

  &.user {
    flex-direction: row-reverse;

    .message-bubble {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      border-radius: 20px 20px 4px 20px;

      .message-text {
        color: #fff;
      }

      .message-citations {
        background: rgba(255, 255, 255, 0.15);

        .citation-header {
          color: rgba(255, 255, 255, 0.8);
        }

        .citation-text {
          color: rgba(255, 255, 255, 0.9);
        }
      }

      .message-time {
        color: rgba(255, 255, 255, 0.6);
      }
    }
  }

  &.assistant {
    .message-bubble {
      background: rgba(102, 126, 234, 0.1);
      border: 1px solid rgba(102, 126, 234, 0.15);
      border-radius: 20px 20px 20px 4px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
    }
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;

  .user-avatar {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
  }

  .ai-avatar {
    background: linear-gradient(135deg, #10b981, #059669);
    border: none;
  }
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 16px 20px;
  line-height: 1.7;

  .message-text {
    color: var(--color-text-primary);
    font-size: 14px;
    white-space: pre-wrap;
  }

  .message-citations {
    margin-top: 12px;
    padding: 12px;
    background: #f9fafb;
    border-radius: 12px;

    .citation-header {
      display: flex;
      align-items: center;
      gap: 6px;
      color: var(--color-text-secondary);
      font-size: 12px;
      margin-bottom: 8px;
    }

    .citation-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 0;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        color: #667eea;
      }

      .citation-text {
        color: var(--color-text-secondary);
        font-size: 12px;
      }
    }
  }

  .message-time {
    font-size: 11px;
    color: var(--color-text-secondary);
    margin-top: 6px;
  }

  &.loading {
    display: flex;
    align-items: center;
    gap: 12px;

    .typing-indicator {
      display: flex;
      gap: 4px;

      span {
        width: 8px;
        height: 8px;
        background: #667eea;
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;

        &:nth-child(1) {
          animation-delay: -0.32s;
        }

        &:nth-child(2) {
          animation-delay: -0.16s;
        }
      }
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  padding: 20px 24px;
  border-top: 1px solid rgba(102, 126, 234, 0.12);
  background: rgba(19, 17, 28, 0.8);

  .input-wrapper {
    display: flex;
    gap: 12px;
    background: rgba(255, 255, 255, 0.06);
    border-radius: 16px;
    padding: 6px 6px 6px 20px;
    border: 2px solid rgba(102, 126, 234, 0.15);
    transition: all 0.3s;

    &:focus-within {
      border-color: rgba(102, 126, 234, 0.5);
      background: rgba(255, 255, 255, 0.08);
      box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.12);
    }

    :deep(.el-input__wrapper) {
      background: transparent;
      box-shadow: none;
      padding: 0;

      .el-input__inner {
        font-size: 15px;
      }
    }

    .send-btn {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
      color: #fff;
      transition: all 0.3s;

      &:hover:not(:disabled) {
        transform: scale(1.05);
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
      }

      &:disabled {
        opacity: 0.5;
      }
    }
  }

  .input-hints {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 10px;
    font-size: 12px;
    color: var(--color-text-secondary);

    .el-icon {
      font-size: 14px;
    }
  }
}

.suggested-questions {
  margin-top: 12px;
  padding: 12px 16px;
  background: #f0f4ff;
  border-radius: 12px;
  border: 1px dashed #c7d2fe;

  .suggested-label {
    font-size: 12px;
    color: #6366f1;
    font-weight: 600;
    margin-bottom: 8px;
  }

  :deep(.suggested-tag) {
    margin-right: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    background: rgba(102, 126, 234, 0.08);
    border: 1px solid rgba(102, 126, 234, 0.2);
    color: var(--color-primary-light);
    transition: all 0.2s;

    &:hover {
      background: rgba(102, 126, 234, 0.25);
      border-color: rgba(102, 126, 234, 0.4);
      color: #fff;
      transform: translateY(-1px);
    }
  }
}

.context-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);

  :deep(.el-card__body) {
    padding: 24px;
  }

  .panel-section {
    .section-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;

      .el-icon {
        font-size: 18px;
        color: #667eea;
      }

      h3 {
        margin: 0;
        font-size: 15px;
        font-weight: 600;
        color: var(--color-text-primary);
      }
    }
  }

  .current-kb {
    margin-bottom: 12px;
  }

  .switch-btn {
    width: 100%;
    border-radius: 10px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    border: none;
    color: #667eea;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
    }
  }
}

.session-list {
  .session-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: #f9fafb;
    border-radius: 12px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
      transform: translateX(4px);

      .session-arrow {
        opacity: 1;
        transform: translateX(0);
      }
    }

    &.active {
      background: linear-gradient(135deg, #667eea, #764ba2);

      .session-icon,
      .session-title,
      .session-date,
      .session-arrow {
        color: #fff;
      }
    }

    .session-detail-btn {
      padding: 4px;
      border-radius: 6px;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all 0.2s;
      &:hover {
        background: rgba(102, 126, 234, 0.1);
        color: #667eea;
      }
    }
    &.active .session-detail-btn { color: #fff; }
    &.active .session-detail-btn:hover { background: rgba(255, 255, 255, 0.2); color: #fff; }

    .session-continue-btn {
      padding: 4px;
      border-radius: 6px;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: all 0.2s;
      &:hover {
        background: rgba(102, 126, 234, 0.1);
        color: #667eea;
      }
    }
    &.active .session-continue-btn { color: #fff; }
    &.active .session-continue-btn:hover { background: rgba(255, 255, 255, 0.2); color: #fff; }

    .session-icon {
      width: 36px;
      height: 36px;
      background: rgba(102, 126, 234, 0.12);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--color-primary-light);
    }

    .session-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .session-title {
        font-size: 14px;
        font-weight: 500;
        color: var(--color-text-primary);
      }

      .session-date {
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }

    .session-arrow {
      color: var(--color-text-secondary);
      opacity: 0;
      transform: translateX(-10px);
      transition: all 0.3s;
    }
  }
}

.quick-questions {
  .quick-question {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px;
    background: #f9fafb;
    border-radius: 12px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.3s;

    .el-icon {
      color: #667eea;
      font-size: 16px;
    }

    span {
      font-size: 13px;
      color: var(--color-text-secondary);
      flex: 1;
    }

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));

      span {
        color: #667eea;
      }
    }
  }
}

:deep(.el-divider) {
  margin: 20px 0;
}

:deep(.kb-dialog) {
  .el-dialog {
    border-radius: 16px;
  }

  .el-dialog__header {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid #f3f4f6;
  }
}
</style>
