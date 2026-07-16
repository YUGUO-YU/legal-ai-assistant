<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">AI 法规导入</h2>
        <p>业务域 · PDF/Word 法规文件智能解析与导入</p>
      </div>
    </div>
    <el-card class="glass table-card">
      <template #header>
        <span>AI 法规导入</span>
      </template>
      <div class="import-layout">
        <div class="left-upload">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx"
            :on-change="handleFileChange"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽法规文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .pdf, .doc, .docx 格式</div>
            </template>
          </el-upload>
          <div class="file-type-tabs">
            <el-radio-group v-model="selectedFileType" size="small">
              <el-radio-button value="pdf">PDF 格式</el-radio-button>
              <el-radio-button value="doc">Word 格式</el-radio-button>
            </el-radio-group>
          </div>
          <el-button type="primary" :disabled="!uploadFile" style="margin-top: 16px; width: 100%;" @click="handlePreview">
            <el-icon v-if="uploading"><Loading class="is-loading" /></el-icon>
            {{ uploading ? '解析中...' : '上传并预览' }}
          </el-button>
          <div class="upload-hint">
            <el-icon><InfoFilled /></el-icon>
            <span>PDF文件将自动提取文本并智能分析章节结构</span>
          </div>
        </div>
        <div class="right-preview">
          <el-card v-if="previewData" shadow="never">
            <template #header>
              <div class="preview-header">
                <span>法规信息</span>
                <el-space>
                  <el-tag type="info" v-if="articleStats.total > 0">
                    {{ articleStats.total }} 个条款
                  </el-tag>
                  <el-button v-if="previewData" type="success" size="small" @click="handleConfirm">确认导入</el-button>
                </el-space>
              </div>
            </template>
            <el-form :model="previewForm" label-width="110px">
              <el-form-item label="法规标题">
                <el-input v-model="previewForm.lawTitle" />
              </el-form-item>
              <el-form-item label="文号">
                <el-input v-model="previewForm.documentNo" />
              </el-form-item>
              <el-form-item label="发布机关">
                <el-input v-model="previewForm.issuingAuthority" />
              </el-form-item>
              <el-form-item label="发布日期">
                <el-date-picker v-model="previewForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
              </el-form-item>
              <el-form-item label="分类">
                <el-select v-model="selectedCategoryIds" multiple placeholder="请选择分类" style="width: 100%;">
                  <el-option v-for="c in categoryList" :key="c.id" :label="c.categoryName" :value="c.id" />
                </el-select>
              </el-form-item>
            </el-form>

            <el-divider v-if="previewData?.articles?.length" content-position="left">
              条款预览
              <el-button text size="small" @click="showAllArticles = !showAllArticles">
                {{ showAllArticles ? '收起' : '展开全部' }}
              </el-button>
            </el-divider>

            <div v-if="previewData?.articles?.length" class="articles-list">
              <div v-for="(article, index) in displayArticles" :key="index" class="article-item">
                <div class="article-header" @click="toggleArticle(index)">
                  <el-icon class="expand-icon" :class="{ expanded: expandedArticles.has(index) }">
                    <ArrowRight />
                  </el-icon>
                  <span class="article-no">{{ article.articleNo || `第${index + 1}条` }}</span>
                  <span class="article-title" :title="article.title">{{ article.title || '无标题' }}</span>
                  <el-tag size="small" type="info" style="margin-left: 8px;">
                    {{ (article.content || '').length }} 字
                  </el-tag>
                </div>
                <div v-if="expandedArticles.has(index)" class="article-content">
                  <el-input
                    v-model="article.content"
                    type="textarea"
                    :rows="4"
                    placeholder="条款内容"
                    @change="handleArticleChange(index)"
                  />
                </div>
              </div>
            </div>
            <el-empty v-else-if="previewData && !previewData.articles?.length" description="暂无法规条款数据" />
          </el-card>
          <el-empty v-else description="上传文件后可预览" />
        </div>
      </div>

      <el-card v-if="showPdfPreview && pdfPreviewUrl" style="margin-top: 16px;">
        <template #header>
          <div class="preview-header">
            <span>PDF 预览</span>
            <el-button text @click="showPdfPreview = false; pdfPreviewUrl = null">关闭预览</el-button>
          </div>
        </template>
        <div class="pdf-preview-container">
          <vue-pdf-embed :url="pdfPreviewUrl" />
        </div>
      </el-card>
    </el-card>

    <el-card style="margin-top: 16px;">
      <template #header>
        <span>导入历史</span>
      </template>
      <el-table :data="historyData" stripe>
        <el-table-column prop="lawName" label="法规标题" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <template v-if="row.status === 'running' || row.status === 'pending'">
              <el-tag type="warning" size="small">
                <el-icon class="is-loading" v-if="row.status === 'running'"><Loading /></el-icon>
                进行中 {{ row.progress || 0 }}%
              </el-tag>
            </template>
            <template v-else-if="row.status === 'success'">
              <el-tag type="success" size="small">完成</el-tag>
            </template>
            <template v-else-if="row.status === 'failed'">
              <el-tag type="danger" size="small">失败</el-tag>
            </template>
            <template v-else>
              <el-tag type="info" size="small">{{ row.status || '未知' }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="结果" min-width="160">
          <template #default="{ row }">
            <span v-if="row.successCount" style="color: var(--color-success);">成功: {{ row.successCount }}</span>
            <span v-if="row.failCount" style="color: var(--color-danger); margin-left: 8px;">失败: {{ row.failCount }}</span>
            <span v-if="!row.successCount && !row.failCount && row.totalArticles" style="color: var(--color-text-muted);">共 {{ row.totalArticles }} 条</span>
          </template>
        </el-table-column>
        <el-table-column prop="startedAt" label="开始时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading, InfoFilled, ArrowRight } from '@element-plus/icons-vue'
import VuePdfEmbed from 'vue-pdf-embed'
import api from '@/api'

const uploadRef = ref(null)
const uploadFile = ref(null)
const previewData = ref(null)
const previewForm = ref({})
const selectedCategoryIds = ref([])
const categoryList = ref([])
const historyData = ref([])
const pollTimers = ref({})
const uploading = ref(false)
const selectedFileType = ref('pdf')
const pdfPreviewUrl = ref(null)
const showPdfPreview = ref(false)
const expandedArticles = ref(new Set())
const showAllArticles = ref(false)

const articleStats = computed(() => {
  if (!previewData.value?.articles?.length) {
    return { total: 0 }
  }
  const articles = previewData.value.articles
  return {
    total: articles.length
  }
})

const displayArticles = computed(() => {
  if (!previewData.value?.articles) return []
  if (showAllArticles.value) {
    return previewData.value.articles
  }
  return previewData.value.articles.slice(0, 10)
})

function toggleArticle(index) {
  if (expandedArticles.value.has(index)) {
    expandedArticles.value.delete(index)
  } else {
    expandedArticles.value.add(index)
  }
}

function handleArticleChange(index) {
  // Article content updated
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
  if (file.raw) {
    const isPdf = file.raw.name?.toLowerCase().endsWith('.pdf')
    if (isPdf) {
      pdfPreviewUrl.value = URL.createObjectURL(file.raw)
      showPdfPreview.value = true
    } else {
      showPdfPreview.value = false
      pdfPreviewUrl.value = null
    }
  }
}

const loadCategories = async () => {
  try {
    const typesRes = await api.categoryTypes()
    const types = typesRes.data || []
    const allCats = []
    for (const t of types) {
      const catsRes = await api.categories(t.id)
      allCats.push(...(catsRes.data || []))
    }
    categoryList.value = allCats
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const handlePreview = async () => {
  if (!uploadFile.value) return
  uploading.value = true
  const formData = new FormData()
  formData.append('file', uploadFile.value)
  try {
    const res = await api.importPreview(formData)
    previewData.value = res || {}
    previewForm.value = {
      lawTitle: res?.lawTitle || res?.title || '',
      shortTitle: res?.shortTitle || '',
      documentNo: res?.documentNo || '',
      issuingAuthority: res?.issuingAuthority || '',
      issueDate: res?.issueDate || '',
      effectiveDate: res?.effectiveDate || ''
    }
    expandedArticles.value.clear()
    showAllArticles.value = false
    ElMessage.success('预览生成成功')
  } catch (e) {
    ElMessage.error('预览失败: ' + (e.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

const handleConfirm = async () => {
  try {
    const res = await api.importConfirm({
      ...previewForm.value,
      categoryIds: selectedCategoryIds.value,
      chapters: previewData.value?.chapters || [],
      articles: previewData.value?.articles || []
    })
    const jobId = res?.jobId
    ElMessage.success('导入任务已提交')
    previewData.value = null
    uploadFile.value = null
    uploadRef.value?.clearFiles()

    if (jobId) {
      const newItem = {
        id: jobId,
        lawName: previewForm.value.lawTitle,
        status: 'running',
        progress: 0,
        successCount: 0,
        failCount: 0,
        startedAt: new Date().toLocaleString()
      }
      historyData.value.unshift(newItem)
      pollJobStatus(jobId)
    } else {
      loadHistory()
    }
  } catch (e) {
    ElMessage.error('导入失败')
  }
}

const pollJobStatus = (jobId) => {
  if (pollTimers.value[jobId]) {
    clearInterval(pollTimers.value[jobId])
  }
  pollTimers.value[jobId] = setInterval(async () => {
    try {
      const res = await api.lawImport.historyById(jobId)
      const job = res
      if (job) {
        const index = historyData.value.findIndex(h => h.id === jobId)
        if (index !== -1) {
          historyData.value[index] = {
            ...historyData.value[index],
            status: job.status,
            progress: job.progress,
            successCount: job.successCount,
            failCount: job.failCount,
            errorMessage: job.errorMessage,
            finishedAt: job.finishedAt
          }
        }
        if (job.status === 'success' || job.status === 'failed') {
          clearInterval(pollTimers.value[jobId])
          delete pollTimers.value[jobId]
        }
      }
    } catch (e) {
      clearInterval(pollTimers.value[jobId])
      delete pollTimers.value[jobId]
    }
  }, 3000)
}

const loadHistory = async () => {
  try {
    const res = await api.lawImport.history(1, 20)
    historyData.value = res || []
  } catch (e) {
    ElMessage.error('加载历史失败')
  }
}

onMounted(() => {
  loadCategories()
  loadHistory()
})

onUnmounted(() => {
  Object.values(pollTimers.value).forEach(timer => clearInterval(timer))
  pollTimers.value = {}
  if (pdfPreviewUrl.value) {
    URL.revokeObjectURL(pdfPreviewUrl.value)
  }
})
</script>

<style scoped>
.page-container { padding: 20px; }
.import-layout { display: flex; gap: 20px; }
.left-upload { width: 320px; flex-shrink: 0; }
.right-preview { flex: 1; }
.preview-header { display: flex; justify-content: space-between; align-items: center; }
.file-type-tabs {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}
.upload-hint {
  margin-top: 12px;
  padding: 8px 12px;
  background: #f4f4f5;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 6px;
}
.pdf-preview-container {
  max-height: 500px;
  overflow-y: auto;
}
.articles-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
.article-item {
  border-bottom: 1px solid #ebeef5;
}
.article-item:last-child {
  border-bottom: none;
}
.article-header {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.article-header:hover {
  background-color: #f5f7fa;
}
.expand-icon {
  transition: transform 0.2s;
  margin-right: 8px;
  color: #909399;
}
.expand-icon.expanded {
  transform: rotate(90deg);
}
.article-no {
  font-weight: 500;
  color: #409eff;
  margin-right: 8px;
  min-width: 60px;
}
.article-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}
.article-content {
  padding: 0 12px 12px 44px;
}
</style>
