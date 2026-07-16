<template>
  <div class="law-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
      </div>
      <div class="header-content">
        <h2>法规详情</h2>
        <p>查看法规详细信息及条款内容</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载法规详情..." />

    <div v-else-if="lawData" class="detail-container">
      <el-card class="main-card">
        <div class="law-header">
          <div class="law-title">
            <h1>{{ lawData.title }}</h1>
            <div class="law-meta">
              <el-tag :type="getStatusType(lawData.status)" effect="dark" round>
                {{ lawData.statusName }}
              </el-tag>
              <el-tag size="small" type="info">
                {{ lawData.categoryL1 }} | {{ lawData.categoryL2 }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="law-section" v-if="lawData.shortTitle">
          <h3>法规简称</h3>
          <div class="section-content">
            {{ lawData.shortTitle }}
          </div>
        </div>

        <div class="law-section" v-if="lawData.issuingAuthority">
          <h3>发布机关</h3>
          <div class="section-content">
            <el-icon><OfficeBuilding /></el-icon>
            {{ lawData.issuingAuthority }}
          </div>
        </div>

        <div class="law-section" v-if="lawData.issueDate || lawData.effectiveDate">
          <h3>日期信息</h3>
          <div class="info-grid">
            <div class="info-item" v-if="lawData.issueDate">
              <span class="label">发布日期</span>
              <span class="value">{{ lawData.issueDate }}</span>
            </div>
            <div class="info-item" v-if="lawData.effectiveDate">
              <span class="label">生效日期</span>
              <span class="value">{{ lawData.effectiveDate }}</span>
            </div>
          </div>
        </div>

        <div class="law-section" v-if="lawData.articleCount">
          <h3>法规规模</h3>
          <div class="info-grid">
            <div class="info-item">
              <span class="label">条款数量</span>
              <span class="value">{{ lawData.articleCount }} 条</span>
            </div>
            <div class="info-item" v-if="lawData.viewCount">
              <span class="label">浏览次数</span>
              <span class="value">{{ lawData.viewCount.toLocaleString() }} 次</span>
            </div>
          </div>
        </div>

        <div class="law-section" v-if="lawData.content">
          <h3>法规全文</h3>
          <div class="section-content text-content law-content">
            {{ lawData.content }}
          </div>
        </div>

        <div class="law-section" v-if="lawArticles.length > 0">
          <h3>法规条款</h3>
          <div class="articles-list">
            <div
              v-for="article in lawArticles"
              :key="article.articleUuid"
              class="article-item"
            >
              <div class="article-header">
                <span class="article-no">{{ article.articleNo }}</span>
                <span class="article-title" v-if="article.title">{{ article.title }}</span>
              </div>
              <div class="article-content">
                {{ article.content }}
              </div>
            </div>
          </div>
        </div>

        <div class="law-section source-section">
          <h3>数据来源</h3>
          <div class="source-info">
            <el-icon><Link /></el-icon>
            <a :href="lawData.sourceUrl" target="_blank" class="source-link">
              {{ lawData.sourceName }}
            </a>
          </div>
        </div>
      </el-card>

      <div class="action-buttons">
        <el-button type="primary" size="large" @click="generateAnalysis">
          <el-icon><DataAnalysis /></el-icon>
          AI法规解读
        </el-button>
        <el-button size="large" @click="copyContent">
          <el-icon><CopyDocument /></el-icon>
          复制全文
        </el-button>
      </div>

      <el-dialog v-model="analysisDialogVisible" title="AI法规解读" width="800px" :close-on-click-modal="false">
        <loading v-if="analysisLoading" text="正在分析法规，请稍候..." />
        <div v-else-if="analysisResult" class="analysis-content">
          <el-alert v-if="analysisResult.summary" :title="analysisResult.summary" type="success" :closable="false" show-icon />
          <el-tabs>
            <el-tab-pane label="立法目的" v-if="analysisResult.legislativePurpose">
              <div class="analysis-text">{{ analysisResult.legislativePurpose }}</div>
            </el-tab-pane>
            <el-tab-pane label="核心条款" v-if="analysisResult.coreProvisions">
              <div class="analysis-text">{{ analysisResult.coreProvisions }}</div>
            </el-tab-pane>
            <el-tab-pane label="重点法条释义" v-if="analysisResult.keyArticles && analysisResult.keyArticles.length">
              <div v-for="(article, index) in analysisResult.keyArticles" :key="index" class="article-interpretation">
                <h4>{{ article.articleNo }} {{ article.title }}</h4>
                <p>{{ article.interpretation }}</p>
              </div>
            </el-tab-pane>
            <el-tab-pane label="适用场景" v-if="analysisResult.practicalScenarios && analysisResult.practicalScenarios.length">
              <ul class="scenario-list">
                <li v-for="(scenario, index) in analysisResult.practicalScenarios" :key="index">{{ scenario }}</li>
              </ul>
            </el-tab-pane>
            <el-tab-pane label="关联法规" v-if="analysisResult.relatedLaws && analysisResult.relatedLaws.length">
              <ul class="scenario-list">
                <li v-for="(law, index) in analysisResult.relatedLaws" :key="index">{{ law }}</li>
              </ul>
            </el-tab-pane>
            <el-tab-pane label="风险点" v-if="analysisResult.riskPoints && analysisResult.riskPoints.length">
              <el-alert v-for="(risk, index) in analysisResult.riskPoints" :key="index" :title="risk" type="warning" show-icon style="margin-bottom: 8px;" />
            </el-tab-pane>
            <el-tab-pane label="合规建议" v-if="analysisResult.complianceSuggestions && analysisResult.complianceSuggestions.length">
              <ul class="scenario-list">
                <li v-for="(suggestion, index) in analysisResult.complianceSuggestions" :key="index">{{ suggestion }}</li>
              </ul>
            </el-tab-pane>
          </el-tabs>
        </div>
        <template #footer>
          <el-button @click="analysisDialogVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>

    <empty-state
      v-else
      icon="Document"
      title="未找到法规详情"
      description="未找到该法规的详细信息，请返回列表重试"
      action-text="返回列表"
      @action="goBack"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  OfficeBuilding,
  CopyDocument,
  Link,
  DataAnalysis,
  Document
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const lawData = ref(null)
const lawArticles = ref([])
const analysisDialogVisible = ref(false)
const analysisLoading = ref(false)
const analysisResult = ref(null)

const loadLawDetail = async () => {
  const lawUuid = route.params.lawUuid
  if (!lawUuid) {
    ElMessage.error('法规ID不能为空')
    router.back()
    return
  }

  loading.value = true
  try {
    const [detailRes, articlesRes] = await Promise.all([
      api.lawSearch.getLawDetail(lawUuid),
      api.lawSearch.getLawArticles(lawUuid)
    ])

    if (detailRes.data) {
      lawData.value = detailRes.data
    } else {
      ElMessage.error('法规不存在')
    }

    if (articlesRes.data) {
      lawArticles.value = articlesRes.data
    }
  } catch (e) {
    console.error('Failed to load law detail:', e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const getStatusType = (status) => {
  const types = { 1: 'success', 2: 'danger', 3: 'warning', 4: 'info', 5: 'warning' }
  return types[status] || 'info'
}

const generateAnalysis = async () => {
  if (!lawData.value && lawArticles.value.length === 0) {
    ElMessage.warning('无法获取法规数据')
    return
  }

  analysisResult.value = null
  analysisDialogVisible.value = true
  analysisLoading.value = true

  try {
    const articles = lawArticles.value.map(a => ({
      articleNo: a.articleNo,
      title: a.title || '',
      content: a.content || ''
    }))

    const res = await api.lawAnalysis.analyze(
      lawData.value?.lawUuid,
      lawData.value?.title,
      articles
    )

    if (res.data) {
      analysisResult.value = res.data
      ElMessage.success('AI法规解读完成')
    } else {
      throw new Error('解读结果为空')
    }
  } catch (e) {
    console.error('AI法规解读失败:', e)
    ElMessage.error(e?.message || e?.response?.data?.message || 'AI法规解读失败')
  } finally {
    analysisLoading.value = false
  }
}

const copyContent = () => {
  let contentToCopy = ''
  if (lawData.value?.content) {
    contentToCopy = lawData.value.content
  } else if (lawArticles.value.length > 0) {
    contentToCopy = lawArticles.value
      .map(a => `${a.articleNo}${a.title ? ' ' + a.title : ''}\n${a.content}`)
      .join('\n\n')
  }

  if (contentToCopy) {
    navigator.clipboard.writeText(contentToCopy)
    ElMessage.success('内容已复制')
  } else if (lawData.value?.title) {
    navigator.clipboard.writeText(lawData.value.title)
    ElMessage.success('法规名称已复制')
  }
}

onMounted(() => {
  loadLawDetail()
})
</script>

<style lang="scss" scoped>
.law-detail {
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

  .header-nav {
    margin-bottom: 16px;

    :deep(.el-button) {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: var(--color-text-secondary);

      &:hover {
        color: #667eea;
      }
    }
  }

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

.detail-container {
  .main-card {
    border: none;
    border-radius: 20px;
    box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
    margin-bottom: 24px;

    :deep(.el-card__body) {
      padding: 32px;
    }
  }

  .law-header {
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid #f3f4f6;

    .law-title {
      h1 {
        margin: 0 0 16px 0;
        font-size: 24px;
        font-weight: 600;
        color: var(--color-text-primary);
        line-height: 1.4;
      }

      .law-meta {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;

        .el-tag {
          border-radius: 6px;
        }
      }
    }
  }

  .law-section {
    margin-bottom: 28px;

    &:last-child {
      margin-bottom: 0;
    }

    h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 4px;
        height: 16px;
        background: linear-gradient(135deg, #667eea, #764ba2);
        border-radius: 2px;
      }
    }

    .section-content {
      padding: 16px 20px;
      background: rgba(102, 126, 234, 0.06);
      border: 1px solid rgba(102, 126, 234, 0.12);
      border-radius: 12px;
      font-size: 14px;
      color: var(--color-text-secondary);
      display: flex;
      align-items: center;
      gap: 8px;

      &.text-content {
        display: block;
        line-height: 1.8;
        padding: 20px 24px;
      }

      .el-icon {
        color: #667eea;
      }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;

      .info-item {
        display: flex;
        flex-direction: column;
        gap: 6px;

        .label {
          font-size: 13px;
          color: var(--color-text-secondary);
        }

        .value {
          font-size: 15px;
          font-weight: 500;
          color: var(--color-text-primary);
        }
      }
    }

    &.source-section {
      padding-top: 20px;
      border-top: 1px solid #f3f4f6;

      .source-info {
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
        }

        .source-link {
          color: #667eea;
          text-decoration: none;

          &:hover {
            text-decoration: underline;
          }
        }
      }
    }
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;

    :deep(.el-button) {
      border-radius: 12px;
      padding: 14px 32px;
      font-size: 15px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.3s;

      &.el-button--primary {
        background: linear-gradient(135deg, #667eea, #764ba2);
        border: none;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
        }
      }

      &:not(.el-button--primary):hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
      }
    }
  }
}

@media (max-width: 768px) {
  .detail-container {
    .info-grid {
      grid-template-columns: 1fr;
    }

    .action-buttons {
      flex-direction: column;

      :deep(.el-button) {
        width: 100%;
      }
    }
  }
}

.articles-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .article-item {
    background: rgba(102, 126, 234, 0.06);
    border: 1px solid rgba(102, 126, 234, 0.12);
    border-radius: 12px;
    padding: 16px 20px;
    border-left: 4px solid var(--color-primary);

    .article-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 12px;

      .article-no {
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        padding: 4px 10px;
        border-radius: 6px;
        font-size: 13px;
        font-weight: 600;
      }

      .article-title {
        font-weight: 600;
        color: var(--color-text-primary);
        font-size: 15px;
      }
    }

    .article-content {
      font-size: 14px;
      line-height: 1.8;
      color: var(--color-text-primary);
      padding: 16px;
      background: rgba(102, 126, 234, 0.06);
      border: 1px solid rgba(102, 126, 234, 0.12);
      border-radius: 8px;
    }

    .article-interpretation {
      padding: 16px;
      background: rgba(102, 126, 234, 0.06);
      border: 1px solid rgba(102, 126, 234, 0.12);
      border-radius: 8px;
      margin-bottom: 12px;
      h4 {
        margin: 0 0 8px 0;
        color: #1890ff;
        font-size: 15px;
      }
      p {
        margin: 0;
        color: var(--color-text-secondary);
        font-size: 14px;
        line-height: 1.6;
      }
    }

    .scenario-list {
      padding-left: 20px;
      li {
        font-size: 14px;
        line-height: 1.8;
        color: var(--color-text-secondary);
        margin-bottom: 8px;
      }
    }
  }
}
</style>
