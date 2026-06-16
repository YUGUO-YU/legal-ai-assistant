<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI搜法</h2>
      <p>输入法律问题，快速检索相关法规条文及解释</p>
    </div>

    <div class="search-box">
      <el-input
        v-model="query"
        placeholder="请输入法律问题，如：合同欺诈如何认定？"
        size="large"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" :loading="loading">搜索</el-button>
        </template>
      </el-input>
    </div>

    <div class="suggested-queries" v-if="!searched && suggestions.length > 0">
      <span class="suggestion-label">试试这样问：</span>
      <el-tag
        v-for="s in suggestions"
        :key="s"
        class="suggestion-tag"
        @click="query = s; handleSearch()"
      >
        {{ s }}
      </el-tag>
    </div>

    <loading v-if="loading" text="正在检索相关法规..." />

    <div v-else-if="results.length > 0">
      <div class="result-stats">
        找到 {{ total }} 条相关法规，耗时 {{ tookMs }}ms
      </div>

      <div v-for="(item, index) in results" :key="item.articleId" class="result-item" @click="toggleExpand(index)">
        <div class="result-header">
          <div class="tag-group">
            <el-tag type="success" size="small">{{ item.lawTitle }}</el-tag>
            <el-tag size="small">{{ item.articleNo }}</el-tag>
            <el-tag v-if="item.categoryL1" size="small">{{ item.categoryL1 }}</el-tag>
            <el-tag v-if="item.relatedCasesCount > 0" type="warning" size="small">
              相关案例 {{ item.relatedCasesCount }} 个
            </el-tag>
          </div>
          <el-rate v-model="item.rating" :max="3" size="small" @click.stop />
        </div>
        <h4 class="result-title">{{ item.title }}</h4>
        <p class="result-content" :class="{ collapsed: !expanded[index] }">
          {{ item.content }}
        </p>
        <div class="result-footer">
          <div class="result-source">
            来源：<a :href="item.sourceUrl" target="_blank">{{ item.sourceName }}</a>
            <span class="score">匹配度：{{ item.score?.toFixed(2) || '0.00' }}</span>
            <el-tag v-if="item.sourceUrl" size="small" type="info" class="source-tag">已溯源</el-tag>
          </div>
          <el-button text size="small" @click.stop="copyContent(item)">复制</el-button>
        </div>
      </div>

      <div v-if="suggestedQueries.length > 0" class="suggested-queries">
        <span class="suggestion-label">您可能想问：</span>
        <el-tag
          v-for="s in suggestedQueries"
          :key="s"
          class="suggestion-tag"
          @click="query = s; handleSearch()"
        >
          {{ s }}
        </el-tag>
      </div>

      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        @current-change="handleSearch"
      />
    </div>

    <empty-state
      v-else-if="searched"
      icon="Search"
      title="未找到相关法规"
      description="未找到相关法规，建议更换关键词或缩短查询语句"
      action-text="清除搜索"
      @action="query = ''; searched = false"
    />

    <div v-if="relatedCases.length > 0" class="related-cases">
      <h3>相关判例推荐</h3>
      <div v-for="c in relatedCases" :key="c.caseUuid" class="case-item">
        <div class="case-header">
          <h4>{{ c.title }}</h4>
          <el-tag size="small" type="info">{{ c.caseNo }}</el-tag>
        </div>
        <p class="case-info">
          <el-icon><OfficeBuilding /></el-icon> {{ c.court }}
        </p>
        <p class="case-summary">{{ c.summary }}</p>
        <div class="case-footer">
          <span class="source">来源：{{ c.sourceName }}</span>
          <el-button text size="small" @click="viewCase(c)">查看详情</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const query = ref('')
const loading = ref(false)
const results = ref([])
const relatedCases = ref([])
const total = ref(0)
const tookMs = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searched = ref(false)
const expanded = reactive({})
const rating = ref(0)
const suggestedQueries = ref([])
const searchLogId = ref(null)

const suggestions = [
  '合同欺诈如何认定？',
  '劳动合同解除的条件是什么？',
  '建设工程合同纠纷怎么处理？',
  '借款合同利息最高多少？',
  '房屋买卖合同违约责任'
]

const handleSearch = async () => {
  if (!query.value.trim()) return

  loading.value = true
  searched.value = true

  try {
    const res = await api.legalSearch.search({
      query: query.value,
      page: page.value,
      pageSize: pageSize.value,
      includeCases: true
    })
    results.value = (res.data.items || []).map(item => ({ ...item, rating: 0 }))
    relatedCases.value = res.data.relatedCases || []
    total.value = res.data.total || 0
    tookMs.value = res.data.tookMs || 0
    searchLogId.value = res.data.searchLogId

    if (page.value === 1 && res.data.items?.length > 0) {
      loadSuggestedQueries()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('检索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadSuggestedQueries = async () => {
  try {
    const res = await api.legalSearch.getSuggestedQueries(query.value)
    suggestedQueries.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

const toggleExpand = (index) => {
  expanded[index] = !expanded[index]
}

const copyContent = (item) => {
  const text = `${item.lawTitle} ${item.articleNo} ${item.title}\n${item.content}`
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

const viewCase = (c) => {
  ElMessage.info('跳转到案例详情页（待实现）')
}

const handleRate = async (item, value) => {
  item.rating = value
  try {
    await api.legalSearch.feedback({
      searchLogId: searchLogId.value,
      articleId: item.articleId,
      isHelpful: value > 0 ? 1 : 0,
      userComment: value > 0 ? '有用' : '无用'
    })
    ElMessage.success('感谢您的反馈')
  } catch (e) {
    console.error(e)
  }
}
</script>

<style lang="scss" scoped>
.suggested-queries {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
  .suggestion-label {
    color: #999;
    font-size: 14px;
  }
  .suggestion-tag {
    cursor: pointer;
    &:hover {
      color: #1890ff;
      border-color: #1890ff;
    }
  }
}

.result-stats {
  color: #999;
  font-size: 14px;
  margin-bottom: 16px;
}

.result-item {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 16px;
  transition: all 0.3s;
  cursor: pointer;
  &:hover {
    border-color: #1890ff;
    box-shadow: 0 2px 12px rgba(24,144,255,0.1);
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.result-title {
  margin: 0 0 8px 0;
  font-size: 16px;
}

.result-content {
  margin: 0 0 12px 0;
  color: #333;
  line-height: 1.6;
  &.collapsed {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

.result-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-source {
  font-size: 13px;
  color: #999;
  a {
    color: #1890ff;
  }
  .score {
    margin-left: 16px;
  }
  .source-tag {
    margin-left: 8px;
  }
}

.related-cases {
  margin-top: 32px;
  h3 {
    font-size: 16px;
    margin-bottom: 16px;
  }
}

.case-item {
  padding: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 12px;
  &:hover {
    border-color: #1890ff;
  }
  .case-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h4 {
      margin: 0;
      font-size: 15px;
    }
  }
  .case-info {
    color: #666;
    font-size: 14px;
    margin: 8px 0;
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .case-summary {
    margin: 0 0 12px 0;
    color: #333;
    line-height: 1.5;
  }
  .case-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .source {
      color: #999;
      font-size: 13px;
    }
  }
}
</style>