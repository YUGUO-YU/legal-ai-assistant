<template>
  <div class="llm-models-page">
    <div class="page-header">
      <div class="header-content">
        <h2>LLM 模型配置</h2>
        <p>AI 域 · 供应商 / 端点 / 权重 · 主备切换 · 健康探测</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增模型</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="model_code" label="代码" width="120" />
        <el-table-column prop="model_name" label="模型名称" min-width="160" />
        <el-table-column prop="provider" label="供应商" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.provider }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="endpoint" label="端点" min-width="200" show-overflow-tooltip />
        <el-table-column label="API Key" width="160">
          <template #default="{ row }">
            <span v-if="row.api_key_enc" class="mono masked-key">{{ maskKey(row.api_key_enc) }}</span>
            <el-tag v-else size="small" type="info">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参数" width="180">
          <template #default="{ row }">
            <span class="mono">T={{ row.temperature }} / max={{ row.max_tokens }} / p={{ row.top_p }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.is_primary === 1" type="success" size="small">当前使用</el-tag>
            <el-tag v-if="row.is_fallback === 1" type="warning" size="small" style="margin-left:4px">备用</el-tag>
            <el-tag v-if="row.is_primary !== 1 && row.is_fallback !== 1" type="info" size="small">普通</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="健康" width="90">
          <template #default="{ row }">
            <span class="dot" :class="healthDot(row.health_status)"></span>
            <span style="margin-left:4px; font-size:12px">{{ healthLabel(row.health_status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggleModel(row)" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.is_primary !== 1" link type="warning" size="small" @click="handleSetActive(row)">设为当前</el-button>
            <el-button link type="info" size="small" @click="openUpdateKey(row)">更新密钥</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="handleCheck(row)">探测</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑模型' : '新增模型'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模型代码" required>
              <el-input v-model="form.model_code" placeholder="例：gpt-4o" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称" required>
              <el-input v-model="form.model_name" placeholder="例：GPT-4o" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="供应商" required>
              <el-select v-model="form.provider" style="width:100%" filterable allow-create>
                <el-option label="OpenAI" value="openai" />
                <el-option label="Azure" value="azure" />
                <el-option label="Anthropic" value="anthropic" />
                <el-option label="智谱AI" value="zhipu" />
                <el-option label="阿里百炼" value="qwen" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="Moonshot" value="moonshot" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Key">
              <el-input v-model="form.api_key_enc" type="password" placeholder="留空不修改" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="端点 URL" required>
          <el-input v-model="form.endpoint" placeholder="例：https://api.openai.com/v1/chat/completions" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="温度">
              <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Max Tokens">
              <el-input-number v-model="form.max_tokens" :min="256" :max="131072" :step="1024" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Top P">
              <el-input-number v-model="form.top_p" :min="0" :max="1" :step="0.05" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色设定">
          <el-checkbox v-model="isPrimary" @change="onRoleChange">主模型（有且仅有一个）</el-checkbox>
          <el-checkbox v-model="isFallback" @change="onRoleChange" style="margin-left:16px">备用模型</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showKeyDialog" title="更新 API 密钥" width="500px">
      <el-form label-width="100px">
        <el-form-item label="模型">
          <el-input :value="keyForm.modelName" disabled />
        </el-form-item>
        <el-form-item label="API 密钥" required>
          <el-input v-model="keyForm.apiKey" type="password" placeholder="请输入新的 API 密钥" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showKeyDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateKey">确认更新</el-button>
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
const showDialog = ref(false)
const showKeyDialog = ref(false)
const form = reactive({ id: null, model_code: '', model_name: '', provider: 'openai', endpoint: '', api_key_enc: '', temperature: 0.7, max_tokens: 4096, top_p: 0.95, is_primary: 0, is_fallback: 0, status: 1 })
const isPrimary = ref(false)
const isFallback = ref(false)
const keyForm = reactive({ id: null, modelName: '', apiKey: '' })

function healthDot(s) { return { 1: 'dot-success', 2: 'dot-warning', 3: 'dot-danger' }[s] || 'dot-idle' }
function healthLabel(s) { return { 1: '健康', 2: '降级', 3: '故障' }[s] || '未知' }
function maskKey(key) {
  if (!key) return ''
  if (key.length <= 8) return '***'
  return key.substring(0, 4) + '***' + key.substring(key.length - 4)
}

function onRoleChange() {
  form.is_primary = isPrimary.value ? 1 : 0
  form.is_fallback = isFallback.value ? 1 : 0
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ai/llm-models')
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, model_code: '', model_name: '', provider: 'openai', endpoint: '', api_key_enc: '', temperature: 0.7, max_tokens: 4096, top_p: 0.95, is_primary: 0, is_fallback: 0, status: 1 })
  isPrimary.value = false; isFallback.value = false
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  isPrimary.value = row.is_primary === 1
  isFallback.value = row.is_fallback === 1
  showDialog.value = true
}

async function handleSave() {
  if (!form.model_code || !form.endpoint) { ElMessage.warning('模型代码和端点URL必填'); return }
  const payload = { ...form }
  if (!payload.api_key_enc) delete payload.api_key_enc
  if (form.id) delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.put('/admin/ai/llm-models/' + form.id, payload)
    } else {
      res = await api.post('/admin/ai/llm-models', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除模型「${row.model_name}」？`, '确认', { type: 'warning' })
    await api.delete('/admin/ai/llm-models/' + row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function handleCheck(row) {
  try {
    const res = await api.post(`/admin/ai/llm-models/${row.id}/health-check`)
    if (res.data?.ok) { ElMessage.success(`${row.model_name} 健康正常`); load() }
    else ElMessage.error(res.data?.error || '探测失败')
  } catch (e) { ElMessage.error('探测失败') }
}

async function toggleModel(row) {
  const action = row.status === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}模型「${row.model_name}」？${action === '停用' ? '停用后该模型将无法使用。' : '启用后该模型将恢复正常使用。'}`, `确认${action}`, { type: 'warning' })
    await api.put('/admin/ai/llm-models/' + row.id, { status: row.status === 1 ? 0 : 1 })
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(`已${action}`)
  } catch (e) { if (e !== 'cancel') ElMessage.error('切换失败') }
}

async function handleSetActive(row) {
  try {
    await ElMessageBox.confirm(`将「${row.model_name}」设为当前使用的大模型？`, '确认切换', { type: 'info' })
    const res = await api.post(`/admin/ai/llm-models/${row.id}/set-active`)
    if (res.data?.ok) {
      ElMessage.success('已切换为当前模型')
      load()
    } else {
      ElMessage.error(res.data?.error || '切换失败')
    }
  } catch (e) { if (e !== 'cancel') ElMessage.error('切换失败') }
}

function openUpdateKey(row) {
  keyForm.id = row.id
  keyForm.modelName = row.model_name
  keyForm.apiKey = ''
  showKeyDialog.value = true
}

async function handleUpdateKey() {
  if (!keyForm.apiKey || !keyForm.apiKey.trim()) {
    ElMessage.warning('请输入 API 密钥')
    return
  }
  try {
    await ElMessageBox.confirm('此操作将更新模型「' + keyForm.modelName + '」的 API 密钥，是否确认？', '确认更新密钥', { type: 'warning' })
    const res = await api.post(`/admin/ai/llm-models/${keyForm.id}/update-key`, { apiKey: keyForm.apiKey })
    if (res.data?.ok) {
      ElMessage.success('API 密钥已更新')
      showKeyDialog.value = false
    } else {
      ElMessage.error(res.data?.error || '更新失败')
    }
  } catch (e) { if (e !== 'cancel') ElMessage.error('更新失败') }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.llm-models-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.mono { font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 12px; color: #475569; }
.dot { display:inline-block; width:8px; height:8px; border-radius:50%; }
.dot-success { background: var(--color-success); box-shadow:0 0 0 3px rgba(16,185,129,0.18); }
.dot-warning { background: var(--color-warning); box-shadow:0 0 0 3px rgba(245,158,11,0.18); }
.dot-danger { background: var(--color-danger); box-shadow:0 0 0 3px rgba(239,68,68,0.18); }
.dot-idle { background: var(--color-border); }
.masked-key { color: var(--color-text-muted); font-size: 12px; letter-spacing: 0.5px; }
</style>