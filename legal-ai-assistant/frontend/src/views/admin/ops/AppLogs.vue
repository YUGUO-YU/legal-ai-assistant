<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">应用日志</h2>
        <p>运营分析域 · 应用运行日志 · 问题排查</p>
      </div>
      <div class="header-actions">
        <el-select v-model="level" @change="loadLogs" size="default" style="width: 120px;">
          <el-option label="全部" value="ALL" />
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARN" value="WARN" />
          <el-option label="INFO" value="INFO" />
          <el-option label="DEBUG" value="DEBUG" />
        </el-select>
        <el-button @click="handleDownload">下载日志</el-button>
        <el-button :icon="Refresh" @click="loadLogs">刷新</el-button>
      </div>
    </div>

    <el-card class="glass table-card">
      <div v-if="error" class="error-tip">
        <el-alert type="error" :title="error" :closable="false" show-icon />
      </div>
      <div v-else-if="logs.length === 0 && !loading" class="empty-tip">
        <el-alert type="info" title="暂无日志数据" :closable="false" show-icon />
      </div>
      <div v-else class="log-container" v-loading="loading">
        <pre v-for="(log, i) in logs" :key="i" :class="['log-line', getLevelClass(log)]">{{ log }}</pre>
      </div>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          :total="total"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          @current-change="loadLogs"
          :background="true"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'

const logs = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(100)
const total = ref(0)
const level = ref('INFO')
const error = ref('')

function getLevelClass(line) {
  const upper = line.toUpperCase()
  if (upper.includes(' ERROR ') || upper.includes('ERROR:')) return 'log-error'
  if (upper.includes(' WARN ') || upper.includes('WARN:')) return 'log-warn'
  if (upper.includes(' INFO ') || upper.includes('INFO:')) return 'log-info'
  if (upper.includes(' DEBUG ') || upper.includes('DEBUG:')) return 'log-debug'
  return ''
}

async function loadLogs() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/admin/ops/app-logs', {
      params: { level: level.value, page: page.value, pageSize: pageSize.value }
    })
    if (res?.error) {
      error.value = res.error
      logs.value = []
      total.value = 0
    } else {
      logs.value = res?.logs || []
      total.value = res?.total || 0
    }
  } catch (e) {
    error.value = e.message || '加载日志失败'
    logs.value = []
  } finally {
    loading.value = false
  }
}

  async function handleDownload() {
    try {
      const res = await fetch('/api/v1/admin/ops/app-logs/download', {
        headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
      })
    if (!res.ok) throw new Error('下载失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'app.log'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败: ' + (e.message || ''))
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style lang="scss" scoped>
.page-container {
  animation: adminFadeIn 0.3s ease;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
  .header-content {
    h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
    p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
  }
  .header-actions { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
}
.log-container {
  max-height: 600px;
  overflow-y: auto;
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}
.log-line {
  margin: 0;
  padding: 1px 4px;
  border-radius: 2px;
  white-space: pre-wrap;
  word-break: break-all;
  &.log-error { background: rgba(239, 68, 68, 0.15); color: var(--color-danger); }
  &.log-warn { background: rgba(245, 158, 11, 0.12); color: var(--color-warning); }
  &.log-info { color: var(--color-text-muted); }
  &.log-debug { color: var(--color-text-muted); }
}
.error-tip, .empty-tip {
  padding: 16px;
}
.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
