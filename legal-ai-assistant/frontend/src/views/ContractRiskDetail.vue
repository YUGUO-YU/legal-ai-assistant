<template>
  <div class="contract-risk-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回审查
        </el-button>
      </div>
      <div class="header-content">
        <h2>合同审查详情</h2>
        <p>查看风险点详情、修改建议与改进方案</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载审查详情..." />

    <div v-else-if="reviewData" class="detail-container">
      <el-card class="overview-card">
        <div class="overview-header">
          <div class="overview-title">
            <h1>{{ reviewData.contractName || '未命名合同' }}</h1>
            <div class="overview-tags">
              <el-tag effect="dark" round>{{ getRiskLabel(reviewData.riskLevel) }}</el-tag>
              <el-tag type="info" effect="plain" round>{{ getContractTypeName(reviewData.contractType) }}</el-tag>
              <el-tag size="small" type="info" effect="plain">UUID: {{ reviewData.reviewUuid }}</el-tag>
            </div>
          </div>
          <el-progress
            type="circle"
            :width="120"
            :stroke-width="10"
            :percentage="reviewData.totalScore"
            :color="getScoreColor(reviewData.totalScore)"
          >
            <template #default>
              <div class="score-content">
                <span class="score-value">{{ reviewData.totalScore }}</span>
                <span class="score-unit">分</span>
              </div>
            </template>
          </el-progress>
        </div>

        <el-row :gutter="16" class="overview-stats">
          <el-col :span="8">
            <div class="stat-cell high">
              <span class="stat-value">{{ riskCount('HIGH') }}</span>
              <span class="stat-label">高风险项</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-cell medium">
              <span class="stat-value">{{ riskCount('MEDIUM') }}</span>
              <span class="stat-label">中风险项</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-cell low">
              <span class="stat-value">{{ riskCount('LOW') }}</span>
              <span class="stat-label">低风险项</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="14">
          <el-card class="risk-card">
            <template #header>
              <div class="card-header">
                <el-icon><Warning /></el-icon>
                <span>风险清单</span>
                <el-radio-group v-model="filterLevel" size="small">
                  <el-radio-button label="all">全部</el-radio-button>
                  <el-radio-button label="HIGH">高风险</el-radio-button>
                  <el-radio-button label="MEDIUM">中风险</el-radio-button>
                  <el-radio-button label="LOW">低风险</el-radio-button>
                </el-radio-group>
              </div>
            </template>

            <div v-if="filteredRisks.length" class="risk-list">
              <div
                v-for="(item, index) in filteredRisks"
                :key="index"
                class="risk-block"
                :class="['level-' + item.level.toLowerCase(), { active: activeIndex === index }]"
                @click="activeIndex = index"
              >
                <div class="risk-block-header">
                  <el-tag :type="getLevelTagType(item.level)" effect="dark" size="small">
                    {{ getLevelLabel(item.level) }}
                  </el-tag>
                  <span class="risk-dim" v-if="item.dimension">{{ item.dimension }}</span>
                  <span class="risk-title">{{ item.title }}</span>
                </div>
                <div class="risk-block-desc">{{ item.description }}</div>
                <div class="risk-block-foot">
                  <el-icon><Star /></el-icon>
                  <span>{{ item.suggestion }}</span>
                </div>
              </div>
            </div>
            <empty-state v-else icon="CircleCheck" title="该等级无风险项" description="选择其他等级查看更多风险" />
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card class="dimension-card">
            <template #header>
              <div class="card-header">
                <el-icon><DataAnalysis /></el-icon>
                <span>维度评分</span>
              </div>
            </template>
            <div class="dim-list">
              <div v-for="dim in reviewData.dimensions" :key="dim.dimensionCode" class="dim-row">
                <div class="dim-row-head">
                  <span class="dim-name">{{ dim.dimensionName }}</span>
                  <span class="dim-score" :style="{ color: getScoreColor(dim.score) }">{{ dim.score }}分</span>
                </div>
                <el-progress
                  :percentage="dim.score"
                  :color="getScoreColor(dim.score)"
                  :show-text="false"
                  :stroke-width="6"
                />
                <div class="dim-comment">{{ dim.comment }}</div>
              </div>
            </div>
          </el-card>

          <el-card class="comment-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <el-icon><ChatLineSquare /></el-icon>
                <span>综合评价</span>
              </div>
            </template>
            <p class="comment-text">{{ reviewData.overallComment || '暂未生成综合评价' }}</p>
            <div class="comment-actions">
              <el-button type="primary" @click="exportReport">
                <el-icon><Download /></el-icon>
                导出报告
              </el-button>
              <el-button @click="copySummary">
                <el-icon><CopyDocument /></el-icon>
                复制摘要
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <empty-state
      v-else
      icon="Document"
      title="未找到审查详情"
      description="该审查记录可能已过期或不存在"
      action-text="返回审查"
      @action="goBack"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Warning,
  DataAnalysis,
  ChatLineSquare,
  Download,
  CopyDocument,
  Star,
  CircleCheck
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const reviewData = ref(null)
const filterLevel = ref('all')
const activeIndex = ref(0)

const allRisks = computed(() => {
  if (!reviewData.value) return []
  return [
    ...(reviewData.value.highRiskItems || []),
    ...(reviewData.value.mediumRiskItems || []),
    ...(reviewData.value.lowRiskItems || [])
  ]
})

const filteredRisks = computed(() => {
  if (filterLevel.value === 'all') return allRisks.value
  return allRisks.value.filter(r => r.level === filterLevel.value)
})

const riskCount = (level) => {
  if (!reviewData.value) return 0
  if (level === 'HIGH') return reviewData.value.highRiskItems?.length || 0
  if (level === 'MEDIUM') return reviewData.value.mediumRiskItems?.length || 0
  if (level === 'LOW') return reviewData.value.lowRiskItems?.length || 0
  return 0
}

const loadDetail = async () => {
  const uuid = route.params.reviewUuid
  if (!uuid) {
    ElMessage.error('审查ID不能为空')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await api.contract.getReview(uuid)
    if (res) {
      reviewData.value = res
    } else {
      ElMessage.error('审查记录不存在或已过期')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/contract-review')
  }
}

const getRiskLabel = (level) => {
  return { HIGH: '高风险', MEDIUM: '中等风险', LOW: '低风险' }[level] || '未知'
}

const getContractTypeName = (type) => {
  return { sale: '买卖合同', lease: '租赁合同', construction: '建设工程合同', loan: '借款合同', labor: '劳动合同', other: '其他' }[type] || '其他'
}

const getLevelLabel = (level) => {
  return { HIGH: '高', MEDIUM: '中', LOW: '低' }[level] || level
}

const getLevelTagType = (level) => {
  return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[level] || 'info'
}

const getScoreColor = (score) => {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

const exportReport = () => {
  if (!reviewData.value) return
  const data = reviewData.value
  const lines = [
    `# 合同审查报告 - ${data.contractName || '未命名'}`,
    '',
    `## 综合评分: ${data.totalScore} 分 (${getRiskLabel(data.riskLevel)})`,
    '',
    `## 各维度评分`
  ]
  for (const dim of data.dimensions || []) {
    lines.push(`- ${dim.dimensionName}: ${dim.score} 分 - ${dim.comment}`)
  }
  lines.push('', '## 风险清单')
  for (const r of allRisks.value) {
    lines.push(`### [${getLevelLabel(r.level)}] ${r.title}`)
    lines.push(`- 维度: ${r.dimension || '-'}`)
    lines.push(`- 描述: ${r.description}`)
    lines.push(`- 建议: ${r.suggestion}`)
  }
  lines.push('', '## 综合评价', data.overallComment || '-')
  const text = lines.join('\n')
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `合同审查报告_${data.reviewUuid}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('报告已导出')
}

const copySummary = async () => {
  if (!reviewData.value) return
  const data = reviewData.value
  const text = `【合同审查】 ${data.contractName || ''} | 评分 ${data.totalScore} | 风险 ${getRiskLabel(data.riskLevel)} | 高 ${riskCount('HIGH')} 中 ${riskCount('MEDIUM')} 低 ${riskCount('LOW')}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('摘要已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
.contract-risk-detail {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: 24px;

  .header-nav :deep(.el-button) {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--color-text-secondary);
  }

  .header-content h2 {
    margin: 0 0 8px 0;
    font-size: 26px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .header-content p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 14px;
  }
}

.detail-container {
  .overview-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

    :deep(.el-card__body) {
      padding: 24px;
    }

    .overview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 24px;
      margin-bottom: 20px;
    }

    .overview-title h1 {
      margin: 0 0 12px 0;
      font-size: 22px;
      color: var(--color-text-primary);
    }

    .overview-tags {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .score-content {
      display: flex;
      align-items: baseline;
      gap: 2px;

      .score-value {
        font-size: 32px;
        font-weight: 700;
        color: var(--color-text-primary);
      }
      .score-unit {
        font-size: 14px;
        color: var(--color-text-secondary);
      }
    }

    .overview-stats {
      margin-top: 12px;

      .stat-cell {
        padding: 16px;
        border-radius: 12px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;

        .stat-value {
          font-size: 24px;
          font-weight: 700;
        }
        .stat-label {
          font-size: 13px;
        }

        &.high { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
        &.medium { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
        &.low { background: rgba(16, 185, 129, 0.1); color: #10b981; }
      }
    }
  }

  .risk-card, .dimension-card, .comment-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

    :deep(.el-card__header) {
      padding: 16px 20px;
    }
    :deep(.el-card__body) {
      padding: 16px 20px 20px;
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);

    .el-icon { color: #667eea; }
  }

  .risk-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .risk-block {
    background: #f9fafb;
    border-radius: 12px;
    padding: 14px 16px;
    border-left: 4px solid #d1d5db;
    cursor: pointer;
    transition: all 0.3s;

    &.level-high { border-color: #ef4444; }
    &.level-medium { border-color: #f59e0b; }
    &.level-low { border-color: #10b981; }

    &.active, &:hover {
      background: #fff;
      box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
      transform: translateX(2px);
    }

    .risk-block-header {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
      margin-bottom: 8px;

      .risk-dim {
        font-size: 12px;
        color: var(--color-text-secondary);
        background: #fff;
        padding: 2px 8px;
        border-radius: 4px;
      }
      .risk-title {
        font-weight: 600;
        color: var(--color-text-primary);
      }
    }

    .risk-block-desc {
      font-size: 13px;
      color: var(--color-text-secondary);
      line-height: 1.6;
      margin-bottom: 8px;
    }

    .risk-block-foot {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      padding: 8px 12px;
      background: rgba(102, 126, 234, 0.08);
      border-radius: 8px;
      font-size: 12px;
      color: #667eea;
      .el-icon { color: #667eea; flex-shrink: 0; margin-top: 2px; }
    }
  }

  .dim-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .dim-row {
    padding: 12px 14px;
    background: #f9fafb;
    border-radius: 10px;

    .dim-row-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }

    .dim-name {
      font-size: 14px;
      color: var(--color-text-primary);
      font-weight: 500;
    }
    .dim-score {
      font-weight: 700;
    }
    .dim-comment {
      font-size: 12px;
      color: var(--color-text-secondary);
      margin-top: 6px;
      line-height: 1.5;
    }
  }

  .comment-card .comment-text {
    font-size: 14px;
    line-height: 1.8;
    color: var(--color-text-secondary);
    background: #f9fafb;
    border-radius: 10px;
    padding: 14px 16px;
    margin: 0 0 16px 0;
  }

  .comment-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}
</style>
