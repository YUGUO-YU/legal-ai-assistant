<template>
  <div class="page-card">
    <div class="page-header">
      <h2>案例查询</h2>
      <p>检索司法判例，支持多维度过滤和精准筛选</p>
    </div>

    <div class="filter-section">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索案件名称、案号" clearable />
        </el-form-item>
        <el-form-item label="案件类型">
          <el-select v-model="filters.caseType" placeholder="全部" clearable multiple>
            <el-option label="民事" :value="1" />
            <el-option label="刑事" :value="2" />
            <el-option label="行政" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="法院层级">
          <el-select v-model="filters.courtLevel" placeholder="全部" clearable multiple>
            <el-option label="最高院" :value="1" />
            <el-option label="高院" :value="2" />
            <el-option label="中院" :value="3" />
            <el-option label="基层" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="审理程序">
          <el-select v-model="filters.trialProcedure" placeholder="全部" clearable>
            <el-option label="一审" :value="1" />
            <el-option label="二审" :value="2" />
            <el-option label="再审" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁判结果">
          <el-select v-model="filters.judgmentResult" placeholder="全部" clearable>
            <el-option label="全部支持" :value="1" />
            <el-option label="部分支持" :value="2" />
            <el-option label="驳回" :value="3" />
           <el-option label="撤诉" :value="4" />
            <el-option label="调解" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="resetFilters">
            <el-icon><Refresh /></el-icon> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <loading v-if="loading" text="正在检索案例..." />

    <div v-else-if="results.length > 0">
      <div class="result-stats">
        共找到 <strong>{{ total }}</strong> 个相关案例，耗时 {{ tookMs }}ms
      </div>

      <div v-for="item in results" :key="item.caseUuid" class="result-item">
        <div class="case-header">
          <h4>{{ item.title }}</h4>
          <el-tag size="small" :type="getResultType(item.judgmentResult)">
            {{ getResultName(item.judgmentResult) }}
          </el-tag>
        </div>
        <div class="case-meta">
          <span><el-icon><OfficeBuilding /></el-icon> {{ item.court }}</span>
          <span><el-icon><Calendar /></el-icon> {{ item.judgeDate }}</span>
          <span><el-tag type="info" size="small">{{ item.trialProcedure }}</el-tag></span>
         <span class="case-type">{{ item.caseType }} | {{ item.caseCause }}</span>
        </div>
        <p class="case-summary">{{ item.summary }}</p>
        <div class="case-footer">
          <span class="source">来源：{{ item.sourceName }}</span>
          <div class="case-actions">
            <el-button text size="small" @click="viewDetail(item)">查看详情</el-button>
            <el-button text size="small" @click="copyCaseNo(item.caseNo)">复制案号</el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handleSearch"
      />
    </div>

    <empty-state
      v-else-if="searched"
      icon="Files"
      title="未找到符合条件的案例"
      description="未找到符合条件的案例，请调整筛选条件后重试"
      action-text="重置筛选"
      @action="resetFilters"
    />
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const loading = ref(false)
const results = ref([])
const total = ref(0)
const tookMs = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searched = ref(false)

const filters = reactive({
  keyword: '',
  caseType: [],
  courtLevel: [],
  trialProcedure: null,
  judgmentResult: null,
  judgeYearMin: null,
  judgeYearMax: null
})

const handleSearch = async () => {
  loading.value = true
  searched.value = true

  try {
    const res = await api.caseSearch.search({
      keyword: filters.keyword,
      caseType: filters.caseType.length > 0 ? filters.caseType : null,
      courtLevel: filters.courtLevel.length > 0 ? filters.courtLevel : null,
      trialProcedure: filters.trialProcedure,
      judgmentResult: filters.judgmentResult,
      judgeYearMin: filters.judgeYearMin,
      judgeYearMax: filters.judgeYearMax,
      page: page.value,
      pageSize: pageSize.value
    })

    results.value = res.data.items || []
    total.value = res.data.total || 0
    tookMs.value = res.data.tookMs || 0
  } catch (e) {
    console.error(e)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.keyword = ''
  filters.caseType = []
  filters.courtLevel = []
  filters.trialProcedure = null
  filters.judgmentResult = null
  filters.judgeYearMin = null
  filters.judgeYearMax = null
  page.value = 1
  handleSearch()
}

const getResultType = (result) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info', 5: 'success' }
  return types[result] || 'info'
}

const getResultName = (result) => {
  const names = { 1: '全部支持', 2: '部分支持', 3: '驳回', 4: '撤诉', 5: '调解' }
  return names[result] || '未知'
}

const viewDetail = (item) => {
  ElMessage.info('案例详情页开发中...')
}

const copyCaseNo = (caseNo) => {
  navigator.clipboard.writeText(caseNo)
  ElMessage.success('案号已复制')
}
</script>

<style lang="scss" scoped>
.filter-section {
  background: #fafafa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 24px;
}

.loading-state {
  text-align: center;
  padding: 48px;
  color: #999;
}

.result-stats {
  margin-bottom: 16px;
  color: #666;
  strong {
    color: #1890ff;
  }
}

.result-item {
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 16px;
  transition: all 0.3s;
  &:hover {
    border-color: #1890ff;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.15);
  }
}

.case-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  h4 {
    margin: 0;
    font-size: 16px;
    flex: 1;
  }
}

.case-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  color: #666;
  font-size: 14px;

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .case-type {
    color: #999;
    font-size: 13px;
  }
}

.case-summary {
  margin: 0 0 16px 0;
  line-height: 1.6;
  color: #333;
}

.case-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;

  .source {
    color: #999;
    font-size: 13px;
  }

  .case-actions {
    display: flex;
    gap: 8px;
  }
}

.empty-state {
  padding: 48px 0;
}
</style>