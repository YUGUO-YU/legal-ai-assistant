<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">案件主数据管理</h2>
        <p>MOD-02 · 案由 / 法院 / 裁判结果 · 多维筛选 + 详情查看</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card class="glass filter-card" style="margin-bottom: 14px;">
      <el-form inline :model="filter">
        <el-form-item label="案由">
          <el-input v-model="filter.cause" placeholder="案由关键词" clearable style="width:160px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="案件类型">
          <el-select v-model="filter.type" clearable placeholder="全部" style="width:120px">
            <el-option label="民事" :value="1" />
            <el-option label="刑事" :value="2" />
            <el-option label="行政" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="裁判结果">
          <el-select v-model="filter.result" clearable placeholder="全部" style="width:130px">
            <el-option label="全部支持" :value="1" />
            <el-option label="部分支持" :value="2" />
            <el-option label="驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="showImportDialog">导入案例</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <div class="kpi-card" style="border-left:4px solid var(--color-primary)"><div class="kpi-label">案件总数</div><div class="kpi-value">{{ stats.total }}</div></div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left:4px solid var(--color-success)"><div class="kpi-label">民事</div><div class="kpi-value">{{ stats.civil }}</div></div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left:4px solid var(--color-warning)"><div class="kpi-label">刑事</div><div class="kpi-value">{{ stats.criminal }}</div></div>
      </el-col>
      <el-col :span="6">
        <div class="kpi-card" style="border-left:4px solid var(--color-info)"><div class="kpi-label">行政</div><div class="kpi-value">{{ stats.admin }}</div></div>
      </el-col>
    </el-row>

    <el-card class="glass table-card">
      <template v-if="rows.length === 0 && !loading">
        <table-empty-state text="暂无数据" />
      </template>
      <el-table v-else :data="rows" v-loading="loading" stripe border row-key="id">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="case_no" label="案号" width="180" show-overflow-tooltip />
        <el-table-column prop="case_name" label="案件名称" min-width="240" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.case_type)">{{ typeLabel(row.case_type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="case_cause" label="案由" width="120" show-overflow-tooltip />
        <el-table-column prop="court_name" label="法院" min-width="160" show-overflow-tooltip />
        <el-table-column prop="judge_date" label="裁判日期" width="110" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="resultTag(row.judgment_result)">{{ resultLabel(row.judgment_result) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span v-if="row.litigation_amount" class="mono">¥{{ Number(row.litigation_amount).toLocaleString() }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="入库时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="showDetail" title="案件详情" size="55%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="案号">{{ detail.case_no }}</el-descriptions-item>
        <el-descriptions-item label="案件名称">{{ detail.case_name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ typeLabel(detail.case_type) }}</el-descriptions-item>
        <el-descriptions-item label="案由">{{ detail.case_cause }}</el-descriptions-item>
        <el-descriptions-item label="法院">{{ detail.court_name }}</el-descriptions-item>
        <el-descriptions-item label="审理程序">{{ detail.trial_procedure }}</el-descriptions-item>
        <el-descriptions-item label="裁判日期">{{ detail.judge_date }}</el-descriptions-item>
        <el-descriptions-item label="裁判结果">{{ resultLabel(detail.judgment_result) }}</el-descriptions-item>
        <el-descriptions-item label="诉讼金额">¥{{ Number(detail.litigation_amount || 0).toLocaleString() }}</el-descriptions-item>
        <el-descriptions-item label="原告">{{ detail.plaintiff || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被告">{{ detail.defendant || '-' }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top:16px">关键事实</h4>
      <pre class="content-preview">{{ detail.key_facts || '-' }}</pre>
      <h4 style="margin-top:16px">裁判摘要</h4>
      <pre class="content-preview">{{ detail.judgment_summary || '-' }}</pre>
    </el-drawer>

    <el-dialog v-model="showImport" title="导入案例" width="800px" :close-on-click-modal="false">
      <div class="import-layout">
        <div class="left-upload">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".docx,.xlsx"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽 Word/Excel 文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .docx, .xlsx 格式</div>
            </template>
          </el-upload>
          <el-button type="primary" :disabled="!uploadFile || importing" style="margin-top: 16px; width: 100%;" @click="handlePreview">
            {{ importing ? '解析中...' : '上传并预览' }}
          </el-button>
        </div>
        <div class="right-preview">
          <template v-if="importProgress">
            <el-progress :percentage="importProgress.progress || 0" :stroke-width="10" style="margin-bottom: 16px;" />
            <div style="text-align: center; margin-bottom: 16px;">
              <template v-if="importProgress.status === 'running'">
                <el-tag type="warning">
                  <el-icon class="is-loading"><Refresh /></el-icon> 导入中 {{ importProgress.processed || 0 }} / {{ importProgress.total || 0 }}
                </el-tag>
              </template>
              <template v-else-if="importProgress.status === 'success'">
                <el-tag type="success">导入完成</el-tag>
                <div style="margin-top: 8px;">
                  <span style="color: var(--color-success);">成功: {{ importProgress.imported }}</span>
                  <span style="margin-left: 12px; color: var(--color-danger);">跳过: {{ importProgress.skipped }}</span>
                </div>
              </template>
              <template v-else-if="importProgress.status === 'failed'">
                <el-tag type="danger">导入失败: {{ importProgress.error }}</el-tag>
              </template>
            </div>
          </template>
          <template v-else-if="importPreview">
            <el-alert v-if="importPreview.errors > 0" type="warning" :closable="false" style="margin-bottom: 12px;">
              存在 {{ importPreview.errors }} 条无效数据，请检查后确认导入
            </el-alert>
            <el-table :data="importPreview.previewRows" stripe border max-height="400" size="small">
              <el-table-column prop="caseName" label="案件名称" min-width="160" show-overflow-tooltip />
              <el-table-column prop="caseNo" label="案号" width="140" show-overflow-tooltip />
              <el-table-column prop="courtName" label="法院" width="120" show-overflow-tooltip />
              <el-table-column prop="judgeDate" label="裁判日期" width="100" />
              <el-table-column prop="valid" label="有效" width="60">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.valid ? 'success' : 'danger'">{{ row.valid ? '是' : '否' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="error" label="错误" width="100" show-overflow-tooltip />
            </el-table>
            <div style="margin-top: 12px; text-align: right;">
              <span style="color: var(--color-success);">成功: {{ importPreview.totalRows - importPreview.errors }}</span>
              <span style="margin-left: 12px; color: var(--color-danger);">失败: {{ importPreview.errors }}</span>
              <span style="margin-left: 12px;">共 {{ importPreview.totalRows }} 条</span>
            </div>
            <div style="margin-top: 16px; text-align: right;">
              <el-button @click="showImport = false">取消</el-button>
              <el-button type="primary" :disabled="importPreview.errors > 0" @click="handleConfirm">确认导入</el-button>
            </div>
          </template>
          <el-empty v-else description="上传文件后可预览" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed, onMounted, onUnmounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'
import TableEmptyState from '../components/TableEmptyState.vue'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const filter = reactive({ cause: '', type: '', result: '' })
const showDetail = ref(false)
const detail = ref(null)
const showImport = ref(false)
const uploadRef = ref(null)
const uploadFile = ref(null)
const importPreview = ref(null)
const importing = ref(false)
const importProgress = ref(null)
const importPollTimer = ref(null)

const stats = reactive({ total: 0, civil: 0, criminal: 0, admin: 0 })

function typeLabel(t) { return ({ 1: '民事', 2: '刑事', 3: '行政' }[t] || t) }
function typeTag(t) { return ({ 1: 'primary', 2: 'danger', 3: 'warning' }[t] || '') }
function resultLabel(r) { return ({ 1: '全部支持', 2: '部分支持', 3: '驳回' }[r] || r) }
function resultTag(r) { return ({ 1: 'success', 2: 'warning', 3: 'danger' }[r] || '') }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod02/cases', { params: { page: page.value, pageSize: pageSize.value, cause: filter.cause || undefined, caseType: filter.type || undefined, judgment: filter.result || undefined } })
    const list = res?.list || []
    rows.value = list
    total.value = res?.total || list.length
    stats.civil = list.filter(r => r.case_type === 1).length
    stats.criminal = list.filter(r => r.case_type === 2).length
    stats.admin = list.filter(r => r.case_type === 3).length
    stats.total = list.length
  } catch (e) { rows.value = []; total.value = 0 }
  finally { loading.value = false }
}

function openDetail(row) { detail.value = row; showDetail.value = true }
function reset() { filter.cause = ''; filter.type = ''; filter.result = ''; load() }
watch([page, pageSize], load)
onMounted(load)

onUnmounted(() => {
  if (importPollTimer.value) {
    clearInterval(importPollTimer.value)
    importPollTimer.value = null
  }
})

function showImportDialog() {
  showImport.value = true
  importPreview.value = null
  uploadFile.value = null
  importProgress.value = null
  uploadRef.value?.clearFiles()
  if (importPollTimer.value) {
    clearInterval(importPollTimer.value)
    importPollTimer.value = null
  }
}

function handleFileChange(file) {
  uploadFile.value = file.raw
}

async function handlePreview() {
  if (!uploadFile.value) return
  importing.value = true
  const formData = new FormData()
  formData.append('file', uploadFile.value)
  try {
    const res = await api.judgmentImport.preview(formData)
    importPreview.value = res
    ElMessage.success('预览生成成功')
  } catch (e) {
    ElMessage.error('预览失败: ' + (e.message || '未知错误'))
    importPreview.value = null
  } finally {
    importing.value = false
  }
}

async function handleConfirm() {
  if (!importPreview.value?.data) return
  try {
    const res = await api.judgmentImport.confirm({ cases: importPreview.value.data })
    const jobId = res?.jobId
    const total = res?.total || 0

    importProgress.value = { status: 'running', progress: 0, processed: 0, total, imported: 0, skipped: 0 }
    importPreview.value = null

    if (jobId) {
      importPollTimer.value = setInterval(async () => {
        try {
          const statusRes = await api.get(`/admin/data-import/judgments/status/${jobId}`)
          const status = statusRes.data
          if (status) {
            importProgress.value = status
            if (status.status === 'success' || status.status === 'failed') {
              clearInterval(importPollTimer.value)
              importPollTimer.value = null
              if (status.status === 'success') {
                ElMessage.success(`导入完成：成功 ${status.imported} 条，跳过 ${status.skipped} 条`)
                load()
                setTimeout(() => {
                  showImport.value = false
                  importProgress.value = null
                }, 1500)
              } else {
                ElMessage.error('导入失败: ' + (status.error || '未知错误'))
              }
            }
          }
        } catch (e) {
          clearInterval(importPollTimer.value)
          importPollTimer.value = null
        }
      }, 1000)
    }
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.message || '未知错误'))
  }
}
</script>

<style lang="scss" scoped>
.cases-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom:16px; }
.kpi-card { background:var(--color-bg-card); border-radius:10px; padding:16px; border:1px solid var(--color-border); .kpi-label { font-size:12px; color:var(--color-text-muted); margin-bottom:6px; } .kpi-value { font-size:22px; font-weight:700; color:var(--color-text-primary); } }
.mono { font-family:'Cascadia Code','Consolas',monospace; font-size:13px; }

.content-preview { background:var(--color-bg-page); padding:16px; border-radius:8px; white-space:pre-wrap; word-break:break-word; font-size:13px; line-height:1.6; border:1px solid var(--color-border); max-height:30vh; overflow-y:auto; }
.import-layout { display: flex; gap: 20px; }
.left-upload { width: 260px; flex-shrink: 0; }
.right-preview { flex: 1; }
</style>