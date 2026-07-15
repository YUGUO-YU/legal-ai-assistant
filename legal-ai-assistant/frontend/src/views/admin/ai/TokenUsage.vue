<template>
  <div class="token-page">
    <div class="page-header">
      <div class="header-content">
        <h2>Token 用量分析</h2>
        <p>AI 能力域 · 按模块 & 按模型 · 近 30 日走势 + 模块分布</p>
      </div>
      <div class="header-actions">
        <el-select v-model="range" style="width:120px">
          <el-option label="近 7 日" value="7" />
          <el-option label="近 30 日" value="30" />
          <el-option label="近 90 日" value="90" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <div class="kpi-card" style="border-left: 4px solid var(--color-primary)">
          <div class="kpi-label">今日 Token</div>
          <div class="kpi-value">{{ summary.todayTokens.toLocaleString() }}</div>
          <div class="kpi-sub">调用 {{ summary.todayCalls.toLocaleString() }} 次</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left: 4px solid var(--color-success)">
          <div class="kpi-label">{{ range }} 日总量</div>
          <div class="kpi-value">{{ summary.periodTokens.toLocaleString() }}</div>
          <div class="kpi-sub">日均 {{ summary.avgDaily.toLocaleString() }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left: 4px solid var(--color-warning)">
          <div class="kpi-label">预估月成本</div>
          <div class="kpi-value">¥{{ summary.estimatedCost.toLocaleString() }}</div>
          <div class="kpi-sub">≈ ${{ (summary.estimatedCost / 7.2).toFixed(0) }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left: 4px solid var(--color-info)">
          <div class="kpi-label">活跃模型</div>
          <div class="kpi-value">{{ summary.activeModels }}</div>
          <div class="kpi-sub">共 {{ summary.totalModels }} 个注册</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="13">
        <el-card>
          <template #header>
            <span>近 {{ range }} 日用量趋势</span>
          </template>
          <svg :viewBox="`0 0 ${chartW} ${chartH}`" class="trend-chart" v-if="trendPoints.length">
            <defs>
              <linearGradient id="tokenGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#6366f1" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#6366f1" stop-opacity="0.02" />
              </linearGradient>
              <linearGradient id="costGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#f59e0b" stop-opacity="0.28" />
                <stop offset="100%" stop-color="#f59e0b" stop-opacity="0.02" />
              </linearGradient>
            </defs>
            <!-- grid lines -->
            <line v-for="(_, i) in 5" :key="'g'+i" :x1="margin" :y1="margin + i * gridStepY" :x2="chartW - marginR" :y2="margin + i * gridStepY" stroke="#e2e8f0" stroke-width="1" />
            <!-- x labels -->
            <text v-for="(p, i) in labeledX" :key="'xl'+i" :x="p.x" :y="chartH - 8" text-anchor="middle" font-size="11" fill="#64748b">{{ p.label }}</text>
            <!-- token area -->
            <polygon :points="trendArea" fill="url(#tokenGrad)" />
            <polyline :points="trendLine" fill="none" stroke="#6366f1" stroke-width="2.2" stroke-linejoin="round" />
            <!-- cost area -->
            <polygon :points="costArea" fill="url(#costGrad)" />
            <polyline :points="costLine" fill="none" stroke="#f59e0b" stroke-width="2.2" stroke-linejoin="round" stroke-dasharray="6,2" />
            <!-- legend -->
            <rect :x="chartW - marginR - 200" y="4" width="12" height="12" rx="2" fill="#6366f1" />
            <text :x="chartW - marginR - 184" y="14" font-size="11" fill="#334155">Token</text>
            <rect :x="chartW - marginR - 100" y="4" width="12" height="12" rx="2" fill="#f59e0b" />
            <text :x="chartW - marginR - 84" y="14" font-size="11" fill="#334155">成本 (¥)</text>
          </svg>
          <div v-else class="empty-hint">暂无用量数据</div>
        </el-card>
      </el-col>

      <el-col :span="11">
        <el-card>
          <template #header>
            <span>模块分布</span>
          </template>
          <div v-if="moduleDistribution.length" class="module-bars">
            <div v-for="(m, i) in moduleDistribution" :key="m.name" class="bar-row">
              <span class="bar-label">{{ m.name }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: m.pct + '%', background: palette[i % palette.length] }">{{ m.pct.toFixed(1) }}%</div>
              </div>
              <span class="bar-value mono">{{ m.amount.toLocaleString() }}</span>
            </div>
          </div>
          <div v-else class="empty-hint">暂无分布数据</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <template #header>
        <span>详细记录</span>
      </template>
      <el-table :data="rows" v-loading="loading" stripe border size="small" max-height="400">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="model_name" label="模型" min-width="160" />
        <el-table-column prop="module" label="模块" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="total_tokens" label="Token" width="100">
          <template #default="{ row }">
            <span class="mono">{{ row.total_tokens.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="prompt_tokens" label="输入" width="90" />
        <el-table-column prop="completion_tokens" label="输出" width="90" />
        <el-table-column prop="cost" label="费用" width="100">
          <template #default="{ row }">
            <span class="mono">¥{{ row.cost != null ? Number(row.cost).toFixed(6) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="latency_ms" label="延迟" width="90">
          <template #default="{ row }">
            <span :class="row.latency_ms < 500 ? 'text-success' : row.latency_ms < 2000 ? 'text-warning' : 'text-danger'">{{ row.latency_ms }} ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="时间" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        class="pager"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../../api'

const range = ref('7')
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const rawTrendData = ref([])
const rawModuleData = ref([])

const palette = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316', '#3b82f6']

const summary = reactive({ todayTokens: 0, todayCalls: 0, periodTokens: 0, avgDaily: 0, estimatedCost: 0, activeModels: 0, totalModels: 0 })

const margin = 32
const marginR = 20
const chartW = 580
const chartH = 280
const gridStepY = computed(() => (chartH - margin - 28) / 4)

const trendPoints = computed(() => {
  if (!rawTrendData.value.length) return []
  const days = Number(range.value)
  const stepX = (chartW - margin - marginR) / Math.max(days - 1, 1)
  return rawTrendData.value.map((d, i) => ({
    x: margin + i * stepX,
    tokens: d.total_tokens || 0,
    cost: d.cost || 0,
    label: d.date?.substring(5) || ''
  }))
})

const tokenMax = computed(() => Math.max(...trendPoints.value.map(p => p.tokens), 1))
const costMax = computed(() => Math.max(...trendPoints.value.map(p => p.cost), 1))

const trendLine = computed(() =>
  trendPoints.value.map(p => `${p.x},${margin + gridStepY.value * 4 * (1 - p.tokens / tokenMax.value)}`).join(' ')
)
const trendArea = computed(() => {
  const pts = trendPoints.value
  return `${pts[0].x},${chartH - 28} ${trendLine.value} ${pts[pts.length - 1].x},${chartH - 28}`
})
const costLine = computed(() =>
  trendPoints.value.map(p => `${p.x},${margin + gridStepY.value * 4 * (1 - p.cost / costMax.value)}`).join(' ')
)
const costArea = computed(() => {
  const pts = trendPoints.value
  return `${pts[0].x},${chartH - 28} ${costLine.value} ${pts[pts.length - 1].x},${chartH - 28}`
})

const labeledX = computed(() => {
  const pts = trendPoints.value
  if (!pts.length) return []
  const labels = []
  const step = Math.max(1, Math.floor(pts.length / 6))
  for (let i = 0; i < pts.length; i += step) labels.push({ x: pts[i].x, label: pts[i].label })
  if (labels.length && labels[labels.length - 1] !== pts[pts.length - 1]) {
    labels.push({ x: pts[pts.length - 1].x, label: pts[pts.length - 1].label })
  }
  return labels
})

const moduleDistribution = computed(() => {
  const map = {}
  rawModuleData.value.forEach(r => {
    const key = r.module || '未知'
    map[key] = (map[key] || 0) + (r.total_tokens || 0)
  })
  const totalTokens = Object.values(map).reduce((s, v) => s + v, 0)
  return Object.entries(map)
    .map(([name, amount]) => ({ name, amount, pct: totalTokens ? (amount / totalTokens) * 100 : 0 }))
    .sort((a, b) => b.amount - a.amount)
})

async function load() {
  loading.value = true
  try {
    const [res, trendRes] = await Promise.all([
      api.get('/admin/ai/token-usage', { params: { page: page.value, pageSize: pageSize.value } }),
      api.get('/admin/monitor/overview')
    ])
    const data = res.data || {}
    rows.value = data.list || []
    total.value = data.total || 0
    rawModuleData.value = data.list || []
    const ov = trendRes.data || {}
    rawTrendData.value = ov.dailyTokens || []
    summary.todayTokens = ov.todayTokens || 0
    summary.todayCalls = ov.todayCalls || 0
    summary.periodTokens = ov.periodTokens || rawTrendData.value.reduce((s, d) => s + (d.total_tokens || 0), 0)
    summary.avgDaily = range.value > 0 ? Math.round(summary.periodTokens / Number(range.value)) : 0
    summary.estimatedCost = Math.round(summary.periodTokens / Number(range.value) * 30 / 1000 * 0.02)
    summary.activeModels = ov.activeModels || 0
    summary.totalModels = ov.totalModels || 0
  } catch (e) {
    rows.value = []; total.value = 0; rawTrendData.value = []; rawModuleData.value = []
  } finally {
    loading.value = false
  }
}

watch([range, page, pageSize], load, { immediate: false })
onMounted(load)
</script>

<style lang="scss" scoped>
.token-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.kpi-card {
  background: var(--color-bg-card);
  border-radius: 10px;
  padding: 16px;
  border: 1px solid var(--color-border);
  .kpi-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; }
  .kpi-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary); }
  .kpi-sub { font-size: 12px; color: var(--color-text-muted); margin-top: 4px; }
}
.trend-chart {
  width: 100%; height: auto;
}
.empty-hint {
  height: 200px;
  display: flex; align-items: center; justify-content: center;
  color: var(--color-text-muted); font-size: 13px;
}
.module-bars {
  .bar-row {
    display: flex; align-items: center; gap: 10px; margin-bottom: 10px;
    .bar-label { width: 90px; font-size: 12px; color: #475569; text-align: right; flex-shrink: 0; }
    .bar-track {
      flex: 1; height: 22px; background: #f1f5f9; border-radius: 6px; overflow: hidden;
      .bar-fill {
        height: 100%; border-radius: 6px; font-size: 11px; color: #fff;
        display: flex; align-items: center; padding: 0 8px;
        min-width: 32px; transition: width 0.5s ease;
      }
    }
    .bar-value { width: 70px; font-size: 12px; color: #334155; text-align: right; flex-shrink: 0; }
  }
}
.text-success { color: #10b981; font-weight: 600; }
.text-warning { color: #f59e0b; font-weight: 600; }
.text-danger { color: #ef4444; font-weight: 600; }
.mono { font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 13px; }
.pager { margin-top: 14px; justify-content: flex-end; display: flex; }
</style>