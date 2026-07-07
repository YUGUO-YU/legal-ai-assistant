<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <div class="header-content">
        <h2>后台管理 · 监控大盘</h2>
        <p>实时指标 · 待办 · 趋势 · 6 大域 / 27 张管理表</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="dbStatus.connected" type="success" size="small">数据库已连接</el-tag>
        <el-tag v-else type="danger" size="small">数据库未连接</el-tag>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-alert v-if="!dbStatus.connected" type="warning" :closable="false" show-icon class="db-alert">
      <template #title>
        <span>数据库未连接或未初始化。请确保：</span>
        <ol style="margin: 8px 0 0 16px;">
          <li>MySQL 服务已启动</li>
          <li>数据库 <code>legal_ai</code> 已创建</li>
          <li>后端已执行 schema.sql 初始化表和数据</li>
        </ol>
        <el-button size="small" type="warning" style="margin-top:8px" @click="checkDbStatus">重新检测</el-button>
        <el-button size="small" type="primary" style="margin-top:8px;margin-left:8px" @click="initDb">初始化数据库</el-button>
      </template>
    </el-alert>

    <el-row :gutter="14" class="kpi-row">
      <el-col :xs="12" :sm="6" :md="4" :lg="3" v-for="m in kpis" :key="m.label">
        <el-card class="kpi-card" :body-style="{ padding: '14px' }" :class="m.tone">
          <div class="kpi-label">{{ m.label }}</div>
          <div class="kpi-value">{{ m.value }}</div>
          <div class="kpi-foot">{{ m.foot }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="llm-row">
      <el-col :span="24">
        <el-card class="llm-status-card" :body-style="{ padding: '16px' }">
          <div class="llm-header">
            <div class="llm-title">
              <span class="dot" :class="aiStatus.status === 'online' ? 'dot-success' : 'dot-danger'"></span>
              <span class="llm-name">{{ aiStatus.model || '未配置' }}</span>
              <el-tag :type="aiStatus.status === 'online' ? 'success' : 'danger'" size="small">
                {{ aiStatus.status === 'online' ? '在线' : '离线' }}
              </el-tag>
            </div>
            <div class="llm-meta">
              <span v-if="aiStatus.baseUrl"><el-icon><Link /></el-icon> {{ aiStatus.baseUrl }}</span>
              <span v-if="aiStatus.message"><el-icon><InfoFilled /></el-icon> {{ aiStatus.message }}</span>
              <span class="llm-time">检测于 {{ aiStatus.time || '-' }}</span>
            </div>
          </div>
          <div class="llm-actions">
            <el-button size="small" @click="loadAiStatus">刷新状态</el-button>
            <el-button size="small" type="primary" @click="$router.push('/admin/ai/llm-models')">模型管理</el-button>
          </div>
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
            <v-chart :option="tokenTrendOption" autoresize />
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
          <div class="chart-area">
            <v-chart :option="moduleTokenOption" autoresize />
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
import { Refresh, Link, InfoFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import api from '../../api'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const counts = ref({})
const overview = ref({})
const recentAlerts = ref([])
const loading = ref(false)
const aiStatus = ref({ status: 'checking', model: '', baseUrl: '', message: '', time: '' })
const dbStatus = ref({ connected: false, message: '' })
let timer = null

const kpis = computed(() => [
  { label: '在线告警', value: overview.value.activeAlerts ?? '-', foot: '未解决', tone: 'danger' },
  { label: '待审法规', value: overview.value.pendingLaws ?? '-', foot: 'MOD-01', tone: 'warning' },
  { label: '待复核草稿', value: overview.value.pendingDrafts ?? '-', foot: 'MOD-03', tone: 'warning' },
  { label: '待处理反馈', value: overview.value.pendingFeedback ?? '-', foot: '运营域', tone: 'info' },
  { label: '前端用户', value: overview.value.totalFrontendUsers ?? '-', foot: '总注册量', tone: 'primary' },
  { label: '待审核', value: overview.value.pendingApprovals ?? '-', foot: '新注册', tone: overview.value.pendingApprovals > 0 ? 'warning' : 'primary' },
  { label: '今日登录', value: overview.value.todayLogins ?? '-', foot: '活跃用户', tone: 'success' },
  { label: '今日注册', value: overview.value.todayRegs ?? '-', foot: '新用户', tone: 'info' },
  { label: '7日 Token', value: formatNum(overview.value.weeklyTokens), foot: '全部模块', tone: 'primary' },
  { label: '7日成本', value: '¥' + (overview.value.weeklyCost ?? 0), foot: 'LLM 用量', tone: 'primary' },
  { label: '活跃公告', value: overview.value.activeAnnouncements ?? '-', foot: '在有效期', tone: 'info' }
])

const tokenTrend = computed(() => overview.value.tokenTrend || [])

const tokenTrendOption = computed(() => {
  const t = tokenTrend.value
  const palette = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']
  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>Tokens: ${Number(p.value).toLocaleString()}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: t.map(x => String(x.day).slice(5)),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    series: [{
      type: 'line',
      data: t.map(x => Number(x.tokens) || 0),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: palette[0], width: 2 },
      itemStyle: { color: palette[0] },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: `${palette[0]}40` },
            { offset: 1, color: `${palette[0]}05` }
          ]
        }
      }
    }]
  }
})

const moduleTokenOption = computed(() => {
  const list = overview.value.moduleTokens || []
  const palette = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316', '#6366f1']
  const data = list.map((x, i) => ({
    name: x.module || '未分类',
    value: Number(x.tokens) || 0,
    itemStyle: { color: palette[i % palette.length] }
  }))
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        return `${p.name}<br/>Tokens: ${Number(p.value).toLocaleString()}`
      }
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
      axisLabel: { color: '#94a3b8', fontSize: 10 }
    },
    yAxis: {
      type: 'category',
      data: list.map(x => x.module || '未分类'),
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#475569', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data,
      barWidth: 14,
      itemStyle: { borderRadius: [0, 6, 6, 0] },
      label: { show: true, position: 'right', formatter: (p) => formatNum(p.value), fontSize: 10, color: '#64748b' }
    }]
  }
})

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
  await Promise.all([checkDbStatus(), loadStats(), loadOverview(), loadAiStatus()])
  loading.value = false
}

async function checkDbStatus() {
  try {
    const res = await api.get('/admin/db/health')
    dbStatus.value = {
      connected: res.data?.connected || false,
      message: res.data?.message || ''
    }
  } catch (e) {
    dbStatus.value = { connected: false, message: '无法连接后端服务' }
  }
}

async function initDb() {
  ElMessage.info('请在 MySQL 命令行中执行：source backend/src/main/resources/schema.sql')
}

async function loadAiStatus() {
  try {
    const res = await fetch('/api/v1/ai-status')
    const data = await res.json()
    aiStatus.value = {
      status: data.status || 'offline',
      model: data.model || 'MiniMax-M3',
      baseUrl: data.baseUrl || '',
      message: data.message || '',
      time: new Date().toLocaleTimeString('zh-CN')
    }
  } catch (e) {
    aiStatus.value = { status: 'offline', model: '', baseUrl: '', message: '无法连接', time: new Date().toLocaleTimeString('zh-CN') }
  }
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
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
}

.db-alert {
  margin-bottom: 16px;
  :deep(.el-alert__title) {
    font-size: 13px;
  }
}

.kpi-row { margin-bottom: 14px; }

.kpi-card {
  position: relative;
  overflow: hidden;
  border-left: 4px solid var(--color-border-dark);
  transition: transform 0.2s;

  &:hover { transform: translateY(-2px); }

  &.danger { border-left-color: var(--color-danger); }
  &.warning { border-left-color: var(--color-warning); }
  &.info { border-left-color: var(--color-info); }
  &.primary { border-left-color: var(--color-primary); }

  .kpi-label { font-size: 12px; color: var(--color-text-secondary); }
  .kpi-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary); margin: 4px 0; }
  .kpi-foot { font-size: 11px; color: var(--color-text-placeholder); }
}

.charts-row, .bottom-row { margin-bottom: 14px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-area {
  width: 100%;
  height: 200px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 10px;
}

.quick-item {
  padding: 10px;
  border-radius: var(--radius-md);
  background: var(--color-bg-secondary);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--color-border);

  &:hover {
    background: var(--color-bg);
    border-color: var(--color-primary);
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  .quick-tag {
    display: inline-block;
    padding: 2px 8px;
    border-radius: var(--radius-sm);
    font-size: 11px;
    font-weight: 600;
    color: #fff;
    margin-bottom: 6px;

    &.m01, &.m07 { background: var(--color-info); }
    &.m02, &.m06 { background: var(--color-success); }
    &.m03, &.m08 { background: var(--color-warning); }
    &.m04 { background: #8b5cf6; }
    &.m05 { background: #ec4899; }
    &.m09 { background: #06b6d4; }
    &.m10 { background: #84cc16; }
    &.ai { background: #6366f1; }
    &.mon { background: var(--color-danger); }
  }

  .quick-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin-bottom: 2px;
  }

  .quick-desc {
    font-size: 11px;
    color: var(--color-text-placeholder);
  }
}

.llm-row { margin-bottom: 14px; }

.llm-status-card {
  border-left: 4px solid #8b5cf6;

  .llm-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
  }

  .llm-title {
    display: flex;
    align-items: center;
    gap: 10px;

    .llm-name {
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
    }
  }

  .llm-meta {
    display: flex;
    align-items: center;
    gap: 20px;
    font-size: 13px;
    color: var(--color-text-secondary);

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .llm-time {
      color: var(--color-text-placeholder);
      font-size: 12px;
    }
  }

  .llm-actions {
    margin-top: 12px;
    display: flex;
    gap: 8px;
  }
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  &.dot-success { background: var(--color-success); }
  &.dot-danger { background: var(--color-danger); }
}
</style>