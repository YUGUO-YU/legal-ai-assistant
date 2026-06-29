<template>
  <div class="law-audit-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法规主数据 · 审核工作台</h2>
        <p>MOD-01 · 4 态状态机：待审核 → 初审通过 → 已发布；任意环节可驳回</p>
      </div>
      <div class="header-actions">
        <el-tag :type="statusFilter === 'pending' ? 'warning' : 'info'" size="small" @click="filterStatus('pending')" style="cursor:pointer">待审核 {{ statusCounts[0] || 0 }}</el-tag>
        <el-tag :type="statusFilter === 'first' ? 'primary' : 'info'" size="small" @click="filterStatus('first')" style="cursor:pointer">初审通过 {{ statusCounts[1] || 0 }}</el-tag>
        <el-tag :type="statusFilter === 'done' ? 'success' : 'info'" size="small" @click="filterStatus('done')" style="cursor:pointer">已发布 {{ statusCounts[2] || 0 }}</el-tag>
        <el-tag :type="statusFilter === 'reject' ? 'danger' : 'info'" size="small" @click="filterStatus('reject')" style="cursor:pointer">已驳回 {{ statusCounts[3] || 0 }}</el-tag>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="ID / 标题 / UUID" clearable style="width:240px" @keyup.enter="load" />
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
        <el-table-column prop="title" label="标题" min-width="320" show-overflow-tooltip />
        <el-table-column prop="category_l1" label="效力级别" width="110" />
        <el-table-column prop="issuing_authority" label="制定机关" width="180" show-overflow-tooltip />
        <el-table-column prop="effective_date" label="生效日期" width="120" />
        <el-table-column label="效力状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="auditTagType(row.audit_status)">{{ auditLabel(row.audit_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="audit_time" label="审核时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button link type="success" size="small" :disabled="!canPass(row.audit_status)" @click="handleAction(row, 1)">通过</el-button>
            <el-button link type="danger" size="small" :disabled="!canReject(row.audit_status)" @click="handleAction(row, 2)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="filter.page"
        v-model:page-size="filter.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        class="pager"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-drawer v-model="showDetail" :title="detail?.title || '详情'" size="55%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="UUID">{{ detail.law_uuid }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="效力级别">{{ detail.category_l1 }} / {{ detail.category_l2 }}</el-descriptions-item>
        <el-descriptions-item label="制定机关">{{ detail.issuing_authority }}</el-descriptions-item>
        <el-descriptions-item label="生效日期">{{ detail.effective_date }}</el-descriptions-item>
        <el-descriptions-item label="来源 URL"><a :href="detail.source_url" target="_blank" v-if="detail.source_url">{{ detail.source_url }}</a><span v-else>-</span></el-descriptions-item>
        <el-descriptions-item label="效力状态">
          <el-tag :type="detail.status === 1 ? 'success' : 'info'">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="auditTagType(detail.audit_status)">{{ auditLabel(detail.audit_status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const filter = reactive({ page: 1, pageSize: 20, keyword: '' })
const statusFilter = ref('all')
const showDetail = ref(false)
const detail = ref(null)

const statusCounts = computed(() => {
  const c = { 0: 0, 1: 0, 2: 0, 3: 0 }
  rows.value.forEach(r => { c[r.audit_status] = (c[r.audit_status] || 0) + 1 })
  return c
})

function statusLabel(s) { return ({ 1: '现行', 2: '废止', 3: '修订中', 4: '未生效', 5: '部分失效' }[s] || s) }
function auditLabel(a) { return ({ 0: '待审核', 1: '初审通过', 2: '已发布', 3: '已驳回' }[a] || a) }
function auditTagType(a) { return ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'danger' }[a] || '') }
function canPass(s) { return s === 0 || s === 1 }
function canReject(s) { return s === 0 || s === 1 }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod01/laws', { params: { page: filter.page, pageSize: filter.pageSize, keyword: filter.keyword || undefined } })
    let list = res.data?.list || []
    if (statusFilter.value === 'pending') list = list.filter(r => r.audit_status === 0)
    else if (statusFilter.value === 'first') list = list.filter(r => r.audit_status === 1)
    else if (statusFilter.value === 'done') list = list.filter(r => r.audit_status === 2)
    else if (statusFilter.value === 'reject') list = list.filter(r => r.audit_status === 3)
    rows.value = list
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []; total.value = 0
  } finally {
    loading.value = false
  }
}

function filterStatus(s) { statusFilter.value = statusFilter.value === s ? 'all' : s; load() }
function reset() { filter.keyword = ''; statusFilter.value = 'all'; filter.page = 1; load() }

async function openDetail(row) {
  try {
    const res = await api.get(`/admin/{table}/${row.id}`.replace('{table}', 'law_document'))
    detail.value = res.data?.data || row
    showDetail.value = true
  } catch (e) {
    detail.value = row
    showDetail.value = true
  }
}

async function handleAction(row, action) {
  const actionName = action === 1 ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(`确认${actionName}法规《${row.title}》？`, '审核操作', { type: action === 1 ? 'success' : 'warning' })
    const res = await api.post(`/admin/biz/mod01/laws/${row.id}/audit`, null, { params: { action, auditorId: 1 } })
    if (res.data?.ok) {
      const from = auditLabel(res.data.fromStatus)
      const to = auditLabel(res.data.toStatus)
      ElMessage.success(`${actionName}成功：${from} → ${to}`)
      load()
    } else {
      ElMessage.error(res.data?.error || `${actionName}失败`)
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(`${actionName}失败：${e.message || ''}`)
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.law-audit-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap: wrap; gap: 10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; flex-wrap: wrap; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
</style>