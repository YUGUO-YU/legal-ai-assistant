<template>
  <div class="kb-page">
    <div class="page-header">
      <div class="header-content">
        <h2>知识库管理</h2>
        <p>MOD-09 · 知识库 CRUD · 公开/私有 · 文档统计</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建知识库</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="kb_uuid" label="UUID" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.kb_uuid?.substring(0, 12) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.is_public === 1 ? 'success' : 'info'">{{ row.is_public === 1 ? '公开' : '私有' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文档数" width="80">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ row.doc_count || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner_id" label="Owner" width="100" />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑知识库' : '新建知识库'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="例：民事判例库" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="知识库用途说明" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="Owner ID" required>
              <el-input v-model="form.owner_id" placeholder="用户ID" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可见范围">
              <el-switch v-model="isPublic" active-text="公开" inactive-text="私有" />
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
import api from '../../api'

const rows = ref([])
const loading = ref(false)
const showDialog = ref(false)
const isPublic = ref(false)
const form = reactive({ id: null, kb_uuid: '', name: '', description: '', owner_id: '', is_public: 0, doc_count: 0 })

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod09/kb-bases')
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, kb_uuid: '', name: '', description: '', owner_id: '', is_public: 0, doc_count: 0 })
  isPublic.value = false; showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  isPublic.value = row.is_public === 1; showDialog.value = true
}

async function handleSave() {
  if (!form.name || !form.owner_id) { ElMessage.warning('名称和Owner必填'); return }
  form.is_public = isPublic.value ? 1 : 0
  const payload = { ...form }; delete payload.id; delete payload.kb_uuid; delete payload.doc_count
  try {
    const res = form.id
      ? await api.post(`/admin/{table}/${form.id}/update`.replace('{table}', 'kb_knowledge_base'), payload)
      : await api.post('/admin/{table}/create'.replace('{table}', 'kb_knowledge_base'), payload)
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除知识库「${row.name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/{table}/${row.id}/delete`.replace('{table}', 'kb_knowledge_base'))
    ElMessage.success('已删除'); load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.kb-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.mono { font-family:'Cascadia Code','Consolas',monospace; font-size:12px; }
</style>