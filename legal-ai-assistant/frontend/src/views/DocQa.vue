<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI文件问答</h2>
      <p>基于上传的文档进行智能问答，支持多轮对话</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="16">
        <div class="chat-container">
          <div class="chat-header">
            <span>当前会话：{{ sessionTitle }}</span>
            <el-button text size="small" @click="clearHistory">清空对话</el-button>
          </div>

          <div class="chat-messages" ref="chatContainer">
            <div v-if="messages.length === 0" class="empty-chat">
              <el-empty description="开始对话吧，向我提问关于文档的问题">
                <template #image>
                  <el-icon :size="60"><ChatDotRound /></el-icon>
                </template>
              </el-empty>
            </div>

            <div v-else>
              <div
                v-for="(msg, index) in messages"
                :key="index"
                :class="['message', msg.role]"
              >
                <div class="message-avatar">
                  <el-avatar :size="32" :icon="msg.role === 'user' ? User : Robot" />
                </div>
                <div class="message-content">
                  <div class="message-bubble" v-html="formatContent(msg.content)"></div>
                  <div v-if="msg.citations?.length" class="message-citations">
                    <div class="citation-header">
                      <el-icon><Document /></el-icon> 参考来源
                    </div>
                    <div
                      v-for="(c, ci) in msg.citations"
                      :key="ci"
                      class="citation-item"
                      @click="scrollToCitation(c)"
                    >
                      <span class="citation-score">{{ (c.score * 100).toFixed(0) }}%</span>
                      <span class="citation-text">{{ c.content?.substring(0, 80) }}...</span>
                    </div>
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
                </div>
              </div>
            </div>

            <div v-if="loading" class="message assistant">
              <div class="message-avatar">
                <el-avatar :size="32" :icon="Robot" />
              </div>
              <div class="message-content">
                <div class="message-bubble loading">
                  <el-icon class="is-loading"><Loading /></el-icon>
                  <span>正在思考...</span>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <el-input
              v-model="question"
              placeholder="请输入问题，按Enter发送"
              :disabled="loading"
              @keyup.enter="handleAsk"
            >
              <template #suffix>
                <el-button
                  :icon="Promotion"
                  @click="handleAsk"
                  :disabled="!question.trim() || loading"
                />
              </template>
            </el-input>
            <div class="input-hints">
              <span>支持追问和上下文理解</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="context-panel">
          <div class="panel-section">
            <h3>
              <el-icon><Collection /></el-icon>
              当前知识库
            </h3>
            <div class="current-kb">
              <el-tag v-if="currentKb">{{ currentKb }}</el-tag>
              <el-tag v-else type="info">暂未选择知识库</el-tag>
            </div>
            <el-button size="small" @click="showKbSelector = true">切换知识库</el-button>
          </div>

          <div class="panel-section">
            <h3>
              <el-icon><ChatDotSquare /></el-icon>
              会话历史
            </h3>
            <div class="session-list">
              <div
                v-for="s in sessions"
                :key="s.id"
                :class="['session-item', { active: s.id === currentSession }]"
                @click="switchSession(s.id)"
              >
                <span class="session-title">{{ s.title }}</span>
                <span class="session-date">{{ s.date }}</span>
              </div>
            </div>
          </div>

          <div class="panel-section">
            <h3>
              <el-icon><QuestionFilled /></el-icon>
              快捷问题
            </h3>
            <div class="quick-questions">
              <div
                v-for="q in quickQuestions"
                :key="q"
                class="quick-question"
                @click="question = q; handleAsk()"
              >
                {{ q }}
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="showKbSelector" title="选择知识库" width="500px">
      <el-select v-model="selectedKb" placeholder="请选择知识库" style="width: 100%">
        <el-option label="劳动法法规库" value="KB-001" />
        <el-option label="合同纠纷案例" value="KB-002" />
        <el-option label="知识产权法规" value="KB-003" />
      </el-select>
      <template #footer>
        <el-button @click="showKbSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmKb">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'

const route = useRoute()
const question = ref('')
const messages = ref([])
const chatContainer = ref(null)
const currentSession = ref(null)
const sessionId = ref(null)
const sessionTitle = ref('新会话')
const currentKb = ref('劳动法法规库')
const showKbSelector = ref(false)
const selectedKb = ref('KB-001')
const loading = ref(false)

const sessions = ref([
  { id: 1, title: '劳动合同解除问题', date: '2024-06-10' },
  { id: 2, title: '竞业限制纠纷', date: '2024-06-08' },
  { id: 3, title: '合同欺诈认定', date: '2024-06-05' }
])

const quickQuestions = [
  '合同欺诈的构成要件是什么？',
  '用人单位可以单方解除劳动合同的情形有哪些？',
  '建设工程合同纠纷的管辖法院如何确定？',
  '借款合同利息的法律保护上限是多少？'
]

const formatContent = (content) => {
  if (!content) return ''
  return content
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

const handleAsk = async () => {
  if (!question.value.trim() || loading.value) return

  const userMsg = {
    id: Date.now(),
    role: 'user',
    content: question.value,
    time: new Date().toLocaleTimeString()
  }
  messages.value.push(userMsg)
  const q = question.value
  question.value = ''

  loading.value = true

  await nextTick()
  chatContainer.value.scrollTop = chatContainer.value.scrollHeight

  try {
    const res = await api.docQa.ask({
      question: q,
      sessionId: sessionId.value
    })
    sessionId.value = res.data.sessionId

    const aiMsg = {
      id: Date.now() + 1,
      role: 'assistant',
      content: res.data.answer,
      citations: res.data.citations,
      time: new Date().toLocaleTimeString()
    }
    messages.value.push(aiMsg)
  } catch (e) {
    console.error(e)
    ElMessage.error('回答生成失败，请稍后重试')
  } finally {
    loading.value = false
    await nextTick()
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

const clearHistory = () => {
  messages.value = []
  sessionId.value = null
  ElMessage.success('对话已清空')
}

const switchSession = (id) => {
  currentSession.value = id
  messages.value = []
  ElMessage.info('已切换会话')
}

const confirmKb = () => {
  currentKb.value = selectedKb.value
  showKbSelector.value = false
  ElMessage.success('已切换知识库')
}

const scrollToCitation = (c) => {
  ElMessage.info('定位到文档引用位置（待实现）')
}

onMounted(() => {
  if (route.query.kbId) {
    selectedKb.value = route.query.kbId
    currentKb.value = '已加载知识库'
  }
})
</script>

<style lang="scss" scoped>
.chat-container {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  height: 650px;
}

.chat-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
  border-radius: 8px 8px 0 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.empty-chat {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;

  &.user {
    flex-direction: row-reverse;
    .message-bubble {
      background: #1890ff;
      color: #fff;
      border-radius: 16px 16px 0 16px;
    }
  }

  &.assistant {
    .message-bubble {
      background: #f5f5f5;
      color: #333;
      border-radius: 16px 16px 16px 0;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 75%;
}

.message-bubble {
  padding: 12px 16px;
  line-height: 1.6;
  white-space: pre-wrap;

  &.loading {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.message-citations {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f5ff;
  border-radius: 8px;
  font-size: 12px;

  .citation-header {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #666;
    margin-bottom: 4px;
  }

  .citation-item {
    padding: 4px 0;
    cursor: pointer;
    &:hover {
      color: #1890ff;
    }
    .citation-score {
      color: #1890ff;
      margin-right: 8px;
    }
    .citation-text {
      color: #666;
    }
  }
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #f0f0f0;

  .input-hints {
    margin-top: 8px;
    font-size: 12px;
    color: #999;
  }
}

.context-panel {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  height: 650px;
  overflow-y: auto;

  .panel-section {
    margin-bottom: 24px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #666;
    }
  }
}

.current-kb {
  margin-bottom: 12px;
}

.session-list {
  .session-item {
    padding: 12px;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fff;
    border: 1px solid #f0f0f0;

    &:hover {
      border-color: #1890ff;
    }

    &.active {
      border-color: #1890ff;
      background: #e6f7ff;
    }

    .session-title {
      font-size: 14px;
    }

    .session-date {
      font-size: 12px;
      color: #999;
    }
  }
}

.quick-questions {
  .quick-question {
    padding: 10px 12px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #333;
    transition: all 0.3s;

    &:hover {
      border-color: #1890ff;
      color: #1890ff;
      background: #e6f7ff;
    }
  }
}
</style>