<template>
  <div class="law-relations-page">
    <div class="page-header">
      <div class="header-content">
        <h2>法规关联管理</h2>
        <p>管理法规条款之间的关联关系，支持参照、援引、修改、废止、配套等关联类型</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openDialog()">新增关联</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="来源条款ID">
          <el-input-number v-model="filter.sourceArticleId" placeholder="来源法规ID" clearable :min="1" style="width:150px" />
        </el-form-item>
        <el-form-item label="目标条款ID">
          <el-input-number v-model="filter.targetArticleId" placeholder="目标法规ID" clearable :min="1" style="width:150px" />
        </el-form-item>
        <el-form-item label="关联类型">
          <el-select v-model="filter.relationType" placeholder="请选择" clearable style="width:140px">
            <el-option label="参照" value="参照" />
            <el-option label="援引" value="援引" />
            <el-option label="修改" value="修改" />
            <el-option label="废止" value="废止" />
            <el-option label="配套" value="配套" />
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
        <el-table-column label="来源法规" min-width="200">
          <template #default="{ row }">
            <span>{{ row.source_article_id }}</span>
            <el-tag size="small" type="info" style="margin-left:8px" v-if="row.source_article_title">{{ row.source_article_title }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="relation_type" label="关联类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="relationTypeTag(row.relation_type)">{{ row.relation_type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标法规" min-width="200">
          <template #default="{ row }">
            <span>{{ row.target_article_id }}</span>
            <el-tag size="small" type="info" style="margin-left:8px" v-if="row.target_article_title">{{ row.target_article_title }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="80">
          <template #default="{ row }">
            {{ row.weight }}
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="来源条款ID" prop="sourceArticleId">
          <el-input-number v-model="form.sourceArticleId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="目标条款ID" prop="targetArticleId">
          <el-input-number v-model="form.targetArticleId" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联类型" prop="relationType">
          <el-select v-model="form.relationType" placeholder="请选择关联类型" style="width:100%">
            <el-option label="参照" value="参照" />
            <el-option label="援引" value="援引" />
            <el-option label="修改" value="修改" />
            <el-option label="废止" value="废止" />
            <el-option label="配套" value="配套" />
          </el-select>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" :min="0.01" :max="10" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增关联')
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const filter = reactive({
  page: 1,
  pageSize: 20,
  sourceArticleId: null,
  targetArticleId: null,
  relationType: ''
})

const form = reactive({
  sourceArticleId: null,
  targetArticleId: null,
  relationType: '',
  weight: 1.0
})

const rules = {
  sourceArticleId: [{ required: true, message: '请输入来源条款ID', trigger: 'blur' }],
  targetArticleId: [{ required: true, message: '请输入目标条款ID', trigger: 'blur' }]
}

function relationTypeTag(type) {
  const map = {
    '参照': 'primary',
    '援引': 'success',
    '修改': 'warning',
    '废止': 'danger',
    '配套': 'info'
  }
  return map[type] || 'info'
}

async function load() {
  loading.value = true
  try {
    const params = {
      page: filter.page,
      pageSize: filter.pageSize
    }
    if (filter.sourceArticleId) params.sourceArticleId = filter.sourceArticleId
    if (filter.targetArticleId) params.targetArticleId = filter.targetArticleId
    if (filter.relationType) params.relationType = filter.relationType

    const res = await api.get('/admin/biz/mod01/law-relations', { params })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reset() {
  filter.page = 1
  filter.sourceArticleId = null
  filter.targetArticleId = null
  filter.relationType = ''
  load()
}

function openDialog(row) {
  if (row) {
    dialogTitle.value = '编辑关联'
    isEdit.value = true
    editingId.value = row.id
    form.sourceArticleId = row.source_article_id
    form.targetArticleId = row.target_article_id
    form.relationType = row.relation_type || ''
    form.weight = parseFloat(row.weight) || 1.0
  } else {
    dialogTitle.value = '新增关联'
    isEdit.value = false
    editingId.value = null
    form.sourceArticleId = null
    form.targetArticleId = null
    form.relationType = ''
    form.weight = 1.0
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (form.sourceArticleId === form.targetArticleId) {
    ElMessage.error('来源条款和目标条款不能相同')
    return
  }

  try {
    if (isEdit.value) {
      const res = await api.put(`/admin/biz/mod01/law-relations/${editingId.value}`, {
        relationType: form.relationType,
        weight: form.weight
      })
      if (res.data?.ok) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.data?.error || '更新失败')
      }
    } else {
      const res = await api.post('/admin/biz/mod01/law-relations', {
        sourceArticleId: form.sourceArticleId,
        targetArticleId: form.targetArticleId,
        relationType: form.relationType,
        weight: form.weight
      })
      if (res.data?.ok) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.data?.error || '创建失败')
      }
    }
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除该关联关系吗？`, '删除确认', { type: 'warning' })
    const res = await api.delete(`/admin/biz/mod01/law-relations/${row.id}`)
    if (res.data?.ok) {
      ElMessage.success('删除成功')
      load()
    } else {
      ElMessage.error(res.data?.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.law-relations-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap: wrap; gap: 10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
</style>
