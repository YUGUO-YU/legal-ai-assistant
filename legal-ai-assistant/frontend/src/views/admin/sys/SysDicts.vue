<template>
  <div class="sys-dicts-page">
    <div class="page-header">
      <div class="header-content">
        <h2>数据字典</h2>
        <p>系统配置域 · 分组枚举 / 排序 / 状态管理</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增字典项</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="分组">
          <el-select v-model="filter.dict_type" clearable filterable placeholder="全部" style="width:180px">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Grouped Card Layout -->
    <div v-if="groupedRows.length">
      <el-card v-for="group in groupedRows" :key="group.type" style="margin-bottom:16px">
        <template #header>
          <div class="group-header">
            <el-tag type="primary" size="small">{{ group.type }}</el-tag>
            <span class="group-count">{{ group.items.length }} 项</span>
          </div>
        </template>
        <el-table :data="group.items" stripe border size="small">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="dict_label" label="标签" min-width="140" />
          <el-table-column prop="dict_value" label="值" min-width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.dict_value }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sort_order" label="排序" width="70" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    <div v-else class="empty-hint" v-loading="loading">暂无字典数据</div>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑字典项' : '新增字典项'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分组" required>
          <el-select v-model="form.dict_type" filterable allow-create style="width:100%">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" required>
          <el-input v-model="form.dict_label" placeholder="例：民事一审" />
        </el-form-item>
        <el-form-item label="值" required>
          <el-input v-model="form.dict_value" placeholder="例：CIVIL_FIRST" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort_order" :min="0" :max="9999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="statusOn" active-text="启用" />
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const filter = reactive({ dict_type: '' })
const showDialog = ref(false)
const form = reactive({ id: null, dict_type: '', dict_label: '', dict_value: '', sort_order: 0, status: 1 })
const statusOn = ref(true)

const dictTypes = computed(() => [...new Set(rows.value.map(r => r.dict_type))].sort())

const groupedRows = computed(() => {
  const map = {}
  rows.value.forEach(r => {
    const key = r.dict_type || '未分组'
    if (!map[key]) map[key] = { type: key, items: [] }
    map[key].items.push(r)
  })
  return Object.values(map).sort((a, b) => a.type.localeCompare(b.type))
})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/dicts/list', { params: { dict_type: filter.dict_type || undefined } })
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, dict_type: '', dict_label: '', dict_value: '', sort_order: 0, status: 1 })
  statusOn.value = true
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  statusOn.value = row.status === 1
  showDialog.value = true
}

async function handleSave() {
  if (!form.dict_type || !form.dict_label || !form.dict_value) { ElMessage.warning('分组/标签/值必填'); return }
  form.status = statusOn.value ? 1 : 0
  const payload = { ...form }
  delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.put('/admin/infra/dicts/' + form.id, payload)
    } else {
      res = await api.post('/admin/infra/dicts', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除 ${row.dict_label}？`, '确认', { type: 'warning' })
    await api.delete('/admin/infra/dicts/' + row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

function reset() { filter.dict_type = ''; load() }
onMounted(load)
</script>

<style lang="scss" scoped>
.sys-dicts-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.group-header { display:flex; align-items:center; gap:10px; .group-count { font-size:13px; color:#64748b; } }
.empty-hint { display:flex; align-items:center; justify-content:center; height:200px; color:#94a3b8; font-size:13px; }
</style>