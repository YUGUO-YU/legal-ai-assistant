<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">知识库分块管理</h2>
        <p>AI 域 · 分块统计与监控</p>
      </div>
    </div>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.totalChunks || 0 }}</div>
          <div class="stat-label">总分块数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.totalTokens || 0 }}</div>
          <div class="stat-label">总 Token 数</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.avgChunkSize || 0 }}</div>
          <div class="stat-label">平均块大小</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="glass table-card">
      <div class="filter-row">
        <el-select v-model="filterKbId" placeholder="选择知识库" clearable filterable style="width: 200px" @change="loadChunks">
          <el-option v-for="kb in kbList" :key="kb.id" :label="kb.kb_name" :value="kb.id" />
        </el-select>
        <el-input v-model="filterFileName" placeholder="文件名" clearable style="width: 200px" @change="loadChunks" />
        <el-button @click="loadChunks">搜索</el-button>
        <el-button type="primary" @click="loadChunks">刷新</el-button>
      </div>

      <el-table :data="chunks" v-loading="loading" stripe style="width: 100%; margin-top: 16px">
        <el-table-column prop="kb_name" label="知识库" min-width="120" />
        <el-table-column prop="file_name" label="文件名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="chunk_index" label="分块序号" width="100" align="center">
          <template #default="{ row }">{{ row.chunk_index }}</template>
        </el-table-column>
        <el-table-column prop="content" label="内容预览" min-width="200">
          <template #default="{ row }">
            <span class="content-preview">{{ row.content ? row.content.substring(0, 100) : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="token_count" label="Token数" width="100" align="center" />
        <el-table-column label="向量状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.vector_id" type="success" size="small">已向量化</el-tag>
            <el-tag v-else type="warning" size="small">待向量化</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="showContent(row)">查看内容</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadChunks"
        @size-change="loadChunks"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="分块内容" width="800px">
      <div class="content-full">{{ currentChunk?.content }}</div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'

const chunks = ref([])
const stats = ref({})
const kbList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterKbId = ref(null)
const filterFileName = ref('')
const dialogVisible = ref(false)
const currentChunk = ref(null)

const loadStats = async () => {
  try {
    const res = await api.get('/admin/ai/kb-chunks/stats')
    if (res.code === 0 || res.code === 200) {
      stats.value = res || {}
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const loadKbList = async () => {
  try {
    const res = await api.get('/admin/biz/mod09/kb-bases')
    if (res.code === 0 || res.code === 200) {
      kbList.value = res?.list || []
    }
  } catch (e) {
    console.error('加载知识库列表失败', e)
  }
}

const loadChunks = async () => {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterKbId.value) params.kbId = filterKbId.value
    if (filterFileName.value) params.fileName = filterFileName.value
    const res = await api.get('/admin/ai/kb-chunks', { params })
    if (res.code === 0 || res.code === 200) {
      chunks.value = res?.list || []
      total.value = res?.total || 0
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (e) {
    ElMessage.error('加载分块列表失败')
  } finally {
    loading.value = false
  }
}

const showContent = (row) => {
  currentChunk.value = row
  dialogVisible.value = true
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN')
}

onMounted(() => {
  loadStats()
  loadKbList()
  loadChunks()
})
</script>

<style scoped>
.kb-chunks-page {
  padding: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 8px;
}

.table-card {
  margin-top: 0;
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.content-preview {
  font-size: 12px;
  color: #606266;
}

.content-full {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.6;
  max-height: 500px;
  overflow-y: auto;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
