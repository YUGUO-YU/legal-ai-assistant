<template>
  <div class="search-logs-page">
    <div class="page-header">
      <div class="header-content">
        <h2>搜索分析 · 查询日志</h2>
        <p>运营分析域 · 意图分布 / 响应耗时 / 源关键词 TOP N</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="5">
        <div class="kpi-card" style="border-left:4px solid #6366f1">
          <div class="kpi-label">今日搜索</div>
          <div class="kpi-value">{{ stats.todayCount }}</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="kpi-card" style="border-left:4px solid #10b981">
          <div class="kpi-label">平均耗时</div>
          <div class="kpi-value">{{ stats.avgLatency }} ms</div>
        </div>
      </el-col>
      <el-col :span="5">
        <div class="kpi-card" style="border-left:4px solid #f59e0b">
          <div class="kpi-label">零结果查询</div>
          <div class="kpi-value">{{ stats.zeroResultCount }}</div>
        </div>
      </el-col>
      <el-col :span="9">
        <div class="kpi-card" style="border-left:4px solid #06b6d4">
          <div class="kpi-label">热门关键词</div>
          <div class="kpi-value" style="font-size:14px">{{ stats.topKeywords }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="14">
        <el-card>
          <template #header><span>意图分布</span></template>
          <div v-if="intentBars.length" class="intent-section">
            <div v-for="item in intentBars" :key="item.name" class="bar-row">
              <span class="bar-label">{{ item.name }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: item.pct + '%', background: item.color }">{{ item.count }}</div></div>
            </div>
          </div>
          <div v-else class="empty-hint">暂无数据</div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header><span>耗时分布</span></template>
          <div class="latency-dist">
            <div class="lat-item"><span class="lat-label">&lt; 100ms</span><span class="lat-value">{{ stats.lt100 }}</span><el-progress :percentage="latPct(stats.lt100)" :stroke-width="8" /></div>
            <div class="lat-item"><span class="lat-label">100-500ms</span><span class="lat-value">{{ stats.lt500 }}</span><el-progress :percentage="latPct(stats.lt500)" :stroke-width="8" status="warning" /></div>
            <div class="lat-item"><span class="lat-label">500-2000ms</span><span class="lat-value">{{ stats.lt2000 }}</span><el-progress :percentage="latPct(stats.lt2000)" :stroke-width="8" status="exception" /></div>
            <div class="lat-item"><span class="lat-label">&gt; 2000ms</span><span class="lat-value">{{ stats.gt2000 }}</span><el-progress :percentage="latPct(stats.gt2000)" :stroke-width="8" color="#ef4444" /></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top:16px">
      <el-table :data="rows" v-loading="loading" stripe border size="small" max-height="420">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="query_text" label="检索关键词" min-width="280" show-overflow-tooltip />
        <el-table-column prop="intent_type" label="意图" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="intentColor(row.intent_type)">{{ row.intent_type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result_count" label="结果数" width="80" />
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">
            <span :class="row.response_time_ms < 200 ? 'text-success' : row.response_time_ms < 1000 ? 'text-warning' : 'text-danger'">{{ row.response_time_ms }} ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="user_id" label="用户" width="100" show-overflow-tooltip />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../api'

const rows = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const palette = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899']

const stats = reactive({ todayCount: 0, avgLatency: 0, zeroResultCount: 0, topKeywords: '-', lt100: 0, lt500: 0, lt2000: 0, gt2000: 0 })

const intentBars = computed(() => {
  const map = {}
  rows.value.forEach(r => { const k = r.intent_type || '未知'; map[k] = (map[k] || 0) + 1 })
  const total = Object.values(map).reduce((s, v) => s + v, 0)
  return Object.entries(map)
    .map(([name, count], i) => ({ name, count, pct: total ? (count / total) * 100 : 0, color: palette[i % palette.length] }))
    .sort((a, b) => b.count - a.count)
})

function intentColor(t) {
  const m = { case_search: 'primary', law_search: 'success', company_query: 'warning', precedent: 'danger', qa: 'info' }
  return m[t] || ''
}

function latPct(v) {
  const total = stats.lt100 + stats.lt500 + stats.lt2000 + stats.gt2000
  return total > 0 ? Math.round((v / total) * 100) : 0
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ops/search-logs', { params: { page: page.value, pageSize: pageSize.value } })
    const list = res.data?.list || []
    rows.value = list
    total.value = res.data?.total || 0

    stats.todayCount = list.filter(r => {
      const d = new Date(r.created_at)
      const t = new Date()
      return d.toDateString() === t.toDateString()
    }).length
    stats.avgLatency = list.length ? Math.round(list.reduce((s, r) => s + (r.response_time_ms || 0), 0) / list.length) : 0
    stats.zeroResultCount = list.filter(r => r.result_count === 0).length
    stats.lt100 = list.filter(r => r.response_time_ms < 100).length
    stats.lt500 = list.filter(r => r.response_time_ms >= 100 && r.response_time_ms < 500).length
    stats.lt2000 = list.filter(r => r.response_time_ms >= 500 && r.response_time_ms < 2000).length
    stats.gt2000 = list.filter(r => r.response_time_ms >= 2000).length

    const kwMap = {}
    list.forEach(r => { const w = (r.query_text || '')?.substring(0, 20); if (w) kwMap[w] = (kwMap[w] || 0) + 1 })
    stats.topKeywords = Object.entries(kwMap).sort((a, b) => b[1] - a[1]).slice(0, 3).map(e => e[0]).join(' / ') || '-'
  } catch (e) { rows.value = []; total.value = 0 }
  finally { loading.value = false }
}

watch([page, pageSize], load)
onMounted(load)
</script>

<style lang="scss" scoped>
.search-logs-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.kpi-card { background:#fff; border-radius:10px; padding:16px; border:1px solid #e2e8f0; .kpi-label { font-size:12px; color:#64748b; margin-bottom:6px; } .kpi-value { font-size:22px; font-weight:700; color:#0f172a; } }
.intent-section {
  .bar-row { display:flex; align-items:center; gap:10px; margin-bottom:8px;
    .bar-label { width:90px; font-size:12px; color:#475569; text-align:right; flex-shrink:0; }
    .bar-track { flex:1; height:24px; background:#f1f5f9; border-radius:6px; overflow:hidden;
      .bar-fill { height:100%; border-radius:6px; font-size:11px; color:#fff; display:flex; align-items:center; padding:0 8px; min-width:28px; transition:width 0.5s; }
    }
  }
}
.latency-dist {
  .lat-item { display:flex; align-items:center; gap:8px; margin-bottom:12px;
    .lat-label { width:80px; font-size:12px; color:#475569; flex-shrink:0; }
    .lat-value { width:30px; font-size:13px; font-weight:600; color:#0f172a; flex-shrink:0; }
    .el-progress { flex:1; }
  }
}
.text-success { color:#10b981; font-weight:600; }
.text-warning { color:#f59e0b; font-weight:600; }
.text-danger { color:#ef4444; font-weight:600; }
.empty-hint { height:120px; display:flex; align-items:center; justify-content:center; color:#94a3b8; font-size:13px; }
.pager { margin-top:14px; justify-content:flex-end; display:flex; }
</style>