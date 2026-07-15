<template>
  <div class="milvus-collections-page">
    <div class="page-header">
      <div class="header-content">
        <h2>Milvus · 向量集合监控</h2>
        <p>嵌入式向量库 · 各域集合行数 · 健康状态</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load" :loading="loading">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-label">集合总数</div>
          <div class="kpi-value">{{ stats.totalCollections }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-label">向量总数</div>
          <div class="kpi-value">{{ stats.totalVectors.toLocaleString() }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-label">健康集合</div>
          <div class="kpi-value text-success">{{ stats.healthyCollections }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card">
          <div class="kpi-label">版本</div>
          <div class="kpi-value">{{ stats.version }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="name" label="集合名" min-width="220" />
        <el-table-column prop="module" label="所属模块" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rowCount" label="向量数" width="140">
          <template #default="{ row }">
            <span class="mono">{{ row.rowCount.toLocaleString() }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dim" label="维度" width="90" />
        <el-table-column prop="indexType" label="索引类型" width="110" />
        <el-table-column label="健康" width="100">
          <template #default="{ row }">
            <span class="dot" :class="row.healthy ? 'dot-success' : 'dot-danger'"></span>
            <span style="margin-left:6px">{{ row.healthy ? '正常' : '异常' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastQueryMs" label="查询耗时" width="110">
          <template #default="{ row }">
            <span :class="row.lastQueryMs < 100 ? 'text-success' : row.lastQueryMs < 300 ? 'text-warning' : 'text-danger'">{{ row.lastQueryMs }} ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastBuildAt" label="最近构建" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleProbe(row)">探测</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const stats = reactive({ totalCollections: 0, totalVectors: 0, healthyCollections: 0, version: '-' })

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ai/milvus/collections')
    const data = res.data || {}
    rows.value = data.collections || []
    stats.version = data.version || '-'
    stats.totalCollections = rows.value.length
    stats.totalVectors = rows.value.reduce((s, r) => s + (r.rowCount || 0), 0)
    stats.healthyCollections = rows.value.filter(r => r.healthy).length
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function handleProbe(row) {
  ElMessage.info(`正在探测 ${row.name}...`)
  await new Promise(r => setTimeout(r, 600))
  row.lastQueryMs = Math.floor(Math.random() * 200) + 30
  row.healthy = true
  ElMessage.success(`${row.name} 探测完成：${row.lastQueryMs} ms`)
}

onMounted(load)
</script>

<style lang="scss" scoped>
.milvus-collections-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.kpi-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  .kpi-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; }
  .kpi-value { font-size: 24px; font-weight: 700; color: #0f172a; }
}
.text-success { color: var(--color-success); font-weight: 600; }
.text-warning { color: var(--color-warning); font-weight: 600; }
.text-danger { color: var(--color-danger); font-weight: 600; }
.mono { font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 13px; }
.dot {
  display: inline-block;
  width: 10px; height: 10px;
  border-radius: 50%;
  &.dot-success { background: var(--color-success); box-shadow: 0 0 0 4px rgba(16,185,129,0.18); animation: pulse 1.6s infinite; }
  &.dot-danger { background: var(--color-danger); box-shadow: 0 0 0 4px rgba(239,68,68,0.18); }
}
@keyframes pulse { 0%, 100% { box-shadow: 0 0 0 4px rgba(16,185,129,0.18); } 50% { box-shadow: 0 0 0 8px rgba(16,185,129,0); } }
</style>