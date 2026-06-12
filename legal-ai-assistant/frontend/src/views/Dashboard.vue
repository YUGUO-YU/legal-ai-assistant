<template>
  <div class="dashboard">
    <div class="page-header">
      <h2>工作台</h2>
      <p>欢迎回来，{{ username }}！今天是 {{ today }}</p>
    </div>

    <el-row :gutter="24" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon search">
            <el-icon><Search /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.searchCount }}</span>
            <span class="stat-label">检索次数</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon case">
            <el-icon><Files /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.caseCount }}</span>
            <span class="stat-label">类案分析</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon document">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.docCount }}</span>
            <span class="stat-label">文书起草</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon session">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ stats.sessionCount }}</span>
            <span class="stat-label">会话数</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24">
      <el-col :span="16">
        <el-card class="quick-access">
          <template #header>
            <div class="card-header">
              <span>快捷入口</span>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="8" v-for="item in quickAccess" :key="item.path">
              <div class="quick-item" @click="$router.push(item.path)">
                <div class="quick-icon" :style="{ background: item.bgColor }">
                  <el-icon :size="24"><component :is="item.icon" /></el-icon>
                </div>
                <span class="quick-title">{{ item.title }}</span>
                <span class="quick-desc">{{ item.desc }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <el-card class="recent-activity" style="margin-top: 24px">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
              <el-button text size="small" @click="loadMore">查看更多</el-button>
            </div>
          </template>
          <div class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
              <div class="activity-icon" :style="{ background: activity.bgColor }">
                <el-icon><component :is="activity.icon" /></el-icon>
              </div>
              <div class="activity-info">
                <span class="activity-title">{{ activity.title }}</span>
                <span class="activity-time">{{ activity.time }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="hot-topics">
          <template #header>
            <span>热门话题</span>
          </template>
          <div class="topic-list">
            <div v-for="(topic, index) in hotTopics" :key="topic.title" class="topic-item">
              <span class="topic-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
              <div class="topic-info">
                <span class="topic-title">{{ topic.title }}</span>
                <span class="topic-count">{{ topic.count }} 次检索</span>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="tips-card" style="margin-top: 24px">
          <template #header>
            <span>使用技巧</span>
          </template>
          <div class="tips-list">
            <div v-for="tip in tips" :key="tip.title" class="tip-item">
              <el-icon><component :is="tip.icon" /></el-icon>
              <div class="tip-info">
                <span class="tip-title">{{ tip.title }}</span>
                <span class="tip-desc">{{ tip.desc }}</span>
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

const stats = reactive({
  searchCount: 156,
  caseCount: 89,
  docCount: 45,
  sessionCount: 23
})

const username = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    return JSON.parse(userInfo).nickname || JSON.parse(userInfo).username || '用户'
  }
  return '用户'
})

const today = computed(() => {
  return new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
})

const quickAccess = [
  { path: '/legal-search', title: 'AI搜法', desc: '法规检索与溯源', icon: 'Search', bgColor: '#1890ff' },
  { path: '/case-similar', title: 'AI类案', desc: '相似案例匹配', icon: 'Connection', bgColor: '#722ed1' },
  { path: '/case-search', title: '案例搜索', desc: '判例检索分析', icon: 'Document', bgColor: '#eb2f96' },
  { path: '/law-search', title: '法规搜索', desc: '法律法规查询', icon: 'Collection', bgColor: '#fa541c' },
  { path: '/document', title: 'AI文书', desc: '法律文书起草', icon: 'DocumentCopy', bgColor: '#13c2c2' },
  { path: '/contract-review', title: 'AI审查', desc: '合同风险分析', icon: 'Stamp', bgColor: '#fa8c16' },
  { path: '/company', title: '企业查询', desc: '工商风险预警', icon: 'OfficeBuilding', bgColor: '#f5222d' },
  { path: '/knowledge-base', title: '知识库', desc: '文档管理与问答', icon: 'Books', bgColor: '#722ed1' },
  { path: '/doc-qa', title: 'AI问答', desc: '文档智能问答', icon: 'ChatDotRound', bgColor: '#52c41a' },
  { path: '/legal-research', title: '法律研究', desc: '专题深度研究', icon: 'Coin', bgColor: '#13c2c2' }
]

const recentActivities = ref([
  { id: 1, title: '检索"合同欺诈认定"', time: '10分钟前', icon: 'Search', bgColor: '#1890ff' },
  { id: 2, title: '起草"民事起诉状"', time: '30分钟前', icon: 'Document', bgColor: '#13c2c2' },
  { id: 3, title: '审查"采购合同"', time: '1小时前', icon: 'Stamp', bgColor: '#fa8c16' },
  { id: 4, title: '查询"北京某科技公司"', time: '2小时前', icon: 'OfficeBuilding', bgColor: '#f5222d' },
  { id: 5, title: '类案检索"装修合同纠纷"', time: '3小时前', icon: 'Connection', bgColor: '#722ed1' },
  { id: 6, title: '法规搜索"劳动合同法"', time: '4小时前', icon: 'Collection', bgColor: '#fa541c' },
  { id: 7, title: '案例搜索"民间借贷纠纷"', time: '5小时前', icon: 'Document', bgColor: '#eb2f96' },
  { id: 8, title: '知识库问答"', time: '6小时前', icon: 'ChatDotRound', bgColor: '#52c41a' }
])

const hotTopics = ref([
  { title: '合同欺诈认定', count: 1256 },
  { title: '劳动仲裁流程', count: 987 },
  { title: '民间借贷利息', count: 856 },
  { title: '建设工程优先权', count: 743 },
  { title: '商标侵权赔偿', count: 621 },
  { title: '公司股权纠纷', count: 587 },
  { title: '房产买卖合同', count: 543 },
  { title: '交通事故责任', count: 498 }
])

const tips = [
  { title: '智能提示', desc: '搜索时使用自然语言，系统会自动匹配相关法规', icon: 'MagicStick' },
  { title: '追问建议', desc: '检索结果底部有追问推荐，帮助深入了解', icon: 'ChatLineSquare' },
  { title: '类案参考', desc: 'AI类案功能帮您了解类似案件的判决结果', icon: 'Connection' }
]

const loadMore = () => {
  console.log('load more activities')
}

onMounted(() => {
  // 加载用户统计数据
})
</script>

<style lang="scss" scoped>
.dashboard {
  .page-header {
    margin-bottom: 24px;
    h2 {
      margin: 0 0 8px 0;
      font-size: 24px;
    }
    p {
      margin: 0;
      color: #666;
    }
  }
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 24px;

    &.search { background: linear-gradient(135deg, #1890ff, #096dd9); }
    &.case { background: linear-gradient(135deg, #722ed1, #531d93); }
    &.document { background: linear-gradient(135deg, #13c2c2, #08979c); }
    &.session { background: linear-gradient(135deg, #52c41a, #389e0d); }
  }

  .stat-info {
    display: flex;
    flex-direction: column;
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #333;
    }
    .stat-label {
      font-size: 13px;
      color: #999;
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-item {
  padding: 20px 16px;
  background: #fafafa;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  margin-bottom: 16px;

  &:hover {
    background: #e6f7ff;
    transform: translateY(-2px);
  }

  .quick-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    margin: 0 auto 12px;
  }

  .quick-title {
    display: block;
    font-size: 14px;
    font-weight: 500;
    color: #333;
    margin-bottom: 4px;
  }

  .quick-desc {
    display: block;
    font-size: 12px;
    color: #999;
  }
}

.activity-list {
  .activity-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }
  }

  .activity-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
  }

  .activity-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .activity-title {
      font-size: 14px;
      color: #333;
    }

    .activity-time {
      font-size: 12px;
      color: #999;
    }
  }
}

.topic-list {
  .topic-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }
  }

  .topic-rank {
    width: 20px;
    height: 20px;
    border-radius: 4px;
    background: #f0f0f0;
    color: #666;
    font-size: 12px;
    display: flex;
    align-items: center;
    justify-content: center;

    &.top {
      background: #ff4d4f;
      color: #fff;
    }
  }

  .topic-info {
    flex: 1;
    display: flex;
    flex-direction: column;

    .topic-title {
      font-size: 14px;
      color: #333;
    }

    .topic-count {
      font-size: 12px;
      color: #999;
    }
  }
}

.tips-list {
  .tip-item {
    display: flex;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .el-icon {
      color: #1890ff;
      font-size: 20px;
      margin-top: 2px;
    }

    .tip-info {
      flex: 1;
      .tip-title {
        display: block;
        font-size: 14px;
        color: #333;
        margin-bottom: 4px;
      }
      .tip-desc {
        display: block;
        font-size: 12px;
        color: #999;
      }
    }
  }
}
</style>