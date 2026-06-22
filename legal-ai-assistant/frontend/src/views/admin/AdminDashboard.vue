<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <div class="header-content">
        <h2>后台管理 · 监控大盘</h2>
        <p>实时指标 · 待办 · 趋势 · 6 大域 / 27 张管理表</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="4" v-for="m in kpis" :key="m.label">
        <el-card class="kpi-card" :body-style="{ padding: '14px' }" :class="m.tone">
          <div class="kpi-label">{{ m.label }}</div>
          <div class="kpi-value">{{ m.value }}</div>
          <div class="kpi-foot">{{ m.foot }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="charts-row">
      <el-col :xs="24" :md="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>近 7 日 Token 用量趋势</span>
              <el-tag size="small" type="info">{{ totalTokensFmt }} tokens · ¥{{ totalCostFmt }}</el-tag>
            </div>
          </template>
          <div class="chart-area">
            <svg viewBox="0 0 600 180" preserveAspectRatio="none" class="line-chart">
              <g v-for="(g, i) in gridLines" :key="'g'+i">
                <line :x1="0" :x2="600" :y1="g.y" :y2="g.y" stroke="#e2e8f0" stroke-dasharray="3 3" stroke-width="1" />
                <text :x="0" :y="g.y - 4" font-size="10" fill="#94a3b8">{{ g.label }}</text>
              </g>
              <polyline v-if="tokenPoints.length > 1" :points="tokenPoints" fill="none" stroke="#3b82f6" stroke-width="2" />
              <polygon v-if="tokenPoints.length > 1" :points="tokenAreaPoints" fill="url(#grad)" opacity="0.25" />
              <defs>
                <linearGradient id="grad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.6" />
                  <stop offset="100%" stop-color="#3b82f6" stop-opacity="0" />
                </linearGradient>
              </defs>
              <g v-for="(p, i) in tokenCircles" :key="'c'+i">
                <circle :cx="p.x" :cy="p.y" r="3" fill="#3b82f6" />
                <text :x="p.x" :y="180 - 8" font-size="10" fill="#64748b" text-anchor="middle">{{ p.label }}</text>
              </g>
              <text v-if="tokenPoints.length === 0" x="300" y="90" font-size="13" fill="#94a3b8" text-anchor="middle">暂无数据</text>
            </svg>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>各模块 Token 分布</span>
              <el-tag size="small" type="info">近 7 日</el-tag>
            </div>
          </template>
          <div class="bar-chart">
            <div v-for="b in moduleBars" :key="b.module" class="bar-row">
              <div class="bar-label">{{ b.module || '未分类' }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: b.pct + '%', background: b.color }"></div>
              </div>
              <div class="bar-value">{{ formatNum(b.tokens) }}</div>
            </div>
            <el-empty v-if="moduleBars.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="bottom-row">
      <el-col :xs="24" :md="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近告警</span>
              <el-button link type="primary" @click="$router.push('/admin/monitor/history')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentAlerts" stripe size="small" :max-height="280">
            <el-table-column prop="id" label="#" width="60" />
            <el-table-column prop="rule_id" label="规则ID" width="90" />
            <el-table-column label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.level)" size="small">{{ levelLabel(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="triggered_at" label="触发时间" width="170" />
            <el-table-column prop="resolved_at" label="解决时间" width="170">
              <template #default="{ row }">
                <span v-if="row.resolved_at">{{ row.resolved_at }}</span>
                <el-tag v-else type="danger" size="small">未解决</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>各业务模块治理入口</span>
              <el-tag size="small">10 MOD</el-tag>
            </div>
          </template>
          <div class="quick-grid">
            <div v-for="d in quickModules" :key="d.path" class="quick-item" @click="$router.push(d.path)">
              <div class="quick-tag" :class="d.tag">{{ d.code }}</div>
              <div class="quick-name">{{ d.name }}</div>
              <div class="quick-desc">{{ d.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../api'

const counts = ref({})
const overview = ref({})
const recentAlerts = ref([])
const loading = ref(false)
let timer = null

const kpis = computed(() => [
  { label: '在线告警', value: overview.value.activeAlerts ?? '-', foot: '未解决', tone: 'danger' },
  { label: '待审法规', value: overview.value.pendingLaws ?? '-', foot: 'MOD-01', tone: 'warning' },
  { label: '待复核草稿', value: overview.value.pendingDrafts ?? '-', foot: 'MOD-03', tone: 'warning' },
  { label: '待处理反馈', value: overview.value.pendingFeedback ?? '-', foot: '运营域', tone: 'info' },
  { label: '7日 Token', value: formatNum(overview.value.weeklyTokens), foot: '全部模块', tone: 'primary' },
  { label: '7日成本', value: '¥' + (overview.value.weeklyCost ?? 0), foot: 'LLM 用量', tone: 'primary' }
])

const moduleBars = computed(() => {
  const list = overview.value.moduleTokens || []
  if (!list.length) return []
  const max = Math.max(...list.map(x => Number(x.tokens) || 0))
  const palette = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1']
  return list.map((x, i) => ({
    module: x.module,
    tokens: Number(x.tokens) || 0,
    pct: max ? Math.round((Number(x.tokens) / max) * 100) : 0,
    color: palette[i % palette.length]
  }))
})

const tokenTrend = computed(() => overview.value.tokenTrend || [])

const tokenPoints = computed(() => {
  const t = tokenTrend.value
  if (t.length < 2) return ''
  const max = Math.max(...t.map(x => Number(x.tokens) || 1))
  return t.map((x, i) => {
    const x0 = (i / (t.length - 1)) * 600
    const y0 = 180 - 30 - ((Number(x.tokens) || 0) / max) * 120
    return `${x0.toFixed(1)},${y0.toFixed(1)}`
  }).join(' ')
})

const tokenAreaPoints = computed(() => {
  if (!tokenPoints.value) return ''
  return `0,180 ${tokenPoints.value} 600,180`
})

const tokenCircles = computed(() => {
  const t = tokenTrend.value
  if (t.length < 2) return []
  const max = Math.max(...t.map(x => Number(x.tokens) || 1))
  return t.map((x, i) => {
    const x0 = (i / (t.length - 1)) * 600
    const y0 = 180 - 30 - ((Number(x.tokens) || 0) / max) * 120
    return { x: x0, y: y0, label: String(x.day).slice(5) }
  })
})

const gridLines = [
  { y: 30, label: '' },
  { y: 75, label: '' },
  { y: 120, label: '' },
  { y: 165, label: '' }
]

const totalTokensFmt = computed(() => formatNum(overview.value.weeklyTokens))
const totalCostFmt = computed(() => Number(overview.value.weeklyCost || 0).toFixed(2))

const quickModules = [
  { code: 'M01', name: '法规主数据', desc: '法规/爬虫/版本', path: '/admin/biz/mod01', tag: 'm01' },
  { code: 'M02', name: '案件主数据', desc: '案件/要素/标签', path: '/admin/biz/mod02', tag: 'm02' },
  { code: 'M03', name: '文书模板/草稿', desc: '模板/复核/规则', path: '/admin/biz/mod03-templates', tag: 'm03' },
  { code: 'M04', name: '研究任务', desc: '任务监控/抽检', path: '/admin/biz/mod04', tag: 'm04' },
  { code: 'M05', name: '企业 API', desc: '配额/限流', path: '/admin/biz/mod05', tag: 'm05' },
  { code: 'M06', name: '案例日志', desc: '搜索/脱敏', path: '/admin/biz/mod06', tag: 'm06' },
  { code: 'M07', name: '法规查询', desc: '版本/效力', path: '/admin/biz/mod07', tag: 'm07' },
  { code: 'M08', name: '合同规则', desc: '8 维度/阈值', path: '/admin/biz/mod08', tag: 'm08' },
  { code: 'M09', name: '知识库', desc: '分块/共享', path: '/admin/biz/mod09-kb', tag: 'm09' },
  { code: 'M10', name: '文件问答', desc: '会话/Rerank', path: '/admin/biz/mod10', tag: 'm10' },
  { code: 'AI', name: 'Prompt 管理', desc: '灰度/回滚', path: '/admin/ai/prompts', tag: 'ai' },
  { code: 'MON', name: '告警中心', desc: '规则/历史', path: '/admin/monitor/rules', tag: 'mon' }
]

function formatNum(v) {
  const n = Number(v) || 0
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString()
}

function levelTagType(l) {
  return ({ 1: 'danger', 2: 'warning', 3: 'info' }[l] || '')
}
function levelLabel(l) {
  return ({ 1: 'P0', 2: 'P1', 3: 'P2' }[l] || l)
}

async function loadStats() {
  try {
    const res = await api.get('/admin/stats')
    counts.value = res.data?.counts || {}
  } catch (e) { counts.value = {} }
}

async function loadOverview() {
  try {
    const res = await api.get('/admin/monitor/overview')
    overview.value = res.data || {}
    recentAlerts.value = overview.value.recentAlerts || []
  } catch (e) {
    overview.value = {}
    recentAlerts.value = []
  }
}

async function loadAll() {
  loading.value = true
  await Promise.all([loadStats(), loadOverview()])
  loading.value = false
}

onMounted(() => {
  loadAll()
  timer = setInterval(loadOverview, 30000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style lang="scss" scoped>
.admin-dashboard { animation: fadeIn 0.4s ease; }

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
  .header-content p { margin: 0; color: #64748b; font-size: 13px; }
}

.kpi-row { margin-bottom: 14px; }

.kpi-card {
  position: relative;
  overflow: hidden;
  border-left: 4px solid #cbd5e1;
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  &.danger { border-left-color: #ef4444; }
  &.warning { border-left-color: #f59e0b; }
  &.info { border-left-color: #3b82f6; }
  &.primary { border-left-color: #8b5cf6; }

  .kpi-label { font-size: 12px; color: #64748b; }
  .kpi-value { font-size: 24px; font-weight: 700; color: #1e293b; margin: 4px 0; }
  .kpi-foot { font-size: 11px; color: #94a3b8; }
}

.charts-row, .bottom-row { margin-bottom: 14px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-area {
  width: 100%;
  height: 180px;
}

.line-chart {
  width: 100%;
  height: 100%;
  display: block;
}

.bar-chart {
  max-height: 200px;
  overflow-y: auto;

  .bar-row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px 0;
  }

  .bar-label {
    width: 80px;
    font-size: 12px;
    color: #475569;
  }

  .bar-track {
    flex: 1;
    height: 14px;
    background: #f1f5f9;
    border-radius: 7px;
    overflow: hidden;
  }

  .bar-fill {
    height: 100%;
    border-radius: 7px;
    transition: width 0.6s;
  }

  .bar-value {
    width: 60px;
    text-align: right;
    font-size: 12px;
    color: #64748b;
    font-variant-numeric: tabular-nums;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.quick-item {
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e2e8f0;

  &:hover {
    background: #fff;
    border-color: #3b82f6;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  }

  .quick-tag {
    display: inline-block;
    padding: 2px 8px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 6px;

    &.m01, &.m07 { background: #3b82f6; }
    &.m02, &.m06 { background: #10b981; }
    &.m03, &.m08 { background: #f59e0b; }
    &.m04 { background: #8b5cf6; }
    &.m05 { background: #ec4899; }
    &.m09 { background: #06b6d4; }
    &.m10 { background: #84cc16; }
    &.ai { background: #6366f1; }
    &.mon { background: #ef4444; }
  }

  .quick-name {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
    margin-bottom: 2px;
  }

  .quick-desc {
    font-size: 11px;
    color: #94a3b8;
  }
}
</style>