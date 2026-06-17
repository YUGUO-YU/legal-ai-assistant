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
  DataAnalysis
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const lawData = ref(null)

const loadLawDetail = async () => {
  const lawUuid = route.params.lawUuid
  if (!lawUuid) {
    ElMessage.error('法规ID不能为空')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await api.lawSearch.getLawDetail(lawUuid)
    if (res.data) {
      lawData.value = res.data
    } else {
      ElMessage.error('法规不存在')
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

const generateAnalysis = () => {
  ElMessage.info('AI法规解读功能开发中...')
}

const copyContent = () => {
  if (lawData.value?.content) {
    navigator.clipboard.writeText(lawData.value.content)
    ElMessage.success('法规全文已复制')
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
      color: #6b7280;

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
      color: #6b7280;
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
        color: #1f2937;
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
      color: #1f2937;
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
      background: #f9fafb;
      border-radius: 12px;
      font-size: 14px;
      color: #4b5563;
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
          color: #6b7280;
        }

        .value {
          font-size: 15px;
          font-weight: 500;
          color: #1f2937;
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
</style>
