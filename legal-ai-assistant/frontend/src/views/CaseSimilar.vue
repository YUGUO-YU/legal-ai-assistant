<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI类案</h2>
      <p>输入案件描述，智能匹配相似判例供参考</p>
    </div>

    <div class="search-box">
      <el-input
        v-model="caseDescription"
        type="textarea"
        :rows="5"
        placeholder="请详细描述案件情况，包括：当事人、案由、事实经过、诉讼请求等"
        size="large"
      />
    </div>

    <div class="filter-row">
      <el-select v-model="caseType" placeholder="案件类型" clearable>
        <el-option label="民事" :value="1" />
        <el-option label="刑事" :value="2" />
        <el-option label="行政" :value="3" />
      </el-select>
      <el-input v-model="caseCause" placeholder="案由" clearable />
      <el-button type="primary" @click="handleSearch" :loading="loading">查找类案</el-button>
    </div>

    <loading v-if="loading" text="正在检索相似案例..." />

    <div v-else-if="results.length > 0">
      <div class="stats-panel">
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.totalCount }}</div>
              <div class="stat-label">相似案例</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ (statistics.winRate * 100).toFixed(0) }}%</div>
              <div class="stat-label">胜诉率</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-value">{{ statistics.avgCompensation }}</div>
              <div class="stat-label">平均判赔</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div v-for="item in results" :key="item.caseId" class="result-item">
        <div class="case-header">
          <h4>{{ item.caseName }}</h4>
          <span class="similarity-badge">相似度 {{ (item.similarityScore * 100).toFixed(0) }}%</span>
        </div>
        <p class="case-info">
          {{ getCourtLevel(item.courtLevel) }} | {{ item.courtName }} | {{ item.judgeDate }}
        </p>
        <div class="matching-features">
          <span v-for="(score, key) in item.matchingFeatures" :key="key" class="feature-tag">
            {{ getFeatureName(key) }}: {{ (score * 100).toFixed(0) }}%
          </span>
        </div>
        <p class="case-summary">{{ item.judgmentSummary }}</p>
        <div class="legal-basis">
          <strong>法律依据：</strong>
          <span v-for="law in item.legalBasis" :key="law" class="law-tag">{{ law }}</span>
        </div>
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
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const caseDescription = ref('')
const caseType = ref(null)
const caseCause = ref('')
const loading = ref(false)
const results = ref([])
const hasSearched = ref(false)
const statistics = ref({ totalCount: 0, winRate: 0, avgCompensation: 0 })

const handleSearch = async () => {
  if (!caseDescription.value.trim()) return

  loading.value = true
  hasSearched.value = true
  try {
    const res = await api.caseSimilar.search({
      caseDescription: caseDescription.value,
      caseType: caseType.value,
      caseCause: caseCause.value
    })
    results.value = res.data.items || []
    statistics.value = res.data.statistics || {}
  } catch (e) {
    console.error(e)
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
    fact_similarity: '事实相似度',
    claim_similarity: '诉求相似度',
    dispute_similarity: '争议相似度'
  }
  return names[key] || key
}
</script>

<style lang="scss" scoped>
.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.stats-panel {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 24px;
  .stat-item {
    text-align: center;
    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #1890ff;
    }
    .stat-label {
      color: #666;
      font-size: 14px;
    }
  }
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  h4 { margin: 0; }
}

.similarity-badge {
  background: #1890ff;
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
}

.case-info {
  color: #666;
  font-size: 14px;
  margin: 8px 0;
}

.matching-features {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  .feature-tag {
    background: #e6f7ff;
    color: #1890ff;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 12px;
  }
}

.case-summary {
  margin: 0 0 12px 0;
  line-height: 1.6;
}

.legal-basis {
  font-size: 13px;
  .law-tag {
    background: #f5f5f5;
    padding: 2px 8px;
    border-radius: 4px;
    margin-right: 8px;
  }
}
</style>