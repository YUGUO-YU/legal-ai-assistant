<template>
  <div class="prompts-page">
    <div class="page-header">
      <div class="header-content">
        <h2>Prompt 模板管理</h2>
        <p>AI 域核心资产 · 创建 / 灰度 / 回滚 / A/B</p>
      </div>
      <div class="header-actions">
        <el-tag type="warning" size="small">AI 能力域</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建版本</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="prompt_code / scene" clearable style="width:200px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="filter.module" clearable placeholder="全部" style="width:140px">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
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
        <el-table-column prop="prompt_code" label="代码" width="170" />
        <el-table-column prop="module" label="模块" width="90" />
        <el-table-column prop="scene" label="场景" width="110" />
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="状态" width="220">
          <template #default="{ row }">
            <el-tag v-if="row.is_active === 1 && row.is_gray !== 1" type="success" size="small">已激活</el-tag>
            <el-tag v-else-if="row.is_gray === 1" type="warning" size="small">灰度中 {{ row.gray_ratio }}%</el-tag>
            <el-tag v-else type="info" size="small">未激活</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adopt_rate" label="采纳率" width="100">
          <template #default="{ row }">
            <span v-if="row.adopt_rate != null">{{ (Number(row.adopt_rate) * 100).toFixed(1) }}%</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="feedback_score" label="评分" width="80" />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button link type="success" size="small" :disabled="row.is_active === 1 && row.is_gray !== 1" @click="handlePublish(row)">发布</el-button>
            <el-button link type="warning" size="small" @click="openGray(row)">灰度</el-button>
            <el-button link type="danger" size="small" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑 -->
    <el-dialog v-model="showCreate" title="新建 Prompt 版本" width="780px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="代码" required>
          <el-input v-model="form.prompt_code" placeholder="例：MOD-01.q&a" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模块" required>
              <el-select v-model="form.module" style="width:100%">
                <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场景" required>
              <el-input v-model="form.scene" placeholder="例：search/case/draft" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="版本" required>
          <el-input v-model="form.version" placeholder="例：v1.0 / v1.1" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="Prompt 正文，支持 {variable} 占位符" />
        </el-form-item>
        <el-form-item label="变量">
          <el-input v-model="form.variables" type="textarea" :rows="2" placeholder='JSON 数组，例：["query","context"]' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 灰度 -->
    <el-dialog v-model="showGray" title="灰度发布" width="460px">
      <el-form label-width="100px">
        <el-form-item label="Prompt">
          <span>{{ grayForm.code }} {{ grayForm.version }}</span>
        </el-form-item>
        <el-form-item label="灰度比例">
          <el-slider v-model="grayForm.ratio" :min="0" :max="100" :step="5" show-stops />
        </el-form-item>
        <el-form-item label="灰度团队">
          <el-input v-model="grayForm.teams" placeholder="逗号分隔的 team_id，留空走比例" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGray = false">取消</el-button>
        <el-button type="primary" @click="handleGraySubmit">发布灰度</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="showDetail" title="Prompt 详情" size="55%" direction="rtl">
      <div v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="代码">{{ detail.prompt_code }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.module }}</el-descriptions-item>
          <el-descriptions-item label="场景">{{ detail.scene }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="detail.is_active === 1 && detail.is_gray !== 1" type="success" size="small">已激活</el-tag>
            <el-tag v-else-if="detail.is_gray === 1" type="warning" size="small">灰度中</el-tag>
            <el-tag v-else type="info" size="small">未激活</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:20px">内容</h4>
        <pre class="prompt-content">{{ detail.content }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const modules = ['MOD-01', 'MOD-02', 'MOD-03', 'MOD-04', 'MOD-05', 'MOD-06', 'MOD-07', 'MOD-08', 'MOD-09', 'MOD-10']
const rows = ref([])
const loading = ref(false)
const filter = reactive({ page: 1, pageSize: 50, keyword: '', module: '' })
const showCreate = ref(false)
const showGray = ref(false)
const showDetail = ref(false)
const detail = ref(null)
const form = reactive({ prompt_code: '', module: 'MOD-01', scene: '', version: 'v1.0', content: '', variables: '' })
const grayForm = reactive({ id: null, code: '', version: '', ratio: 10, teams: '' })

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ai/prompts', { params: filter })
    rows.value = res.data?.list || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function reset() { filter.keyword = ''; filter.module = ''; load() }

function openCreate() {
  Object.assign(form, { prompt_code: '', module: 'MOD-01', scene: '', version: 'v1.0', content: '', variables: '' })
  showCreate.value = true
}

async function handleCreate() {
  if (!form.prompt_code || !form.content || !form.version) {
    ElMessage.warning('代码 / 版本 / 内容 必填')
    return
  }
  const payload = { ...form }
  if (payload.variables) {
    try { payload.variables = JSON.stringify(payload.variables.split(',').map(s => s.trim()).filter(Boolean)) }
    catch (e) { /* keep as string */ }
  }
  try {
    const res = await api.post('/admin/{table}/create'.replace('{table}', 'prompt_template'), payload)
    if (res.data?.ok) {
      ElMessage.success('已创建')
      showCreate.value = false
      load()
    } else {
      ElMessage.error(res.data?.error || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || ''))
  }
}

async function openDetail(row) {
  try {
    const res = await api.get(`/admin/{table}/${row.id}`.replace('{table}', 'prompt_template'))
    detail.value = res.data?.data || row
    showDetail.value = true
  } catch (e) {
    detail.value = row
    showDetail.value = true
  }
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`发布 ${row.prompt_code} ${row.version}？同代码其他版本将自动下线`, '确认', { type: 'success' })
    const res = await api.post(`/admin/ai/prompts/${row.id}/publish`)
    if (res.data?.ok) {
      ElMessage.success('已发布')
      load()
    } else {
      ElMessage.error(res.data?.error || '发布失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('发布失败：' + (e.message || ''))
  }
}

function openGray(row) {
  Object.assign(grayForm, { id: row.id, code: row.prompt_code, version: row.version, ratio: row.gray_ratio || 10, teams: '' })
  showGray.value = true
}

async function handleGraySubmit() {
  try {
    const res = await api.post(`/admin/ai/prompts/${grayForm.id}/gray`, null, { params: { ratio: grayForm.ratio, teams: grayForm.teams || '' } })
    if (res.data?.ok) {
      ElMessage.success(`已发布 ${grayForm.ratio}% 灰度`)
      showGray.value = false
      load()
    } else {
      ElMessage.error(res.data?.error || '灰度失败')
    }
  } catch (e) {
    ElMessage.error('灰度失败：' + (e.message || ''))
  }
}

async function handleRollback(row) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入回滚原因', '回滚', { inputPlaceholder: '例：采纳率下降 5%' })
    const res = await api.post(`/admin/ai/prompts/${row.id}/rollback`, null, { params: { reason } })
    if (res.data?.ok) {
      ElMessage.success(`已回滚 ${res.data?.rolledBackVersion || row.version}`)
      load()
    } else {
      ElMessage.error(res.data?.error || '回滚失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('回滚失败：' + (e.message || ''))
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.prompts-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.prompt-content {
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid #e2e8f0;
}
</style>