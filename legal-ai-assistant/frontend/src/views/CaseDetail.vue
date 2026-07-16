<template>
  <div class="case-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      <div class="header-content">
        <h2>案例详情</h2>
        <p>查看案例详细信息及法律依据</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载案例详情..." />

    <div v-else-if="caseData" class="detail-container">
      <el-card class="main-card">
        <div class="case-header">
          <div class="case-title">
            <h1>{{ caseData.title || caseData.caseName }}</h1>
            <div class="case-meta">
              <el-tag size="small" type="info">
                <el-icon><OfficeBuilding /></el-icon>
                {{ caseData.court }}
              </el-tag>
              <el-tag size="small" type="info">
                <el-icon><Calendar /></el-icon>
                {{ caseData.judgeDate }}
              </el-tag>
              <el-tag size="small" v-if="caseData.trialProcedure">
                {{ caseData.trialProcedure }}
              </el-tag>
              <el-tag :type="getResultType(caseData.judgmentResult)" effect="dark" round>
                {{ getResultName(caseData.judgmentResult) }}
              </el-tag>
            </div>
          </div>
          <div class="similarity-badge" v-if="caseData.similarityScore">
            <span class="similarity-value">{{ formatScore(caseData.similarityScore) }}%</span>
            <span class="similarity-label">相似度</span>
          </div>
        </div>

        <div class="case-section" v-if="caseData.caseNo">
          <h3>案号</h3>
          <div class="section-content">
            <div class="case-no">
              <el-icon><Document /></el-icon>
              {{ caseData.caseNo }}
            </div>
            <el-button type="primary" link @click="copyCaseNo">
              <el-icon><CopyDocument /></el-icon>
              复制案号
            </el-button>
          </div>
        </div>

        <div class="case-section" v-if="caseData.caseType || caseData.caseCause">
          <h3>案件信息</h3>
          <div class="info-grid">
            <div class="info-item" v-if="caseData.caseType">
              <span class="label">案件类型</span>
              <span class="value">{{ caseData.caseType }}</span>
            </div>
            <div class="info-item" v-if="caseData.caseCause">
              <span class="label">案由</span>
              <span class="value">{{ caseData.caseCause }}</span>
            </div>
            <div class="info-item" v-if="caseData.courtLevel">
              <span class="label">法院层级</span>
              <span class="value">{{ getCourtLevel(caseData.courtLevel) }}</span>
            </div>
            <div class="info-item" v-if="caseData.litigationAmount">
              <span class="label">诉讼金额</span>
              <span class="value">{{ formatAmount(caseData.litigationAmount) }}</span>
            </div>
          </div>
        </div>

        <div class="case-section" v-if="caseData.keyFacts || caseData.summary">
          <h3>{{ caseData.keyFacts ? '关键事实' : '案件摘要' }}</h3>
          <div class="section-content text-content">
            {{ caseData.keyFacts || caseData.summary }}
          </div>
        </div>

        <div class="case-section" v-if="caseData.judgmentSummary">
          <h3>裁判要旨</h3>
          <div class="section-content text-content highlight">
            {{ caseData.judgmentSummary }}
          </div>
        </div>

        <div class="case-section" v-if="caseData.legalBasis && caseData.legalBasis.length">
          <h3>法律依据</h3>
          <div class="legal-basis-list">
            <el-tag
              v-for="(law, index) in caseData.legalBasis"
              :key="index"
              effect="plain"
              class="law-tag"
            >
              <el-icon><Collection /></el-icon>
              {{ law }}
            </el-tag>
          </div>
        </div>

        <div class="case-section" v-if="caseData.matchingFeatures">
          <h3>匹配特征</h3>
          <div class="matching-features">
            <div
              v-for="(score, key) in caseData.matchingFeatures"
              :key="key"
              class="feature-item"
            >
              <span class="feature-name">{{ getFeatureName(key) }}</span>
              <el-progress
                :percentage="formatPercentage(score)"
                :color="getProgressColor(score)"
                :stroke-width="8"
              />
              <span class="feature-score">{{ formatPercentage(score) }}%</span>
            </div>
          </div>
        </div>

        <div class="case-section source-section">
          <h3>数据来源</h3>
          <div class="source-info">
            <el-icon><Link /></el-icon>
            <a :href="caseData.sourceUrl" target="_blank" class="source-link">
              {{ caseData.sourceName }}
            </a>
          </div>
        </div>
      </el-card>

      <div class="action-buttons">
        <el-button type="primary" size="large" @click="generateAnalysis">
          <el-icon><DataAnalysis /></el-icon>
          AI案情分析
        </el-button>
        <el-button size="large" @click="generatePpt">
          <el-icon><Files /></el-icon>
          生成PPT
        </el-button>
      </div>
    </div>

    <empty-state
      v-else
      icon="Document"
      title="未找到案例详情"
      description="未找到该案例的详细信息，请返回列表重试"
      action-text="返回列表"
      @action="goBack"
    />

    <case-analysis-dialog v-model="showAnalysisDialog" :case-uuid="caseUuid" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  OfficeBuilding,
  Calendar,
  Document,
  CopyDocument,
  Collection,
  Link,
  DataAnalysis,
  Files
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'
import CaseAnalysisDialog from '../components/CaseAnalysisDialog.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const caseData = ref(null)
const caseUuid = ref('')
const showAnalysisDialog = ref(false)

const loadCaseDetail = async () => {
  const uuid = route.params.caseUuid
  caseUuid.value = uuid
  if (!uuid) {
    ElMessage.error('案例ID不能为空')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await api.caseSearch.getCaseDetail(uuid)
    if (res) {
      caseData.value = res
    } else {
      ElMessage.error('案例不存在')
    }
  } catch (e) {
    console.error('Failed to load case detail:', e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const getResultType = (result) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info', 5: 'success' }
  return types[result] || 'info'
}

const getResultName = (result) => {
  const names = { 1: '全部支持', 2: '部分支持', 3: '驳回', 4: '撤诉', 5: '调解' }
  return names[result] || '未知'
}

const getCourtLevel = (level) => {
  const levels = { 1: '最高人民法院', 2: '高级人民法院', 3: '中级人民法院', 4: '基层人民法院' }
  return levels[level] || '未知'
}

const getFeatureName = (key) => {
  const names = {
    fact_similarity: '事实相似度',
    claim_similarity: '诉求相似度',
    dispute_similarity: '争议相似度'
  }
  return names[key] || key
}

const formatScore = (score) => {
  if (score >= 1) {
    return (score * 100).toFixed(0)
  }
  return (score * 100).toFixed(0)
}

const formatPercentage = (score) => {
  if (score >= 1) {
    return Math.min(100, (score * 100).toFixed(0))
  }
  return (score * 100).toFixed(0)
}

const formatAmount = (amount) => {
  if (!amount) return '-'
  if (amount >= 10000) {
    return (amount / 10000).toFixed(2) + '万元'
  }
  return amount.toLocaleString() + '元'
}

const getProgressColor = (score) => {
  const percentage = score >= 1 ? score * 100 : score * 100
  if (percentage >= 80) return '#10b981'
  if (percentage >= 60) return '#f59e0b'
  return '#6b7280'
}

const copyCaseNo = () => {
  if (caseData.value?.caseNo) {
    navigator.clipboard.writeText(caseData.value.caseNo)
    ElMessage.success('案号已复制')
  }
}

const generateAnalysis = () => {
  if (!caseUuid.value) {
    ElMessage.warning('请先加载案例')
    return
  }
  showAnalysisDialog.value = true
}

const generatePpt = async () => {
  if (!caseData.value) {
    ElMessage.warning('案例数据加载中，请稍候')
    return
  }

  const data = {
    title: caseData.value.title || caseData.value.caseName || '案例分析PPT',
    content: [
      { type: 'title', text: caseData.value.title || caseData.value.caseName },
      { type: 'section', title: '基本信息', items: [
        caseData.value.caseNo ? `案号：${caseData.value.caseNo}` : null,
        caseData.value.court ? `法院：${caseData.value.court}` : null,
        caseData.value.judgeDate ? `裁判日期：${caseData.value.judgeDate}` : null,
        caseData.value.trialProcedure ? `程序：${caseData.value.trialProcedure}` : null,
        caseData.value.caseType ? `案件类型：${caseData.value.caseType}` : null,
        caseData.value.caseCause ? `案由：${caseData.value.caseCause}` : null,
      ].filter(Boolean) },
      { type: 'section', title: '案件摘要', items: [
        caseData.value.keyFacts || caseData.value.summary || '无'
      ]},
      { type: 'section', title: '裁判结果', items: [
        caseData.value.judgmentResult || '无'
      ]}
    ]
  }

  localStorage.setItem('ppt_draft_data', JSON.stringify(data))
  router.push('/ppt-editor')
  ElMessage.success('正在跳转PPT编辑器')
}

onMounted(() => {
  loadCaseDetail()
})
</script>

<style lang="scss" scoped>
.case-detail {
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

  .header-nav {
    margin-bottom: 16px;

    :deep(.el-button) {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--color-text-secondary);

      &:hover {
        color: #667eea;
      }
    }
  }

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

.detail-container {
  .main-card {
    border: none;
    border-radius: 20px;
    box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
    margin-bottom: 24px;

    :deep(.el-card__body) {
      padding: 32px;
    }
  }

  .case-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid #f3f4f6;

    .case-title {
      flex: 1;

      h1 {
        margin: 0 0 16px 0;
        font-size: 24px;
        font-weight: 600;
        color: var(--color-text-primary);
        line-height: 1.4;
      }

      .case-meta {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;

        .el-tag {
          border-radius: 6px;
          display: inline-flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .similarity-badge {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 80px;
      height: 80px;
      border-radius: 16px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      flex-shrink: 0;

      .similarity-value {
        font-size: 22px;
        font-weight: 700;
      }

      .similarity-label {
        font-size: 11px;
        opacity: 0.9;
      }
    }
  }

  .case-section {
    margin-bottom: 28px;

    &:last-child {
      margin-bottom: 0;
    }

    h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 4px;
        height: 16px;
        background: linear-gradient(135deg, #667eea, #764ba2);
        border-radius: 2px;
      }
    }

    .section-content {
      padding: 16px 20px;
      background: #f9fafb;
      border-radius: 12px;

      &.text-content {
        font-size: 14px;
        line-height: 1.8;
        color: var(--color-text-secondary);
      }

      &.highlight {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
        border-left: 4px solid #667eea;
      }

      .case-no {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 500;
        color: var(--color-text-primary);

        .el-icon {
          color: #667eea;
        }
      }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;

      .info-item {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .label {
          font-size: 13px;
          color: var(--color-text-secondary);
        }

        .value {
          font-size: 15px;
          font-weight: 500;
          color: var(--color-text-primary);
        }
      }
    }

    .legal-basis-list {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;

      .law-tag {
        border-radius: 8px;
        padding: 8px 14px;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
        border: 1px solid rgba(102, 126, 234, 0.2);
        color: #667eea;

        .el-icon {
          font-size: 14px;
        }
      }
    }

    .matching-features {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 16px;

        .feature-name {
          width: 100px;
          font-size: 14px;
          color: var(--color-text-secondary);
          flex-shrink: 0;
        }

        .el-progress {
          flex: 1;
        }

        .feature-score {
          width: 50px;
          text-align: right;
          font-size: 14px;
          font-weight: 600;
          color: #667eea;
        }
      }
    }

    &.source-section {
      padding-top: 20px;
      border-top: 1px solid #f3f4f6;

      .source-info {
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }

        .source-link {
          color: #667eea;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;

    :deep(.el-button) {
      border-radius: 12px;
      padding: 14px 32px;
      font-size: 15px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.3s;

      &.el-button--primary {
        background: linear-gradient(135deg, #667eea, #764ba2);
        border: none;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
        }
      }

      &:not(.el-button--primary):hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
      }
    }
  }
}

@media (max-width: 768px) {
  .detail-container {
    .case-header {
      flex-direction: column;
      gap: 20px;

      .similarity-badge {
        align-self: flex-start;
      }
    }

    .info-grid {
      grid-template-columns: 1fr;
    }

    .action-buttons {
      flex-direction: column;

      :deep(.el-button) {
        width: 100%;
      }
    }
  }
}
</style>
