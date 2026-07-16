<template>
  <div class="kb-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回知识库
        </el-button>
      </div>
      <div class="header-content">
        <h2>知识库详情</h2>
        <p>查看文档、语义分块与检索效果</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载知识库详情..." />

    <div v-else-if="kb" class="detail-container">
      <el-card class="info-card overview-card">
        <div class="overview-row">
          <div class="logo" :class="{ public: kb.isPublic }">
            <el-icon :size="32"><Box /></el-icon>
          </div>
          <div class="meta">
            <h1>{{ kb.name }}</h1>
            <p class="desc">{{ kb.description || '暂无描述' }}</p>
            <div class="meta-tags">
              <el-tag :type="kb.isPublic ? 'success' : 'info'" effect="dark" round>
                {{ kb.isPublic ? '公开' : '私有' }}
              </el-tag>
              <el-tag type="info" effect="plain" round>ID: {{ kb.id }}</el-tag>
              <el-tag size="small" type="info" effect="plain" round>类型: {{ kb.type || '-' }}</el-tag>
            </div>
          </div>
        </div>

        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ kb.docCount || 0 }}</span>
              <span class="stat-label">文档数</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ kb.chunkCount || 0 }}</span>
              <span class="stat-label">语义块</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value">{{ kb.size || '-' }}</span>
              <span class="stat-label">占用空间</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-cell">
              <span class="stat-value" :class="getStatusClass(kb.parseStatus)">
                {{ getStatusText(kb.parseStatus) }}
              </span>
              <span class="stat-label">解析状态</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="info-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <el-icon><Search /></el-icon>
            <span>检索测试</span>
          </div>
        </template>
        <el-input
          v-model="query"
          placeholder="输入关键词进行检索测试"
          @keyup.enter="runTest"
        >
          <template #append>
            <el-button @click="runTest" :loading="testing">
              <el-icon><Search /></el-icon>
              检索
            </el-button>
          </template>
        </el-input>
        <div v-if="testResults.length" class="test-results">
          <div v-for="(r, idx) in testResults" :key="idx" class="test-item">
            <div class="test-head">
              <el-tag size="small" type="info">#{{ idx + 1 }}</el-tag>
              <span class="test-source">来源: {{ r.sourceName || 'KB-' + kb.id }}</span>
              <span class="test-score">相关度: {{ formatScore(r.score) }}%</span>
            </div>
            <p class="test-content">{{ r.text || r.content }}</p>
          </div>
        </div>
      </el-card>

      <el-card class="info-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <el-icon><Files /></el-icon>
            <span>语义分块（{{ chunks.length }}）</span>
            <el-input
              v-model="chunkFilter"
              size="small"
              placeholder="按内容过滤"
              style="width: 240px; margin-left: auto"
              clearable
            />
          </div>
        </template>

        <div v-if="filteredChunks.length" class="chunk-list">
          <div
            v-for="chunk in filteredChunks"
            :key="chunk.chunkId"
            class="chunk-card"
          >
            <div class="chunk-head">
              <el-tag size="small" effect="dark">#{{ chunk.chunkIndex + 1 }}</el-tag>
              <span class="chunk-file">{{ chunk.fileName }}</span>
              <span class="chunk-meta">{{ chunk.tokenCount }} tokens</span>
              <el-tag v-if="chunk.vectorId" size="small" type="success" effect="plain">
                {{ chunk.vectorId }}
              </el-tag>
            </div>
            <div class="chunk-content">{{ chunk.content }}</div>
          </div>
        </div>
        <empty-state v-else icon="Document" title="暂无语义块" description="上传文档后将自动分块" />
      </el-card>

      <el-card class="info-card" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <el-icon><Operation /></el-icon>
            <span>操作</span>
          </div>
        </template>
        <div class="action-row">
          <el-button type="primary" @click="goToUpload">
            <el-icon><UploadFilled /></el-icon>
            上传文档
          </el-button>
          <el-button @click="copyInfo">
            <el-icon><CopyDocument /></el-icon>
            复制信息
          </el-button>
          <el-button type="danger" @click="onDelete" plain>
            <el-icon><Delete /></el-icon>
            删除知识库
          </el-button>
        </div>
      </el-card>
    </div>

    <empty-state v-else icon="Box" title="未找到知识库" action-text="返回" @action="goBack" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Box,
  Search,
  Files,
  Operation,
  UploadFilled,
  CopyDocument,
  Delete
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const kb = ref(null)
const chunks = ref([])
const query = ref('')
const chunkFilter = ref('')
const testing = ref(false)
const testResults = ref([])

const filteredChunks = computed(() => {
  if (!chunkFilter.value) return chunks.value
  const q = chunkFilter.value.toLowerCase()
  return chunks.value.filter(c => (c.content || '').toLowerCase().includes(q))
})

const loadDetail = async () => {
  const id = route.params.kbId
  if (!id) {
    ElMessage.error('参数缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    const [detailRes, chunksRes] = await Promise.all([
      api.knowledgeBase.detail(id),
      api.knowledgeBase.chunks(id)
    ])
    if (detailRes.data) {
      kb.value = detailRes.data
    } else {
      ElMessage.error('知识库不存在')
    }
    if (chunksRes.data) {
      chunks.value = chunksRes.data
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/knowledge-base')
}

const goToUpload = () => {
  router.push({ path: '/knowledge-base', query: { kbId: kb.value?.id, action: 'upload' } })
}

const copyInfo = async () => {
  if (!kb.value) return
  const text = `【知识库】${kb.value.name} | 文档 ${kb.value.docCount} | 语义块 ${kb.value.chunkCount} | ${kb.value.isPublic ? '公开' : '私有'}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const onDelete = async () => {
  if (!kb.value) return
  try {
    await ElMessageBox.confirm(`确认删除知识库「${kb.value.name}」？该操作不可恢复`, '删除确认', { type: 'warning' })
    await api.knowledgeBase.delete(kb.value.id)
    ElMessage.success('已删除')
    router.push('/knowledge-base')
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error('删除失败')
    }
  }
}

const runTest = async () => {
  if (!kb.value || !query.value.trim()) {
    ElMessage.warning('请输入查询词')
    return
  }
  testing.value = true
  try {
    const res = await api.legalSearch.search({ query: query.value, kbId: kb.value.id })
    testResults.value = res?.items || []
    if (!testResults.value.length) {
      ElMessage.info('未检索到相关结果')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('检索失败')
  } finally {
    testing.value = false
  }
}

const formatScore = (score) => {
  if (!score) return 0
  return (parseFloat(score) * 100).toFixed(0)
}

const getStatusText = (s) => ({ 0: '待解析', 1: '解析中', 2: '已完成' }[s] || '未知')
const getStatusClass = (s) => ({ 0: 'pending', 1: 'processing', 2: 'success' }[s] || '')

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.kb-detail { animation: fadeIn 0.4s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.page-header {
  margin-bottom: 24px;
  .header-nav :deep(.el-button) { display: inline-flex; align-items: center; gap: 6px; color: var(--color-text-secondary); }
  .header-content h2 {
    margin: 0 0 8px 0; font-size: 26px; font-weight: 600;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 14px; }
}

.detail-container {
  .info-card {
    border: none; border-radius: 16px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    :deep(.el-card__header) { padding: 16px 20px; }
    :deep(.el-card__body) { padding: 16px 20px 20px; }
  }

  .overview-card :deep(.el-card__body) { padding: 24px; }
  .overview-row { display: flex; align-items: flex-start; gap: 20px; margin-bottom: 20px; }
  .logo {
    width: 64px; height: 64px; border-radius: 14px;
    background: linear-gradient(135deg, #43e97b, #38f9d7);
    display: flex; align-items: center; justify-content: center; color: #fff;
    &.public { background: linear-gradient(135deg, #4facfe, #00f2fe); }
  }
  .meta { flex: 1; h1 { margin: 0 0 8px 0; font-size: 22px; color: var(--color-text-primary); } }
  .desc { margin: 0 0 12px 0; color: var(--color-text-secondary); font-size: 13px; line-height: 1.6; }
  .meta-tags { display: flex; gap: 8px; flex-wrap: wrap; }

  .stat-row { margin-top: 12px; }
  .stat-cell {
    background: #f9fafb; border-radius: 10px; padding: 14px;
    display: flex; flex-direction: column; align-items: center; gap: 4px;
    .stat-value { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
    .stat-value.pending { color: var(--color-text-secondary); }
    .stat-value.processing { color: #f59e0b; }
    .stat-value.success { color: #10b981; }
    .stat-label { font-size: 12px; color: var(--color-text-secondary); }
  }

  .card-header {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: 600; color: var(--color-text-primary);
    .el-icon { color: #667eea; }
  }

  .test-results { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
  .test-item {
    background: #f9fafb; border-radius: 8px; padding: 10px 14px;
    .test-head { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--color-text-secondary); margin-bottom: 6px; }
    .test-score { margin-left: auto; color: #667eea; font-weight: 600; }
    .test-content { margin: 0; font-size: 13px; color: var(--color-text-primary); line-height: 1.6; }
  }

  .chunk-list { display: flex; flex-direction: column; gap: 12px; max-height: 600px; overflow-y: auto; padding-right: 4px; }
  .chunk-card {
    background: #f9fafb; border-radius: 10px; padding: 12px 14px;
    border-left: 3px solid #667eea;
    .chunk-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
    .chunk-file { font-size: 13px; color: var(--color-text-primary); font-weight: 500; }
    .chunk-meta { font-size: 12px; color: var(--color-text-secondary); }
    .chunk-content { font-size: 13px; line-height: 1.7; color: var(--color-text-secondary); white-space: pre-wrap; max-height: 200px; overflow-y: auto; }
  }

  .action-row { display: flex; gap: 12px; flex-wrap: wrap; }
}
</style>
