<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">Prompt 模板管理</h2>
        <p>AI 域核心资产 · 创建 / 灰度 / 回滚 / A/B</p>
      </div>
      <div class="header-actions">
        <el-tag type="warning" size="small">AI 能力域</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建版本</el-button>
      </div>
    </div>

    <el-card class="glass filter-card" style="margin-bottom: 14px;">
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

    <el-card class="glass table-card">
      <el-table :data="displayRows" v-loading="loading" stripe border @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="70" sortable="custom" />
        <el-table-column prop="prompt_code" label="代码" width="170" sortable="custom" />
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
        <el-table-column prop="created_at" label="创建时间" width="170" sortable="custom" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看</el-button>
            <el-button link type="success" size="small" :disabled="row.is_active === 1 && row.is_gray !== 1" @click="handlePublish(row)">发布</el-button>
            <el-button link type="warning" size="small" @click="openGray(row)">灰度</el-button>
            <el-button link type="danger" size="small" @click="handleRollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="filter.page"
          v-model:page-size="filter.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :background="true"
          style="margin-top:16px;justify-content:center;"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <!-- 创建/编辑 -->
    <el-dialog v-model="showCreate" title="新建 Prompt 版本" width="780px">
      <el-steps :active="createStep" finish-status="success" style="margin-bottom:24px">
        <el-step title="基本信息" description="代码 / 模块 / 场景 / 版本" />
        <el-step title="内容配置" description="Prompt 正文 / 变量定义" />
        <el-step title="确认保存" description="检查并提交" />
      </el-steps>
      <el-form :model="form" label-width="100px">
        <template v-if="createStep === 0">
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
        </template>
        <template v-else-if="createStep === 1">
          <el-form-item label="内容" required>
            <el-input v-model="form.content" type="textarea" :rows="10" placeholder="Prompt 正文，支持 {variable} 占位符" />
          </el-form-item>
          <el-form-item label="变量">
            <el-input v-model="form.variables" type="textarea" :rows="2" placeholder='JSON 数组，例：["query","context"]' />
          </el-form-item>
        </template>
        <template v-else>
          <el-descriptions :column="1" border style="margin-bottom:12px">
            <el-descriptions-item label="代码">{{ form.prompt_code }}</el-descriptions-item>
            <el-descriptions-item label="模块">{{ form.module }}</el-descriptions-item>
            <el-descriptions-item label="场景">{{ form.scene }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ form.version }}</el-descriptions-item>
            <el-descriptions-item label="变量">{{ form.variables || '无' }}</el-descriptions-item>
          </el-descriptions>
          <div class="glass" style="padding:12px;border-radius:8px;max-height:200px;overflow:auto">
            <pre style="margin:0;font-size:12px;color:var(--color-text-secondary)">{{ form.content }}</pre>
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button v-if="createStep > 0" @click="createStep--">上一步</el-button>
        <el-button v-if="createStep < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-else type="primary" :loading="createLoading" @click="handleCreate">保存</el-button>
        <el-button @click="showCreate = false">取消</el-button>
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
          <el-descriptions-item label="采纳率" v-if="detail.adopt_rate != null">{{ (Number(detail.adopt_rate) * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="评分" v-if="detail.feedback_score != null">{{ detail.feedback_score }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin-top:20px">内容</h4>
        <pre class="prompt-content">{{ detail.content }}</pre>
        <div v-if="detail.variables" style="margin-top:16px">
          <h4>变量定义</h4>
          <div class="var-tags">
            <el-tag v-for="v in parseVars(detail.variables)" :key="v" size="small" style="margin:2px">{{ v }}</el-tag>
          </div>
        </div>
        <div v-if="relatedVersions.length" style="margin-top:20px">
          <h4>同代码其他版本</h4>
          <el-table :data="relatedVersions" size="small" stripe>
            <el-table-column prop="version" label="版本" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.is_active === 1 && row.is_gray !== 1" type="success" size="small">已激活</el-tag>
                <el-tag v-else-if="row.is_gray === 1" type="warning" size="small">灰度中</el-tag>
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
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="compareVersion(row)">对比</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>

    <!-- 版本对比 -->
    <el-dialog v-model="showCompare" title="版本对比" width="900px">
      <div v-if="compareData" class="compare-container">
        <div class="compare-header">
          <el-tag type="info">{{ compareData.old?.version || '旧版本' }}</el-tag>
          <span style="margin:0 12px;color: var(--color-text-muted)">VS</span>
          <el-tag type="success">{{ compareData.new?.version || '新版本' }}</el-tag>
        </div>
        <el-tabs>
          <el-tab-pane label="内容对比">
            <div class="diff-content">
              <div class="diff-old"><pre>{{ compareData.old?.content || '(无)' }}</pre></div>
              <div class="diff-new"><pre>{{ compareData.new?.content || '(无)' }}</pre></div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="变量对比">
            <div class="diff-content">
              <div class="diff-old">
                <div class="var-tags">
                  <el-tag v-for="v in parseVars(compareData.old?.variables)" :key="v" size="small" style="margin:2px">{{ v }}</el-tag>
                  <span v-if="!parseVars(compareData.old?.variables).length" style="color:var(--color-text-muted)">无变量</span>

                  <span v-if="!parseVars(compareData.new?.variables).length" style="color:var(--color-text-muted)">无变量</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const modules = ['MOD-01', 'MOD-02', 'MOD-03', 'MOD-04', 'MOD-05', 'MOD-06', 'MOD-07', 'MOD-08', 'MOD-09', 'MOD-10']
const rows = ref([])
const loading = ref(false)
const total = ref(0)
const filter = reactive({ page: 1, pageSize: 50, keyword: '', module: '' })
const showCreate = ref(false)
const showGray = ref(false)
const showDetail = ref(false)
const showCompare = ref(false)
const detail = ref(null)
const relatedVersions = ref([])
const compareData = ref(null)
const form = reactive({ prompt_code: '', module: 'MOD-01', scene: '', version: 'v1.0', content: '', variables: '' })
const grayForm = reactive({ id: null, code: '', version: '', ratio: 10, teams: '' })
const createStep = ref(0)
const createLoading = ref(false)
const sortState = ref({ prop: '', order: '' })

const displayRows = computed(() => {
  if (!sortState.value.prop || !sortState.value.order) return rows.value
  return [...rows.value].sort((a, b) => {
    const valA = a[sortState.value.prop] ?? ''
    const valB = b[sortState.value.prop] ?? ''
    const cmp = String(valA).localeCompare(String(valB), 'zh-CN', { numeric: true })
    return sortState.value.order === 'ascending' ? cmp : -cmp
  })
})

const handleSortChange = ({ prop, order }) => {
  sortState.value = { prop, order }
}

function parseVars(v) {
  if (!v) return []
  if (Array.isArray(v)) return v
  try { return JSON.parse(v) } catch { return [] }
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/ai/prompts', { params: filter })
    rows.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

function reset() { filter.keyword = ''; filter.module = ''; load() }

function openCreate() {
  Object.assign(form, { prompt_code: '', module: 'MOD-01', scene: '', version: 'v1.0', content: '', variables: '' })
  createStep.value = 0
  showCreate.value = true
}

function nextStep() {
  if (createStep.value === 0 && (!form.prompt_code || !form.version)) {
    ElMessage.warning('代码和版本为必填项')
    return
  }
  if (createStep.value === 1 && !form.content) {
    ElMessage.warning('Prompt 内容为必填项')
    return
  }
  createStep.value++
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
  createLoading.value = true
  try {
    const res = await api.post('/admin/prompt_template/create', payload)
    if (res?.ok) {
      ElMessage.success('已创建')
      showCreate.value = false
      createStep.value = 0
      load()
    } else {
      ElMessage.error(res?.error || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败：' + (e.message || ''))
  } finally {
    createLoading.value = false
  }
}

async function openDetail(row) {
  try {
    const res = await api.get(`/admin/prompt_template/${row.id}`)
    detail.value = res?.data || row
    showDetail.value = true
    loadRelatedVersions(row.prompt_code, row.id)
  } catch (e) {
    detail.value = row
    showDetail.value = true
    relatedVersions.value = []
  }
}

async function loadRelatedVersions(code, excludeId) {
  try {
    const allRes = await api.get('/admin/prompt_template/list')
    const all = allRes?.list || []
    relatedVersions.value = all.filter(r => r.prompt_code === code && r.id !== excludeId)
  } catch (e) {
    relatedVersions.value = []
  }
}

function compareVersion(otherRow) {
  compareData.value = { old: otherRow, new: detail.value }
  showCompare.value = true
}

async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`发布 ${row.prompt_code} ${row.version}？同代码其他版本将自动下线`, '确认', { type: 'success' })
    const res = await api.post(`/admin/ai/prompts/${row.id}/publish`)
    if (res?.ok) {
      ElMessage.success('已发布')
      load()
    } else {
      ElMessage.error(res?.error || '发布失败')
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
    await ElMessageBox.confirm(`确定要对「${grayForm.code} ${grayForm.version}」进行灰度发布？灰度比例：${grayForm.ratio}%。${grayForm.teams ? '（指定团队：' + grayForm.teams + '）' : ''}`, '确认灰度发布', { type: 'warning' })
    const res = await api.post(`/admin/ai/prompts/${grayForm.id}/gray`, null, { params: { ratio: grayForm.ratio, teams: grayForm.teams || '' } })
    if (res?.ok) {
      ElMessage.success(`已发布 ${grayForm.ratio}% 灰度`)
      showGray.value = false
      load()
    } else {
      ElMessage.error(res?.error || '灰度失败')
    }
  } catch (e) { if (e !== 'cancel') ElMessage.error('灰度失败：' + (e.message || '')) }
}

async function handleRollback(row) {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入回滚原因', '回滚', { inputPlaceholder: '例：采纳率下降 5%' })
    const res = await api.post(`/admin/ai/prompts/${row.id}/rollback`, null, { params: { reason } })
    if (res?.ok) {
      ElMessage.success(`已回滚 ${res?.rolledBackVersion || row.version}`)
      load()
    } else {
      ElMessage.error(res?.error || '回滚失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('回滚失败：' + (e.message || ''))
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.prompts-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.prompt-content {
  background: var(--color-bg-page);
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  border: 1px solid var(--color-border);
}
.var-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.compare-container { padding: 0 8px; }
.compare-header { display: flex; align-items: center; margin-bottom: 16px; justify-content: center; }
.diff-content { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.diff-old pre, .diff-new pre {
  background: var(--color-diff-old-bg); padding: 12px; border-radius: 6px; white-space: pre-wrap;
  font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 12px; line-height: 1.6;
  border: 1px solid var(--color-diff-old-border); height: 300px; overflow-y: auto;
}
.diff-new pre { background: var(--color-diff-new-bg); border: 1px solid var(--color-diff-new-border); }
</style>