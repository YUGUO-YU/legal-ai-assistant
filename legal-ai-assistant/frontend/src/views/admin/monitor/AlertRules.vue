<template>
  <div class="alert-rules-page">
    <div class="page-header">
      <div class="header-content">
        <h2>告警规则管理</h2>
        <p>监控域 · 指标阈值 / 通知渠道 / 静默期 / 启用停用</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增规则</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="告警级别">
          <el-select v-model="filter.level" clearable placeholder="全部" style="width:130px">
            <el-option label="P0 紧急" :value="1" />
            <el-option label="P1 严重" :value="2" />
            <el-option label="P2 提示" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" clearable placeholder="全部" style="width:110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
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
        <el-table-column prop="rule_name" label="规则名称" min-width="180" />
        <el-table-column prop="metric" label="指标" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.metric }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="条件" width="150">
          <template #default="{ row }">
            <span class="mono">{{ row.metric }} {{ operLabel(row.operator) }} {{ row.threshold }} / {{ row.duration_sec }}s</span>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="levelTag(row.level)" effect="dark">{{ levelLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="通知渠道" width="160">
          <template #default="{ row }">
            <el-tag v-for="ch in parseChannels(row.channels)" :key="ch" size="small" type="info" style="margin:1px">{{ ch }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="静默" width="100">
          <template #default="{ row }">{{ Math.floor(row.silence_sec / 60) }} 分钟</template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggleRule(row)" size="small" />
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

    <!-- Dialog -->
    <el-dialog v-model="showDialog" :title="form.id ? '编辑规则' : '新增规则'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="规则名称" required>
          <el-input v-model="form.rule_name" placeholder="例：LLM 调用延迟超限" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="指标" required>
              <el-select v-model="form.metric" style="width:100%" filterable allow-create>
                <el-option label="llm_latency_ms" value="llm_latency_ms" />
                <el-option label="llm_token_rate" value="llm_token_rate" />
                <el-option label="llm_error_rate" value="llm_error_rate" />
                <el-option label="es_query_latency" value="es_query_latency" />
                <el-option label="milvus_query_latency" value="milvus_query_latency" />
                <el-option label="api_error_rate" value="api_error_rate" />
                <el-option label="db_connection_pool" value="db_connection_pool" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="运算符">
              <el-select v-model="form.operator" style="width:100%">
                <el-option label="> 大于" value="gt" />
                <el-option label=">= 大于等于" value="gte" />
                <el-option label="< 小于" value="lt" />
                <el-option label="<= 小于等于" value="lte" />
                <el-option label="== 等于" value="eq" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="阈值" required>
              <el-input-number v-model="form.threshold" :min="0" :step="0.1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="持续秒数">
              <el-input-number v-model="form.duration_sec" :min="10" :max="3600" :step="10" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="告警级别">
              <el-select v-model="form.level" style="width:100%">
                <el-option label="P0 紧急" :value="1" />
                <el-option label="P1 严重" :value="2" />
                <el-option label="P2 提示" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="静默(分钟)">
              <el-input-number v-model="silenceMin" :min="1" :max="1440" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="通知渠道">
          <el-checkbox-group v-model="channels">
            <el-checkbox label="feishu">飞书</el-checkbox>
            <el-checkbox label="wechat">微信</el-checkbox>
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="webhook">Webhook</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="接收人">
          <el-input v-model="form.receivers" placeholder="逗号分隔的手机号/飞书ID/邮箱" />
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
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const rows = ref([])
const loading = ref(false)
const filter = reactive({ level: '', status: '' })
const showDialog = ref(false)
const form = reactive({ id: null, rule_name: '', metric: 'llm_latency_ms', operator: 'gt', threshold: 500, duration_sec: 60, level: 2, channels: '["feishu"]', receivers: '', silence_sec: 1800, biz_module: '', status: 1 })
const channels = ref(['feishu'])
const silenceMin = ref(30)

function operLabel(o) { return ({ gt: '>', gte: '>=', lt: '<', lte: '<=', eq: '==' }[o] || o) }
function levelLabel(l) { return ({ 1: 'P0 紧急', 2: 'P1 严重', 3: 'P2 提示' }[l] || l) }
function levelTag(l) { return ({ 1: 'danger', 2: 'warning', 3: 'info' }[l] || '') }
function parseChannels(raw) { try { return JSON.parse(raw) } catch { return [] } }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/monitor/alert-rules', { params: { ...filter } })
    rows.value = res.data?.list || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: null, rule_name: '', metric: 'llm_latency_ms', operator: 'gt', threshold: 500, duration_sec: 60, level: 2, channels: '["feishu"]', receivers: '', silence_sec: 1800, status: 1 })
  channels.value = ['feishu']
  silenceMin.value = 30
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  channels.value = parseChannels(row.channels)
  silenceMin.value = Math.floor((row.silence_sec || 1800) / 60)
  showDialog.value = true
}

async function handleSave() {
  if (!form.rule_name || !form.metric) { ElMessage.warning('规则名称和指标必填'); return }
  form.channels = JSON.stringify(channels.value)
  form.silence_sec = silenceMin.value * 60
  const payload = { ...form }
  delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.post(`/admin/{table}/${form.id}/update`.replace('{table}', 'alert_rule'), payload)
    } else {
      res = await api.post('/admin/{table}/create'.replace('{table}', 'alert_rule'), payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除规则「${row.rule_name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/{table}/${row.id}/delete`.replace('{table}', 'alert_rule'))
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function toggleRule(row) {
  try {
    await api.post(`/admin/{table}/${row.id}/toggle`.replace('{table}', 'alert_rule'), { status: row.status === 1 ? 0 : 1 })
    row.status = row.status === 1 ? 0 : 1
  } catch (e) { ElMessage.error('切换失败') }
}

function reset() { filter.level = ''; filter.status = ''; load() }
onMounted(load)
</script>

<style lang="scss" scoped>
.alert-rules-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.mono { font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 12px; color: #475569; }
</style>