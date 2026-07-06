<template>
  <div class="draft-review-page">
    <div class="page-header">
      <div class="header-content">
        <h2>AI 文书复核 · 工作台</h2>
        <p>MOD-03 · 3 态状态机：待复核 → 已通过 / 驳回 / 退回修改</p>
      </div>
      <div class="header-actions">
        <el-tag :type="filterStatus === 'pending' ? 'warning' : 'info'" size="small" @click="filterStatus = filterStatus === 'pending' ? 'all' : 'pending'; load()" style="cursor:pointer">待复核 {{ statusCounts[0] || 0 }}</el-tag>
        <el-tag type="success" size="small" @click="filterStatus = filterStatus === 'pass' ? 'all' : 'pass'; load()" style="cursor:pointer">已通过 {{ statusCounts[1] || 0 }}</el-tag>
        <el-tag type="danger" size="small" @click="filterStatus = filterStatus === 'reject' ? 'all' : 'reject'; load()" style="cursor:pointer">已驳回 {{ statusCounts[2] || 0 }}</el-tag>
        <el-tag size="small" @click="filterStatus = filterStatus === 'return' ? 'all' : 'return'; load()" style="cursor:pointer">已退回 {{ statusCounts[3] || 0 }}</el-tag>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="标题 / 类型 / ID" clearable style="width:240px" @keyup.enter="load" />
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
        <el-table-column prop="doc_title" label="文书标题" min-width="280" show-overflow-tooltip />
        <el-table-column prop="doc_type" label="类型" width="120" />
        <el-table-column label="内容来源" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.content_source === 'AI_GENERATED'" type="success" size="small">AI</el-tag>
            <el-tag v-else-if="row.content_source === 'TEMPLATE_LOCAL'" type="info" size="small">模板</el-tag>
            <el-tag v-else-if="row.content_source === 'AI_FALLBACK'" type="warning" size="small">AI 兜底</el-tag>
            <el-tag v-else size="small">{{ row.content_source || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ai_score" label="AI 评分" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.ai_score >= 80 ? 'success' : row.ai_score >= 60 ? 'warning' : 'danger'">{{ row.ai_score ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核状态" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="reviewTagType(row.review_status)">{{ reviewLabel(row.review_status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewer_name" label="复核人" width="100" />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button link type="success" size="small" :disabled="row.review_status !== 0" @click="handleAction(row, 1)">通过</el-button>
            <el-button link type="danger" size="small" :disabled="row.review_status !== 0" @click="handleAction(row, 2)">驳回</el-button>
            <el-button link type="warning" size="small" :disabled="row.review_status !== 0" @click="handleAction(row, 3)">退回修改</el-button>
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

    <el-drawer v-model="showDetail" :title="detail?.doc_title || '文书详情'" size="60%" direction="rtl">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.doc_type }}</el-descriptions-item>
          <el-descriptions-item label="案件 ID">{{ detail.case_id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当事人">{{ detail.party_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内容来源">
            <el-tag size="small" :type="detail.content_source === 'AI_GENERATED' ? 'success' : detail.content_source === 'AI_FALLBACK' ? 'warning' : 'info'">{{ detail.content_source || '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="AI 评分">{{ detail.ai_score ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="复核状态">
            <el-tag size="small" :type="reviewTagType(detail.review_status)">{{ reviewLabel(detail.review_status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="复核人">{{ detail.reviewer_name || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:20px">文书正文</h4>
        <pre class="content-preview">{{ detail.content }}</pre>
      </div>
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
const filterStatus = ref('all')
const showDetail = ref(false)
const detail = ref(null)

const statusCounts = computed(() => {
  const c = { 0: 0, 1: 0, 2: 0, 3: 0 }
  rows.value.forEach(r => { c[r.review_status] = (c[r.review_status] || 0) + 1 })
  return c
})

function reviewLabel(s) { return ({ 0: '待复核', 1: '已通过', 2: '已驳回', 3: '已退回' }[s] || s) }
function reviewTagType(s) { return ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }[s] || '') }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod03/drafts', { params: { page: filter.page, pageSize: filter.pageSize, keyword: filter.keyword || undefined } })
    let list = res.data?.list || []
    if (filterStatus.value !== 'all') {
      const map = { pending: 0, pass: 1, reject: 2, return: 3 }
      const target = map[filterStatus.value]
      list = list.filter(r => r.review_status === target)
    }
    rows.value = list
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []; total.value = 0
  } finally {
    loading.value = false
  }
}

function reset() { filter.keyword = ''; filterStatus.value = 'all'; filter.page = 1; load() }

async function openDetail(row) {
  try {
    const res = await api.get(`/admin/doc_draft/${row.id}`)
    detail.value = res.data?.data || row
    showDetail.value = true
  } catch (e) {
    detail.value = row
    showDetail.value = true
  }
}

async function handleAction(row, action) {
  const actionMap = { 1: '通过', 2: '驳回', 3: '退回修改' }
  const actionName = actionMap[action]
  try {
    await ElMessageBox.confirm(`确认${actionName}文书《${row.doc_title}》？`, '复核操作', { type: action === 1 ? 'success' : 'warning' })
    const res = await api.post(`/admin/biz/mod03/drafts/${row.id}/review`, null, { params: { action, reviewerId: 1, reviewerName: 'admin' } })
    if (res.data?.ok) {
      const from = reviewLabel(res.data.fromStatus)
      const to = reviewLabel(res.data.toStatus)
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
.draft-review-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap: wrap; gap: 10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; flex-wrap: wrap; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
.content-preview {
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #e2e8f0;
  max-height: 50vh;
  overflow-y: auto;
}
</style>