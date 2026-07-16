<template>
  <div class="case-similar">
    <div class="page-header">
      <div class="header-content">
        <h2>AI类案</h2>
        <p>输入案件描述，智能匹配相似判例供参考</p>
      </div>
    </div>

    <el-card class="search-card">
      <div class="search-section">
        <div class="section-header">
          <el-icon><Edit /></el-icon>
          <span>案件描述</span>
        </div>
        <el-input
          v-model="caseDescription"
          type="textarea"
          :rows="5"
          placeholder="请详细描述案件情况，包括：当事人、案由、事实经过、诉讼请求等"
        />
      </div>

      <div class="filter-section">
        <div class="filter-item">
          <label>
            <el-icon><FolderOpened /></el-icon>
            案件类型
          </label>
          <el-select v-model="caseType" placeholder="请选择" clearable>
            <el-option label="民事" :value="1" />
            <el-option label="刑事" :value="2" />
            <el-option label="行政" :value="3" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>
            <el-icon><Document /></el-icon>
            案由
          </label>
          <el-input v-model="caseCause" placeholder="请输入" clearable />
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="large" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            查找类案
          </el-button>
        </div>
      </div>
    </el-card>

    <loading v-if="loading" text="正在检索相似案例..." />

    <div v-else-if="results.length > 0" class="results-container">
      <el-card class="stats-card">
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-icon total">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-content">
                <span class="stat-value">{{ statistics.totalCount }}</span>
                <span class="stat-label">相似案例</span>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-icon win">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stat-content">
                <span class="stat-value">{{ (statistics.winRate * 100).toFixed(0) }}%</span>
                <span class="stat-label">胜诉率</span>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-icon money">
                <el-icon><Coin /></el-icon>
              </div>
              <div class="stat-content">
                <span class="stat-value">{{ statistics.avgCompensation }}</span>
                <span class="stat-label">平均判赔(万元)</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <div class="results-list">
        <el-card
          v-for="item in results"
          :key="item.caseId"
          class="case-card"
          @click="viewDetail(item)"
        >
          <div class="case-header">
            <div class="case-info">
              <h3>{{ item.caseName }}</h3>
              <div class="case-meta">
                <el-tag size="small" type="info">{{ getCourtLevel(item.courtLevel) }}</el-tag>
                <span class="divider">|</span>
                <span>{{ item.courtName }}</span>
                <span class="divider">|</span>
                <span>{{ item.judgeDate }}</span>
              </div>
            </div>
            <div class="similarity-badge" :style="{ background: getSimilarityColor(item.similarityScore) }">
              <span class="similarity-value">{{ (item.similarityScore > 1 ? item.similarityScore : (item.similarityScore * 100)).toFixed(0) }}%</span>
              <span class="similarity-label">相似度</span>
            </div>
          </div>

          <div class="matching-features">
            <span class="feature-label">匹配特征：</span>
            <el-tag
              v-for="(score, key) in item.matchingFeatures"
              :key="key"
              size="small"
              :type="getFeatureTagType(score)"
            >
              {{ getFeatureName(key) }}: {{ (score > 1 ? score : (score * 100)).toFixed(0) }}%
            </el-tag>
          </div>

          <p class="case-summary">{{ item.judgmentSummary }}</p>

          <div class="legal-basis">
            <span class="basis-label">
              <el-icon><Collection /></el-icon>
              法律依据：
            </span>
            <el-tag
              v-for="law in item.legalBasis"
              :key="law"
              size="small"
              effect="plain"
              class="law-tag"
            >
              {{ law }}
            </el-tag>
          </div>

          <div class="case-footer">
            <el-button type="primary" link @click.stop="viewDetail(item)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
          </div>
        </el-card>
      </div>

      <div v-if="results.length > 0 && statistics.winRatePrediction" class="analysis-section">
        <el-card class="analysis-card">
          <template #header>
            <div class="analysis-header">
              <div class="analysis-title">
                <el-icon><DataAnalysis /></el-icon>
                <span>类案分析报告</span>
              </div>
            </div>
          </template>
          <AnalysisReport :statistics="statistics" />
        </el-card>
      </div>
    </div>

    <empty-state
      v-else-if="hasSearched"
      icon="Connection"
      title="未找到相似案例"
      description="未找到相似案例，请尝试调整案件描述或筛选条件"
      action-text="清除搜索"
      @action="caseDescription = ''; caseType = null; caseCause = ''; results = []; hasSearched = false"
    />

    <empty-state
      v-else
      icon="ChatLineSquare"
      title="AI 类案分析"
      description="请输入案件描述，AI将为您匹配相似案例并分析裁判要点"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Edit,
  FolderOpened,
  Document,
  Search,
  CircleCheck,
  Coin,
  Collection,
  View,
  DataAnalysis
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'
import AnalysisReport from '../components/AnalysisReport.vue'
import { useUsageMemory } from '@/composables/useUsageMemory'

const { addRecord } = useUsageMemory()

const router = useRouter()
const caseDescription = ref('')
const caseType = ref(null)
const caseCause = ref('')
const loading = ref(false)
const results = ref([])
const hasSearched = ref(false)
const statistics = ref({})

const handleSearch = async () => {
  if (!caseDescription.value.trim()) {
    ElMessage.warning('请输入案件描述')
    return
  }

  loading.value = true
  hasSearched.value = true
  try {
    const res = await api.caseSimilar.search({
      caseDescription: caseDescription.value,
      caseType: caseType.value,
      caseCause: caseCause.value
    })
    results.value = res?.items || []
    statistics.value = res?.statistics || {}
    addRecord('case', `类案检索"${caseDescription.value.slice(0, 20)}..."`, `找到 ${results.value.length} 个相似案例`)
  } catch (e) {
    console.error(e)
    ElMessage.error('检索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const getCourtLevel = (level) => {
  const levels = { 1: '最高院', 2: '高院', 3: '中院', 4: '基层' }
  return levels[level] || '未知'
}

const getFeatureName = (key) => {
  const names = {
    fact_similarity: '事实相似',
    claim_similarity: '诉求相似',
    dispute_similarity: '争议相似'
  }
  return names[key] || key
}

const getSimilarityColor = (score) => {
  if (score >= 0.9) return 'linear-gradient(135deg, #667eea, #764ba2)'
  if (score >= 0.8) return 'linear-gradient(135deg, #10b981, #059669)'
  if (score >= 0.7) return 'linear-gradient(135deg, #f59e0b, #d97706)'
  return 'linear-gradient(135deg, #6b7280, #4b5563)'
}

const getFeatureTagType = (score) => {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'info'
}

const viewDetail = (item) => {
  router.push(`/case-detail/${item.caseId}`)
}
</script>

<style lang="scss" scoped>
.case-similar {
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

.search-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 24px;
  }

  .search-section {
    margin-bottom: 24px;

    .section-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 14px;
      font-size: 15px;
      font-weight: 500;
      color: var(--color-text-primary);

      .el-icon {
        color: #667eea;
        font-size: 18px;
      }
    }

    :deep(.el-textarea__inner) {
      border-radius: 12px;
      padding: 16px;
      font-size: 14px;
      line-height: 1.8;
      resize: none;

      &::placeholder {
        color: var(--color-text-secondary);
      }
    }
  }

  .filter-section {
    display: flex;
    gap: 16px;
    align-items: flex-end;

    .filter-item {
      flex: 1;

      label {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 13px;
        color: var(--color-text-secondary);
        margin-bottom: 8px;

        .el-icon {
          color: #667eea;
        }
      }

      :deep(.el-select),
      :deep(.el-input) {
        width: 100%;
      }
    }

    .filter-actions {
      :deep(.el-button) {
        height: 40px;
        padding: 0 32px;
        border-radius: 10px;
        background: linear-gradient(135deg, #667eea, #764ba2);
        border: none;
        transition: all 0.3s;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
        }
      }
    }
  }
}

.results-container {
  .stats-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    margin-bottom: 24px;

    :deep(.el-card__body) {
      padding: 24px;
    }

    .stat-item {
      display: flex;
      align-items: center;
      gap: 16px;

      .stat-icon {
        width: 52px;
        height: 52px;
        border-radius: 14px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 22px;

        &.total {
          background: linear-gradient(135deg, #667eea, #764ba2);
        }

        &.win {
          background: linear-gradient(135deg, #10b981, #059669);
        }

        &.money {
          background: linear-gradient(135deg, #f59e0b, #d97706);
        }
      }

      .stat-content {
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
          color: var(--color-text-secondary);
          margin-top: 4px;
        }
      }
    }
  }
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.case-card {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .case-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 16px;

    .case-info {
      flex: 1;

      h3 {
        margin: 0 0 8px 0;
        font-size: 17px;
        font-weight: 600;
        color: var(--color-text-primary);
      }

      .case-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 13px;
        color: var(--color-text-secondary);

        .divider {
          color: #d1d5db;
        }
      }
    }

    .similarity-badge {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 64px;
      height: 64px;
      border-radius: 14px;
      color: #fff;
      flex-shrink: 0;

      .similarity-value {
        font-size: 18px;
        font-weight: 700;
      }

      .similarity-label {
        font-size: 10px;
        opacity: 0.9;
      }
    }
  }

  .matching-features {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 16px;

    .feature-label {
      font-size: 13px;
      color: var(--color-text-secondary);
    }
  }

  .case-summary {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: var(--color-text-secondary);
    line-height: 1.7;
  }

  .legal-basis {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    padding-top: 16px;
    border-top: 1px solid #f3f4f6;

    .basis-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--color-text-secondary);

      .el-icon {
        color: #667eea;
      }
    }

    .law-tag {
      border-radius: 6px;
    }
  }

  .case-footer {
    display: flex;
    justify-content: flex-end;
    padding-top: 16px;
    border-top: 1px solid #f3f4f6;
    margin-top: 16px;

    :deep(.el-button) {
      border-radius: 8px;
      display: inline-flex;
      align-items: center;
      gap: 6px;
    }
  }
}

.analysis-section {
  margin-top: 24px;

  .analysis-card {
    border-radius: 16px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);

    :deep(.el-card__header) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border: none;
      padding: 16px 20px;
    }

    .analysis-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .analysis-title {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
      }
    }
  }
}
</style>
