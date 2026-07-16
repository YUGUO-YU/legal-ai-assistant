<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">搜索反馈统计</h2>
        <p>用户对搜索结果的满意度跟踪与分析</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="kpi-row">
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card" :body-style="{ padding: '14px' }">
          <div class="kpi-label">总反馈数</div>
          <div class="kpi-value">{{ stats.totalFeedbacks ?? '-' }}</div>
          <div class="kpi-foot">搜索结果反馈</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card helpful" :body-style="{ padding: '14px' }">
          <div class="kpi-label">有用</div>
          <div class="kpi-value">{{ stats.helpfulCount ?? '-' }}</div>
          <div class="kpi-foot">is_helpful = 1</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card unhelpful" :body-style="{ padding: '14px' }">
          <div class="kpi-label">无用</div>
          <div class="kpi-value">{{ stats.unhelpfulCount ?? '-' }}</div>
          <div class="kpi-foot">is_helpful = 0</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card class="kpi-card rate" :body-style="{ padding: '14px' }">
          <div class="kpi-label">有用率</div>
          <div class="kpi-value">{{ stats.helpfulRate ?? '-' }}%</div>
          <div class="kpi-foot">整体满意度</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="chart-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>每日反馈趋势（近7天）</span>
              <el-tag size="small" type="info">每日汇总</el-tag>
            </div>
          </template>
          <div class="chart-area">
            <svg viewBox="0 0 600 180" preserveAspectRatio="none" class="line-chart" v-if="chartData.length > 0">
              <defs>
                <linearGradient id="gradHelpful" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#10b981" stop-opacity="0.5" />
                  <stop offset="100%" stop-color="#10b981" stop-opacity="0" />
                </linearGradient>
                <linearGradient id="gradUnhelpful" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#ef4444" stop-opacity="0.4" />
                  <stop offset="100%" stop-color="#ef4444" stop-opacity="0" />
                </linearGradient>
              </defs>
              <g v-for="(g, i) in gridLines" :key="'g'+i">
                <line :x1="0" :x2="600" :y1="g.y" :y2="g.y" stroke="#e2e8f0" stroke-dasharray="3 3" stroke-width="1" />
              </g>
              <polyline v-if="helpfulPoints" :points="helpfulPoints" fill="none" stroke="#10b981" stroke-width="2" />
              <polygon v-if="helpfulAreaPoints" :points="helpfulAreaPoints" fill="url(#gradHelpful)" opacity="0.3" />
              <polyline v-if="unhelpfulPoints" :points="unhelpfulPoints" fill="none" stroke="#ef4444" stroke-width="2" stroke-dasharray="4 2" />
              <polygon v-if="unhelpfulAreaPoints" :points="unhelpfulAreaPoints" fill="url(#gradUnhelpful)" opacity="0.2" />
              <g v-for="(p, i) in chartCircles" :key="'c'+i">
                <circle :cx="p.x" :cy="p.helpfulY" r="3" fill="#10b981" />
                <circle :cx="p.x" :cy="p.unhelpfulY" r="3" fill="#ef4444" />
                <text :x="p.x" :y="175" font-size="10" fill="#64748b" text-anchor="middle">{{ p.label }}</text>
              </g>
              <g class="legend" transform="translate(450, 10)">
                <rect x="0" y="0" width="12" height="12" fill="#10b981" rx="2" />
                <text x="18" y="10" font-size="11" fill="#64748b">有用</text>
                <rect x="0" y="18" width="12" height="12" fill="#ef4444" rx="2" />
                <text x="18" y="28" font-size="11" fill="#64748b">无用</text>
              </g>
            </svg>
            <el-empty v-else description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="glass table-card">
      <template #header>
        <div class="card-header">
          <span>反馈列表</span>
          <div class="filter-area">
            <el-input v-model="filter.articleId" placeholder="法规ID" size="small" style="width: 120px; margin-right: 8px;" clearable @keyup.enter="loadList" />
            <el-select v-model="filter.isHelpful" placeholder="是否有用" size="small" style="width: 120px; margin-right: 8px;" clearable>
              <el-option label="有用" :value="1" />
              <el-option label="无用" :value="0" />
            </el-select>
            <el-date-picker v-model="filter.dateRange" type="daterange" size="small" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="yyyy-MM-dd" style="margin-right: 8px;" @change="handleDateChange" />
            <el-button size="small" type="primary" @click="loadList">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" stripe size="small" v-loading="loading" max-height="400">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="article_id" label="法规ID" width="100" />
        <el-table-column prop="article_title" label="法规标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="is_helpful" label="是否有用" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.is_helpful === 1" type="success" size="small">有用</el-tag>
            <el-tag v-else-if="row.is_helpful === 0" type="danger" size="small">无用</el-tag>
            <el-tag v-else type="info" size="small">未评价</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="user_comment" label="用户评论" min-width="200" show-overflow-tooltip />
        <el-table-column prop="created_at" label="反馈时间" width="170" />
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          :background="true"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '@/api'

const stats = ref({})
const list = ref([])
const loading = ref(false)
const filter = ref({
  articleId: null,
  isHelpful: null,
  dateRange: null
})
const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0
})

const chartData = computed(() => stats.value.dailyStats || [])

const chartCircles = computed(() => {
  const t = chartData.value
  if (t.length < 1) return []
  const max = Math.max(...t.map(x => (Number(x.total) || 0)), 1)
  return t.map((x, i) => {
    const x0 = t.length === 1 ? 300 : (i / (t.length - 1)) * 600
    const helpfulY = 160 - ((Number(x.helpful) || 0) / max) * 120
    const unhelpfulY = 160 - ((Number(x.unhelpful) || 0) / max) * 120
    return {
      x: x0,
      helpfulY,
      unhelpfulY,
      label: String(x.day).slice(5)
    }
  })
})

const helpfulPoints = computed(() => {
  const t = chartData.value
  if (t.length < 2) return ''
  const max = Math.max(...t.map(x => (Number(x.total) || 0)), 1)
  return t.map((x, i) => {
    const x0 = (i / (t.length - 1)) * 600
    const y0 = 160 - ((Number(x.helpful) || 0) / max) * 120
    return `${x0.toFixed(1)},${y0.toFixed(1)}`
  }).join(' ')
})

const unhelpfulPoints = computed(() => {
  const t = chartData.value
  if (t.length < 2) return ''
  const max = Math.max(...t.map(x => (Number(x.total) || 0)), 1)
  return t.map((x, i) => {
    const x0 = (i / (t.length - 1)) * 600
    const y0 = 160 - ((Number(x.unhelpful) || 0) / max) * 120
    return `${x0.toFixed(1)},${y0.toFixed(1)}`
  }).join(' ')
})

const helpfulAreaPoints = computed(() => {
  if (!helpfulPoints.value) return ''
  return `0,160 ${helpfulPoints.value} 600,160`
})

const unhelpfulAreaPoints = computed(() => {
  if (!unhelpfulPoints.value) return ''
  return `0,160 ${unhelpfulPoints.value} 600,160`
})

const gridLines = [
  { y: 40 },
  { y: 80 },
  { y: 120 },
  { y: 160 }
]

function handleDateChange(val) {
  if (val && val.length === 2) {
    filter.value.startDate = val[0]
    filter.value.endDate = val[1]
  } else {
    filter.value.startDate = null
    filter.value.endDate = null
  }
}

async function loadStats() {
  try {
    const res = await api.get('/admin/infra/search-feedback/stats', {
      params: {
        startDate: filter.value.startDate,
        endDate: filter.value.endDate
      }
    })
    stats.value = res.data || {}
  } catch (e) {
    stats.value = {}
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/search-feedback', {
      params: {
        articleId: filter.value.articleId || undefined,
        isHelpful: filter.value.isHelpful ?? undefined,
        startDate: filter.value.startDate || undefined,
        endDate: filter.value.endDate || undefined,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize
      }
    })
    list.value = res.data?.list || []
    pagination.value.total = res.data?.total || 0
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await Promise.all([loadStats(), loadList()])
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.search-feedback-page { animation: adminFadeIn 0.4s ease; }


.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
}

.kpi-row { margin-bottom: 14px; }

.kpi-card {
  border-left: 4px solid var(--color-border-dark);
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  &.helpful { border-left-color: var(--color-success); }
  &.unhelpful { border-left-color: var(--color-danger); }
  &.rate { border-left-color: var(--color-info); }

  .kpi-label { font-size: 12px; color: var(--color-text-secondary); }
  .kpi-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary); margin: 4px 0; }
  .kpi-foot { font-size: 11px; color: var(--color-text-placeholder); }
}

.chart-row { margin-bottom: 14px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .filter-area {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
}

.chart-area {
  width: 100%;
  height: 200px;
}

.line-chart {
  width: 100%;
  height: 100%;
  display: block;
}

.table-card {
  :deep(.el-card__header) {
    padding-bottom: 8px;
  }
}
</style>
