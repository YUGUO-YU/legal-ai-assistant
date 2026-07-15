<template>
  <div class="admin-list-page">
    <div class="page-header">
      <div class="header-content">
        <h2>告警历史</h2>
        <p>触发 / 确认 / 解决 · 支持在线操作</p>
      </div>
      <div class="header-actions">
        <el-tag :type="activeFilter === 'open' ? 'danger' : 'info'" size="small" @click="filterOpen" style="cursor:pointer">
          未解决 {{ activeCount }}
        </el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="success" @click="handleExport" :loading="exporting">导出</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="状态">
          <el-select v-model="filter.notify" clearable style="width:140px" placeholder="全部">
            <el-option label="待发送" :value="0" />
            <el-option label="已发送" :value="1" />
            <el-option label="已确认" :value="2" />
            <el-option label="已解决" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="filter.level" clearable style="width:120px" placeholder="全部">
            <el-option label="P0" :value="1" />
            <el-option label="P1" :value="2" />
            <el-option label="P2" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="rule_id" label="规则" width="80" />
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
        <el-table-column prop="metric_value" label="指标值" width="110" />
        <el-table-column prop="message" label="消息" min-width="240" show-overflow-tooltip />
        <el-table-column label="通知" width="90">
          <template #default="{ row }">
            <el-tag :type="notifyTagType(row.notify_status)" size="small">{{ notifyLabel(row.notify_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :disabled="row.notify_status >= 2" @click="handleAck(row)">确认</el-button>
            <el-button link type="success" size="small" :disabled="!!row.resolved_at" @click="handleResolve(row)">解决</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="filter.page"
          v-model:page-size="filter.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          :background="true"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const filter = reactive({ page: 1, pageSize: 20, notify: null, level: null })
const activeFilter = ref('all')

const activeCount = computed(() => rows.value.filter(r => !r.resolved_at).length)

function levelTagType(l) { return ({ 1: 'danger', 2: 'warning', 3: 'info' }[l] || '') }
function levelLabel(l) { return ({ 1: 'P0', 2: 'P1', 3: 'P2' }[l] || l) }
function notifyTagType(n) { return ({ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success' }[n] || '') }
function notifyLabel(n) { return ({ 0: '待发送', 1: '已发送', 2: '已确认', 3: '已解决' }[n] || '-') }

async function load() {
  loading.value = true
  try {
    const params = { page: filter.page, pageSize: filter.pageSize }
    if (filter.notify != null) params.notify_status = filter.notify
    if (filter.level != null) params.level = filter.level
    const res = await api.get('/admin/alert_history/list', { params })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reset() {
  filter.notify = null
  filter.level = null
  filter.page = 1
  load()
}

function filterOpen() {
  activeFilter.value = activeFilter.value === 'open' ? 'all' : 'open'
  filter.notify = activeFilter.value === 'open' ? null : null
  filter.level = null
  load()
}

async function handleAck(row) {
  try {
    const res = await api.post(`/admin/monitor/alert-history/${row.id}/ack`)
    if (res.data?.ok) {
      ElMessage.success('已确认')
      load()
    } else {
      ElMessage.warning(res.data?.error || '操作失败')
    }
  } catch (e) {
    ElMessage.error('确认失败：' + (e.message || ''))
  }
}

async function handleResolve(row) {
  try {
    await ElMessageBox.confirm(`确认将告警 #${row.id} 标记为已解决？`, '操作确认', { type: 'success' })
    const res = await api.post(`/admin/monitor/alert-history/${row.id}/resolve`)
    if (res.data?.ok) {
      ElMessage.success('已解决')
      load()
    } else {
      ElMessage.warning(res.data?.error || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败：' + (e.message || ''))
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await fetch('/api/v1/admin/monitor/alert-history/export', {
      headers: { Authorization: 'Bearer ' + localStorage.getItem('admin_token') }
    })
    const text = await res.text()
    const blob = new Blob(['\ufeff' + text], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'alert-history-' + new Date().toISOString().slice(0, 10) + '.csv'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.admin-list-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }

</style>