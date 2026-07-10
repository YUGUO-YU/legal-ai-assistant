<template>
  <div class="dashboard">
    <div class="page-header dashboard-header">
      <div class="header-content">
        <div class="greeting-area">
          <h1 class="greeting-text">{{ greeting }}，{{ username }}</h1>
          <p class="greeting-sub">{{ currentDate }} · {{ greetingTip }}</p>
        </div>
        <div class="header-decoration">
          <div class="decoration-circle circle-1"></div>
          <div class="decoration-circle circle-2"></div>
          <div class="decoration-circle circle-3"></div>
        </div>
      </div>
    </div>

    <el-alert
      v-if="showExpiredNotice"
      :title="expiredNoticeMessage"
      type="warning"
      show-icon
      :closable="true"
      @close="showExpiredNotice = false"
      style="margin-bottom: 20px;"
    />

    <el-row :gutter="24" class="stats-row" v-loading="statsLoading">
      <el-col :span="6" v-for="(stat, index) in statsData" :key="index">
        <el-card class="stat-card card-hover" :class="stat.class" @click="goTo(stat.path)">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <el-icon :size="24"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
              <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
                <el-icon v-if="stat.trend > 0"><Top /></el-icon>
                <el-icon v-else><Bottom /></el-icon>
                <span>{{ Math.abs(stat.trend) }}%</span>
              </div>
            </div>
            <el-icon class="stat-arrow"><Right /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="content-row">
      <el-col :span="16">
        <el-card class="quick-access">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Lightning /></el-icon>
                <span>快捷入口</span>
              </div>
              <el-tag type="success" size="small">常用功能</el-tag>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="8" v-for="item in quickAccess" :key="item.path">
              <div class="quick-item card-hover" @click="$router.push(item.path)">
                <div class="quick-icon" :style="{ background: item.gradient, boxShadow: item.shadow }">
                  <el-icon :size="24"><component :is="item.icon" /></el-icon>
                </div>
                <div class="quick-text">
                  <span class="quick-title">{{ item.title }}</span>
                  <span class="quick-desc">{{ item.desc }}</span>
                </div>
                <el-icon class="quick-arrow"><Right /></el-icon>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card class="recent-activity" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Clock /></el-icon>
                <span>最近活动</span>
              </div>
              <el-button type="primary" link size="small" @click="loadMore">
                查看全部
                <el-icon><Right /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item stagger-item">
              <div class="activity-icon" :style="{ background: activity.gradient }">
                <el-icon><component :is="activity.icon" /></el-icon>
              </div>
              <div class="activity-info">
                <span class="activity-title">{{ activity.title }}</span>
                <span class="activity-desc">{{ activity.desc }}</span>
              </div>
              <div class="activity-time">
                <span>{{ activity.time }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="hot-topics">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><TrendCharts /></el-icon>
                <span>热门检索</span>
              </div>
              <el-tag type="warning" size="small">实时更新</el-tag>
            </div>
          </template>
          <div class="topic-list">
            <div v-for="(topic, index) in hotTopics" :key="topic.title" class="topic-item">
              <div class="topic-rank" :class="{ top: index < 3 }">
                {{ index + 1 }}
              </div>
              <div class="topic-info">
                <span class="topic-title">{{ topic.title }}</span>
                <el-progress
                  :percentage="topic.percentage"
                  :stroke-width="4"
                  :show-text="false"
                  :color="topic.color"
                />
              </div>
              <span class="topic-count">{{ topic.count }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="tips-card" style="margin-top: 24px">
          <template #header>
            <div class="header-title">
              <el-icon><Star /></el-icon>
              <span>使用技巧</span>
            </div>
          </template>
          <div class="tips-list">
            <div v-for="tip in tips" :key="tip.title" class="tip-item">
              <div class="tip-icon" :style="{ background: tip.gradient }">
                <el-icon><component :is="tip.icon" /></el-icon>
              </div>
              <div class="tip-info">
                <span class="tip-title">{{ tip.title }}</span>
                <span class="tip-desc">{{ tip.desc }}</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="ai-status-card" style="margin-top: 24px">
          <div class="ai-status">
            <div class="ai-status-header">
              <div class="ai-status-icon">
                <el-icon :size="24"><MagicStick /></el-icon>
              </div>
              <div class="ai-status-info">
                <span class="ai-status-title">大模型状态</span>
                <span class="ai-status-model">{{ aiStatusData.model }}</span>
              </div>
              <div class="ai-status-actions">
                <div class="status-light" :class="lightClass" :title="lightTooltip">
                  <span class="light-dot" :class="lightClass"></span>
                  <span class="light-text">{{ lightText }}</span>
                </div>
              </div>
            </div>
            <div class="ai-status-stats">
              <div class="status-item">
                <span class="status-value">{{ aiStatusData.status === 'online' ? '正常' : '不可用' }}</span>
                <span class="status-label">服务状态</span>
              </div>
              <div class="status-item">
                <span class="status-value">{{ aiStatusData.status === 'checking' ? '...' : aiStatusData.message }}</span>
                <span class="status-label">详细信息</span>
              </div>
              <div class="status-item">
                <span class="status-value">{{ aiStatusData.baseUrl || '-' }}</span>
                <span class="status-label">接入地址</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="detail-cards-row" style="margin-top: 24px">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><DataBoard /></el-icon>
            <span>功能详情卡</span>
          </div>
          <el-tag type="info" size="small">点击查看详情</el-tag>
        </div>
      </template>
      <div class="detail-cards-grid">
        <div class="detail-card card-hover" :class="card.class" @click="openDetail(card)" @contextmenu.prevent="handleFeatureContextMenu(card, $event)" v-for="card in detailCards.slice(0, 4)" :key="card.key">
          <div class="detail-card-header">
            <div class="detail-card-icon" :style="{ background: card.gradient }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <el-tag size="small" :type="card.tagType" effect="plain">{{ card.status }}</el-tag>
          </div>
          <div class="detail-card-body">
            <span class="detail-card-title">{{ card.title }}</span>
            <span class="detail-card-desc">{{ card.desc }}</span>
          </div>
          <div class="detail-card-footer">
            <span class="detail-card-meta">{{ card.meta }}</span>
            <el-icon class="detail-card-arrow"><Right /></el-icon>
          </div>
        </div>
      </div>
      <div class="detail-cards-grid" style="margin-top: 20px;">
        <div class="detail-card card-hover" :class="card.class" @click="openDetail(card)" @contextmenu.prevent="handleFeatureContextMenu(card, $event)" v-for="card in detailCards.slice(4, 8)" :key="card.key">
          <div class="detail-card-header">
            <div class="detail-card-icon" :style="{ background: card.gradient }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <el-tag size="small" :type="card.tagType" effect="plain">{{ card.status }}</el-tag>
          </div>
          <div class="detail-card-body">
            <span class="detail-card-title">{{ card.title }}</span>
            <span class="detail-card-desc">{{ card.desc }}</span>
          </div>
          <div class="detail-card-footer">
            <span class="detail-card-meta">{{ card.meta }}</span>
            <el-icon class="detail-card-arrow"><Right /></el-icon>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="usage-memory-card" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Operation /></el-icon>
                <span>使用记忆</span>
              </div>
              <div class="header-actions">
                <el-tag v-if="memoryCount > 0" type="info" size="small">{{ memoryCount }} 条</el-tag>
                <el-button v-if="memoryCount > 0" type="danger" size="small" link @click="handleClearMemory">清空</el-button>
              </div>
            </div>
          </template>
          <div v-if="justCleared" class="cleared-notice">
            <el-icon><CircleCheck /></el-icon>
            <span>使用记忆已清空</span>
          </div>
          <div v-else-if="memoryCount === 0" class="empty-memory">
            <el-icon><Clock /></el-icon>
            <span>暂无使用记录</span>
            <p>搜索、起草文书、审查合同等操作会被自动记录</p>
          </div>
          <div v-else class="memory-list">
            <div v-for="group in memoryGroups" :key="group.date" class="memory-group">
              <div class="memory-date">{{ formatMemoryDate(group.date) }}</div>
              <div
                v-for="item in group.items"
                :key="item.id"
                class="memory-item"
                @click="handleMemoryClick(item)"
              >
                <div class="memory-item-icon" :style="{ background: getTypeColor(item.type) + '22' }">
                  <el-icon><component :is="getTypeIcon(item.type)" /></el-icon>
                </div>
                <div class="memory-item-info">
                  <span class="memory-item-title">{{ item.title }}</span>
                  <span class="memory-item-desc">{{ item.desc }}</span>
                </div>
                <span class="memory-item-time">{{ formatAge(item.timestamp) }}</span>
                <el-button link size="small" type="danger" @click.stop="handleRemoveMemory(item.id)">
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-card>

    <el-drawer
      v-model="detailDrawerVisible"
      :title="activeDetailCard?.title"
      direction="rtl"
      size="560px"
      :destroy-on-close="true"
      class="drawer-slide-right"
    >
      <div v-if="activeDetailCard" class="detail-drawer">
        <div class="drawer-banner" :style="{ background: activeDetailCard.gradient }">
          <el-icon :size="40"><component :is="activeDetailCard.icon" /></el-icon>
          <div>
            <h3>{{ activeDetailCard.title }}</h3>
            <p>{{ activeDetailCard.desc }}</p>
          </div>
        </div>
        <div class="drawer-section">
          <h4>运行指标</h4>
          <el-row :gutter="12">
            <el-col :span="8" v-for="m in activeDetailCard.metrics" :key="m.label">
              <div class="metric-card">
                <span class="metric-value">{{ m.value }}</span>
                <span class="metric-label">{{ m.label }}</span>
              </div>
            </el-col>
          </el-row>
        </div>
        <div class="drawer-section">
          <h4>最新动态</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(event, idx) in activeDetailCard.events"
              :key="idx"
              :timestamp="event.time"
              :type="event.type"
              :hollow="idx !== 0"
              placement="top"
            >
              <div class="event-card">
                <strong>{{ event.title }}</strong>
                <p>{{ event.desc }}</p>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
        <div class="drawer-section">
          <h4>快捷操作</h4>
          <div class="drawer-actions">
            <el-button
              v-for="(action, idx) in activeDetailCard.actions"
              :key="idx"
              :type="action.primary ? 'primary' : 'default'"
              :icon="action.icon"
              @click="onDrawerAction(action)"
            >
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>

    <ContextMenu
      v-model:visible="featureContextMenu.visible"
      :x="featureContextMenu.x"
      :y="featureContextMenu.y"
      :menus="featureContextMenu.menus"
      @select="featureContextMenu.handleSelect"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  Files,
  Document,
  ChatDotRound,
  Clock,
  CircleCheck,
  Top,
  Bottom,
  Right,
  Lightning,
  TrendCharts,
  Star,
  MagicStick,
  Connection,
  DocumentCopy,
  Collection,
  Stamp,
  OfficeBuilding,
  Box,
  QuestionFilled,
  ChatLineSquare,
  Coin,
  DataBoard,
  DataAnalysis,
  FolderOpened,
  EditPen,
  View,
  Operation,
  Close
} from '@element-plus/icons-vue'

import { useUsageMemory } from '@/composables/useUsageMemory'
import ContextMenu from '@/components/common/ContextMenu.vue'
import { useContextMenu } from '@/composables/useContextMenu'

const {
  groupByDate: memoryGroups,
  justCleared,
  records: memoryRecords,
  clearAll,
  removeRecord,
  formatAge,
  getTypeColor,
  checkAndClean,
  cleanedCount
} = useUsageMemory()

const memoryCount = computed(() => memoryRecords.value.length)
const showExpiredNotice = ref(false)
const expiredNoticeMessage = ref('')

function handleClearMemory() {
  clearAll()
}

function handleRemoveMemory(id) {
  removeRecord(id)
}

function handleMemoryClick(item) {
  const pathMap = {
    search: '/legal-search',
    document: '/document',
    contract: '/contract-review',
    company: '/company-search',
    docqa: '/doc-qa',
    law: '/law-search',
    case: '/case-search',
    ppt: '/ppt',
    other: '/dashboard'
  }
  router.push(pathMap[item.type] || '/dashboard')
}

function getTypeIcon(type) {
  const map = {
    search: 'Search',
    document: 'DocumentCopy',
    contract: 'Stamp',
    company: 'OfficeBuilding',
    docqa: 'ChatDotRound',
    law: 'Collection',
    case: 'Files',
    ppt: 'Coin',
    other: 'Clock'
  }
  return map[type] || 'Clock'
}

function formatMemoryDate(dateStr) {
  const today = new Date()
  const [y, m, d] = dateStr.split('-').map(Number)
  const target = new Date(y, m - 1, d)
  const diff = Math.floor((today - target) / 86400000)
  if (diff === 0) return '今天'
  if (diff === 1) return '昨天'
  if (diff === 2) return '前天'
  return dateStr
}

onMounted(() => {
  loadStats()
  const cleaned = checkAndClean()
  if (cleaned && cleanedCount.value > 0) {
    expiredNoticeMessage.value = `已自动清理 ${cleanedCount.value} 条过期记忆`
    showExpiredNotice.value = true
    setTimeout(() => {
      showExpiredNotice.value = false
    }, 5000)
  }
})

const statsData = reactive([
  {
    icon: 'Search',
    value: '156',
    label: '检索次数',
    trend: 12,
    gradient: 'linear-gradient(135deg, #667eea, #764ba2)',
    shadow: '0 8px 20px rgba(102, 126, 234, 0.35)',
    class: 'stat-purple',
    path: '/'
  },
  {
    icon: 'Connection',
    value: '89',
    label: '类案分析',
    trend: 8,
    gradient: 'linear-gradient(135deg, #f093fb, #f5576c)',
    shadow: '0 8px 20px rgba(245, 87, 108, 0.35)',
    class: 'stat-pink',
    path: '/case-similar'
  },
  {
    icon: 'DocumentCopy',
    value: '45',
    label: '文书起草',
    trend: -3,
    gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    shadow: '0 8px 20px rgba(79, 172, 254, 0.35)',
    class: 'stat-blue',
    path: '/document'
  },
  {
    icon: 'ChatDotRound',
    value: '23',
    label: '会话数',
    trend: 25,
    gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)',
    shadow: '0 8px 20px rgba(67, 233, 123, 0.35)',
    class: 'stat-green',
    path: '/doc-qa'
  }
])

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await fetch('/api/v1/user/stats')
    if (!res.ok) throw new Error('not ok')
    const data = await res.json()
    statsData[0].value = String(data.searchCount ?? statsData[0].value)
    statsData[1].value = String(data.sessionCount ?? statsData[1].value)
    activeDays.value = data.activeDays ?? activeDays.value
    if (data.recentActivities?.length) {
      recentActivities.value = data.recentActivities
    }
  } catch {
    const mockData = {
      searchCount: 156,
      sessionCount: 89,
      activeDays: 5,
      efficiencyRate: 32,
      recentActivities: [
        { id: 1, title: '检索"合同欺诈认定"', desc: '找到了 12 条相关法规和 8 个类案', time: '10分钟前', icon: 'Search', gradient: 'rgba(102, 126, 234, 0.15)' },
        { id: 2, title: '起草"民事起诉状"', desc: '已生成起诉状模板', time: '30分钟前', icon: 'DocumentCopy', gradient: 'rgba(79, 172, 254, 0.15)' },
        { id: 3, title: '审查"采购合同"', desc: '发现 3 处风险条款', time: '1小时前', icon: 'Stamp', gradient: 'rgba(161, 140, 209, 0.15)' }
      ]
    }
    statsData[0].value = String(mockData.searchCount)
    statsData[1].value = String(mockData.sessionCount)
    activeDays.value = mockData.activeDays
    recentActivities.value = mockData.recentActivities
  } finally {
    statsLoading.value = false
  }
}

const username = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).nickname || JSON.parse(userInfo).username || '用户'
  }
  return '用户'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '上午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const greetingTip = computed(() => {
  const tips = [
    '新的一天，新的开始',
    '保持专注，高效工作',
    '法律助手，随时待命',
    '今天有哪些案件需要处理？'
  ]
  return tips[Math.floor(Math.random() * tips.length)]
})

const currentDate = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const statsLoading = ref(false)
const activeDays = ref(5)

const today = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const quickAccess = [
  { path: '/legal-search', title: 'AI搜法', desc: '法规检索与溯源', icon: 'Search', gradient: 'linear-gradient(135deg, #667eea, #764ba2)', shadow: '0 8px 20px rgba(102, 126, 234, 0.35)' },
  { path: '/case-similar', title: 'AI类案', desc: '相似案例匹配', icon: 'Connection', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', shadow: '0 8px 20px rgba(245, 87, 108, 0.35)' },
  { path: '/case-search', title: '案例搜索', desc: '判例检索分析', icon: 'Files', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', shadow: '0 8px 20px rgba(250, 112, 154, 0.35)' },
  { path: '/law-search', title: '法规搜索', desc: '法律法规查询', icon: 'Collection', gradient: 'linear-gradient(135deg, #ff9a56, #ff6a00)', shadow: '0 8px 20px rgba(255, 154, 0, 0.35)' },
  { path: '/document', title: 'AI文书', desc: '法律文书起草', icon: 'DocumentCopy', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', shadow: '0 8px 20px rgba(79, 172, 254, 0.35)' },
  { path: '/contract-review', title: 'AI审查', desc: '合同风险分析', icon: 'Stamp', gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', shadow: '0 8px 20px rgba(161, 140, 209, 0.35)' },
  { path: '/data-manager', title: '数据管理', desc: 'AI 导入法律法规', icon: 'DataAnalysis', gradient: 'linear-gradient(135deg, #11998e, #38ef7d)', shadow: '0 8px 20px rgba(17, 153, 142, 0.35)' }
]

const recentActivities = computed(() => {
  return memoryRecords.value.slice(0, 10).map(record => ({
    id: record.id,
    title: record.title,
    desc: record.desc,
    time: formatAge(record.timestamp),
    icon: getTypeIcon(record.type),
    gradient: `${getTypeColor(record.type)}25`
  }))
})

const hotTopics = ref([
  { title: '合同欺诈认定', count: 1256, percentage: 95, color: '#ff4d4f' },
  { title: '劳动仲裁流程', count: 987, percentage: 75, color: '#ff7a45' },
  { title: '民间借贷利息', count: 856, percentage: 65, color: '#ffa940' },
  { title: '建设工程优先权', count: 743, percentage: 56, color: '#ffd666' },
  { title: '商标侵权赔偿', count: 621, percentage: 47, color: '#95de64' }
])

const tips = [
  { title: '智能提示', desc: '搜索时使用自然语言，系统会自动匹配', icon: 'MagicStick', gradient: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { title: '追问建议', desc: '检索结果底部有追问推荐，帮助深入', icon: 'ChatLineSquare', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { title: '类案参考', desc: 'AI类案功能帮您了解类似案件的判决', icon: 'Connection', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
]

const aiStatusData = ref({
  status: 'checking',
  model: 'MiniMax-M3',
  baseUrl: '',
  message: '检测中...'
})

const lightClass = computed(() => {
  return aiStatusData.value.status === 'online' ? 'light-green' : 'light-red'
})

const lightText = computed(() => {
  return aiStatusData.value.status === 'online' ? '在线' : '离线'
})

const lightTooltip = computed(() => {
  const m = aiStatusData.value.message || lightText.value
  return `状态: ${lightText.value}\n${m}\n接入: ${aiStatusData.value.baseUrl || '-'}\n模型: ${aiStatusData.value.model}`
})

const loadAiStatus = async () => {
  try {
    const res = await fetch('/api/v1/ai-status')
    const data = await res.json()
    const status = data.status === 'online' ? 'online' : 'offline'
    aiStatusData.value = {
      status,
      model: data.model || 'MiniMax-M3',
      baseUrl: data.baseUrl || '',
      message: data.message || (status === 'online' ? 'AI 服务在线' : 'AI 服务离线')
    }
    console.log('[AI Status]', aiStatusData.value)
  } catch (e) {
    aiStatusData.value = {
      status: 'offline',
      model: 'MiniMax-M3',
      baseUrl: '',
      message: '无法连接后端: ' + (e?.message || e)
    }
    console.warn('[AI Status] 请求失败', e)
  }
}

onMounted(() => {
  loadAiStatus()
  setInterval(loadAiStatus, 15000)
})

const detailDrawerVisible = ref(false)
const activeDetailCard = ref(null)

const detailCards = [
  {
    key: 'legal-search',
    title: 'AI 搜法',
    desc: '法规检索与引用溯源',
    status: '运行中',
    tagType: 'success',
    meta: '本周 156 次检索',
    icon: 'Search',
    gradient: 'linear-gradient(135deg, #667eea, #764ba2)',
    class: 'card-purple',
    metrics: [
      { label: '本月检索', value: '632' },
      { label: '引用次数', value: '1,284' },
      { label: '平均耗时', value: '1.2s' }
    ],
    events: [
      { time: '刚刚', title: '检索"合同欺诈认定"', desc: '找到 12 条法规，3 个相关案例', type: 'primary' },
      { time: '2 小时前', title: '新增民法典司法解释 4 条', desc: '系统已自动更新索引', type: 'success' },
      { time: '昨天', title: '引用溯源升级', desc: '支持多层级跳转', type: 'info' }
    ],
    actions: [
      { label: '立即检索', icon: 'Search', primary: true, path: '/' },
      { label: '查看历史', icon: 'Clock', path: '/dashboard' }
    ]
  },
  {
    key: 'case-similar',
    title: 'AI 类案',
    desc: '相似案例智能匹配',
    status: '运行中',
    tagType: 'success',
    meta: '本周 89 次分析',
    icon: 'Connection',
    gradient: 'linear-gradient(135deg, #f093fb, #f5576c)',
    class: 'card-pink',
    metrics: [
      { label: '类案匹配', value: '89' },
      { label: '要素提取', value: '320 条' },
      { label: '准确率', value: '92%' }
    ],
    events: [
      { time: '30 分钟前', title: '类案检索"装修合同纠纷"', desc: '匹配到 5 个高度相似案例', type: 'primary' },
      { time: '昨天', title: '新增 1.2 万条判例数据', desc: '覆盖 2024 年新案件', type: 'success' }
    ],
    actions: [
      { label: '类案检索', icon: 'Connection', primary: true, path: '/case-similar' },
      { label: '判例查询', icon: 'Files', path: '/case-search' }
    ]
  },
  {
    key: 'document',
    title: 'AI 文书起草',
    desc: '法律文书智能生成',
    status: '运行中',
    tagType: 'success',
    meta: '本周 45 份文书',
    icon: 'DocumentCopy',
    gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    class: 'card-blue',
    metrics: [
      { label: '本周起草', value: '45' },
      { label: '模板数量', value: '20' },
      { label: '采纳率', value: '87%' }
    ],
    events: [
      { time: '1 小时前', title: '起草"民事起诉状"', desc: '已生成 4 段待编辑内容', type: 'primary' },
      { time: '昨天', title: '新增 3 个文书模板', desc: '劳动仲裁、证据清单、答辩状', type: 'success' }
    ],
    actions: [
      { label: '起草文书', icon: 'EditPen', primary: true, path: '/document' },
      { label: '查看模板', icon: 'Files', path: '/document' }
    ]
  },
  {
    key: 'contract-review',
    title: 'AI 合同审查',
    desc: '8 维度风险评估',
    status: '运行中',
    tagType: 'success',
    meta: '本周 28 份合同',
    icon: 'Stamp',
    gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)',
    class: 'card-violet',
    metrics: [
      { label: '本月审查', value: '108' },
      { label: '高风险', value: '17 项' },
      { label: '平均分', value: '76' }
    ],
    events: [
      { time: '3 小时前', title: '审查"采购合同"', desc: '识别 3 处高风险条款', type: 'danger' },
      { time: '昨天', title: '维度模型升级', desc: '新增个人信息审查维度', type: 'success' }
    ],
    actions: [
      { label: '开始审查', icon: 'Stamp', primary: true, path: '/contract-review' }
    ]
  },
  {
    key: 'company',
    title: '企业查询',
    desc: '工商/股东/风险',
    status: '运行中',
    tagType: 'success',
    meta: '本周 64 次查询',
    icon: 'OfficeBuilding',
    gradient: 'linear-gradient(135deg, #fa709a, #fee140)',
    class: 'card-orange',
    metrics: [
      { label: '本月查询', value: '264' },
      { label: '风险预警', value: '18' },
      { label: '数据源', value: '3' }
    ],
    events: [
      { time: '2 小时前', title: '查询"北京某科技公司"', desc: '风险等级：中等', type: 'warning' }
    ],
    actions: [
      { label: '立即查询', icon: 'OfficeBuilding', primary: true, path: '/company' }
    ]
  },
  {
    key: 'knowledge-base',
    title: '案例法规库',
    desc: '团队共享知识管理',
    status: '运行中',
    tagType: 'success',
    meta: '已上传 32 份文档',
    icon: 'Box',
    gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)',
    class: 'card-green',
    metrics: [
      { label: '知识库', value: '5' },
      { label: '文档', value: '32' },
      { label: '问答', value: '128' }
    ],
    events: [
      { time: '昨天', title: '上传《劳动争议司法解释》', desc: '已分块 142 段、向量化完成', type: 'success' }
    ],
    actions: [
      { label: '进入知识库', icon: 'Box', primary: true, path: '/knowledge-base' },
      { label: '文件问答', icon: 'ChatDotRound', path: '/doc-qa' }
    ]
  },
  {
    key: 'legal-research',
    title: 'AI 法律研究',
    desc: '结构化研究报告',
    status: '运行中',
    tagType: 'success',
    meta: '本周 12 篇报告',
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #ff9a56, #ff6a00)',
    class: 'card-deeporange',
    metrics: [
      { label: '本月报告', value: '47' },
      { label: '平均字数', value: '2,800' },
      { label: '引用条数', value: '38' }
    ],
    events: [
      { time: '4 小时前', title: '研究"工期延误索赔"', desc: '6 段式报告已生成', type: 'primary' }
    ],
    actions: [
      { label: '开始研究', icon: 'TrendCharts', primary: true, path: '/legal-research' }
    ]
  },
  {
    key: 'ppt',
    title: 'PPT 演讲稿',
    desc: '一键生成法律 PPT',
    status: '运行中',
    tagType: 'success',
    meta: '已生成 8 个 PPT',
    icon: 'Memo',
    gradient: 'linear-gradient(135deg, #5ee7df, #b490ca)',
    class: 'card-cyan',
    metrics: [
      { label: '本月生成', value: '24' },
      { label: '模板数量', value: '6' },
      { label: '下载次数', value: '76' }
    ],
    events: [
      { time: '昨天', title: '生成"建设工程纠纷"PPT', desc: '18 页，模板：商务蓝', type: 'success' }
    ],
    actions: [
      { label: '新建 PPT', icon: 'EditPen', primary: true, path: '/ppt-editor' },
      { label: '文件管理', icon: 'FolderOpened', path: '/ppt-files' }
    ]
  }
]

const router = useRouter()

const goTo = (path) => {
  if (path) router.push(path)
}

const openDetail = (card) => {
  activeDetailCard.value = card
  detailDrawerVisible.value = true
}

const onDrawerAction = (action) => {
  if (action.path) {
    detailDrawerVisible.value = false
    router.push(action.path)
  }
}

const loadMore = () => {
  console.log('load more activities')
}

const openInNewWindow = (card) => {
  window.open(card.path || '/', '_blank')
}

const toggleFavorite = (card) => {
  ElMessage.success(`已收藏：${card.title}`)
}

const showUsageStats = (card) => {
  ElMessage.info(`查看使用统计：${card.title}`)
}

const featureContextMenu = reactive({
  ...useContextMenu(),
  menus: [
    {
      label: '新窗口打开',
      icon: 'el-icon-right',
      action: (card) => openInNewWindow(card)
    },
    {
      label: '收藏',
      icon: 'el-icon-star',
      action: (card) => toggleFavorite(card)
    },
    {
      label: '',
      divided: true
    },
    {
      label: '查看使用统计',
      icon: 'el-icon-data-analysis',
      action: (card) => showUsageStats(card)
    }
  ]
})

const handleFeatureContextMenu = (card, event) => {
  featureContextMenu.show(event, card, featureContextMenu.menus)
}
</script>

<style lang="scss" scoped>
.dashboard {
  animation: fadeIn 0.5s ease;
}

.stat-card {
  opacity: 0;
  transform: translateY(20px);
  animation: cardFadeInUp 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards;

  &:nth-child(1) { animation-delay: 0.05s; }
  &:nth-child(2) { animation-delay: 0.1s; }
  &:nth-child(3) { animation-delay: 0.15s; }
  &:nth-child(4) { animation-delay: 0.2s; }
}

@keyframes cardFadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.quick-item {
  opacity: 0;
  transform: translateX(-20px);
  animation: quickItemFadeIn 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;

  &:nth-child(1) { animation-delay: 0.25s; }
  &:nth-child(2) { animation-delay: 0.3s; }
  &:nth-child(3) { animation-delay: 0.35s; }
  &:nth-child(4) { animation-delay: 0.4s; }
  &:nth-child(5) { animation-delay: 0.45s; }
  &:nth-child(6) { animation-delay: 0.5s; }
  &:nth-child(7) { animation-delay: 0.55s; }
}

@keyframes quickItemFadeIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.detail-card {
  opacity: 0;
  transform: translateY(30px);
  animation: cardFadeInUp 0.5s cubic-bezier(0.4, 0, 0.2, 1) forwards;

  @for $i from 1 through 8 {
    &:nth-child(#{$i}) {
      animation-delay: #{0.4 + ($i - 1) * 0.08}s;
    }
  }
}

.stat-value {
  animation: statFadeIn 0.6s ease forwards;
  animation-delay: 0.3s;
}

@keyframes statFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.wave-icon {
  display: inline-block;
  animation: wave 1.2s ease-in-out infinite;
  vertical-align: middle;
}

@keyframes wave {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(20deg); }
  75% { transform: rotate(-20deg); }
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 28px;

  .welcome-section {
    h2 {
      margin: 0 0 8px 0;
      font-size: 26px;
      font-weight: 600;
      color: var(--color-text-primary);
    }

    p {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }

  .header-stats {
    display: flex;
    gap: 12px;

    .stat-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: var(--color-bg);
      border-radius: 20px;
      font-size: 13px;
      color: var(--color-text-secondary);
      box-shadow: var(--shadow-sm);

      .el-icon {
        color: var(--color-primary);
      }

      &.success {
        background: linear-gradient(135deg, rgba(67, 233, 123, 0.1), rgba(56, 249, 215, 0.1));
        color: #059669;

        .el-icon {
          color: #059669;
        }
      }
    }
  }
}

.dashboard-header {
  position: relative;
  padding: 32px 32px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: var(--radius-xl);
  overflow: hidden;
  margin-bottom: 24px;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 300px;
    height: 100%;
    background: radial-gradient(circle at top right, rgba(255,255,255,0.15) 0%, transparent 60%);
    pointer-events: none;
  }

  .header-content {
    position: relative;
    z-index: 1;
  }

  .greeting-area {
    position: relative;
    z-index: 1;

    .greeting-text {
      font-size: 28px;
      font-weight: 700;
      color: #fff;
      margin-bottom: 8px;
      text-shadow: 0 2px 4px rgba(0,0,0,0.1);
      animation: fadeInUp 0.6s ease;
    }

    .greeting-sub {
      font-size: 14px;
      color: rgba(255,255,255,0.85);
      animation: fadeInUp 0.6s ease 0.1s both;
    }
  }

  .header-decoration {
    position: absolute;
    right: 40px;
    top: 50%;
    transform: translateY(-50%);
    opacity: 0.3;

    .decoration-circle {
      position: absolute;
      border-radius: 50%;
      background: rgba(255,255,255,0.2);

      &.circle-1 {
        width: 120px;
        height: 120px;
        top: -60px;
        right: 0;
        animation: float 6s ease-in-out infinite;
      }

      &.circle-2 {
        width: 80px;
        height: 80px;
        top: 20px;
        right: 100px;
        animation: float 5s ease-in-out 1s infinite;
      }

      &.circle-3 {
        width: 50px;
        height: 50px;
        top: 60px;
        right: 40px;
        animation: float 4s ease-in-out 2s infinite;
      }
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-15px); }
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: all 0.3s;
  overflow: hidden;
  cursor: pointer;

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);

    .stat-arrow {
      opacity: 1;
      transform: translateX(0);
      color: var(--color-primary);
    }
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  .stat-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: var(--color-text-primary);
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: var(--color-text-placeholder);
      margin-top: 2px;
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 12px;
      margin-top: 4px;

      &.up {
        color: var(--color-success);
      }

      &.down {
        color: var(--color-danger);
      }
    }
  }

  .stat-arrow {
    color: var(--color-border-dark);
    opacity: 0;
    transform: translateX(-6px);
    transition: all 0.3s;
  }
}

.detail-cards-row {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
  }

  :deep(.el-card__body) {
    padding: 20px 24px 24px;
  }

  .detail-card {
    background: var(--color-bg);
    border: 1px solid var(--color-border-light);
    border-radius: var(--radius-lg);
    padding: 18px;
    margin-bottom: 16px;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    flex-direction: column;
    gap: 12px;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: linear-gradient(180deg, var(--color-primary), var(--color-primary-dark));
      opacity: 0;
      transition: opacity 0.3s;
    }

    &:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-lg);
      border-color: rgba(102, 126, 234, 0.3);

      &::before {
        opacity: 1;
      }

      .detail-card-arrow {
        color: var(--color-primary);
        transform: translateX(0);
      }
    }

    .detail-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .detail-card-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .detail-card-body {
      display: flex;
      flex-direction: column;
      gap: 4px;

      .detail-card-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .detail-card-desc {
        font-size: 12px;
        color: var(--color-text-placeholder);
      }
    }

    .detail-card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 10px;
      border-top: 1px dashed var(--color-border-light);

      .detail-card-meta {
        font-size: 12px;
        color: var(--color-text-secondary);
      }

      .detail-card-arrow {
        color: var(--color-border-dark);
        transform: translateX(-4px);
        transition: all 0.3s;
      }
    }
  }
}

.detail-drawer {
  padding: 0 8px 24px;

  .drawer-banner {
    border-radius: var(--radius-lg);
    color: #fff;
    padding: 24px;
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 24px;
    box-shadow: var(--shadow-lg);

    h3 {
      margin: 0 0 4px 0;
      font-size: 20px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 13px;
      opacity: 0.9;
    }
  }

  .drawer-section {
    margin-bottom: 24px;

    h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--color-text-primary);
      display: flex;
      align-items: center;
      gap: 6px;

      &::before {
        content: '';
        width: 3px;
        height: 14px;
        background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));
        border-radius: 2px;
      }
    }

    .metric-card {
      background: var(--color-bg-secondary);
      border-radius: var(--radius-md);
      padding: 14px 12px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      margin-bottom: 8px;

      .metric-value {
        font-size: 20px;
        font-weight: 700;
        color: var(--color-text-primary);
      }

      .metric-label {
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }

    .event-card {
      background: var(--color-bg-secondary);
      padding: 10px 14px;
      border-radius: var(--radius-md);

      strong {
        font-size: 13px;
        color: var(--color-text-primary);
      }

      p {
        margin: 4px 0 0 0;
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }
  }

  .drawer-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);

    .el-icon {
      color: var(--color-primary);
      font-size: 20px;
    }
  }
}

.quick-access {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
  }

  :deep(.el-card__body) {
    padding: 16px 24px 24px;
  }

  .quick-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    background: var(--color-bg-secondary);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all 0.3s;
    margin-bottom: 12px;

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
      transform: translateX(4px);

      .quick-arrow {
        opacity: 1;
        transform: translateX(0);
      }
    }

    &:last-child {
      margin-bottom: 0;
    }

    .quick-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }

    .quick-text {
      flex: 1;
      display: flex;
      flex-direction: column;

      .quick-title {
        font-size: 15px;
        font-weight: 500;
        color: var(--color-text-primary);
      }

      .quick-desc {
        font-size: 12px;
        color: var(--color-text-placeholder);
        margin-top: 2px;
      }
    }

    .quick-arrow {
      color: var(--color-text-placeholder);
      opacity: 0;
      transform: translateX(-10px);
      transition: all 0.3s;
    }
  }
}

.recent-activity {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
  }

  :deep(.el-card__body) {
    padding: 8px 24px 24px;
  }

  .activity-list {
    .activity-item {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 0;
      border-bottom: 1px solid var(--color-border-light);
      transition: all 0.3s;

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        padding-left: 8px;
      }
    }

    .activity-icon {
      width: 42px;
      height: 42px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;

      .el-icon {
        font-size: 18px;
        color: var(--color-primary);
      }
    }

    .activity-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .activity-title {
        font-size: 14px;
        font-weight: 500;
        color: var(--color-text-primary);
      }

      .activity-desc {
        font-size: 12px;
        color: var(--color-text-placeholder);
        margin-top: 2px;
      }
    }

    .activity-time {
      font-size: 12px;
      color: var(--color-text-placeholder);
    }
  }
}

.hot-topics {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
  }

  :deep(.el-card__body) {
    padding: 16px 24px 24px;
  }

  .topic-list {
    .topic-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 0;

      &:last-child {
        border-bottom: none;
      }
    }

    .topic-rank {
      width: 24px;
      height: 24px;
      border-radius: 6px;
      background: var(--color-border-light);
      color: var(--color-text-secondary);
      font-size: 12px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;

      &.top {
        background: linear-gradient(135deg, var(--color-danger), #ff7a45);
        color: #fff;
      }
    }

    .topic-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .topic-title {
        font-size: 14px;
        color: var(--color-text-primary);
      }

      :deep(.el-progress) {
        .el-progress-bar__outer {
          background: var(--color-border-light);
        }
      }
    }

    .topic-count {
      font-size: 12px;
      color: var(--color-text-placeholder);
      min-width: 50px;
      text-align: right;
    }
  }
}

.tips-card {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-light);
  }

  :deep(.el-card__body) {
    padding: 16px 24px 24px;
  }

  .tips-list {
    .tip-item {
      display: flex;
      gap: 14px;
      padding: 12px 0;

      &:last-child {
        border-bottom: none;
      }
    }

    .tip-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;

      .el-icon {
        font-size: 18px;
      }
    }

    .tip-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .tip-title {
        font-size: 14px;
        font-weight: 500;
        color: var(--color-text-primary);
      }

      .tip-desc {
        font-size: 12px;
        color: var(--color-text-placeholder);
        margin-top: 2px;
        line-height: 1.5;
      }
    }
  }
}

.ai-status-card {
  border: none;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-dark));

  :deep(.el-card__body) {
    padding: 20px;
  }

  .ai-status {
    .ai-status-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .ai-status-icon {
        width: 48px;
        height: 48px;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }

      .ai-status-info {
        flex: 1;
        display: flex;
        flex-direction: column;

        .ai-status-title {
          font-size: 14px;
          color: rgba(255, 255, 255, 0.8);
        }

        .ai-status-model {
          font-size: 16px;
          font-weight: 600;
          color: #fff;
        }
      }

      .ai-status-actions {
        display: flex;
        align-items: center;
        gap: 12px;
        flex-shrink: 0;
      }

      .status-light {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 4px 10px;
        border-radius: 999px;
        background: rgba(255, 255, 255, 0.08);
        font-size: 12px;
        color: rgba(255, 255, 255, 0.9);
        cursor: default;
      }

      .light-dot {
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        box-shadow: 0 0 0 0 currentColor;
        animation: pulse 2s infinite;
      }

      .light-green { color: var(--color-success); }
      .light-green .light-dot { background: var(--color-success); box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.3); }

      .light-red { color: var(--color-danger); }
      .light-red .light-dot { background: var(--color-danger); box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.3); animation: none; }

      @keyframes pulse {
        0%   { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.6); }
        70%  { box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
        100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
      }

      :deep(.el-tag) {
        background: rgba(16, 185, 129, 0.2);
        border-color: transparent;
        color: var(--color-success);
      }
    }

    .ai-status-stats {
      display: flex;
      justify-content: space-between;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 12px;
      padding: 14px;

      .status-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        text-align: center;

        .status-value {
          font-size: 16px;
          font-weight: 600;
          color: #fff;
        }

        .status-label {
          font-size: 11px;
          color: rgba(255, 255, 255, 0.7);
          margin-top: 2px;
        }
      }
    }
  }
}

.usage-memory-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .cleared-notice {
    display: flex;
    align-items: center;
    gap: 8px;
    color: var(--color-success);
    font-size: 14px;
    padding: 12px;
    background: rgba(16, 185, 129, 0.1);
    border-radius: var(--radius-md);
  }

  .empty-memory {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 32px;
    color: var(--color-text-placeholder);
    font-size: 14px;

    .el-icon {
      font-size: 32px;
      color: var(--color-border-dark);
    }

    p {
      margin: 0;
      font-size: 12px;
      color: var(--color-text-placeholder);
    }
  }

  .memory-list {
    max-height: 400px;
    overflow-y: auto;
  }

  .memory-group {
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .memory-date {
    font-size: 12px;
    color: var(--color-text-secondary);
    margin-bottom: 8px;
    padding-left: 4px;
  }

  .memory-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 8px;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: var(--color-bg-secondary);
    }
  }

  .memory-item-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    .el-icon {
      font-size: 16px;
    }
  }

  .memory-item-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .memory-item-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .memory-item-desc {
    font-size: 11px;
    color: var(--color-text-placeholder);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .memory-item-time {
    font-size: 11px;
    color: var(--color-text-placeholder);
    flex-shrink: 0;
  }
}

// 移动端适配
@media (max-width: 768px) {
  .dashboard {
    .dashboard-header {
      padding: 20px;
      margin-bottom: 16px;

      .greeting-area {
        .greeting-text {
          font-size: 22px !important;
        }
      }

      .header-decoration {
        display: none;
      }
    }

    .stats-row {
      .el-col {
        margin-bottom: 12px;
      }

      .stat-card {
        :deep(.el-card__body) {
          padding: 16px;
        }

        .stat-icon {
          width: 44px;
          height: 44px;
          border-radius: 10px;
        }

        .stat-info {
          .stat-value {
            font-size: 22px;
          }
        }
      }
    }

    .content-row {
      .el-col {
        margin-bottom: 16px;
      }

      .quick-access {
        .quick-item {
          padding: 12px;
        }

        .quick-icon {
          width: 40px;
          height: 40px;
          border-radius: 10px;
        }
      }
    }

    .detail-cards-grid {
      grid-template-columns: 1fr !important;
    }

    .detail-cards-row {
      .detail-card {
        margin-bottom: 12px;
      }
    }
  }
}
</style>
