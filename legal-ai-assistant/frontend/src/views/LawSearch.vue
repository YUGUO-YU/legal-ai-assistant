<template>
  <div class="page-card">
    <div class="page-header">
      <div class="header-content">
        <div>
          <h2>法规查询</h2>
          <p>查询法律法规，支持分类浏览和版本追溯</p>
        </div>
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Upload /></el-icon>
          上传法规
        </el-button>
      </div>
    </div>

    <el-dialog v-model="uploadDialogVisible" title="AI 联网导入法规" width="500px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="法律名称">
          <el-input
            v-model="uploadForm.lawName"
            placeholder="例如：民法典、刑法、劳动合同法"
            clearable
            @keyup.enter="submitLawUpload"
          />
        </el-form-item>
        <el-form-item>
          <div class="upload-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>AI 将自动联网搜索该法律的完整条文，结构化后写入数据库</span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploadLoading" @click="submitLawUpload">
          开始导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="articlesDialogVisible" :title="selectedLaw?.title + ' - 条款列表'" width="800px" :close-on-click-modal="false">
      <loading v-if="articlesLoading" text="正在加载条款..." />
      <div v-else-if="articles.length > 0" class="articles-preview">
        <div v-for="article in articles" :key="article.articleUuid" class="article-item">
          <div class="article-header">
            <span class="article-no">{{ article.articleNo }}</span>
            <span class="article-title" v-if="article.title">{{ article.title }}</span>
          </div>
          <div class="article-content">{{ article.content }}</div>
        </div>
      </div>
      <empty-state v-else icon="Document" title="暂无条款" description="该法规尚未包含条款内容" />
      <template #footer>
        <el-button @click="articlesDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewLaw(selectedLaw)">
          查看详情
        </el-button>
      </template>
    </el-dialog>

    <el-row :gutter="24">
      <el-col :span="6">
        <div class="category-panel">
          <div class="panel-header">
            <h3>法规分类</h3>
            <el-button text size="small" @click="refreshCategories">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
          <el-tree
            :data="treeData"
            :props="treeProps"
            default-expand-all
            @node-click="handleNodeClick"
            node-key="id"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span>{{ data.label }}</span>
                <span v-if="data.count" class="node-count">{{ data.count }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </el-col>
      <el-col :span="18">
        <div class="filter-row">
          <el-input
            v-model="keyword"
            placeholder="搜索法规名称或关键词"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
          <el-select v-model="statusFilter" placeholder="法规状态" clearable>
            <el-option
              v-for="s in statusOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </div>

        <div v-if="loading" class="skeleton-laws">
          <div v-for="i in 5" :key="i" class="skeleton-law-item">
            <div class="skeleton-law-header skeleton"></div>
            <div class="skeleton-law-meta skeleton"></div>
            <div class="skeleton-law-info skeleton"></div>
          </div>
        </div>

        <div v-else-if="results.length > 0" class="result-list">
          <div v-for="law in results" :key="law.lawUuid" class="law-item">
            <div class="law-header">
              <h4>{{ law.title }}</h4>
              <el-tag :type="getStatusType(law.status)" size="small">
                {{ law.statusName }}
              </el-tag>
            </div>
            <div class="law-meta">
              <span class="short-title" v-if="law.shortTitle">简称：{{ law.shortTitle }}</span>
              <span>{{ law.categoryL1 }} | {{ law.categoryL2 }}</span>
              <span>{{ law.issuingAuthority }}</span>
            </div>
            <div class="law-info">
              <span><el-icon><Calendar /></el-icon> 发布日期：{{ law.issueDate }}</span>
              <span><el-icon><Clock /></el-icon> 生效日期：{{ law.effectiveDate }}</span>
              <span><el-icon><Document /></el-icon> {{ law.articleCount }} 条条款</span>
              <span><el-icon><View /></el-icon> {{ law.viewCount }} 次浏览</span>
            </div>
            <div class="law-actions">
              <el-button type="primary" size="small" @click="viewLaw(law)">
                <el-icon><View /></el-icon> 查看全文
              </el-button>
              <el-button size="small" @click="browseArticles(law)">
                <el-icon><Document /></el-icon> 浏览条款
              </el-button>
              <el-button size="small" @click="collectLaw(law)">
                <el-icon><Star /></el-icon> 收藏
              </el-button>
            </div>
          </div>

          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="handleSearch"
          />
        </div>

        <empty-state
          v-else-if="searched"
          icon="Collection"
          title="未找到匹配的法规"
          description="请尝试更换关键词或浏览其他分类"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const results = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searched = ref(false)
const keyword = ref('')
const statusFilter = ref(null)

const uploadDialogVisible = ref(false)
const uploadLoading = ref(false)
const uploadForm = reactive({
  lawName: ''
})

const articlesDialogVisible = ref(false)
const articlesLoading = ref(false)
const articles = ref([])
const selectedLaw = ref(null)

const treeData = ref([
  {
    id: 'law',
    label: '法律',
    count: 256,
    children: [
      { id: 'civil', label: '民法', count: 45 },
      { id: 'criminal', label: '刑法', count: 38 },
      { id: 'admin', label: '行政法', count: 52 },
      { id: 'commercial', label: '商法', count: 28 }
    ]
  },
  {
    id: 'admin_regulation',
    label: '行政法规',
    count: 189,
    children: [
      { id: 'state_council', label: '国务院规章', count: 156 },
      { id: 'ministry', label: '部委规章', count: 33 }
    ]
  },
  {
    id: 'local_regulation',
    label: '地方性法规',
    count: 1024,
    children: [
      { id: 'provincial', label: '省级法规', count: 568 },
      { id: 'city', label: '市级法规', count: 456 }
    ]
  },
  {
    id: 'judicial',
    label: '司法解释',
    count: 89,
    children: [
      { id: 'supreme_court', label: '最高人民法院', count: 67 },
      { id: 'procuratorate', label: '最高人民检察院', count: 22 }
    ]
  }
])

const treeProps = {
  label: 'label',
  children: 'children'
}

const statusOptions = ref([])

const loadStatusOptions = async () => {
  try {
    const res = await api.lawSearch.getCategories()
    statusOptions.value = res?.statusOptions || []
  } catch (e) {
    ElMessage.error('Failed to load status options')
    statusOptions.value = [
      { value: 1, label: '现行有效' },
      { value: 2, label: '已废止' },
      { value: 3, label: '修订中' },
      { value: 4, label: '尚未生效' },
      { value: 5, label: '部分失效' }
    ]
  }
}

const handleSearch = async () => {
  loading.value = true

  try {
    const res = await api.lawSearch.search({
      keyword: keyword.value,
      status: statusFilter.value,
      page: page.value,
      pageSize: pageSize.value
    })

    searched.value = true
    results.value = res?.items || []
    total.value = res?.total || 0
  } catch (e) {
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const showUploadDialog = () => {
  uploadForm.lawName = ''
  uploadDialogVisible.value = true
}

const submitLawUpload = async () => {
  if (!uploadForm.lawName.trim()) {
    ElMessage.warning('请输入法律名称')
    return
  }

  uploadLoading.value = true
  try {
    const res = await api.lawImport.webSearch({ lawName: uploadForm.lawName.trim() })
    if (res) {
      ElMessage.success('法规导入任务已启动，请到数据管理中心查看进度')
      uploadDialogVisible.value = false
    }
  } catch (e) {
    ElMessage.error('法规上传失败')
    ElMessage.error(e?.message || e?.response?.data?.message || '导入失败')
  } finally {
    uploadLoading.value = false
  }
}

const handleNodeClick = (data) => {
  if (!data.children || data.children.length === 0) {
    keyword.value = data.label
    handleSearch()
  }
}

const refreshCategories = async () => {
  await loadCategories()
  ElMessage.success('分类已刷新')
}

const getStatusType = (status) => {
  const types = { 1: 'success', 2: 'danger', 3: 'warning', 4: 'info', 5: 'warning' }
  return types[status] || 'info'
}

const viewLaw = (law) => {
  articlesDialogVisible.value = false
  router.push(`/law-detail/${law.lawUuid}`)
}

const browseArticles = async (law) => {
  selectedLaw.value = law
  articles.value = []
  articlesDialogVisible.value = true
  articlesLoading.value = true

  try {
    const res = await api.lawSearch.getLawArticles(law.lawUuid)
    articles.value = res || []
  } catch (e) {
    ElMessage.error('加载条款失败')
  } finally {
    articlesLoading.value = false
  }
}

const collectLaw = async (law) => {
  try {
    await api.lawFavorite.add(law.lawUuid, law.title)
    ElMessage.success('已添加到收藏')
  } catch (e) {
    ElMessage.error('收藏失败')
    ElMessage.error(e?.message || e?.response?.data?.message || '收藏失败')
  }
}

onMounted(async () => {
  await loadCategories()
  handleSearch()
})

const loadCategories = async () => {
  try {
    const res = await api.lawSearch.getCategories()
    const categories = res?.categoryL1 || []
    treeData.value = categories.map((cat) => ({
      id: cat.code,
      label: cat.name,
      count: null,
      children: []
    }))
    statusOptions.value = res?.statusOptions || [
      { value: 1, label: '现行有效' },
      { value: 2, label: '已废止' },
      { value: 3, label: '修订中' },
      { value: 4, label: '尚未生效' },
      { value: 5, label: '部分失效' }
    ]
  } catch (e) {
    ElMessage.error('Failed to load categories')
  }
}
</script>

<style lang="scss" scoped>
.skeleton-laws {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-law-item {
  padding: 20px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;

  .skeleton-law-header {
    height: 24px;
    width: 60%;
    margin-bottom: 12px;
    border-radius: 4px;
  }

  .skeleton-law-meta {
    height: 16px;
    width: 80%;
    margin-bottom: 12px;
    border-radius: 4px;
  }

  .skeleton-law-info {
    height: 16px;
    width: 50%;
    border-radius: 4px;
  }
}

.skeleton {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.category-panel {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  height: fit-content;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    h3 {
      margin: 0;
      font-size: 16px;
    }
  }
}

.page-header {
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.upload-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  background: #f6f8fa;
  padding: 8px 12px;
  border-radius: 4px;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  width: 100%;
  .node-count {
    background: #e6f7ff;
    color: #1890ff;
    padding: 2px 8px;
    border-radius: 10px;
    font-size: 12px;
  }
}

.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;

  .el-input {
    flex: 1;
  }
}

.loading-state {
  text-align: center;
  padding: 48px;
  color: #999;
}

.result-list {
  .law-item {
    padding: 20px;
    border: 1px solid #f0f0f0;
    border-radius: 8px;
    margin-bottom: 16px;
    transition: all 0.3s;
    &:hover {
      border-color: #1890ff;
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.1);
    }
  }

  .law-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
    h4 {
      margin: 0;
      font-size: 16px;
    }
  }

  .law-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 12px;
    color: #666;
    font-size: 14px;

    .short-title {
      color: #1890ff;
    }
  }

  .law-info {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    margin-bottom: 16px;
    color: #999;
    font-size: 13px;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .law-actions {
    display: flex;
    gap: 12px;
    padding-top: 12px;
    border-top: 1px solid #f5f5f5;
  }
}

.empty-state {
  padding: 48px 0;
}

.articles-preview {
  max-height: 60vh;
  overflow-y: auto;

  .article-item {
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    &:last-child {
      border-bottom: none;
    }

    .article-header {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 8px;
    }

    .article-no {
      font-weight: 600;
      color: #1890ff;
      font-size: 15px;
    }

    .article-title {
      color: #333;
      font-size: 14px;
    }

    .article-content {
      color: #666;
      font-size: 14px;
      line-height: 1.8;
      text-align: justify;
    }
  }
}
</style>