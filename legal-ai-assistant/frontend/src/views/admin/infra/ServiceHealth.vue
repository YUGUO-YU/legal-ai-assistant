<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">基础设施 · 服务健康</h2>
        <p>LLM 网关 / Elasticsearch / Milvus 向量库 · 实时探测 + 指标</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll" :loading="loading">一键刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card class="health-card">
          <div class="health-head">
            <span class="dot" :class="healthClass(llmStatus)"></span>
            <h3>LLM 网关</h3>
            <el-tag :type="healthTagType(llmStatus)" size="small">{{ healthLabel(llmStatus) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="活跃模型">{{ llmSummary?.activeModels ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="平均延迟">{{ llmSummary?.avgLatencyMs ?? '-' }} ms</el-descriptions-item>
            <el-descriptions-item label="7 日调用">{{ llmSummary?.weekCalls?.toLocaleString() ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="7 日 Token">{{ llmSummary?.weekTokens?.toLocaleString() ?? '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="health-actions">
            <el-button type="primary" size="small" @click="loadLLM" :loading="llmLoading">探测</el-button>
            <el-button size="small" @click="$router.push('/admin/ai/llm-models')">模型列表</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="health-card">
          <div class="health-head">
            <span class="dot" :class="healthClass(mysqlStatus)"></span>
            <h3>MySQL</h3>
            <el-tag :type="healthTagType(mysqlStatus)" size="small">{{ healthLabel(mysqlStatus) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="连接状态">{{ mysqlSummary?.message || '-' }}</el-descriptions-item>
            <el-descriptions-item label="延迟">{{ mysqlSummary?.latencyMs != null ? mysqlSummary.latencyMs + ' ms' : '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="health-actions">
            <el-button type="primary" size="small" @click="loadMysql" :loading="mysqlLoading">探测</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="health-card">
          <div class="health-head">
            <span class="dot" :class="healthClass(redisStatus)"></span>
            <h3>Redis</h3>
            <el-tag :type="healthTagType(redisStatus)" size="small">{{ healthLabel(redisStatus) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="连接状态">{{ redisSummary?.message || '-' }}</el-descriptions-item>
            <el-descriptions-item label="延迟">{{ redisSummary?.latencyMs != null ? redisSummary.latencyMs + ' ms' : '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="health-actions">
            <el-button type="primary" size="small" @click="loadRedis" :loading="redisLoading">探测</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="health-card">
          <div class="health-head">
            <span class="dot" :class="healthClass(esStatus)"></span>
            <h3>Elasticsearch</h3>
            <el-tag :type="healthTagType(esStatus)" size="small">{{ healthLabel(esStatus) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="集群状态">{{ esSummary?.status || '-' }}</el-descriptions-item>
            <el-descriptions-item label="节点数">{{ esSummary?.nodes ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="索引总数">{{ esSummary?.indices ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="文档总量">{{ esSummary?.docsCount?.toLocaleString() ?? '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="health-actions">
            <el-button type="primary" size="small" @click="loadES" :loading="esLoading">探测</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card class="health-card">
          <div class="health-head">
            <span class="dot" :class="healthClass(milvusStatus)"></span>
            <h3>Milvus 向量库</h3>
            <el-tag :type="healthTagType(milvusStatus)" size="small">{{ healthLabel(milvusStatus) }}</el-tag>
          </div>
          <el-descriptions :column="1" border size="small" style="margin-bottom:12px">
            <el-descriptions-item label="集合数">{{ milvusSummary?.collections ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="向量总数">{{ milvusSummary?.vectors?.toLocaleString() ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ milvusSummary?.version || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最近探测">{{ milvusSummary?.lastCheckAt || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="health-actions">
            <el-button type="primary" size="small" @click="loadMilvus" :loading="milvusLoading">探测健康</el-button>
            <el-button size="small" @click="$router.push('/admin/ai/milvus-collections')">集合详情</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="glass table-card" style="margin-top:16px">
      <template #header>
        <span>LLM 模型健康矩阵</span>
      </template>
      <el-table :data="llmModels" v-loading="llmLoading" stripe border size="small">
        <el-table-column prop="model_name" label="模型" min-width="180" />
        <el-table-column prop="provider" label="供应商" width="100" />
        <el-table-column prop="endpoint" label="Endpoint" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="dot" :class="healthClass(row.health_status)"></span>
            <span style="margin-left:6px">{{ healthLabel(row.health_status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="last_check_at" label="上次探测" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="checkOne(row)">探测</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'

const loading = ref(false)
const llmLoading = ref(false)
const esLoading = ref(false)
const milvusLoading = ref(false)
const mysqlLoading = ref(false)
const redisLoading = ref(false)

const llmStatus = ref(-1)
const esStatus = ref(-1)
const milvusStatus = ref(-1)
const mysqlStatus = ref(-1)
const redisStatus = ref(-1)

const llmSummary = ref(null)
const esSummary = ref(null)
const milvusSummary = ref(null)
const llmModels = ref([])
const mysqlSummary = ref(null)
const redisSummary = ref(null)

function healthClass(v) {
  if (v === 1) return 'dot-success'
  if (v === 2) return 'dot-warning'
  if (v === 3 || v === -1) return 'dot-danger'
  return 'dot-idle'
}
function healthLabel(v) { return ({ 1: '健康', 2: '降级', 3: '故障', 0: '未知', '-1': '未探测' }[v] || v) }
function healthTagType(v) { return ({ 1: 'success', 2: 'warning', 3: 'danger', '-1': 'info' }[v] || 'info') }

async function loadLLM() {
  llmLoading.value = true
  try {
    const [sum, list, health] = await Promise.all([
      api.get('/admin/ai/llm-models/summary'),
      api.get('/admin/ai/llm-models'),
      api.get('/admin/ai/llm-models/health-check')
    ])
    llmSummary.value = sum || {}
    llmModels.value = list?.list || []
    llmStatus.value = health?.overall || -1
  } catch (e) {
    llmStatus.value = 3
  } finally {
    llmLoading.value = false
  }
}

async function loadES() {
  esLoading.value = true
  try {
    const res = await api.get('/admin/infra/es-health')
    esSummary.value = res || {}
    esStatus.value = res?.ok ? 1 : 3
  } catch (e) {
    esStatus.value = 3
  } finally {
    esLoading.value = false
  }
}

async function loadMilvus() {
  milvusLoading.value = true
  try {
    const res = await api.get('/admin/ai/milvus/collections')
    milvusSummary.value = {
      collections: res?.collections?.length || 0,
      vectors: res?.collections?.reduce((s, c) => s + (c.rowCount || 0), 0) || 0,
      version: res?.version || 'v2.4.x',
      lastCheckAt: new Date().toLocaleString('zh-CN')
    }
    milvusStatus.value = 1
  } catch (e) {
    milvusStatus.value = 3
  } finally {
    milvusLoading.value = false
  }
}

async function loadMysql() {
  mysqlLoading.value = true
  try {
    const res = await api.get('/admin/infra/mysql-health')
    mysqlSummary.value = res || {}
    mysqlStatus.value = res?.status === 'UP' ? 1 : 3
  } catch (e) {
    mysqlStatus.value = 3
  } finally {
    mysqlLoading.value = false
  }
}

async function loadRedis() {
  redisLoading.value = true
  try {
    const res = await api.get('/admin/infra/redis-health')
    redisSummary.value = res || {}
    redisStatus.value = res?.status === 'UP' ? 1 : 3
  } catch (e) {
    redisStatus.value = 3
  } finally {
    redisLoading.value = false
  }
}

async function loadAll() {
  loading.value = true
  await Promise.allSettled([loadLLM(), loadES(), loadMilvus(), loadMysql(), loadRedis()])
  loading.value = false
}

async function checkOne(row) {
  try {
    const res = await api.post(`/admin/ai/llm-models/${row.id}/health-check`)
    if (res?.ok) {
      ElMessage.success(`${row.model_name} 健康`)
      loadLLM()
    } else {
      ElMessage.error(res.data?.error || '探测失败')
    }
  } catch (e) {
    ElMessage.error('探测失败：' + (e.message || ''))
  }
}

onMounted(loadAll)
</script>

<style lang="scss" scoped>
.service-health-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
.health-card { animation: adminFadeIn 0.4s ease; }
.health-head {
  display: flex; align-items: center; gap: 10px; margin-bottom: 14px;
  h3 { margin: 0; font-size: 16px; font-weight: 600; }
}
.dot {
  display: inline-block;
  width: 10px; height: 10px;
  border-radius: 50%;
  &.dot-success { background: var(--color-success); box-shadow: 0 0 0 4px rgba(16,185,129,0.18); animation: pulse 1.6s infinite; }
  &.dot-warning { background: var(--color-warning); box-shadow: 0 0 0 4px rgba(245,158,11,0.18); }
  &.dot-danger { background: var(--color-danger); box-shadow: 0 0 0 4px rgba(239,68,68,0.18); }
  &.dot-idle { background: var(--color-border); }
}
@keyframes pulse { 0%, 100% { box-shadow: 0 0 0 4px rgba(16,185,129,0.18); } 50% { box-shadow: 0 0 0 8px rgba(16,185,129,0); } }
.health-actions { margin-top: 12px; display: flex; gap: 8px; justify-content: flex-end; }
</style>