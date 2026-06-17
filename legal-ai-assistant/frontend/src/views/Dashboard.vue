<template>
  <div class="dashboard">
    <div class="page-header">
      <div class="welcome-section">
        <h2>👋 {{ greeting }}，{{ username }}</h2>
        <p>今天是 {{ today }}，为您准备了一些快捷功能</p>
      </div>
      <div class="header-stats">
        <div class="stat-pill">
          <el-icon><Clock /></el-icon>
          <span>本周活跃 {{ activeDays }} 天</span>
        </div>
        <div class="stat-pill success">
          <el-icon><CircleCheck /></el-icon>
          <span>效率提升 32%</span>
        </div>
      </div>
    </div>

    <el-row :gutter="24" class="stats-row">
      <el-col :span="6" v-for="(stat, index) in statsData" :key="index">
        <el-card class="stat-card" :class="stat.class">
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
              <div class="quick-item" @click="$router.push(item.path)">
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
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
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
                <span class="ai-status-title">AI 服务状态</span>
                <span class="ai-status-model">{{ aiStatusData.model }}</span>
              </div>
              <el-tag :type="aiStatusData.status === 'online' ? 'success' : aiStatusData.status === 'degraded' ? 'warning' : 'danger'" size="small" effect="dark">
                <span v-if="aiStatusData.status === 'online'" class="pulse"></span>
                {{ aiStatusData.status === 'online' ? '在线' : aiStatusData.status === 'degraded' ? '异常' : aiStatusData.status === 'error' ? '错误' : '离线' }}
              </el-tag>
            </div>
            <div class="ai-status-stats">
              <div class="status-item">
                <span class="status-value">{{ aiStatusData.status === 'online' ? '正常' : aiStatusData.status === 'degraded' ? '降级' : '不可用' }}</span>
                <span class="status-label">服务状态</span>
              </div>
              <div class="status-item">
                <span class="status-value">{{ aiStatusData.status === 'checking' ? '...' : aiStatusData.message }}</span>
                <span class="status-label">详细信息</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
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
  Coin
} from '@element-plus/icons-vue'

const statsData = reactive([
  {
    icon: 'Search',
    value: '156',
    label: '检索次数',
    trend: 12,
    gradient: 'linear-gradient(135deg, #667eea, #764ba2)',
    shadow: '0 8px 20px rgba(102, 126, 234, 0.35)',
    class: 'stat-purple'
  },
  {
    icon: 'Connection',
    value: '89',
    label: '类案分析',
    trend: 8,
    gradient: 'linear-gradient(135deg, #f093fb, #f5576c)',
    shadow: '0 8px 20px rgba(245, 87, 108, 0.35)',
    class: 'stat-pink'
  },
  {
    icon: 'DocumentCopy',
    value: '45',
    label: '文书起草',
    trend: -3,
    gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)',
    shadow: '0 8px 20px rgba(79, 172, 254, 0.35)',
    class: 'stat-blue'
  },
  {
    icon: 'ChatDotRound',
    value: '23',
    label: '会话数',
    trend: 25,
    gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)',
    shadow: '0 8px 20px rgba(67, 233, 123, 0.35)',
    class: 'stat-green'
  }
])

const username = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).nickname || JSON.parse(userInfo).username || '用户'
  }
  return '用户'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

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
  { path: '/contract-review', title: 'AI审查', desc: '合同风险分析', icon: 'Stamp', gradient: 'linear-gradient(135deg, #a18cd1, #fbc2eb)', shadow: '0 8px 20px rgba(161, 140, 209, 0.35)' }
]

const recentActivities = ref([
  { id: 1, title: '检索"合同欺诈认定"', desc: '找到了 12 条相关法规和 8 个类案', time: '10分钟前', icon: 'Search', gradient: 'rgba(102, 126, 234, 0.15)' },
  { id: 2, title: '起草"民事起诉状"', desc: '已生成起诉状模板，正在编辑', time: '30分钟前', icon: 'DocumentCopy', gradient: 'rgba(79, 172, 254, 0.15)' },
  { id: 3, title: '审查"采购合同"', desc: '发现 3 处风险条款', time: '1小时前', icon: 'Stamp', gradient: 'rgba(161, 140, 209, 0.15)' },
  { id: 4, title: '查询"北京某科技公司"', desc: '企业风险等级：中等', time: '2小时前', icon: 'OfficeBuilding', gradient: 'rgba(250, 112, 154, 0.15)' },
  { id: 5, title: '类案检索"装修合同纠纷"', desc: '相似度 > 85% 的案例 5 个', time: '3小时前', icon: 'Connection', gradient: 'rgba(245, 87, 108, 0.15)' }
])

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
  message: '检测中...'
})

const loadMore = () => {
  console.log('load more activities')
}

const loadAiStatus = async () => {
  try {
    const res = await fetch('/api/v1/ai-status')
    const data = await res.json()
    if (data.status === 'online') {
      aiStatusData.value = {
        status: 'online',
        model: data.model || 'MiniMax-M3',
        message: data.message || 'AI 服务正常运行'
      }
    } else if (data.status === 'degraded') {
      aiStatusData.value = {
        status: 'degraded',
        model: data.model || 'MiniMax-M3',
        message: data.message || 'AI 服务响应异常'
      }
    } else {
      aiStatusData.value = {
        status: 'offline',
        model: data.model || 'MiniMax-M3',
        message: data.message || 'AI 服务暂时不可用'
      }
    }
  } catch (e) {
    aiStatusData.value = {
      status: 'error',
      model: 'MiniMax-M3',
      message: '无法连接 AI 服务'
    }
  }
}

onMounted(() => {
  loadAiStatus()
})
</script>

<style lang="scss" scoped>
.dashboard {
  animation: fadeIn 0.5s ease;
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
      color: #1f2937;
    }

    p {
      margin: 0;
      color: #6b7280;
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
      background: #fff;
      border-radius: 20px;
      font-size: 13px;
      color: #6b7280;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);

      .el-icon {
        color: #667eea;
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

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
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
      color: #1f2937;
      line-height: 1.2;
    }

    .stat-label {
      font-size: 13px;
      color: #9ca3af;
      margin-top: 2px;
    }

    .stat-trend {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 12px;
      margin-top: 4px;

      &.up {
        color: #10b981;
      }

      &.down {
        color: #ef4444;
      }
    }
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
    color: #1f2937;

    .el-icon {
      color: #667eea;
      font-size: 20px;
    }
  }
}

.quick-access {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
  }

  :deep(.el-card__body) {
    padding: 16px 24px 24px;
  }

  .quick-item {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 16px;
    background: #f9fafb;
    border-radius: 14px;
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
        color: #1f2937;
      }

      .quick-desc {
        font-size: 12px;
        color: #9ca3af;
        margin-top: 2px;
      }
    }

    .quick-arrow {
      color: #9ca3af;
      opacity: 0;
      transform: translateX(-10px);
      transition: all 0.3s;
    }
  }
}

.recent-activity {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
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
      border-bottom: 1px solid #f5f5f5;
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
        color: #667eea;
      }
    }

    .activity-info {
      flex: 1;
      display: flex;
      flex-direction: column;

      .activity-title {
        font-size: 14px;
        font-weight: 500;
        color: #1f2937;
      }

      .activity-desc {
        font-size: 12px;
        color: #9ca3af;
        margin-top: 2px;
      }
    }

    .activity-time {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.hot-topics {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
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
      background: #f3f4f6;
      color: #6b7280;
      font-size: 12px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;

      &.top {
        background: linear-gradient(135deg, #ff4d4f, #ff7a45);
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
        color: #1f2937;
      }

      :deep(.el-progress) {
        .el-progress-bar__outer {
          background: #f3f4f6;
        }
      }
    }

    .topic-count {
      font-size: 12px;
      color: #9ca3af;
      min-width: 50px;
      text-align: right;
    }
  }
}

.tips-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
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
        color: #1f2937;
      }

      .tip-desc {
        font-size: 12px;
        color: #9ca3af;
        margin-top: 2px;
        line-height: 1.5;
      }
    }
  }
}

.ai-status-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  background: linear-gradient(135deg, #667eea, #764ba2);

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

      :deep(.el-tag) {
        background: rgba(16, 185, 129, 0.2);
        border-color: transparent;
        color: #10b981;

        .pulse {
          display: inline-block;
          width: 6px;
          height: 6px;
          background: #10b981;
          border-radius: 50%;
          margin-right: 4px;
          animation: pulse 2s infinite;
        }
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
</style>
