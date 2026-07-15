<template>
  <div class="contract-rules-page">
    <div class="page-header">
      <div class="header-content">
        <h2>合同审查规则</h2>
        <p>MOD-08 · 多维维度 / 权重 / 高低阈值 · 风险评估基准</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="dimension" label="审查维度" min-width="200">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.dimension }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权重" width="120">
          <template #default="{ row }">
            <div class="weight-bar">
              <el-progress :percentage="Number(row.weight) * 100" :stroke-width="10" :color="weightColor(Number(row.weight))" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="高风险阈值" width="110">
          <template #default="{ row }">
            <el-tag type="danger" size="small" effect="dark">{{ row.threshold_high }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="低风险阈值" width="110">
          <template #default="{ row }">
            <el-tag type="warning" size="small" effect="dark">{{ row.threshold_low }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑规则' : '新增规则'" width="540px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="审查维度" required>
          <el-select v-model="form.dimension" style="width:100%" filterable allow-create>
            <el-option label="合同主体合规" value="合同主体合规" />
            <el-option label="价款条款" value="价款条款" />
            <el-option label="违约责任" value="违约责任" />
            <el-option label="知识产权" value="知识产权" />
            <el-option label="保密条款" value="保密条款" />
            <el-option label="争议解决" value="争议解决" />
            <el-option label="不可抗力" value="不可抗力" />
            <el-option label="合同期限" value="合同期限" />
            <el-option label="交付验收" value="交付验收" />
            <el-option label="法律适用" value="法律适用" />
          </el-select>
        </el-form-item>
        <el-form-item label="权重 (0-1)">
          <el-slider v-model="weightPct" :min="0" :max="100" :step="5" show-input />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="高风险阈值(%)">
              <el-input-number v-model="form.threshold_high" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="低风险阈值(%)">
              <el-input-number v-model="form.threshold_low" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="启用" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const showDialog = ref(false)
const statusOn = ref(true)
const weightPct = ref(50)
const form = reactive({ id: null, dimension: '', weight: 0.5, threshold_high: 70, threshold_low: 40, status: 1 })

function weightColor(v) {
  if (v >= 0.7) return '#6366f1'
  if (v >= 0.4) return '#10b981'
  return '#94a3b8'
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/biz/mod08/contract-rules')
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, dimension: '', weight: 0.5, threshold_high: 70, threshold_low: 40, status: 1 })
  statusOn.value = true; weightPct.value = 50; showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  statusOn.value = row.status === 1
  weightPct.value = Math.round(Number(row.weight) * 100)
  showDialog.value = true
}

async function handleSave() {
  if (!form.dimension) { ElMessage.warning('审查维度必填'); return }
  form.weight = weightPct.value / 100
  form.status = statusOn.value ? 1 : 0
  const payload = { ...form }; delete payload.id
  try {
    const res = form.id
      ? await api.post(`/admin/contract_review_rule/${form.id}/update`, payload)
      : await api.post('/admin/contract_review_rule/create', payload)
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除规则「${row.dimension}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/contract_review_rule/${row.id}/delete`)
    ElMessage.success('已删除'); load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.contract-rules-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.weight-bar { max-width:180px; }
</style>