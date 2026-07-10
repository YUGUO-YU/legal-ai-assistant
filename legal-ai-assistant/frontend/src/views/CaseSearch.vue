<template>
  <div class="case-search">
    <div class="page-header">
      <div class="header-content">
        <h2>案例查询</h2>
        <p>检索司法判例，支持多维度过滤和精准筛选</p>
      </div>
    </div>

    <el-card class="filter-card">
      <div class="filter-section">
        <div class="filter-row">
          <div class="filter-item">
            <label>
              <el-icon><Search /></el-icon>
              关键词
            </label>
            <el-input v-model="filters.keyword" placeholder="搜索案件名称、案号" clearable />
          </div>
          <div class="filter-item">
            <label>
              <el-icon><FolderOpened /></el-icon>
              案件类型
            </label>
            <el-select v-model="filters.caseType" placeholder="全部" multiple clearable>
              <el-option label="民事" :value="1" />
              <el-option label="刑事" :value="2" />
              <el-option label="行政" :value="3" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>
              <el-icon><OfficeBuilding /></el-icon>
              法院层级
            </label>
            <el-select v-model="filters.courtLevel" placeholder="全部" multiple clearable>
              <el-option label="最高院" :value="1" />
              <el-option label="高院" :value="2" />
              <el-option label="中院" :value="3" />
              <el-option label="基层" :value="4" />
            </el-select>
          </div>
        </div>
        <div class="filter-row">
          <div class="filter-item">
            <label>
              <el-icon><Document /></el-icon>
              审理程序
            </label>
            <el-select v-model="filters.trialProcedure" placeholder="全部" clearable>
              <el-option label="一审" :value="1" />
              <el-option label="二审" :value="2" />
              <el-option label="再审" :value="3" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>
              <el-icon><SuccessFilled /></el-icon>
              裁判结果
            </label>
            <el-select v-model="filters.judgmentResult" placeholder="全部" clearable>
              <el-option label="全部支持" :value="1" />
              <el-option label="部分支持" :value="2" />
              <el-option label="驳回" :value="3" />
              <el-option label="撤诉" :value="4" />
              <el-option label="调解" :value="5" />
            </el-select>
          </div>
          <div class="filter-actions">
            <el-button type="primary" @click="handleSearch" :loading="loading">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="resetFilters">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <div v-if="loading" class="skeleton-cases">
      <div v-for="i in 6" :key="i" class="skeleton-case-card">
        <div class="skeleton-case-header skeleton"></div>
        <div class="skeleton-case-content skeleton"></div>
      </div>
    </div>

    <div v-else-if="results.length > 0" class="results-container">
      <div class="result-stats">
        <div class="stats-info">
          <el-icon><Document /></el-icon>
          <span>共找到 <strong>{{ total }}</strong> 个相关案例</span>
        </div>
        <div class="stats-time">
          <el-icon><Clock /></el-icon>
          <span>耗时 {{ tookMs }}ms</span>
        </div>
      </div>

      <div class="result-list">
        <el-card
          v-for="item in results"
          :key="item.caseUuid"
          class="result-item"
        >
          <div class="case-header">
            <div class="case-info">
              <h3>{{ item.title }}</h3>
              <div class="case-meta">
                <el-tag size="small" type="info">
                  <el-icon><OfficeBuilding /></el-icon>
                  {{ item.court }}
                </el-tag>
                <el-tag size="small" type="info">
                  <el-icon><Calendar /></el-icon>
                  {{ item.judgeDate }}
                </el-tag>
                <el-tag size="small">{{ item.trialProcedure }}</el-tag>
                <span class="case-type">{{ item.caseType }} | {{ item.caseCause }}</span>
              </div>
            </div>
            <el-tag :type="getResultType(item.judgmentResult)" effect="dark" round>
              {{ getResultName(item.judgmentResult) }}
            </el-tag>
          </div>

          <p class="case-summary">{{ item.summary }}</p>

          <div class="case-footer">
            <div class="source">
              <el-icon><Link /></el-icon>
              <span>来源：{{ item.sourceName }}</span>
            </div>
            <div class="case-actions">
              <el-button type="primary" link @click="viewDetail(item)">
                <el-icon><View /></el-icon>
                查看详情
              </el-button>
              <el-button type="primary" link @click="copyCaseNo(item.caseNo)">
                <el-icon><CopyDocument /></el-icon>
                复制案号
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
        />
      </div>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  FolderOpened,
  OfficeBuilding,
  Document,
  SuccessFilled,
  Refresh,
  Calendar,
  Clock,
  Link,
  CopyDocument
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
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
  router.push(`/case-detail/${item.caseUuid}`)
}

const copyCaseNo = (caseNo) => {
  navigator.clipboard.writeText(caseNo)
  ElMessage.success('案号已复制')
}
</script>

<style lang="scss" scoped>
.case-search {
  animation: fadeIn 0.4s ease;
}

.skeleton-cases {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-case-card {
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);

  .skeleton-case-header {
    height: 60px;
    margin-bottom: 16px;
    border-radius: 8px;
  }

  .skeleton-case-content {
    height: 40px;
    border-radius: 8px;
  }
}

.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
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
      color: #6b7280;
      font-size: 14px;
    }
  }
}

.filter-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.filter-section {
  .filter-row {
    display: flex;
    gap: 20px;
    margin-bottom: 16px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .filter-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;

    label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      font-weight: 500;
      color: #4b5563;

      .el-icon {
        color: #667eea;
      }
    }
  }

  .filter-actions {
    display: flex;
    gap: 12px;
    align-items: flex-end;

    :deep(.el-button:first-child) {
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
      border-radius: 10px;
      padding: 12px 24px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
      }
    }

    :deep(.el-button:last-child) {
      border-radius: 10px;
      padding: 12px 20px;
    }
  }
}

.results-container {
  .result-stats {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px 20px;
    background: #fff;
    border-radius: 14px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);

    .stats-info {
      display: flex;
      align-items: center;
      gap: 10px;
      color: #6b7280;
      font-size: 14px;

      .el-icon {
        color: #667eea;
        font-size: 18px;
      }

      strong {
        color: #667eea;
        font-weight: 600;
      }
    }

    .stats-time {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #9ca3af;
      font-size: 13px;

      .el-icon {
        font-size: 16px;
      }
    }
  }
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
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
    margin-bottom: 14px;

    .case-info {
      flex: 1;

      h3 {
        margin: 0 0 10px 0;
        font-size: 17px;
        font-weight: 600;
        color: #1f2937;
      }

      .case-meta {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 10px;

        .el-tag {
          border-radius: 6px;
        }

        .case-type {
          font-size: 13px;
          color: #9ca3af;
        }
      }
    }
  }

  .case-summary {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: #4b5563;
    line-height: 1.7;
  }

  .case-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 14px;
    border-top: 1px solid #f3f4f6;

    .source {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #9ca3af;

      .el-icon {
        font-size: 16px;
      }
    }

    .case-actions {
      display: flex;
      gap: 8px;

      :deep(.el-button) {
        border-radius: 8px;
        padding: 8px 16px;
      }
    }
  }
}

.pagination-wrapper {
  margin-top: 32px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 8px;

      &.is-active {
        background: linear-gradient(135deg, #667eea, #764ba2);
      }
    }
  }
}
</style>
