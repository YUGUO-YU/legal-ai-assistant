<template>
  <div class="announcements-page">
    <div class="page-header">
      <div class="header-content">
        <h2>系统公告</h2>
        <p>系统运维域 · 公告与通知管理</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建公告</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="标题/内容" clearable style="width:220px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template v-if="selectedRows.length > 0">
        <div class="batch-actions">
          <span>已选择 {{ selectedRows.length }} 项</span>
          <el-button size="small" type="danger" @click="handleBatchDelete">批量删除</el-button>
          <el-button size="small" type="success" @click="handleBatchToggle(1)">批量发布</el-button>
          <el-button size="small" type="warning" @click="handleBatchToggle(0)">批量撤回</el-button>
          <el-button size="small" @click="selectedRows = []">取消选择</el-button>
        </div>
      </template>
      <el-table v-else :data="rows" v-loading="loading" stripe border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="priorityTag(row.priority)" :effect="row.priority === 2 ? 'dark' : 'light'">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="published_at" label="发布时间" width="170" />
        <el-table-column prop="expired_at" label="过期时间" width="170" />
        <el-table-column prop="created_by" label="创建人" width="100" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="load"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑公告' : '新建公告'" width="680px">
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="公告标题" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option label="系统公告" :value="1" />
            <el-option label="功能更新" :value="2" />
            <el-option label="维护通知" :value="3" />
            <el-option label="安全警告" :value="4" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width:100%">
                <el-option label="普通" :value="0" />
                <el-option label="重要" :value="1" />
                <el-option label="紧急" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="草稿" :value="0" />
                <el-option label="发布" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="公告内容，支持富文本..." maxlength="2000" show-word-limit />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="发布时间">
              <el-date-picker v-model="form.published_at" type="datetime" placeholder="选择时间" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过期时间">
              <el-date-picker v-model="form.expired_at" type="datetime" placeholder="留空表示永不过期" style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filter = reactive({ keyword: '' })
const showDialog = ref(false)
const form = reactive({ id: null, title: '', content: '', type: 1, priority: 0, status: 1, published_at: '', expired_at: '', created_by: 'admin' })
const selectedRows = ref([])

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 项？`, '批量删除', { type: 'warning' })
    const ids = selectedRows.value.map(r => r.id)
    await api.post('/admin/announcement/batch-delete', { ids })
    ElMessage.success('删除成功')
    selectedRows.value = []
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const handleBatchToggle = async (status) => {
  if (!selectedRows.value.length) return
  const ids = selectedRows.value.map(r => r.id)
  await api.post('/admin/announcement/batch-toggle', { ids, status })
  ElMessage.success('操作成功')
  selectedRows.value = []
  load()
}

const rules = reactive({
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过200字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ],
  priority: [
    { required: true, message: '请选择优先级', trigger: 'change' }
  ]
})

function typeLabel(type) { return ['', '系统公告', '功能更新', '维护通知', '安全警告'][type] || '系统公告' }
function typeTag(type) { return ['', '', 'success', 'warning', 'danger'][type] || '' }
function priorityLabel(p) { return ['普通', '重要', '紧急'][p] || '普通' }
function priorityTag(p) { return ['', 'warning', 'danger'][p] || '' }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/announcements', { params: { page: currentPage.value, pageSize: pageSize.value, keyword: filter.keyword } })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    ElMessage.error('加载失败')
  }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, title: '', content: '', type: 1, priority: 0, status: 1, published_at: '', expired_at: '', created_by: 'admin' })
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row, id: row.id })
  showDialog.value = true
}

async function handleSave() {
  if (!form.title) { ElMessage.warning('标题必填'); return }
  if (!form.content) { ElMessage.warning('内容必填'); return }
  const payload = { ...form }
  if (form.id) delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.put('/admin/infra/announcements/' + form.id, payload)
    } else {
      res = await api.post('/admin/infra/announcements', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除公告「${row.title}」？`, '确认', { type: 'warning' })
    await api.delete('/admin/infra/announcements/' + row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

function handlePageChange(page) { currentPage.value = page; load() }
function reset() { filter.keyword = ''; currentPage.value = 1; load() }
onMounted(load)
</script>

<style lang="scss" scoped>
.announcements-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  margin-bottom: 12px;

  span {
    color: var(--color-text-primary);
    font-weight: 500;
  }
}
</style>
