<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">用户反馈 · SLA 工作台</h2>
        <p>运营分析域 · 实时 SLA 倒计时 · 处理状态流转</p>
      </div>
      <div class="header-actions">
        <el-tag :type="filterStatus === 'pending' ? 'danger' : 'info'" size="small" @click="toggleFilter('pending')" style="cursor:pointer">待处理 {{ counts.pending }}</el-tag>
        <el-tag :type="filterStatus === 'processing' ? 'warning' : 'info'" size="small" @click="toggleFilter('processing')" style="cursor:pointer">处理中 {{ counts.processing }}</el-tag>
        <el-tag :type="filterStatus === 'resolved' ? 'success' : 'info'" size="small" @click="toggleFilter('resolved')" style="cursor:pointer">已解决 {{ counts.resolved }}</el-tag>
        <el-tag v-if="counts.overtime > 0" type="danger" size="small" @click="toggleFilter('overtime')" style="cursor:pointer">已超时 {{ counts.overtime }}</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="success" @click="handleExport" :loading="exporting">导出</el-button>
      </div>
    </div>

    <el-card class="glass table-card">
      <el-table :data="displayRows" v-loading="loading" stripe border row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small" effect="dark">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="来源模块" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="SLA" width="170">
          <template #default="{ row }">
            <div class="sla-cell">
              <span :class="slaClass(row)" class="sla-timer">{{ slaRemaining(row) }}</span>
              <div class="sla-due">截止 {{ formatTs(row.sla_due_at) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigned_to_name" label="处理人" width="90" />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" size="small" v-if="row.status === 'pending'" @click="handleStatus(row, 'processing')">接手</el-button>
            <el-button link type="success" size="small" v-if="row.status === 'processing'" @click="handleStatus(row, 'resolved')">解决</el-button>
            <el-button link type="info" size="small" v-if="row.status === 'resolved' || row.status === 'closed'" @click="handleStatus(row, 'pending')">重开</el-button>
            <el-button link type="danger" size="small" v-if="row.status !== 'closed'" @click="handleStatus(row, 'closed')">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          :background="true"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterStatus = ref('all')
const now = ref(Date.now())
let timer = null

const counts = computed(() => {
  const c = { pending: 0, processing: 0, resolved: 0, overtime: 0 }
  rows.value.forEach(r => {
    if (r.status === 'pending') c.pending++
    else if (r.status === 'processing') c.processing++
    else if (r.status === 'resolved') c.resolved++
    if (r.sla_due_at && new Date(r.sla_due_at).getTime() < now.value && r.status !== 'resolved' && r.status !== 'closed') c.overtime++
  })
  return c
})

const displayRows = computed(() => {
  if (filterStatus.value === 'all') return rows.value
  if (filterStatus.value === 'overtime') {
    return rows.value.filter(r => r.sla_due_at && new Date(r.sla_due_at).getTime() < now.value && r.status !== 'resolved' && r.status !== 'closed')
  }
  return rows.value.filter(r => r.status === filterStatus.value)
})

function priorityLabel(p) { return ({ high: '高', medium: '中', low: '低' }[p] || p) }
function priorityTag(p) { return ({ high: 'danger', medium: 'warning', low: 'info' }[p] || '') }
function statusLabel(s) { return ({ pending: '待处理', processing: '处理中', resolved: '已解决', closed: '已关闭' }[s] || s) }
function statusTag(s) { return ({ pending: 'danger', processing: 'warning', resolved: 'success', closed: 'info' }[s] || '') }

function formatTs(ts) {
  if (!ts) return '-'
  const d = new Date(ts)
  return `${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function slaRemaining(row) {
  if (!row.sla_due_at) return '--'
  const due = new Date(row.sla_due_at).getTime()
  const diff = due - now.value
  if (row.status === 'resolved' || row.status === 'closed') {
    return '已完成'
  }
  if (diff <= 0) {
    const over = Math.abs(Math.floor(diff / 60000))
    return over >= 1440 ? `超 ${Math.floor(over / 1440)} 天` : over >= 60 ? `超 ${Math.floor(over / 60)} h` : `超 ${over} 分钟`
  }
  const mins = Math.floor(diff / 60000)
  return mins >= 1440 ? `剩余 ${Math.floor(mins / 1440)} 天` : mins >= 60 ? `剩余 ${Math.floor(mins / 60)} 小时` : `剩余 ${mins} 分钟`
}

function slaClass(row) {
  if (!row.sla_due_at) return ''
  if (row.status === 'resolved' || row.status === 'closed') return 'sla-done'
  const due = new Date(row.sla_due_at).getTime()
  if (due <= now.value) return 'sla-overtime'
  const diff = due - now.value
  if (diff < 3600000) return 'sla-urgent' // < 1h
  if (diff < 14400000) return 'sla-warning' // < 4h
  return 'sla-ok'
}

function toggleFilter(s) {
  filterStatus.value = filterStatus.value === s ? 'all' : s
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ops/user-feedback', { params: { page: page.value, pageSize: pageSize.value } })
    rows.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    rows.value = []; total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleStatus(row, newStatus) {
  try {
    const res = await api.post(`/admin/ops/user-feedback/${row.id}/update`, { status: newStatus })
    if (res.data?.ok || !res.data?.error) {
      row.status = newStatus
      ElMessage.success(`反馈 ${row.id} → ${statusLabel(newStatus)}`)
    } else {
      ElMessage.error(res.data?.error || '状态更新失败')
    }
  } catch (e) {
    ElMessage.error('状态更新失败：' + (e.message || ''))
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => { now.value = Date.now() }, 15000)
})

async function handleExport() {
  exporting.value = true
  try {
    const res = await fetch('/api/admin/ops/user-feedback/export', {
      headers: { Authorization: 'Bearer ' + localStorage.getItem('token') }
    })
    const text = await res.text()
    const blob = new Blob(['\ufeff' + text], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'user-feedback-' + new Date().toISOString().slice(0, 10) + '.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.feedback-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap: wrap; gap: 10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; flex-wrap: wrap; align-items: center; }
.sla-cell {
  .sla-timer { font-size: 12px; font-weight: 600; }
  .sla-due { font-size: 11px; color: var(--color-text-muted); margin-top: 2px; }
  .sla-overtime { color: var(--color-danger); }
  .sla-urgent { color: var(--color-danger); }
  .sla-warning { color: var(--color-warning); }
  .sla-ok { color: var(--color-success); }
  .sla-done { color: var(--color-primary); }
}

</style>