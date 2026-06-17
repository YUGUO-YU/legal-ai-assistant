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

    <el-row :gutter="20" class="kb-grid">
      <el-col :span="8" v-for="kb in filteredKbList" :key="kb.id">
        <el-card class="kb-card" :body-style="{ padding: '0px' }">
          <div class="kb-header" :style="{ background: getKbColor(kb.type) }">
            <div class="kb-icon">
              <el-icon :size="32"><Folder /></el-icon>
            </div>
            <div class="kb-info">
              <h3>{{ kb.name }}</h3>
              <p>{{ kb.description || '暂无描述' }}</p>
            </div>
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
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Upload,
  Plus,
  Search,
  Folder,
  User,
  Clock,
  ChatDotRound,
  Setting
} from '@element-plus/icons-vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const showUpload = ref(false)
const showCreateKb = ref(false)
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

const knowledgeBases = ref([
  {
    id: 1,
    name: '劳动法法规库',
    description: '劳动法律法规及相关案例汇总',
    type: 'public',
    isPublic: 1,
    docCount: 156,
    chunkCount: 2340,
    size: '128MB',
    owner: '系统管理员',
    updateTime: '2024-06-10',
    parseStatus: 2
  },
  {
    id: 2,
    name: '合同纠纷案例',
    description: '各类合同纠纷案例集',
    type: 'private',
    isPublic: 0,
    docCount: 89,
    chunkCount: 1560,
    size: '96MB',
    owner: '张三',
    updateTime: '2024-06-08',
    parseStatus: 2
  },
  {
    id: 3,
    name: '知识产权法规',
    description: '知识产权相关法律法规',
    type: 'public',
    isPublic: 1,
    docCount: 234,
    chunkCount: 3200,
    size: '156MB',
    owner: '系统管理员',
    updateTime: '2024-06-12',
    parseStatus: 2
  },
  {
    id: 4,
    name: '公司法务文档',
    description: '公司内部法务文档',
    type: 'private',
    isPublic: 0,
    docCount: 45,
    chunkCount: 680,
    size: '45MB',
    owner: '李四',
    updateTime: '2024-06-05',
    parseStatus: 1
  }
])

const filteredKbList = computed(() => {
  if (!searchKeyword.value) return knowledgeBases.value
  const kw = searchKeyword.value.toLowerCase()
  return knowledgeBases.value.filter(kb =>
    kb.name.toLowerCase().includes(kw) ||
    kb.description?.toLowerCase().includes(kw)
  )
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
  await new Promise(r => setTimeout(r, 2000))
  uploading.value = false
  showUpload.value = false
  fileList.value = []
  ElMessage.success('文档上传成功，正在解析中...')
}

const handleCreateKb = async () => {
  if (!kbForm.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }

  const newKb = {
    id: Date.now(),
    name: kbForm.name,
    description: kbForm.description,
    type: kbForm.isPublic === 1 ? 'public' : 'private',
    isPublic: kbForm.isPublic,
    docCount: 0,
    chunkCount: 0,
    size: '0MB',
    owner: '当前用户',
    updateTime: new Date().toLocaleDateString(),
    parseStatus: 0
  }
  knowledgeBases.value.push(newKb)
  showCreateKb.value = false
  kbForm.name = ''
  kbForm.description = ''
  kbForm.isPublic = 0
  ElMessage.success('知识库创建成功')
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
  ElMessage.info(`进入知识库管理：${kb.name}`)
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
      color: #6b7280;
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
        color: #9ca3af;
      }
    }
  }

  .kb-meta {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #6b7280;
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
