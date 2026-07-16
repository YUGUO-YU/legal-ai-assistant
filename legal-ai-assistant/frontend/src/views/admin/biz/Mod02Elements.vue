<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">案件要素字典</h2>
        <p>MOD-02 · 要素分类 / 排序 / 启用状态</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增要素</el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6" v-for="g in groupedStats" :key="g.name">
        <div class="kpi-card" :style="{ borderLeft: `4px solid ${palette[$index]}` }">
          <div class="kpi-label">{{ g.name }}</div>
          <div class="kpi-value">{{ g.count }}</div>
        </div>
      </el-col>
    </el-row>

    <el-card class="glass table-card" v-for="group in grouped" :key="group.category" style="margin-bottom:14px">
      <template #header>
        <div class="group-head">
          <el-tag type="primary" size="small">{{ group.category }}</el-tag>
          <span class="group-num">{{ group.items.length }} 项</span>
        </div>
      </template>
      <el-table :data="group.items" stripe border size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="element_code" label="代码" width="150">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.element_code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="element_name" label="名称" min-width="180" />
        <el-table-column prop="sort_order" label="排序" width="80" />
        <el-table-column label="状态" width="80">
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

    <el-dialog v-model="showDialog" :title="form.id ? '编辑要素' : '新增要素'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="代码" required>
          <el-input v-model="form.element_code" :disabled="!!form.id" placeholder="例：DISPUTE_AMOUNT" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.element_name" placeholder="例：争议金额" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" filterable allow-create style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'
import { adminChartPalette } from '@/utils/adminChartPalette'

const palette = adminChartPalette
const rows = ref([])
const loading = ref(false)
const showDialog = ref(false)
const statusOn = ref(true)
const form = reactive({ id: null, element_code: '', element_name: '', category: '通用', sort_order: 0, status: 1 })

const categories = computed(() => [...new Set(rows.value.map(r => r.category))].sort())
const grouped = computed(() => {
  const map = {}
  rows.value.forEach(r => {
    const k = r.category || '未分类'
    if (!map[k]) map[k] = { category: k, items: [] }
    map[k].items.push(r)
  })
  return Object.values(map).sort((a, b) => a.category.localeCompare(b.category))
})
const groupedStats = computed(() => {
  return Object.entries(
    rows.value.reduce((acc, r) => {
      const k = r.category || '未分类'
      acc[k] = (acc[k] || 0) + 1
      return acc
    }, {})
  ).map(([name, count]) => ({ name, count })).sort((a, b) => b.count - a.count).slice(0, 6)
})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod02/case-elements')
    rows.value = res?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, element_code: '', element_name: '', category: '通用', sort_order: 0, status: 1 })
  statusOn.value = true; showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  statusOn.value = row.status === 1; showDialog.value = true
}

async function handleSave() {
  if (!form.element_code || !form.element_name) { ElMessage.warning('代码和名称必填'); return }
  form.status = statusOn.value ? 1 : 0
  const payload = { ...form }; delete payload.id
  try {
    const res = form.id
      ? await api.post(`/admin/case_element_dict/${form.id}/update`, payload)
      : await api.post('/admin/case_element_dict/create', payload)
    if (res?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除要素「${row.element_name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/case_element_dict/${row.id}/delete`)
    ElMessage.success('已删除'); load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.elements-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.kpi-card { background:var(--color-bg-card); border-radius:10px; padding:12px 16px; border:1px solid var(--color-border); .kpi-label { font-size:12px; color:var(--color-text-muted); margin-bottom:4px; } .kpi-value { font-size:20px; font-weight:700; color:var(--color-text-primary); } }
.group-head { display:flex; align-items:center; gap:10px; .group-num { font-size:13px; color: var(--color-text-muted); } }
</style>