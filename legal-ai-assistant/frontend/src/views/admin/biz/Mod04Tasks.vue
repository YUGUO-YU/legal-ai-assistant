<template>
  <div class="tasks-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法律研究任务</h2>
        <p>MOD-04 · 任务状态流转 / 报告预览 / 来源追溯</p>
      </div>
      <div class="header-actions">
        <el-tag :type="filter.status === 'running' ? 'warning' : 'info'" size="small" @click="toggleFilter('running')" style="cursor:pointer">进行中</el-tag>
        <el-tag :type="filter.status === 'done' ? 'success' : 'info'" size="small" @click="toggleFilter('done')" style="cursor:pointer">已完成</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="task_uuid" label="UUID" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.task_uuid?.substring(0, 12) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="topic" label="研究主题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="user_id" label="用户" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报告" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.report" size="small" type="success">已生成</el-tag>
            <el-tag v-else size="small" type="info">待生成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源数" width="80">
          <template #default="{ row }">
            <span>{{ sourceCount(row.sources) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page" v-model:page-size="pageSize" :total="total"
        layout="total, sizes, prev, pager, next" :page-sizes="[10,20,50]" class="pager"
      />
    </el-card>

    <el-drawer v-model="showDetail" :title="detail?.topic || '任务详情'" size="55%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="UUID">{{ detail.task_uuid }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ detail.user_id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTag(detail.status)">{{ detail.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.created_at }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top:16px">研究主题</h4>
      <pre class="content-preview">{{ detail.topic }}</pre>
      <h4 style="margin-top:16px">研究报告</h4>
      <div v-if="detail.report" class="content-preview report-content" v-html="detail.report"></div>
      <div v-else class="empty-hint">报告尚未生成</div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import api from '../../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const filter = reactive({ status: '' })
const showDetail = ref(false)
const detail = ref(null)

function statusTag(s) {
  return ({ running: 'warning', done: 'success', failed: 'danger', pending: 'info', queued: 'info' }[s] || '')
}

function sourceCount(raw) {
  try { const arr = JSON.parse(raw || '[]'); return Array.isArray(arr) ? arr.length : 0 }
  catch { return 0 }
}

function toggleFilter(s) { filter.status = filter.status === s ? '' : s; load() }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod04/research-tasks', { params: { page: page.value, pageSize: pageSize.value, status: filter.status || undefined } })
    rows.value = res.data?.list || []
    total.value = res.data?.total || rows.value.length
  } catch (e) { rows.value = []; total.value = 0 }
  finally { loading.value = false }
}

async function openDetail(row) {
  try {
    const res = await api.get(`/admin/biz/mod04/research-tasks/${row.id}`)
    detail.value = res.data?.data || row
    showDetail.value = true
  } catch (e) {
    detail.value = row
    showDetail.value = true
  }
}

watch([page, pageSize], load)
onMounted(load)
</script>

<style lang="scss" scoped>
.tasks-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap:wrap; gap:10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.mono { font-family:'Cascadia Code','Consolas',monospace; font-size:12px; }
.pager { margin-top:14px; justify-content:flex-end; display:flex; }
.content-preview { background:#f8fafc; padding:16px; border-radius:8px; white-space:pre-wrap; word-break:break-word; font-size:13px; line-height:1.6; border:1px solid #e2e8f0; max-height:50vh; overflow-y:auto; }
.report-content :deep(p) { margin:0 0 8px; }
.report-content :deep(h1), .report-content :deep(h2), .report-content :deep(h3) { margin:12px 0 8px; font-size:15px; }
.empty-hint { height:100px; display:flex; align-items:center; justify-content:center; color:#94a3b8; font-size:13px; }
</style>