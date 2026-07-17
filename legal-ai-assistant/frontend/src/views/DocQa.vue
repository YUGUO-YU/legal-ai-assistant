<template>
  <div class="doc-qa">
    <!-- Left Sidebar: Knowledge Base + Sessions -->
    <aside class="kb-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <el-icon :size="20"><Document /></el-icon>
          <span>法律AI助手</span>
        </div>
      </div>

      <div class="sidebar-section">
        <div class="section-label">
          <el-icon><Collection /></el-icon>
          当前知识库
        </div>
        <div class="current-kb-badge" @click="showKbSelector = true">
          <el-tag v-if="currentKbName" type="success" effect="dark" round class="kb-tag">
            {{ currentKbName }}
          </el-tag>
          <el-tag v-else type="info" round>选择知识库</el-tag>
          <el-icon class="switch-arrow"><Right /></el-icon>
        </div>
        <el-button class="switch-btn" @click="showKbSelector = true">
          <el-icon><Switch /></el-icon>
          切换知识库
        </el-button>
      </div>

      <div class="sidebar-section sessions-section">
        <div class="section-label">
          <el-icon><ChatLineSquare /></el-icon>
          会话历史
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
            <div class="session-actions">
              <el-icon @click.stop="restoreSession(s)" title="继续对话"><VideoPlay /></el-icon>
              <el-icon @click.stop="goSessionDetail(s)" title="查看详情"><View /></el-icon>
            </div>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">
            暂无会话记录
          </div>
        </div>
      </div>
    </aside>

    <!-- Main Chat -->
    <main class="chat-main">
      <!-- Header -->
      <header class="chat-header">
        <div class="header-left">
          <div class="ai-avatar-header">
            <el-icon :size="18"><MagicStick /></el-icon>
          </div>
          <div class="header-info">
            <span class="ai-name">法律AI助手</span>
            <div class="ai-status">
              <span class="status-dot"></span>
              <span class="ai-model">MiniMax M3</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button type="primary" link @click="clearHistory" :disabled="messages.length === 0">
            <el-icon><Delete /></el-icon>
            清空对话
          </el-button>
        </div>
      </header>

      <!-- Error -->
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="true" @close="errorMsg = ''" class="chat-error">
        <template #default>
          <el-button size="small" type="primary" @click="errorMsg = ''; handleAsk()">重试</el-button>
        </template>
      </el-alert>

      <!-- Messages -->
      <div class="chat-messages" ref="chatContainer">
        <!-- Empty State -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-illustration">
            <svg width="120" height="120" viewBox="0 0 120 120" fill="none">
              <circle cx="60" cy="60" r="56" fill="url(#g1)" opacity="0.12"/>
              <rect x="30" y="38" width="60" height="44" rx="14" fill="url(#g1)" opacity="0.2"/>
              <rect x="40" y="52" width="28" height="4" rx="2" fill="#667eea" opacity="0.6"/>
              <rect x="40" y="62" width="18" height="4" rx="2" fill="#667eea" opacity="0.4"/>
              <circle cx="82" cy="76" r="10" fill="#10b981"/>
              <path d="M77 76l4 4 7-7" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <defs>
                <linearGradient id="g1" x1="0" y1="0" x2="120" y2="120">
                  <stop stop-color="#667eea"/>
                  <stop offset="1" stop-color="#764ba2"/>
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h3>开始智能问答</h3>
          <p>基于知识库文档进行法律问答，支持多轮对话与引用溯源</p>
          <div class="quick-questions">
            <div
              v-for="q in quickQuestions"
              :key="q"
              class="quick-chip"
              @click="question = q; handleAsk()"
            >
              <el-icon><Key /></el-icon>
              {{ q }}
            </div>
          </div>
        </div>

        <!-- Messages List -->
        <div v-else class="messages-list">
          <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['message-row', msg.role]"
          >
            <div class="msg-avatar">
              <el-avatar v-if="msg.role === 'user'" :size="36" class="ua">
                <el-icon><UserFilled /></el-icon>
              </el-avatar>
              <el-avatar v-else :size="36" class="aa">
                <el-icon><MagicStick /></el-icon>
              </el-avatar>
            </div>

            <div class="msg-body">
              <div :class="['msg-bubble', msg.role]">
                <div class="msg-text" v-html="sanitize(formatContent(msg.content))"></div>

                <!-- Citations -->
                <div v-if="msg.citations?.length" class="citations-panel">
                  <div class="citations-hdr">
                    <el-icon><Document /></el-icon>
                    <span>参考来源 ({{ msg.citations.length }})</span>
                  </div>
                  <div class="citations-grid">
                    <div
                      v-for="(c, ci) in msg.citations"
                      :key="ci"
                      class="citation-card"
                      @click="scrollToCitation(c)"
                    >
                      <div class="citation-top">
                        <span class="citation-label">{{ c.label || c.title || `来源 ${ci + 1}` }}</span>
                        <el-tag size="small" type="success" effect="plain" class="score-tag">
                          {{ Math.round((c.score > 1 ? c.score : c.score * 100)) }}% 相关
                        </el-tag>
                      </div>
                      <div class="citation-snippet">{{ c.content?.substring(0, 80) }}...</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="msg-meta">
                <span class="msg-time">{{ msg.time }}</span>
                <div v-if="msg.role === 'user'" class="msg-actions">
                  <el-icon @click="copyMessage(msg.content)" class="copy-icon"><CopyDocument /></el-icon>
                </div>
              </div>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="message-row assistant">
            <div class="msg-avatar">
              <el-avatar :size="36" class="aa">
                <el-icon><MagicStick /></el-icon>
              </el-avatar>
            </div>
            <div class="msg-body">
              <div class="msg-bubble assistant loading-bubble">
                <div class="typing-dots"><span></span><span></span><span></span></div>
                <span class="loading-text">正在分析文档...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Suggested Questions -->
        <div v-if="suggestedQuestions.length > 0 && lastAssistantMsg && !loading" class="suggested-bar">
          <span class="suggested-label">你可以继续问：</span>
          <div class="suggested-chips">
            <el-tag
              v-for="q in suggestedQuestions"
              :key="q"
              class="suggested-chip"
              @click="fillQuestion(q)"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- Input -->
      <footer class="chat-input-area">
        <div class="input-card">
          <el-input
            v-model="question"
            placeholder="输入法律问题，按 Enter 发送..."
            :disabled="loading"
            @keydown.enter.exact.prevent="handleAsk"
            @keydown.enter.ctrl.prevent="handleAsk"
            class="chat-input-el"
          />
          <el-button
            type="primary"
            class="send-btn"
            :disabled="!question.trim() || loading"
            @click="handleAsk"
          >
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
        <div class="input-footer">
          <el-icon><InfoFilled /></el-icon>
          AI仅供参考，具体法律问题请咨询专业律师
        </div>
      </footer>
    </main>

    <!-- KB Selector Dialog -->
    <el-dialog v-model="showKbSelector" title="选择知识库" width="520px" class="kb-dialog" align-center>
      <div class="kb-list">
        <div
          v-for="kb in kbList"
          :key="kb.id"
          :class="['kb-item', { selected: selectedKb === kb.id }]"
          @click="selectedKb = kb.id"
        >
          <div class="kb-item-icon">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="kb-item-info">
            <span class="kb-item-name">{{ kb.name }}</span>
            <span class="kb-item-desc">ID: {{ kb.id }}</span>
          </div>
          <el-icon v-if="selectedKb === kb.id" class="kb-check"><Check /></el-icon>
        </div>
      </div>
      <template #footer>
        <el-button @click="showKbSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmKb">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document, Collection, Right, Switch, ChatDotRound, ChatLineSquare,
  VideoPlay, View, Delete, MagicStick, UserFilled, Promotion,
  CopyDocument, InfoFilled, Key, Check
} from '@element-plus/icons-vue'
import api from '../api'
import { useStats } from '@/composables/useStats'
import { sanitizeHTML as sanitize } from '@/utils/sanitize'

const router = useRouter()
const route = useRoute()
const { increment } = useStats()

const question = ref('')
const messages = ref([])
const chatContainer = ref(null)
const currentSession = ref(null)
const sessionId = ref(null)
const currentKbName = ref('')
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

const quickQuestions = [
  '这份合同的主要条款有哪些？',
  '帮我分析这个协议的法律风险',
  '如何判断合同是否有效？',
  '合同违约应该怎么处理？'
]

onMounted(async () => {
  await loadKbList()
  await loadSessions()
})

const loadKbList = async () => {
  try {
    const res = await api.knowledgeBase.list()
    if (res && res.length > 0) {
      kbList.value = res.map(kb => ({ id: kb.id, name: kb.name }))
      if (!selectedKb.value) {
        selectedKb.value = kbList.value[0].id
        currentKbName.value = kbList.value[0].name
      }
    }
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  }
}

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
    ElMessage.error('加载会话列表失败')
  }
}

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n+/g, '<br/>')
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
}

const scrollToCitation = (citation) => {
  ElMessage.info(`查看: ${citation.label || citation.title || '参考来源'}`)
}

const fillQuestion = (q) => { question.value = q }

const copyMessage = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败')
  }
}

const generateSuggestions = async (q) => {
  if (!q) return
  suggestedQuestions.value = [
    `关于"${q}"还有哪些需要注意的？`,
    `${q}的法律依据是什么？`,
    '能否举例说明？',
    '在实践中如何应用？'
  ]
}

const handleAsk = async () => {
  const q = question.value.trim()
  if (!q || loading.value) return

  if (!currentKbName.value) {
    ElMessage.warning('请先选择知识库')
    showKbSelector.value = true
    return
  }

  question.value = ''
  loading.value = true
  errorMsg.value = ''
  suggestedQuestions.value = []

  messages.value.push({
    id: Date.now(), role: 'user', content: q,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    citations: []
  })

  await nextTick()
  chatContainer.value.scrollTop = chatContainer.value.scrollHeight

  const aiMsg = { id: Date.now() + 1, role: 'assistant', content: '', time: '', citations: [] }
  messages.value.push(aiMsg)

  try {
    const sid = sessionId.value ? `?sessionId=${sessionId.value}` : ''
    const response = await fetch(`/api/v1/doc-qa/ask/stream${sid}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        question: q,
        kbId: selectedKb.value,
        history: messages.value.filter(m => m.role === 'user').slice(0, 10).map(m => ({ role: m.role, content: m.content }))
      })
    })

    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const isNew = !sessionId.value
    sessionId.value = sessionId.value || `session-${Date.now()}`
    if (isNew) increment('sessionCount')

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let pending = false

    const flush = async () => {
      if (pending) {
        pending = false
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
          if (!pending) { pending = true; requestAnimationFrame(flush) }
        }
      }
    }

    await flush()
    generateSuggestions(q)
    await loadSessions()
  } catch (e) {
    aiMsg.content = '回答生成失败，请稍后重试'
    ElMessage.error('回答生成失败，请稍后重试')
    errorMsg.value = '网络错误，请检查网络连接'
  } finally {
    loading.value = false
    await nextTick()
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const clearHistory = async () => {
  if (sessionId.value) {
    try { await api.docQa.clearSession(sessionId.value) } catch (_) {}
  }
  messages.value = []
  sessionId.value = null
  suggestedQuestions.value = []
  ElMessage.success('对话已清空')
}

const switchSession = async (sid) => {
  currentSession.value = sid
  sessionId.value = sid
  messages.value = []
  try {
    const res = await api.docQa.getSessionHistory(sid)
    if (res && res.length > 0) {
      messages.value = res.map((msg, idx) => ({
        id: idx, role: msg.role, content: msg.content,
        time: msg.createdAt ? new Date(msg.createdAt).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }) : ''
      }))
    }
  } catch (e) {
    ElMessage.error('获取会话历史失败')
  }
  ElMessage.info('已切换会话')
}

const confirmKb = async () => {
  const sel = kbList.value.find(kb => kb.id === selectedKb.value)
  if (selectedKb.value !== currentKbName.value) {
    messages.value = []
    currentKbName.value = sel?.name || selectedKb.value
    sessionId.value = null
    currentSession.value = null
    suggestedQuestions.value = []
    await loadSessions()
  }
  showKbSelector.value = false
}

const restoreSession = async (s) => { await switchSession(s.id) }
const goSessionDetail = (s) => { router.push(`/qa-session/${s.sessionUuid || s.id}`) }
</script>

<style lang="scss" scoped>
.doc-qa {
  display: flex;
  height: 100%;
  background: var(--color-bg-page);
}

/* ===================== LEFT SIDEBAR ===================== */
.kb-sidebar {
  width: 272px;
  min-width: 272px;
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-border-glass);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border-glass);

  .sidebar-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 700;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
}

.sidebar-section {
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-glass);

  .section-label {
    display: flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    font-weight: 600;
    color: var(--color-text-muted);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 10px;

    .el-icon { font-size: 12px; }
  }
}

.current-kb-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  cursor: pointer;
  margin-bottom: 8px;
  transition: background var(--transition-base);

  &:hover { background: var(--color-bg-glass-hover); }

  .kb-tag { font-size: 12px; }
  .switch-arrow { font-size: 12px; color: var(--color-text-muted); }
}

.switch-btn {
  width: 100%;
  border-radius: var(--radius-md);
  background: var(--gradient-card);
  border: 1px solid var(--color-border-glass);
  color: var(--color-primary);
  font-size: 13px;
  transition: all var(--transition-base);

  &:hover {
    background: var(--gradient-primary);
    color: #fff;
    border-color: transparent;
  }
}

.sessions-section {
  flex: 1;
  overflow-y: auto;
  border-bottom: none;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--color-border-glass); border-radius: 2px; }
}

.session-list { display: flex; flex-direction: column; gap: 4px; }

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  position: relative;

  &:hover {
    background: var(--color-bg-glass-hover);
    .session-actions { opacity: 1; }
  }

  &.active {
    background: var(--gradient-primary);
    .session-icon, .session-title, .session-date { color: #fff; }
    .session-actions .el-icon { color: rgba(255,255,255,0.7); &:hover { color: #fff; background: rgba(255,255,255,0.15); } }
  }

  .session-icon {
    width: 30px; height: 30px;
    background: var(--color-bg-page);
    border-radius: var(--radius-sm);
    display: flex; align-items: center; justify-content: center;
    color: var(--color-primary);
    flex-shrink: 0;
  }

  .session-info {
    flex: 1; min-width: 0;
    .session-title { display: block; font-size: 13px; font-weight: 500; color: var(--color-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .session-date { display: block; font-size: 11px; color: var(--color-text-muted); margin-top: 1px; }
  }

  .session-actions {
    display: flex; gap: 2px;
    opacity: 0;
    transition: opacity var(--transition-fast);
    .el-icon { padding: 4px; border-radius: 4px; font-size: 13px; color: var(--color-text-secondary); cursor: pointer; transition: all var(--transition-fast); }
  }
}

.empty-sessions { text-align: center; padding: 20px 0; font-size: 13px; color: var(--color-text-muted); }

/* ===================== MAIN CHAT ===================== */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--color-bg-sidebar);
  border-bottom: 1px solid var(--color-border-glass);
  flex-shrink: 0;

  .header-left { display: flex; align-items: center; gap: 12px; }

  .ai-avatar-header {
    width: 38px; height: 38px;
    background: var(--gradient-primary);
    border-radius: var(--radius-md);
    display: flex; align-items: center; justify-content: center;
    color: #fff;
  }

  .header-info {
    .ai-name { display: block; font-size: 15px; font-weight: 600; color: var(--color-text-primary); }
    .ai-status { display: flex; align-items: center; gap: 5px; margin-top: 2px; }
    .status-dot { width: 7px; height: 7px; background: var(--color-success); border-radius: 50%; animation: pulse 2s infinite; }
    .ai-model { font-size: 12px; color: var(--color-text-muted); }
  }

  .header-actions .el-button { font-size: 13px; color: var(--color-text-secondary); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.chat-error { margin: 12px 24px 0; border-radius: var(--radius-md); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--color-bg-page);
  display: flex;
  flex-direction: column;

  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background: var(--color-border-glass); border-radius: 3px; }
}

/* Empty State */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px 20px;
  min-height: 400px;

  .empty-illustration { margin-bottom: 20px; }
  h3 { margin: 0 0 8px; font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
  p { margin: 0 0 24px; font-size: 14px; color: var(--color-text-secondary); max-width: 360px; }
}

.quick-questions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; max-width: 540px; }

.quick-chip {
  display: flex; align-items: center; gap: 6px;
  padding: 7px 14px;
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-full);
  font-size: 13px; color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-base);

  .el-icon { font-size: 12px; color: var(--color-primary); }

  &:hover {
    background: var(--gradient-card);
    border-color: var(--color-primary);
    color: var(--color-primary);
    transform: translateY(-2px);
    box-shadow: var(--shadow-glass);
  }
}

/* Messages */
.messages-list { display: flex; flex-direction: column; gap: 20px; }

.message-row {
  display: flex;
  gap: 12px;
  animation: slideUp 0.3s ease;

  &.user { flex-direction: row-reverse; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-avatar {
  flex-shrink: 0;
  .ua { background: var(--gradient-primary); border: none; }
  .aa { background: var(--color-bg-glass); border: 1px solid var(--color-border-glass); color: var(--color-primary); }
}

.msg-body { max-width: 72%; display: flex; flex-direction: column; gap: 5px; }

.msg-bubble {
  padding: 14px 18px;
  line-height: 1.75;

  &.user {
    background: var(--gradient-primary);
    border-radius: var(--radius-lg) var(--radius-sm) var(--radius-lg) var(--radius-lg);
    .msg-text { color: #fff; }
  }

  &.assistant {
    background: var(--color-bg-sidebar);
    border: 1px solid var(--color-border-glass);
    border-radius: var(--radius-sm) var(--radius-lg) var(--radius-lg) var(--radius-lg);
    box-shadow: var(--shadow-card);
    .msg-text { color: var(--color-text-primary); }
  }

  &.loading-bubble { display: flex; align-items: center; gap: 10px; }
}

.msg-text { font-size: 14px; white-space: pre-wrap; word-break: break-word; }

.loading-text { font-size: 13px; color: var(--color-text-muted); }

.typing-dots {
  display: flex; gap: 4px;
  span {
    width: 7px; height: 7px;
    background: var(--color-primary);
    border-radius: 50%;
    animation: bounceDot 1.4s infinite ease-in-out both;
    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
  }
}

@keyframes bounceDot {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* Citations */
.citations-panel {
  margin-top: 12px;
  padding: 12px;
  background: var(--color-bg-page);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-glass);

  .citations-hdr {
    display: flex; align-items: center; gap: 5px;
    font-size: 12px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px;
    .el-icon { color: var(--color-primary); }
  }

  .citations-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
    gap: 6px;
  }

  .citation-card {
    padding: 8px 10px;
    background: var(--color-bg-sidebar);
    border: 1px solid var(--color-border-glass);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all var(--transition-base);

    &:hover {
      border-color: var(--color-primary);
      box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
      transform: translateY(-1px);
    }

    .citation-top {
      display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px;
      .citation-label { font-size: 12px; font-weight: 600; color: var(--color-text-primary); }
      .score-tag { font-size: 10px; padding: 1px 5px; }
    }

    .citation-snippet { font-size: 11px; color: var(--color-text-muted); line-height: 1.4; }
  }
}

.msg-meta {
  display: flex; align-items: center; gap: 6px;
  padding: 0 2px;
  .msg-time { font-size: 11px; color: var(--color-text-muted); }
  .copy-icon { font-size: 13px; color: var(--color-text-muted); cursor: pointer; transition: color var(--transition-fast); &:hover { color: var(--color-primary); } }
}

/* Suggested */
.suggested-bar {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 12px 14px;
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-md);
  margin-top: 8px;
  flex-wrap: wrap;

  .suggested-label { font-size: 12px; color: var(--color-text-muted); white-space: nowrap; padding-top: 3px; }
  .suggested-chips { display: flex; flex-wrap: wrap; gap: 6px; }

  :deep(.suggested-chip) {
    cursor: pointer;
    padding: 3px 10px;
    border-radius: var(--radius-full);
    font-size: 12px;
    background: var(--color-bg-page);
    border: 1px solid var(--color-border-glass);
    color: var(--color-primary);
    transition: all var(--transition-fast);
    &:hover { background: var(--gradient-card); border-color: var(--color-primary); }
  }
}

/* Input */
.chat-input-area {
  padding: 14px 24px 18px;
  background: var(--color-bg-sidebar);
  border-top: 1px solid var(--color-border-glass);
  flex-shrink: 0;
}

.input-card {
  display: flex; gap: 10px; align-items: flex-end;
  background: var(--color-bg-page);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-lg);
  padding: 6px 6px 6px 14px;
  transition: all var(--transition-base);

  &:focus-within {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.08);
  }

  .chat-input-el {
    flex: 1;
    :deep(.el-input__wrapper) { background: transparent; box-shadow: none; padding: 5px 0; .el-input__inner { font-size: 14px; color: var(--color-text-primary); line-height: 1.6; } }
  }

  .send-btn {
    height: 36px; padding: 0 16px;
    border-radius: var(--radius-md);
    background: var(--gradient-primary); border: none; color: #fff;
    font-size: 14px; font-weight: 500;
    display: flex; align-items: center; gap: 5px;
    flex-shrink: 0;
    transition: all var(--transition-base);
    &:hover:not(:disabled) { opacity: 0.88; transform: translateY(-1px); box-shadow: 0 4px 12px rgba(102,126,234,0.3); }
    &:disabled { opacity: 0.45; cursor: not-allowed; }
  }
}

.input-footer {
  display: flex; align-items: center; gap: 5px;
  margin-top: 7px; font-size: 11px; color: var(--color-text-muted);
  .el-icon { font-size: 12px; }
}

/* KB Dialog */
.kb-dialog {
  :deep(.el-dialog) { border-radius: var(--radius-xl); }
}

.kb-list { display: flex; flex-direction: column; gap: 8px; max-height: 380px; overflow-y: auto; }

.kb-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px;
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover { border-color: var(--color-primary); background: var(--color-bg-glass-hover); }
  &.selected { border-color: var(--color-primary); background: rgba(102,126,234,0.05); }

  .kb-item-icon { width: 34px; height: 34px; background: var(--gradient-card); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; color: var(--color-primary); }
  .kb-item-info { flex: 1; .kb-item-name { display: block; font-size: 14px; font-weight: 500; color: var(--color-text-primary); } .kb-item-desc { display: block; font-size: 11px; color: var(--color-text-muted); margin-top: 2px; } }
  .kb-check { color: var(--color-primary); font-size: 15px; }
}

/* Code */
:deep(.code-block) {
  background: #1e1b4b; border-radius: var(--radius-md); padding: 12px 14px; margin: 8px 0; overflow-x: auto;
  code { color: #e2e8f0; font-size: 13px; font-family: 'JetBrains Mono', monospace; line-height: 1.6; }
}
:deep(.inline-code) { background: rgba(102,126,234,0.1); color: var(--color-primary); padding: 1px 5px; border-radius: 4px; font-size: 13px; font-family: monospace; }
</style>
