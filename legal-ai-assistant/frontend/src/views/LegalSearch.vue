<template>
  <div class="legal-search">
    <div class="page-header">
      <div class="header-content">
        <h2>AI搜法</h2>
        <p>输入法律问题，AI智能检索相关法规条文及解释</p>
      </div>
    </div>

    <div class="search-container">
      <div class="search-box">
        <div class="search-input-wrapper">
          <el-icon class="search-icon"><Search /></el-icon>
          <el-input
            v-model="query"
            placeholder="请输入法律问题，如：合同欺诈如何认定？"
            size="large"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" class="search-btn" :loading="loading" @click="handleSearch">
            <template v-if="!loading">
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </template>
          </el-button>
        </div>

        <div class="search-tips" v-if="!searched">
          <span class="tip-label">试试这样问：</span>
          <div class="tip-tags">
            <el-tag
              v-for="s in suggestions"
              :key="s"
              class="suggestion-tag"
              @click="query = s; handleSearch()"
            >
              {{ s }}
              <el-icon class="tag-arrow"><Right /></el-icon>
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <loading v-if="loading" text="正在检索相关法规..." />

    <div v-else-if="results.length > 0" class="results-container">
      <div class="result-header">
        <div class="result-stats">
          <span class="stats-icon"><el-icon><Document /></el-icon></span>
          <span>找到 <strong>{{ total }}</strong> 条相关法规</span>
          <span class="divider">|</span>
          <span class="time-cost">耗时 {{ tookMs }}ms</span>
        </div>
        <el-button type="primary" @click="generatePpt" class="generate-ppt-btn" :loading="generatingPpt">
          <el-icon><OfficeDocument /></el-icon>
          生成PPT
        </el-button>
      </div>

      <div class="result-list">
        <el-card
          v-for="(item, index) in results"
          :key="item.articleId"
          class="result-item"
          :class="{ expanded: expanded[index] }"
          @click="toggleExpand(index)"
        >
          <div class="result-main">
            <div class="result-meta">
              <div class="tag-group">
                <el-tag type="success" size="small" effect="dark" round>
                  {{ item.lawTitle }}
                </el-tag>
                <el-tag size="small" effect="plain" round>{{ item.articleNo }}</el-tag>
                <el-tag v-if="item.categoryL1" size="small" type="info" effect="plain">
                  {{ item.categoryL1 }}
                </el-tag>
              </div>
              <div class="result-actions">
                <el-rate v-model="item.rating" :max="3" size="small" @click.stop />
                <el-button type="primary" link size="small" @click.stop="copyContent(item)">
                  <el-icon><CopyDocument /></el-icon>
                  复制
        </el-button>
        <PptProgressDialog ref="pptProgressRef" v-model="showPptProgress" />
              </div>
            </div>

            <h3 class="result-title">{{ item.title }}</h3>

            <div class="result-content" :class="{ collapsed: !expanded[index] }">
              <div class="content-text">{{ item.content }}</div>
            </div>

            <div class="result-footer">
              <div class="result-source">
                <el-icon><Link /></el-icon>
                <span>来源：</span>
                <a :href="item.sourceUrl" target="_blank" class="source-link">{{ item.sourceName }}</a>
                <span class="source-tag" v-if="item.sourceUrl">
                  <el-icon><CircleCheck /></el-icon>
                  已溯源
                </span>
              </div>
              <div class="result-score">
                <el-progress
                  type="circle"
                  :percentage="Math.round(item.score || 0)"
                  :width="32"
                  :stroke-width="3"
                  :show-text="false"
                />
                <span class="score-text">匹配度 {{ Math.round(item.score || 0) }}%</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="suggestedQueries.length > 0" class="suggested-section">
        <div class="suggested-header">
          <el-icon><ChatDotRound /></el-icon>
          <span>您可能想问</span>
        </div>
        <div class="suggested-tags">
          <el-tag
            v-for="s in suggestedQueries"
            :key="s"
            class="suggestion-tag"
            @click="query = s; handleSearch()"
          >
            {{ s }}
          </el-tag>
        </div>
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handleSearch"
        />
      </div>
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
      <div class="section-header">
        <el-icon><Connection /></el-icon>
        <h3>相关判例推荐</h3>
        <el-tag type="warning" size="small" effect="plain">{{ relatedCases.length }} 个</el-tag>
      </div>
      <el-row :gutter="16">
        <el-col :span="12" v-for="c in relatedCases" :key="c.caseUuid">
          <el-card class="case-card" @click="viewCase(c)">
            <div class="case-header">
              <h4>{{ c.title }}</h4>
              <el-tag size="small" type="info" effect="plain">{{ c.caseNo }}</el-tag>
            </div>
            <div class="case-info">
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ c.court }}</span>
            </div>
            <p class="case-summary">{{ c.summary }}</p>
            <div class="case-footer">
              <span class="source">来源：{{ c.sourceName }}</span>
              <el-button type="primary" link size="small">
                查看详情
                <el-icon><Right /></el-icon>
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'
import PptProgressDialog from '../components/PptProgressDialog.vue'

const router = useRouter()
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
const suggestedQueries = ref([])
const searchLogId = ref(null)
const generatingPpt = ref(false)
const showPptProgress = ref(false)
const pptProgressRef = ref(null)

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

const generatePpt = async () => {
  if (results.value.length === 0) {
    ElMessage.warning('请先进行搜索后再生成PPT')
    return
  }
  showPptProgress.value = true
  try {
    const searchResults = results.value.map(item => ({
      articleId: item.articleId,
      lawTitle: item.lawTitle,
      articleNo: item.articleNo,
      title: item.title,
      content: item.content,
      score: item.score
    }))
    const title = `法律研究报告 - ${query.value}`
    const response = await api.ppt.generate({
      title,
      searchResults,
      templateId: 'legal-blue',
      userId: localStorage.getItem('userId') || 'default'
    })
    pptProgressRef.value?.markComplete()
    setTimeout(() => {
      router.push({ path: '/ppt-editor', query: { id: response.data.id, title } })
    }, 400)
  } catch (error) {
    pptProgressRef.value?.markError(2)
    ElMessage.error('生成PPT失败，请重试')
  } finally {
    generatingPpt.value = false
    setTimeout(() => {
      showPptProgress.value = false
    }, 1500)
  }
}

const viewCase = (c) => {
  if (c.caseUuid) {
    router.push(`/case-detail/${c.caseUuid}`)
  } else {
    ElMessage.warning('该案例缺少详情信息')
  }
}
</script>

<style lang="scss" scoped>
.legal-search {
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
  margin-bottom: 32px;

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

.search-container {
  margin-bottom: 32px;
}

.search-box {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f9fafb;
  border-radius: 14px;
  padding: 6px 6px 6px 20px;
  border: 2px solid transparent;
  transition: all 0.3s;

  &:focus-within {
    border-color: #667eea;
    background: #fff;
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  }

  .search-icon {
    font-size: 20px;
    color: #667eea;
  }

  :deep(.el-input__wrapper) {
    flex: 1;
    background: transparent;
    box-shadow: none;

    .el-input__inner {
      font-size: 16px;
    }
  }

  .search-btn {
    height: 44px;
    padding: 0 28px;
    border-radius: 10px;
    font-size: 15px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    display: flex;
    align-items: center;
    gap: 8px;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }
  }
}

.generate-ppt-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  color: #fff;
  font-size: 14px;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    opacity: 0.95;
  }
}

.search-tips {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  .tip-label {
    color: #6b7280;
    font-size: 13px;
  }

  .tip-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .suggestion-tag {
    cursor: pointer;
    padding: 6px 14px;
    border-radius: 20px;
    font-size: 13px;
    transition: all 0.3s;
    display: flex;
    align-items: center;
    gap: 6px;
    background: #f3f4f6;
    border-color: #e5e7eb;
    color: #4b5563;

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
      border-color: #667eea;
      color: #667eea;

      .tag-arrow {
        transform: translateX(2px);
      }
    }

    .tag-arrow {
      transition: transform 0.3s;
    }
  }
}

.results-container {
  margin-top: 24px;
}

.result-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .result-stats {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #6b7280;
    font-size: 14px;

    .stats-icon {
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #667eea;
    }

    strong {
      color: #667eea;
      font-weight: 600;
    }

    .divider {
      color: #d1d5db;
    }

    .time-cost {
      color: #9ca3af;
    }
  }
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  border: none;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  cursor: pointer;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  }

  &.expanded {
    .result-content {
      max-height: none;
    }
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  .result-main {
    .result-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 14px;

      .tag-group {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }

      .result-actions {
        display: flex;
        align-items: center;
        gap: 12px;
      }
    }

    .result-title {
      margin: 0 0 12px 0;
      font-size: 17px;
      font-weight: 600;
      color: #1f2937;
      line-height: 1.5;
    }

    .result-content {
      background: #f9fafb;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 16px;
      max-height: 120px;
      overflow: hidden;
      transition: max-height 0.3s ease;

      &.collapsed {
        max-height: 120px;
      }

      .content-text {
        color: #4b5563;
        font-size: 14px;
        line-height: 1.8;
      }
    }

    .result-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .result-source {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        color: #6b7280;

        .el-icon {
          color: #9ca3af;
        }

        .source-link {
          color: #667eea;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }

        .source-tag {
          display: flex;
          align-items: center;
          gap: 4px;
          color: #10b981;
          font-size: 12px;
        }
      }

      .result-score {
        display: flex;
        align-items: center;
        gap: 8px;

        :deep(.el-progress__text) {
          display: none;
        }

        .score-text {
          font-size: 12px;
          color: #6b7280;
        }
      }
    }
  }
}

.suggested-section {
  margin-top: 32px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
  border-radius: 16px;
  border: 1px dashed rgba(102, 126, 234, 0.2);

  .suggested-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
    font-size: 15px;
    font-weight: 500;
    color: #667eea;

    .el-icon {
      font-size: 18px;
    }
  }

  .suggested-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;

    .suggestion-tag {
      cursor: pointer;
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 14px;
      background: #fff;
      border-color: #e5e7eb;
      color: #4b5563;
      transition: all 0.3s;

      &:hover {
        background: linear-gradient(135deg, #667eea, #764ba2);
        border-color: transparent;
        color: #fff;
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
      }
    }
  }
}

.pagination-wrapper {
  margin-top: 32px;
  display: flex;
  justify-content: center;

  :deep(.el-pagination) {
    .el-pager li {
      border-radius: 8px;
      &.is-active {
        background: linear-gradient(135deg, #667eea, #764ba2);
      }
    }
  }
}

.related-cases {
  margin-top: 48px;

  .section-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;

    .el-icon {
      font-size: 22px;
      color: #667eea;
    }

    h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  .case-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
    transition: all 0.3s;
    cursor: pointer;
    margin-bottom: 16px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1);
    }

    :deep(.el-card__body) {
      padding: 20px;
    }

    .case-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 12px;

      h4 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #1f2937;
        flex: 1;
        margin-right: 12px;
      }
    }

    .case-info {
      display: flex;
      align-items: center;
      gap: 6px;
      color: #6b7280;
      font-size: 13px;
      margin-bottom: 12px;

      .el-icon {
        color: #9ca3af;
      }
    }

    .case-summary {
      margin: 0 0 16px 0;
      color: #4b5563;
      font-size: 14px;
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .case-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .source {
        color: #9ca3af;
        font-size: 12px;
      }
    }
  }
}
</style>
