<template>
  <div class="templates-page">
    <div class="page-header">
      <div class="header-content">
        <h2>文书模板 · 生成器</h2>
        <p>MOD-03 · 模板变量 / 内容预览 / 分类维护</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建模板</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="分类">
          <el-select v-model="filter.category" clearable placeholder="全部" style="width:160px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
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
        <el-table-column prop="template_name" label="模板名称" min-width="200" />
        <el-table-column prop="template_code" label="代码" width="130" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变量" width="80">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="plain">{{ varCount(row.schema_json) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="70">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggleTpl(row)" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" size="small" @click="previewTemplate(row)">预览</el-button>
            <el-button link type="warning" size="small" @click="copyTemplate(row)">复制</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="showDialog" :title="form.id ? '编辑模板' : '新建模板'" width="800px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模板代码" required>
              <el-input v-model="form.template_code" :disabled="!!form.id" placeholder="例：COMPLAINT_V1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板名称" required>
              <el-input v-model="form.template_name" placeholder="例：民事起诉状模板" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width:100%" filterable allow-create>
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本">
              <el-input v-model="form.version" placeholder="v1.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="变量定义">
          <div class="var-editor">
            <div class="var-tags">
              <el-tag
                v-for="(v, i) in varList"
                :key="i"
                closable
                size="small"
                style="margin:2px"
                @close="removeVar(i)"
              >{{ v }}</el-tag>
              <el-input
                v-if="showVarInput"
                ref="varInputRef"
                v-model="varInput"
                size="small"
                style="width:140px"
                placeholder="变量名"
                @keyup.enter="addVar"
                @blur="addVar"
              />
              <el-button v-else size="small" @click="startAddVar">+ 添加变量</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="模板内容" required>
          <el-input
            v-model="templateContent"
            type="textarea"
            :rows="16"
            placeholder="使用 {变量名} 作为占位符，例：&#10;原告 {plaintiff_name}，性别 {plaintiff_gender}，&#10;身份证号 {plaintiff_id_number}..."
          />
        </el-form-item>
        <el-form-item>
          <div class="var-helper">
            <span class="helper-title">变量使用提示：</span>
            <span>在模板内容中使用 <code>{变量名}</code> 格式引入变量。保存时自动提取。</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存模板</el-button>
      </template>
    </el-dialog>

    <!-- Preview Dialog -->
    <el-dialog v-model="showPreview" title="模板预览" width="700px">
      <div class="preview-meta">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="模板">{{ previewTpl?.template_name }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ previewTpl?.category }}</el-descriptions-item>
          <el-descriptions-item label="代码">{{ previewTpl?.template_code }}</el-descriptions-item>
          <el-descriptions-item label="变量">{{ varCount(previewTpl?.schema_json) }} 个</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="preview-vars" v-if="previewVarList.length">
        <el-form label-width="100px" size="small">
          <el-form-item v-for="v in previewVarList" :key="v" :label="v">
            <el-input :placeholder="`请输入 ${v}`" />
          </el-form-item>
        </el-form>
      </div>
      <h4>模板正文</h4>
      <pre class="content-preview">{{ previewContent }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const filter = reactive({ category: '' })
const showDialog = ref(false)
const showPreview = ref(false)
const showVarInput = ref(false)
const varInput = ref('')
const varInputRef = ref(null)
const previewTpl = ref(null)
const form = reactive({ id: null, template_code: '', template_name: '', category: '民事诉讼', schema_json: '{}', risk_rules: '[]', review_required: 1, status: 1, version: 'v1.0' })

const currentSchema = computed(() => {
  try { return JSON.parse(form.schema_json || '{}') } catch { return {} }
})

const varList = computed({
  get: () => currentSchema.value.variables || [],
  set: (v) => {
    const s = { ...currentSchema.value, variables: v }
    if (!s.template_content) s.template_content = ''
    form.schema_json = JSON.stringify(s)
  }
})

const templateContent = computed({
  get: () => currentSchema.value.template_content || '',
  set: (v) => {
    const s = { ...currentSchema.value, template_content: v }
    if (!s.variables) s.variables = []
    form.schema_json = JSON.stringify(s)
  }
})

const previewVarList = computed(() => {
  try { const s = JSON.parse(previewTpl.value?.schema_json || '{}'); return s.variables || [] } catch { return [] }
})

const previewContent = computed(() => {
  try { const s = JSON.parse(previewTpl.value?.schema_json || '{}'); return s.template_content || '' } catch { return '' }
})

const categories = ['民事诉讼', '刑事诉讼', '行政诉讼', '仲裁', '劳动仲裁', '公司治理', '知识产权']

function varCount(raw) {
  try { const s = JSON.parse(raw || '{}'); return (s.variables || []).length }
  catch { return 0 }
}

function extractVars() {
  const matches = (templateContent.value || '').match(/\{(\w+)\}/g) || []
  const vars = matches.map(m => m.replace(/[{}]/g, ''))
  varList.value = [...new Set(vars)].sort()
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/doc_template/list', { params: { category: filter.category || undefined } })
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, template_code: '', template_name: '', category: '民事诉讼', schema_json: '{"template_content":"","variables":[]}', risk_rules: '[]', review_required: 1, status: 1, version: 'v1.0' })
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  showDialog.value = true
}

async function handleSave() {
  if (!form.template_code || !form.template_name || !templateContent.value) {
    ElMessage.warning('模板代码/名称/内容必填'); return
  }
  extractVars()
  const payload = { ...form }
  delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.post(`/admin/doc_template/${form.id}/update`, payload)
    } else {
      res = await api.post('/admin/doc_template/create', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除模板「${row.template_name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/doc_template/${row.id}/delete`)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function toggleTpl(row) {
  try {
    await api.post(`/admin/doc_template/${row.id}/toggle`, { status: row.status === 1 ? 0 : 1 })
    row.status = row.status === 1 ? 0 : 1
  } catch (e) { ElMessage.error('切换失败') }
}

async function copyTemplate(row) {
  try {
    const newCode = row.template_code + '_copy_' + Date.now()
    const payload = {
      template_code: newCode,
      template_name: row.template_name + ' (副本)',
      category: row.category,
      schema_json: row.schema_json,
      risk_rules: row.risk_rules,
      review_required: row.review_required,
      status: 0,
      version: row.version + '.copy'
    }
    const res = await api.post('/admin/doc_template/create', payload)
    if (res.data?.ok) { ElMessage.success('已复制为新模板'); load() }
    else ElMessage.error(res.data?.error || '复制失败')
  } catch (e) { ElMessage.error('复制失败') }
}

function previewTemplate(row) {
  previewTpl.value = row
  showPreview.value = true
}

onMounted(load)
</script>

<style lang="scss" scoped>
.templates-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.var-editor { width:100%; .var-tags { display:flex; flex-wrap:wrap; gap:4px; align-items:center; } }
.var-helper { font-size:12px; color: var(--color-text-muted); .helper-title { font-weight:600; color:var(--color-text-secondary); } code { background:var(--color-bg-page); padding:1px 5px; border-radius:4px; font-size:12px; } }
.preview-meta { margin-bottom:16px; }
.content-preview {
  background: var(--color-bg-page);
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap; word-break: break-word;
  font-family: 'Cascadia Code', 'Consolas', monospace;
  font-size: 13px; line-height: 1.7;
  border: 1px solid var(--color-border);
  max-height: 40vh; overflow-y: auto;
}
</style>