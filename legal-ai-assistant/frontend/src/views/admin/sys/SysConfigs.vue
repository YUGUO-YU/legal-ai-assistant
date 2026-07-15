<template>
  <div class="sys-config-page">
    <div class="page-header">
      <div class="header-content">
        <h2>系统参数</h2>
        <p>动态配置 · LLM/Cache/RateLimit/Feature Flag · 即时生效</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="warning" @click="handleRefreshCache" :loading="refreshing">刷新缓存</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="分组">
          <el-select v-model="filter.group" clearable placeholder="全部" style="width:160px">
            <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="config_key" label="Key" min-width="240" />
        <el-table-column prop="config_value" label="Value" min-width="240">
          <template #default="{ row }">
            <el-tag size="small" :type="row.value_type === 'boolean' ? (row.config_value === 'true' ? 'success' : 'info') : ''">{{ row.config_value }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="config_group" label="分组" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ row.config_group }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value_type" label="类型" width="90" />
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column prop="updated_at" label="更新时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑参数' : '新增参数'" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Key" required>
          <el-input v-model="form.config_key" :disabled="!!form.id" placeholder="例：llm.default.temperature" />
        </el-form-item>
        <el-form-item label="Value" required>
          <el-input v-model="form.config_value" placeholder="值" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.value_type" style="width:100%">
            <el-option label="string" value="string" />
            <el-option label="number" value="number" />
            <el-option label="boolean" value="boolean" />
            <el-option label="json" value="json" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="form.config_group" allow-create filterable style="width:100%">
            <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
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
const refreshing = ref(false)
const filter = reactive({ group: '' })
const groups = ['common', 'llm', 'cache', 'rate_limit', 'feature']
const showDialog = ref(false)
const form = reactive({ id: null, config_key: '', config_value: '', config_group: 'common', value_type: 'string', remark: '' })

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/sys/configs', {
      params: { page: 1, pageSize: 100, module: filter.group || undefined }
    })
    rows.value = res.data?.list || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, config_key: '', config_value: '', config_group: 'common', value_type: 'string', remark: '' })
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  showDialog.value = true
}

async function handleSave() {
  if (!form.config_key || !form.config_value) {
    ElMessage.warning('Key 与 Value 必填')
    return
  }
  try {
    let res
    if (form.id) {
      res = await api.put(`/admin/sys/configs/${form.id}`, form)
    } else {
      res = await api.post('/admin/sys/configs', form)
    }
    if (res.data?.ok) {
      ElMessage.success('配置已更新并应用')
      showDialog.value = false
      await api.post('/admin/sys/configs/refresh')
      load()
    } else {
      ElMessage.error(res.data?.error || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败：' + (e.message || ''))
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除配置 ${row.config_key}？`, '确认', { type: 'warning' })
    const res = await api.delete(`/admin/sys/configs/${row.id}`)
    if (res.data?.ok) {
      ElMessage.success('已删除')
      load()
    } else {
      ElMessage.error(res.data?.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败：' + (e.message || ''))
  }
}

async function handleRefreshCache() {
  try {
    await ElMessageBox.confirm('此操作将清除所有系统缓存，可能导致短暂性能下降，是否确认刷新？', '确认刷新缓存', { type: 'warning', confirmButtonText: '确认刷新', cancelButtonText: '取消' })
    refreshing.value = true
    const res = await api.post('/admin/sys/cache/refresh')
    if (res.data?.ok) {
      ElMessage.success(res.data?.message || '缓存已刷新')
    } else {
      ElMessage.error(res.data?.error || '刷新失败')
    }
  } catch (e) { if (e !== 'cancel') ElMessage.error('刷新失败') }
  finally { refreshing.value = false }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.sys-config-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; }
.filter-card { margin-bottom: 16px; }
</style>