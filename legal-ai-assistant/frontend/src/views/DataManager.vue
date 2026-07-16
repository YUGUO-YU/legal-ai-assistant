<template>
  <div class="data-manager">
    <div class="page-header">
      <div>
        <h2>数据管理中心</h2>
        <p>通过 AI 联网搜索、上传 JSON 或预置种子，导入法律法规到本地数据库</p>
      </div>
      <el-button @click="refreshAll" :loading="loading" type="primary" plain>
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
              <el-icon :size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.lawCount || 0 }}</span>
              <span class="stat-label">法规总数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
              <el-icon :size="24"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.articleCount || 0 }}</span>
              <span class="stat-label">条款总数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
              <el-icon :size="24"><Histogram /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.historyCount || 0 }}</span>
              <span class="stat-label">导入任务数</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="manager-tabs">
      <el-tab-pane label="AI 联网导入" name="web">
        <el-card shadow="never" class="form-card">
          <el-form :model="webForm" label-width="100px" :disabled="webLoading">
            <el-form-item label="法律名称">
              <el-input
                v-model="webForm.lawName"
                placeholder="例如：民法典、刑法、个人信息保护法"
                clearable
                @keyup.enter="submitWeb"
              />
            </el-form-item>
            <el-form-item label="操作者">
              <el-input v-model="webForm.operator" placeholder="system" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="webLoading" @click="submitWeb">
                <el-icon><Promotion /></el-icon>
                启动 AI 联网导入
              </el-button>
              <el-button @click="resetWebForm">重置</el-button>
              <span class="form-tip">AI 将自动联网搜索该法律的完整条文，结构化后写入 MySQL / ES / Milvus</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="上传 JSON" name="upload">
        <el-card shadow="never" class="form-card">
          <el-form :model="uploadForm" label-width="100px" :disabled="uploadLoading">
            <el-form-item label="法律名称">
              <el-input v-model="uploadForm.lawName" placeholder="可留空，使用 JSON 内的 lawTitle" clearable />
            </el-form-item>
            <el-form-item label="操作者">
              <el-input v-model="uploadForm.operator" placeholder="system" />
            </el-form-item>
            <el-form-item label="JSON 文件">
              <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :limit="1"
                accept=".json"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                drag
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽 JSON 文件到此处，或<em>点击选择</em></div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持 law_document/law_article 数据结构的 JSON；文件最大 5MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="uploadLoading" :disabled="!uploadFile" @click="submitUpload">
                <el-icon><Upload /></el-icon>
                提交导入
              </el-button>
              <el-button @click="clearUpload">清空</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="预置种子" name="preset">
        <el-card shadow="never" class="form-card">
          <div v-if="presets.length === 0" class="empty-tip">暂无预置数据</div>
          <div v-else class="preset-grid">
            <div
              v-for="p in presets"
              :key="p"
              class="preset-item"
              :class="{ loading: presetLoading === p }"
              @click="submitPreset(p)"
            >
              <el-icon :size="28" color="#667eea"><Files /></el-icon>
              <div class="preset-name">{{ formatPresetName(p) }}</div>
              <el-button v-if="presetLoading === p" loading size="small" type="primary" plain>导入中</el-button>
              <el-button v-else size="small" type="primary" plain @click.stop="submitPreset(p)">
                导入
              </el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="导入历史" name="history">
        <el-card shadow="never">
          <el-table :data="history" stripe v-loading="historyLoading" empty-text="暂无导入记录">
            <el-table-column prop="id" label="#" width="60" />
            <el-table-column prop="lawName" label="法律名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="source" label="来源" width="120">
              <template #default="{ row }">
                <el-tag :type="sourceTagType(row.source)" size="small">
                  {{ sourceLabel(row.source) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="三库" width="180" align="center">
              <template #default="{ row }">
                <el-tooltip :content="`MySQL: ${row.mysqlOk ? '成功' : '失败'}`">
                  <el-tag size="small" :type="row.mysqlOk ? 'success' : 'info'" effect="plain">M</el-tag>
                </el-tooltip>
                <el-tooltip :content="`ES: ${row.esOk ? '成功' : '失败/未启用'}`">
                  <el-tag size="small" :type="row.esOk ? 'success' : 'info'" effect="plain">E</el-tag>
                </el-tooltip>
                <el-tooltip :content="`Milvus: ${row.milvusOk ? '成功' : '失败/未启用'}`">
                  <el-tag size="small" :type="row.milvusOk ? 'success' : 'info'" effect="plain">V</el-tag>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="totalArticles" label="拉取" width="80" align="right" />
            <el-table-column prop="insertedArticles" label="新增" width="80" align="right" />
            <el-table-column prop="updatedArticles" label="更新" width="80" align="right" />
            <el-table-column prop="errorMessage" label="错误" min-width="200" show-overflow-tooltip />
            <el-table-column prop="startedAt" label="开始时间" width="160">
              <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination
              v-model:current-page="historyPage"
              v-model:page-size="historyPageSize"
              :page-sizes="[10, 20, 50]"
              :total="historyTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadHistory"
              @size-change="(s) => { historyPageSize = s; loadHistory() }"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Collection, Histogram, Refresh, Promotion, Upload, UploadFilled,
  Files
} from '@element-plus/icons-vue'
import api from '@/api'

const activeTab = ref('web')
const loading = ref(false)

const stats = ref({ lawCount: 0, articleCount: 0, historyCount: 0 })

const refreshAll = async () => {
  loading.value = true
  try {
    const s = await api.lawImport.stats()
    stats.value = s.data || s
  } catch (e) {
    ElMessage.error('加载统计失败')
  } finally {
    loading.value = false
  }
}

const webForm = reactive({ lawName: '', operator: 'system' })
const webLoading = ref(false)

const submitWeb = async () => {
  if (!webForm.lawName.trim()) {
    ElMessage.warning('请输入法律名称')
    return
  }
  webLoading.value = true
  try {
    const res = await api.lawImport.webSearch({ lawName: webForm.lawName, operator: webForm.operator })
    const job = res
    showJobResult(job, 'AI 联网导入')
  } catch (e) {
    ElMessage.error('导入失败: ' + (e?.message || '未知错误'))
  } finally {
    webLoading.value = false
    refreshAll()
    loadHistory()
  }
}

const resetWebForm = () => {
  webForm.lawName = ''
  webForm.operator = 'system'
}

const uploadForm = reactive({ lawName: '', operator: 'system' })
const uploadFile = ref(null)
const uploadRef = ref(null)
const uploadLoading = ref(false)

const handleFileChange = (file) => {
  uploadFile.value = file.raw
  if (!uploadForm.lawName) {
    const name = file.name
    uploadForm.lawName = name.replace(/\.json$/i, '')
  }
}

const handleFileRemove = () => {
  uploadFile.value = null
}

const submitUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploadLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    formData.append('lawName', uploadForm.lawName)
    formData.append('operator', uploadForm.operator)
    const res = await api.lawImport.uploadFile(formData)
    const job = res
    showJobResult(job, 'JSON 上传')
  } catch (e) {
    ElMessage.error('上传导入失败: ' + (e?.message || '未知错误'))
  } finally {
    uploadLoading.value = false
    refreshAll()
    loadHistory()
  }
}

const clearUpload = () => {
  uploadFile.value = null
  uploadForm.lawName = ''
  uploadRef.value?.clearFiles()
}

const presets = ref([])
const presetLoading = ref('')

const loadPresets = async () => {
  try {
    const res = await api.lawImport.presets()
    presets.value = res || []
  } catch (e) {
    presets.value = []
  }
}

const submitPreset = async (presetKey) => {
  try {
    await ElMessageBox.confirm(
      `确认导入预置数据：${formatPresetName(presetKey)}？该操作会写入 MySQL/ES/Milvus。`,
      '确认导入',
      { type: 'info' }
    )
  } catch { return }
  presetLoading.value = presetKey
  try {
    const res = await api.lawImport.preset(presetKey, 'system')
    const job = res
    showJobResult(job, '预置种子导入')
  } catch (e) {
    ElMessage.error('预置导入失败: ' + (e?.message || '未知错误'))
  } finally {
    presetLoading.value = ''
    refreshAll()
    loadHistory()
  }
}

const history = ref([])
const historyLoading = ref(false)
const historyPage = ref(1)
const historyPageSize = ref(20)
const historyTotal = ref(0)

const loadHistory = async () => {
  historyLoading.value = true
  try {
    const res = await api.lawImport.history(historyPage.value, historyPageSize.value)
    const data = res
    history.value = data.items || []
    historyTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载历史失败')
  } finally {
    historyLoading.value = false
  }
}

const sourceLabel = (s) => ({ web_search: 'AI 联网', upload: '上传', preset: '预置' }[s] || s)
const sourceTagType = (s) => ({ web_search: 'primary', upload: 'success', preset: 'warning' }[s] || 'info')
const statusLabel = (s) => ({ success: '成功', failed: '失败', running: '运行中' }[s] || s)
const statusTagType = (s) => ({ success: 'success', failed: 'danger', running: 'warning' }[s] || 'info')

const formatTime = (t) => {
  if (!t) return '-'
  try {
    return new Date(t).toLocaleString('zh-CN', { hour12: false })
  } catch { return t }
}

const formatPresetName = (k) => {
  const map = {
    'civil-code': '民法典',
    'criminal-law': '刑法',
    'labor-contract-law': '劳动合同法',
    'company-law': '公司法'
  }
  return map[k] || k
}

const showJobResult = (job, label) => {
  if (!job) {
    ElMessage.warning(`${label}：未返回任务信息`)
    return
  }
  if (job.status === 'success') {
    ElMessageBox.alert(
      `<div style="line-height: 1.8">
        <p><b>法律：</b>${job.lawName || '-'}</p>
        <p><b>拉取条款：</b>${job.totalArticles || 0} 条</p>
        <p><b>新增：</b>${job.insertedArticles || 0} / <b>更新：</b>${job.updatedArticles || 0}</p>
        <p><b>MySQL：</b>${job.mysqlOk ? '✓' : '✗'}　<b>ES：</b>${job.esOk ? '✓' : '✗'}　<b>Milvus：</b>${job.milvusOk ? '✓' : '✗'}</p>
        ${job.errorMessage ? `<p style="color: #c00">错误：${job.errorMessage}</p>` : ''}
      </div>`,
      `${label} 完成`,
      { dangerouslyUseHTMLString: true, confirmButtonText: '好的' }
    ).catch(() => {})
  } else {
    ElMessageBox.alert(
      `任务失败：${job.errorMessage || '未知错误'}`,
      `${label} 失败`,
      { type: 'error', confirmButtonText: '好的' }
    ).catch(() => {})
  }
}

onMounted(() => {
  refreshAll()
  loadPresets()
  loadHistory()
})
</script>

<style lang="scss" scoped>
.data-manager {
  padding: 24px;
  animation: fadeIn 0.4s ease;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;

  h2 {
    margin: 0 0 4px;
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
  p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 14px;
  }
}

.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 16px;
  border: none;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .stat-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 2px;
  }
}

.manager-tabs {
  background: #fff;
  border-radius: 16px;
  padding: 8px 24px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.form-card {
  border: none;
  margin-top: 16px;
  border-radius: 12px;

  :deep(.el-card__body) {
    padding: 24px 24px 8px;
  }

  .form-tip {
    margin-left: 16px;
    font-size: 12px;
    color: var(--color-text-secondary);
  }
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  padding: 8px 0 16px;

  .preset-item {
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 20px 16px;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #667eea;
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
      transform: translateY(-2px);
    }

    &.loading {
      border-color: #667eea;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
    }

    .preset-name {
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-primary);
      margin: 8px 0 12px;
    }
  }
}

.empty-tip {
  text-align: center;
  color: var(--color-text-secondary);
  padding: 40px 0;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
