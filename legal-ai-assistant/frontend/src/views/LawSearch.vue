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
              <el-button size="small" @click="collectLaw(law)" :type="favoriteIds.has(law.lawUuid) ? 'warning' : 'default'">
                <el-icon><Star /></el-icon>
                {{ favoriteIds.has(law.lawUuid) ? '已收藏' : '收藏' }}
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
const favoriteIds = ref(new Set())

const treeData = ref([])

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
  if (favoriteIds.value.has(law.lawUuid)) {
    try {
      await api.lawFavorite.remove(law.lawUuid)
      favoriteIds.value.delete(law.lawUuid)
      ElMessage.success('已取消收藏')
    } catch (e) {
      ElMessage.error('取消收藏失败')
    }
  } else {
    try {
      await api.lawFavorite.add(law.lawUuid, law.title)
      favoriteIds.value.add(law.lawUuid)
      ElMessage.success('已添加到收藏')
    } catch (e) {
      ElMessage.error('收藏失败')
    }
  }
}

const loadFavorites = async () => {
  try {
    const res = await api.lawFavorite.list()
    if (res && res.length > 0) {
      res.forEach(item => favoriteIds.value.add(item.lawUuid))
    }
  } catch (_) {}
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadFavorites()])
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
  gap: 14px;
}

.skeleton-law-item {
  padding: 20px;
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-lg);
  background: var(--color-bg-sidebar);

  .skeleton-law-header {
    height: 22px;
    width: 55%;
    margin-bottom: 12px;
    border-radius: var(--radius-sm);
  }

  .skeleton-law-meta {
    height: 14px;
    width: 75%;
    margin-bottom: 10px;
    border-radius: var(--radius-sm);
  }

  .skeleton-law-info {
    height: 14px;
    width: 45%;
    border-radius: var(--radius-sm);
  }
}

.skeleton {
  background: linear-gradient(90deg, var(--color-border-glass) 25%, rgba(102,126,234,0.08) 50%, var(--color-border-glass) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.page-header {
  margin-bottom: 24px;

  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;

    h2 {
      margin: 0 0 4px;
      font-size: 24px;
      font-weight: 700;
      background: var(--gradient-text);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: var(--color-text-secondary);
    }
  }
}

.category-panel {
  background: var(--color-bg-sidebar);
  border: 1px solid var(--color-border-glass);
  border-radius: var(--radius-lg);
  padding: 16px;
  height: fit-content;
  position: sticky;
  top: 0;

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    h3 {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--color-text-primary);
    }

    .el-button {
      color: var(--color-text-muted);
      &:hover { color: var(--color-primary); }
    }
  }

  :deep(.el-tree) {
    background: transparent;
    --el-tree-node-hover-bg-color: var(--color-bg-glass-hover);
    color: var(--color-text-primary);

    .el-tree-node__content {
      border-radius: var(--radius-sm);
      padding: 4px 8px;
      margin-bottom: 2px;
      height: auto;
      min-height: 32px;
    }

    .el-tree-node.is-expanded > .el-tree-node__children {
      padding-left: 16px;
    }
  }
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  font-size: 13px;

  .node-count {
    background: rgba(102, 126, 234, 0.1);
    color: var(--color-primary);
    padding: 1px 8px;
    border-radius: var(--radius-full);
    font-size: 11px;
    font-weight: 500;
  }
}

.filter-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: center;

  .el-input {
    flex: 1;
    :deep(.el-input__wrapper) {
      border-radius: var(--radius-md);
      box-shadow: none;
      border: 1px solid var(--color-border-glass);
      transition: all var(--transition-base);
      &:focus-within {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.08);
      }
    }
  }

  .el-select {
    width: 160px;
    :deep(.el-input__wrapper) {
      border-radius: var(--radius-md);
      box-shadow: none;
      border: 1px solid var(--color-border-glass);
    }
  }
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .law-item {
    padding: 20px 22px;
    background: var(--color-bg-sidebar);
    border: 1px solid var(--color-border-glass);
    border-radius: var(--radius-lg);
    transition: all var(--transition-base);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: var(--gradient-primary);
      opacity: 0;
      transition: opacity var(--transition-base);
    }

    &:hover {
      border-color: var(--color-border-glass-hover);
      box-shadow: var(--shadow-card);
      transform: translateY(-1px);

      &::before {
        opacity: 1;
      }
    }
  }

  .law-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 10px;
    padding-left: 12px;

    h4 {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: var(--color-text-primary);
      flex: 1;
    }
  }

  .law-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 10px;
    padding-left: 12px;
    color: var(--color-text-secondary);
    font-size: 13px;

    .short-title {
      color: var(--color-primary);
      font-weight: 500;
    }
  }

  .law-info {
    display: flex;
    flex-wrap: wrap;
    gap: 14px;
    margin-bottom: 14px;
    padding-left: 12px;
    color: var(--color-text-muted);
    font-size: 12px;

    span {
      display: flex;
      align-items: center;
      gap: 4px;

      .el-icon { font-size: 13px; }
    }
  }

  .law-actions {
    display: flex;
    gap: 10px;
    padding-top: 12px;
    border-top: 1px solid var(--color-border-glass);
    padding-left: 12px;

    .el-button {
      border-radius: var(--radius-md);
      font-size: 13px;
      transition: all var(--transition-base);
    }
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.articles-preview {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--color-border-glass); border-radius: 2px; }

  .article-item {
    padding: 16px 0;
    border-bottom: 1px solid var(--color-border-glass);

    &:last-child { border-bottom: none; }

    .article-header {
      display: flex;
      align-items: baseline;
      gap: 8px;
      margin-bottom: 8px;
    }

    .article-no {
      font-weight: 700;
      color: var(--color-primary);
      font-size: 14px;
    }

    .article-title {
      color: var(--color-text-primary);
      font-size: 13px;
      font-weight: 500;
    }

    .article-content {
      color: var(--color-text-secondary);
      font-size: 13px;
      line-height: 1.8;
      text-align: justify;
    }
  }
}

.upload-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
  background: var(--color-bg-page);
  padding: 8px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-glass);
}

:deep(.el-dialog) {
  border-radius: var(--radius-xl);
  .el-dialog__header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--color-border-glass);
  }
  .el-dialog__body { padding: 24px; }
  .el-dialog__footer { padding: 16px 24px; border-top: 1px solid var(--color-border-glass); }
}
</style>