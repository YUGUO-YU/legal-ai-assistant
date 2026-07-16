<template>
  <div class="file-manager">
    <div class="page-header">
      <div class="header-content">
        <h2>PPT文件管理</h2>
        <p>管理您生成的PPT演示文稿</p>
      </div>
    </div>

    <div class="content-area">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索PPT文件..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        <el-button type="primary" @click="goToCreate" class="create-btn">
          <el-icon><Plus /></el-icon>
          新建PPT
        </el-button>
      </div>

      <loading v-if="loading" text="加载中..." />

      <div v-else-if="filteredDocuments.length === 0" class="empty-state">
        <div class="empty-icon">
          <el-icon><Document /></el-icon>
        </div>
        <h3>暂无PPT文件</h3>
        <p>点击上方按钮，基于法律搜索结果创建您的第一个PPT</p>
        <el-button type="primary" @click="goToCreate">
          <el-icon><Plus /></el-icon>
          创建PPT
        </el-button>
      </div>

      <div v-else class="file-grid">
        <el-card
          v-for="doc in filteredDocuments"
          :key="doc.id"
          class="file-card"
          shadow="hover"
        >
          <div class="file-preview" :style="getPreviewStyle(doc)">
            <div class="file-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="file-page-count">{{ doc.slides?.length || 0 }}页</div>
          </div>

          <div class="file-info">
            <h4 class="file-title">{{ doc.title }}</h4>
            <div class="file-meta">
              <span class="meta-item">
                <el-icon><Clock /></el-icon>
                {{ doc.updatedAt }}
              </span>
              <span class="meta-item">
                <el-icon><Folder /></el-icon>
                {{ getTemplateName(doc.templateId) }}
              </span>
            </div>
          </div>

          <div class="file-actions">
            <el-button type="primary" link @click="openDocument(doc)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="success" link @click="downloadDocument(doc)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button type="danger" link @click="deleteDocument(doc)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Clock, Folder, Edit, Download, Delete, Plus, Search } from '@element-plus/icons-vue'
import { usePptStore } from '@/store/ppt'
import api from '@/api'

const router = useRouter()
const pptStore = usePptStore()

const loading = ref(false)
const searchKeyword = ref('')
const documents = ref([])

const filteredDocuments = computed(() => {
  if (!searchKeyword.value) return documents.value
  const keyword = searchKeyword.value.toLowerCase()
  return documents.value.filter(doc =>
    doc.title.toLowerCase().includes(keyword)
  )
})

const templates = [
  { id: 'legal-blue', name: '法律蓝调' },
  { id: 'purple-peak', name: '紫禁之巅' },
  { id: 'professional', name: '专业沉稳' },
  { id: 'fresh-minimal', name: '清新简约' },
  { id: 'court-gold', name: '法院灰金' }
]

const getTemplateName = (templateId) => {
  const template = templates.find(t => t.id === templateId)
  return template ? template.name : '默认模板'
}

const getPreviewStyle = (doc) => {
  const template = templates.find(t => t.id === doc.templateId)
  if (!template) {
    return { background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' }
  }
  const primaryColors = {
    'legal-blue': { primary: '#1a365d', secondary: '#2c5282' },
    'purple-peak': { primary: '#553c9a', secondary: '#805ad5' },
    'professional': { primary: '#2d3748', secondary: '#4a5568' },
    'fresh-minimal': { primary: '#319795', secondary: '#38b2ac' },
    'court-gold': { primary: '#744210', secondary: '#d69e2e' }
  }
  const colors = primaryColors[doc.templateId] || primaryColors['legal-blue']
  return { background: `linear-gradient(135deg, ${colors.primary} 0%, ${colors.secondary} 100%)` }
}

const goToCreate = () => {
  router.push('/legal-search')
}

const openDocument = (doc) => {
  router.push(`/ppt-editor?id=${doc.id}`)
}

const downloadDocument = async (doc) => {
  try {
    const blob = await pptStore.downloadPpt(doc.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${doc.title || 'PPT演示文稿'}.pptx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${doc.title}" 吗？删除后无法恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await api.ppt.delete(doc.id)
    documents.value = documents.value.filter(d => d.id !== doc.id)
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await api.ppt.list()
    documents.value = response || []
  } catch (error) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<style lang="scss" scoped>
.file-manager {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 28px;
  color: #fff;

  .header-content {
    max-width: 1200px;
    margin: 0 auto;

    h2 {
      font-size: 28px;
      font-weight: 700;
      margin: 0 0 8px 0;
    }

    p {
      font-size: 14px;
      opacity: 0.9;
      margin: 0;
    }
  }
}

.content-area {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .search-input {
    width: 320px;
  }

  .create-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 10px;
    padding: 12px 24px;

    &:hover {
      opacity: 0.9;
      transform: translateY(-1px);
    }
  }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);

  .empty-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 36px;
      color: #667eea;
    }
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text-primary);
    margin: 0 0 8px 0;
  }

  p {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin: 0 0 24px 0;
  }
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.file-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.3s;
  border: none;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
  }

  .file-preview {
    height: 140px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;

    .file-icon {
      font-size: 48px;
      opacity: 0.9;
    }

    .file-page-count {
      font-size: 13px;
      margin-top: 8px;
      opacity: 0.8;
    }
  }

  .file-info {
    padding: 16px;

    .file-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0 0 8px 0;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .file-meta {
      display: flex;
      gap: 16px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--color-text-secondary);

        .el-icon {
          font-size: 12px;
        }
      }
    }
  }

  .file-actions {
    display: flex;
    justify-content: center;
    gap: 8px;
    padding: 12px 16px;
    border-top: 1px solid #f0f0f0;
    background: #f9fafb;
  }
}
</style>
