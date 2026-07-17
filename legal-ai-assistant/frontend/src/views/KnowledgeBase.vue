<template>
  <div class="knowledge-base">
    <div class="page-header">
      <div class="header-content">
        <h2>案例法规库</h2>
        <p>管理个人文档知识库，支持语义检索和团队共享</p>
      </div>
    </div>

    <el-card class="action-card">
      <div class="action-bar">
        <div class="action-buttons">
          <el-button type="primary" @click="showUpload = true">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
          <el-button type="primary" @click="showCreateKb = true">
            <el-icon><Plus /></el-icon>
            新建知识库
          </el-button>
        </div>
        <div class="search-box">
          <el-input v-model="searchKeyword" placeholder="搜索知识库" clearable>
            <template #append>
              <el-button :icon="Search" />
            </template>
          </el-input>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="showUpload" title="上传文档" width="600px">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="目标知识库">
          <el-select v-model="uploadForm.kbId" placeholder="请选择知识库">
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文档名称">
          <el-input v-model="uploadForm.docName" placeholder="自定义文档名称（可选）" />
        </el-form-item>
        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :file-list="fileList"
            :limit="10"
            accept=".pdf,.doc,.docx,.txt"
          >
            <el-button>
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、Word、TXT 格式，单文件不超过 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">
          开始上传
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateKb" title="新建知识库" width="500px">
      <el-form :model="kbForm" label-width="100px">
        <el-form-item label="知识库名称" required>
          <el-input v-model="kbForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="kbForm.description" type="textarea" :rows="3" placeholder="简要描述知识库用途" />
        </el-form-item>
        <el-form-item label="权限">
          <el-radio-group v-model="kbForm.isPublic">
            <el-radio :label="0">私人</el-radio>
            <el-radio :label="1">团队共享</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateKb = false">取消</el-button>
        <el-button type="primary" @click="handleCreateKb">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showManage" title="知识库管理" width="640px">
      <div v-if="managingKb" class="manage-summary">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="知识库名称">{{ managingKb.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ kbTypeLabel(managingKb.type) }}</el-descriptions-item>
          <el-descriptions-item label="可见性">{{ managingKb.visibility ? '团队共享' : '仅个人' }}</el-descriptions-item>
          <el-descriptions-item label="文档数">{{ managingKb.docCount }}</el-descriptions-item>
          <el-descriptions-item label="总分块数">{{ managingKb.chunkCount }}</el-descriptions-item>
          <el-descriptions-item label="总大小">{{ formatSize(managingKb.totalSize) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ managingKb.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ managingKb.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ managingKb.description || '暂无描述' }}</el-descriptions-item>
        </el-descriptions>
        <div class="manage-actions">
          <el-button type="primary" @click="goToDetail(managingKb); showManage = false">
            <el-icon><View /></el-icon>进入详情
          </el-button>
          <el-button @click="goToDocQa(managingKb); showManage = false">
            <el-icon><ChatLineRound /></el-icon>文档问答
          </el-button>
          <el-button @click="goToSearch(managingKb); showManage = false">
            <el-icon><Search /></el-icon>语义检索
          </el-button>
          <el-button @click="goToUploadFromManage">
            <el-icon><Upload /></el-icon>上传文档
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button type="danger" @click="deleteKb">删除知识库</el-button>
        <el-button @click="showManage = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-row :gutter="20" class="kb-grid">
      <el-col :span="8" v-for="kb in filteredKbList" :key="kb.id">
        <el-card class="kb-card" :body-style="{ padding: '0px' }" @click="goToDetail(kb)">
          <div class="kb-header" :style="{ background: getKbColor(kb.type) }">
            <div class="kb-icon">
              <el-icon :size="32"><Folder /></el-icon>
            </div>
            <div class="kb-info">
              <h3>{{ kb.name }}</h3>
              <p>{{ kb.description || '暂无描述' }}</p>
            </div>
            <el-icon class="detail-arrow"><Right /></el-icon>
          </div>
          <div class="kb-body">
            <div class="kb-stats">
              <div class="stat-item">
                <span class="stat-value">{{ kb.docCount }}</span>
                <span class="stat-label">文档</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ kb.chunkCount || 0 }}</span>
                <span class="stat-label">段落</span>
              </div>
              <div class="stat-item">
                <span class="stat-value">{{ kb.size || '0MB' }}</span>
                <span class="stat-label">存储</span>
              </div>
            </div>
            <div class="kb-meta">
              <span><el-icon><User /></el-icon> {{ kb.owner }}</span>
              <span><el-icon><Clock /></el-icon> {{ kb.updateTime }}</span>
            </div>
            <div class="kb-tags">
              <el-tag v-if="kb.isPublic" type="success" size="small">团队共享</el-tag>
              <el-tag v-else size="small">私人</el-tag>
              <el-tag :type="getParseStatusType(kb.parseStatus)" size="small">
                {{ getParseStatusName(kb.parseStatus) }}
              </el-tag>
            </div>
          </div>
          <div class="kb-actions">
            <el-button type="primary" size="small" @click="goToSearch(kb)">
              <el-icon><Search /></el-icon> 检索
            </el-button>
            <el-button size="small" @click="goToDocQa(kb)">
              <el-icon><ChatDotRound /></el-icon> 文档问答
            </el-button>
            <el-button size="small" @click="manageKb(kb)">
              <el-icon><Setting /></el-icon> 管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <empty-state
      v-if="filteredKbList.length === 0"
      icon="Folder"
      :title="searchKeyword ? '未找到匹配的知识库' : '暂无知识库'"
      :description="searchKeyword ? '请尝试其他关键词' : '请先创建知识库或上传文档'"
      :action-text="searchKeyword ? '' : '新建知识库'"
      @action="showCreateKb = true"
    />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload,
  Plus,
  Search,
  Folder,
  User,
  Clock,
  ChatDotRound,
  Setting,
  Right,
  View,
  ChatLineRound
} from '@element-plus/icons-vue'
import EmptyState from '../components/EmptyState.vue'
import api from '../api'

const router = useRouter()
const showUpload = ref(false)
const showCreateKb = ref(false)
const showManage = ref(false)
const managingKb = ref(null)
const uploading = ref(false)
const searchKeyword = ref('')
const fileList = ref([])
const uploadRef = ref(null)

const uploadForm = reactive({
  kbId: '',
  docName: ''
})

const kbForm = reactive({
  name: '',
  description: '',
  isPublic: 0
})

const knowledgeBases = ref([])

const filteredKbList = computed(() => {
  if (!searchKeyword.value) return knowledgeBases.value
  const kw = searchKeyword.value.toLowerCase()
  return knowledgeBases.value.filter(kb =>
    kb.name.toLowerCase().includes(kw) ||
    kb.description?.toLowerCase().includes(kw)
  )
})

const loadKnowledgeBases = async () => {
  try {
    const res = await api.knowledgeBase.list({ page: 1, pageSize: 100 })
    knowledgeBases.value = res?.items || res || []
  } catch (e) {
    ElMessage.error('加载知识库列表失败')
  }
}

onMounted(() => {
  loadKnowledgeBases()
})

const handleFileChange = (file) => {
  fileList.value.push(file)
}

const handleUpload = async () => {
  if (!uploadForm.kbId) {
    ElMessage.warning('请选择目标知识库')
    return
  }
  if (fileList.value.length === 0) {
    ElMessage.warning('请选择要上传的文件')
    return
  }

  uploading.value = true
  try {
    for (const file of fileList.value) {
      const formData = new FormData()
      formData.append('kbId', uploadForm.kbId)
      if (uploadForm.docName) {
        formData.append('docName', uploadForm.docName)
      }
      formData.append('file', file.raw || file)
      await api.knowledgeBase.upload(formData)
    }
    ElMessage.success('文档上传成功，正在解析中...')
    await loadKnowledgeBases()
    fileList.value = []
    uploadForm.docName = ''
    showUpload.value = false
  } catch (e) {
    ElMessage.error('Upload failed')
    ElMessage.error('上传失败，请稍后重试')
  } finally {
    uploading.value = false
  }
}

const handleCreateKb = async () => {
  if (!kbForm.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }

  try {
    await api.knowledgeBase.create({
      name: kbForm.name,
      description: kbForm.description,
      isPublic: kbForm.isPublic
    })
    await loadKnowledgeBases()
    showCreateKb.value = false
    kbForm.name = ''
    kbForm.description = ''
    kbForm.isPublic = 0
    ElMessage.success('知识库创建成功')
  } catch (e) {
    ElMessage.error('创建失败：' + (e?.message || '请稍后重试'))
  }
}

const getKbColor = (type) => {
  return type === 'public'
    ? 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    : 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)'
}

const getParseStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const getParseStatusName = (status) => {
  const names = { 0: '待处理', 1: '解析中', 2: '已完成', 3: '失败' }
  return names[status] || ''
}

const goToSearch = (kb) => {
  router.push({ path: '/doc-qa', query: { kbId: kb.id } })
}

const goToDocQa = (kb) => {
  router.push({ path: '/doc-qa', query: { kbId: kb.id, mode: 'qa' } })
}

const manageKb = (kb) => {
  managingKb.value = kb
  showManage.value = true
}

const goToUploadFromManage = () => {
  showManage.value = false
  showUpload.value = true
}

const kbTypeLabel = (type) => {
  return type === 'labor' ? '劳动法' : type === 'contract' ? '合同' : type === 'ip' ? '知识产权' : type === 'corporate' ? '公司法' : '通用'
}

const formatSize = (size) => {
  if (!size && size !== 0) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const deleteKb = async () => {
  if (!managingKb.value) return
  try {
    await ElMessageBox.confirm(
      `确认删除知识库 "${managingKb.value.name}" 吗？该操作不可恢复。`,
      '删除确认',
      { type: 'warning' }
    )
    await api.knowledgeBase.delete(managingKb.value.id)
    await loadKnowledgeBases()
    ElMessage.success('知识库已删除')
    showManage.value = false
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败：' + (e?.message || e))
    }
  }
}

const goToDetail = (kb) => {
  router.push(`/kb-detail/${kb.id}`)
}
</script>

<style lang="scss" scoped>
.knowledge-base {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-header {
  margin-bottom: 24px;

  .header-content {
    h2 {
      margin: 0 0 8px 0;
      font-size: 26px;
      font-weight: 600;
      background: linear-gradient(135deg, #667eea, #764ba2);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    p {
      margin: 0;
      color: var(--color-text-secondary);
      font-size: 14px;
    }
  }
}

.action-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .action-buttons {
    display: flex;
    gap: 12px;

    :deep(.el-button--primary) {
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
      border-radius: 12px;
      padding: 12px 24px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
      }
    }
  }

  .search-box {
    width: 260px;

    :deep(.el-input__wrapper) {
      border-radius: 12px;
      padding: 8px 16px;
    }
  }
}

.manage-summary {
  .manage-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #ebeef5;
  }
}

.kb-grid {
  .kb-card {
    margin-bottom: 16px;
    border: none;
    border-radius: 20px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    transition: all 0.3s;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
    }
  }
}

.kb-header {
  padding: 24px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;

  .detail-arrow {
    opacity: 0;
    transform: translateX(-4px);
    transition: all 0.3s;
  }

  &:hover .detail-arrow {
    opacity: 1;
    transform: translateX(0);
  }

  .kb-icon {
    width: 56px;
    height: 56px;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    backdrop-filter: blur(10px);
  }

  .kb-info {
    flex: 1;

    h3 {
      margin: 0 0 4px 0;
      font-size: 18px;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 13px;
      opacity: 0.9;
    }
  }
}

.kb-body {
  padding: 20px;

  .kb-stats {
    display: flex;
    justify-content: space-around;
    margin-bottom: 16px;
    padding-bottom: 16px;
    border-bottom: 1px solid #f3f4f6;

    .stat-item {
      text-align: center;

      .stat-value {
        display: block;
        font-size: 22px;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea, #764ba2);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .stat-label {
        font-size: 12px;
        color: var(--color-text-secondary);
      }
    }
  }

  .kb-meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 14px;

    span {
      display: flex;
      align-items: center;
      gap: 6px;

      .el-icon {
        color: #667eea;
      }
    }
  }

  .kb-tags {
    display: flex;
    gap: 8px;

    :deep(.el-tag) {
      border-radius: 6px;
    }
  }
}

.kb-actions {
  display: flex;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border-top: 1px solid #f3f4f6;
  gap: 10px;

  :deep(.el-button) {
    border-radius: 8px;
    padding: 8px 16px;
    font-size: 13px;
  }

  :deep(.el-button--primary) {
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
  }
}
</style>
