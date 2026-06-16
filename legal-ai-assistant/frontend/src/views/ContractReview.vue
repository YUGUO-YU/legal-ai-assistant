<template>
  <div class="contract-review">
    <div class="page-header">
      <div class="header-content">
        <h2>AI合同审查</h2>
        <p>上传合同文本，智能分析8大维度风险</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="handleReview" :loading="loading" :disabled="!contractText.trim()">
          <el-icon><DocumentChecked /></el-icon>
          开始审查
        </el-button>
        <el-button @click="resetForm">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
      </div>
    </div>

    <el-row :gutter="24" class="main-content">
      <el-col :span="12">
        <el-card class="input-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Document /></el-icon>
                <span>合同内容</span>
              </div>
              <el-radio-group v-model="inputMode" size="small">
                <el-radio-button label="paste">粘贴文本</el-radio-button>
                <el-radio-button label="upload">上传文件</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div v-if="inputMode === 'paste'" class="textarea-wrapper">
            <el-input
              v-model="contractText"
              type="textarea"
              :rows="12"
              placeholder="请粘贴合同文本内容，系统将自动分析合同风险..."
            />
          </div>

          <div v-else class="upload-wrapper">
            <el-upload
              drag
              action="#"
              :auto-upload="false"
              :on-change="handleFileChange"
              :file-list="fileList"
              accept=".txt,.doc,.docx,.pdf"
              class="upload-area"
            >
              <div class="upload-content">
                <div class="upload-icon">
                  <el-icon :size="48"><UploadFilled /></el-icon>
                </div>
                <div class="upload-text">
                  <span class="main-text">拖拽文件到此处</span>
                  <span class="sub-text">或点击上传</span>
                </div>
                <div class="upload-hint">
                  支持 .txt, .doc, .docx, .pdf 格式
                </div>
              </div>
            </el-upload>
          </div>

          <div class="review-options">
            <div class="option-item">
              <label>
                <el-icon><Collection /></el-icon>
                合同类型
              </label>
              <el-select v-model="reviewOptions.contractType" placeholder="请选择">
                <el-option label="买卖合同" value="sale" />
                <el-option label="租赁合同" value="lease" />
                <el-option label="建设工程合同" value="construction" />
                <el-option label="借款合同" value="loan" />
                <el-option label="劳动合同" value="labor" />
                <el-option label="其他" value="other" />
              </el-select>
            </div>
            <div class="option-item">
              <label>
                <el-icon><Coin /></el-icon>
                合同金额（万元）
              </label>
              <el-input-number v-model="reviewOptions.amount" :min="0" :precision="2" />
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="result-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon><Stamp /></el-icon>
                <span>审查结果</span>
              </div>
            </div>
          </template>

          <div v-if="loading" class="loading-container">
            <div class="loading-animation">
              <div class="loading-circle"></div>
              <div class="loading-icon">
                <el-icon class="is-loading"><Loading /></el-icon>
              </div>
            </div>
            <p>正在分析合同风险...</p>
          </div>

          <div v-else-if="reviewResult" class="review-result">
            <div class="score-panel">
              <div class="score-circle" :class="getRiskClass(reviewResult.riskLevel)">
                <el-progress
                  type="circle"
                  :percentage="reviewResult.totalScore"
                  :width="120"
                  :stroke-width="10"
                  :color="getScoreColor(reviewResult.totalScore)"
                >
                  <template #default>
                    <div class="score-content">
                      <span class="score-value">{{ reviewResult.totalScore }}</span>
                      <span class="score-unit">分</span>
                    </div>
                  </template>
                </el-progress>
              </div>
              <div class="score-info">
                <div class="risk-badge" :class="getRiskClass(reviewResult.riskLevel)">
                  {{ reviewResult.riskLevel }}
                </div>
                <p class="risk-desc">综合风险评估</p>
              </div>
            </div>

            <div class="dimension-section">
              <h4>
                <el-icon><DataAnalysis /></el-icon>
                各维度评分
              </h4>
              <div class="dimension-list">
                <div v-for="dim in reviewResult.dimensions" :key="dim.dimensionCode" class="dimension-item">
                  <div class="dim-header">
                    <span class="dim-name">{{ dim.dimensionName }}</span>
                    <span class="dim-score" :style="{ color: getScoreColor(dim.score) }">
                      {{ dim.score }}分
                    </span>
                  </div>
                  <el-progress
                    :percentage="dim.score"
                    :color="getScoreColor(dim.score)"
                    :show-text="false"
                    :stroke-width="6"
                  />
                  <div class="dim-comment">{{ dim.comment }}</div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-result">
            <div class="empty-icon">
              <el-icon><Document /></el-icon>
            </div>
            <h3>等待审查</h3>
            <p>请上传或粘贴合同文本，开始智能审查</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="reviewResult" class="risk-details-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon><Warning /></el-icon>
            <span>风险详情</span>
          </div>
        </div>
      </template>

      <el-tabs>
        <el-tab-pane label="高风险" name="high">
          <template #label>
            <span class="tab-label high">
              <el-icon><WarningFilled /></el-icon>
              高风险
              <el-badge :value="reviewResult.highRiskItems?.length || 0" type="danger" />
            </span>
          </template>
          <div v-if="reviewResult.highRiskItems?.length" class="risk-list">
            <div v-for="item in reviewResult.highRiskItems" :key="item.title" class="risk-item high">
              <div class="risk-header">
                <el-icon><WarningFilled /></el-icon>
                <h4>{{ item.title }}</h4>
              </div>
              <p class="risk-desc">{{ item.description }}</p>
              <div class="risk-suggestion">
                <el-icon><LightBulb /></el-icon>
                <span>修改建议：{{ item.suggestion }}</span>
              </div>
            </div>
          </div>
          <div v-else class="no-risk">
            <el-icon><CircleCheck /></el-icon>
            <span>未发现高风险项</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="中风险" name="medium">
          <template #label>
            <span class="tab-label medium">
              <el-icon><Warning /></el-icon>
              中风险
              <el-badge :value="reviewResult.mediumRiskItems?.length || 0" type="warning" />
            </span>
          </template>
          <div v-if="reviewResult.mediumRiskItems?.length" class="risk-list">
            <div v-for="item in reviewResult.mediumRiskItems" :key="item.title" class="risk-item medium">
              <div class="risk-header">
                <el-icon><Warning /></el-icon>
                <h4>{{ item.title }}</h4>
              </div>
              <p class="risk-desc">{{ item.description }}</p>
              <div class="risk-suggestion">
                <el-icon><LightBulb /></el-icon>
                <span>修改建议：{{ item.suggestion }}</span>
              </div>
            </div>
          </div>
          <div v-else class="no-risk">
            <el-icon><CircleCheck /></el-icon>
            <span>未发现中风险项</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="低风险" name="low">
          <template #label>
            <span class="tab-label low">
              <el-icon><SuccessFilled /></el-icon>
              低风险
              <el-badge :value="reviewResult.lowRiskItems?.length || 0" type="success" />
            </span>
          </template>
          <div v-if="reviewResult.lowRiskItems?.length" class="risk-list">
            <div v-for="item in reviewResult.lowRiskItems" :key="item.title" class="risk-item low">
              <div class="risk-header">
                <el-icon><SuccessFilled /></el-icon>
                <h4>{{ item.title }}</h4>
              </div>
              <p class="risk-desc">{{ item.description }}</p>
              <div class="risk-suggestion">
                <el-icon><LightBulb /></el-icon>
                <span>优化建议：{{ item.suggestion }}</span>
              </div>
            </div>
          </div>
          <div v-else class="no-risk">
            <el-icon><CircleCheck /></el-icon>
            <span>未发现低风险项</span>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="overall-comment">
        <div class="comment-header">
          <el-icon><ChatLineSquare /></el-icon>
          <h4>综合评价</h4>
        </div>
        <p>{{ reviewResult.overallComment }}</p>
        <div class="actions">
          <el-button type="primary" @click="exportReport">
            <el-icon><Download /></el-icon>
            导出审查报告
          </el-button>
          <el-button @click="saveDraft">
            <el-icon><Folder /></el-icon>
            保存草稿
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  DocumentChecked,
  Refresh,
  UploadFilled,
  Collection,
  Coin,
  Stamp,
  Loading,
  DataAnalysis,
  Warning,
  WarningFilled,
  SuccessFilled,
  LightBulb,
  CircleCheck,
  ChatLineSquare,
  Download,
  Folder
} from '@element-plus/icons-vue'
import api from '../api'

const inputMode = ref('paste')
const contractText = ref('')
const fileList = ref([])
const loading = ref(false)
const reviewResult = ref(null)

const reviewOptions = reactive({
  contractType: 'sale',
  amount: 0
})

const handleReview = async () => {
  if (!contractText.value.trim() && fileList.value.length === 0) {
    ElMessage.warning('请先上传合同文件或粘贴合同文本')
    return
  }

  loading.value = true
  try {
    const res = await api.contract.review({
      contractText: contractText.value,
      contractType: reviewOptions.contractType,
      contractAmount: reviewOptions.amount
    })
    reviewResult.value = res.data
    ElMessage.success('审查完成')
  } catch (e) {
    console.error(e)
    ElMessage.error('审查失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleFileChange = (file) => {
  fileList.value.push(file)
}

const resetForm = () => {
  contractText.value = ''
  fileList.value = []
  reviewResult.value = null
}

const getRiskClass = (level) => {
  return level === '高风险' ? 'risk-high' : level === '中风险' ? 'risk-medium' : 'risk-low'
}

const getScoreColor = (score) => {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

const exportReport = () => {
  ElMessage.info('导出功能开发中...')
}

const saveDraft = () => {
  ElMessage.success('草稿已保存')
}
</script>

<style lang="scss" scoped>
.contract-review {
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
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

.main-content {
  margin-bottom: 24px;
}

.input-card,
.result-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  height: 100%;

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;

    .el-icon {
      font-size: 20px;
      color: #667eea;
    }
  }
}

.textarea-wrapper {
  margin-bottom: 20px;

  :deep(.el-textarea__inner) {
    border-radius: 12px;
    padding: 16px;
    font-size: 14px;
    line-height: 1.8;
    resize: none;

    &::placeholder {
      color: #9ca3af;
    }
  }
}

.upload-wrapper {
  margin-bottom: 20px;

  .upload-area {
    :deep(.el-upload-dragger) {
      padding: 40px;
      border-radius: 16px;
      border: 2px dashed #e5e7eb;
      background: #f9fafb;
      transition: all 0.3s;

      &:hover {
        border-color: #667eea;
        background: rgba(102, 126, 234, 0.05);
      }
    }
  }

  .upload-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;

    .upload-icon {
      width: 72px;
      height: 72px;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
      border-radius: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #667eea;
    }

    .upload-text {
      text-align: center;

      .main-text {
        display: block;
        font-size: 15px;
        font-weight: 500;
        color: #1f2937;
        margin-bottom: 4px;
      }

      .sub-text {
        font-size: 13px;
        color: #6b7280;
      }
    }

    .upload-hint {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.review-options {
  display: flex;
  gap: 24px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
  border-radius: 16px;

  .option-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 10px;

    label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      font-weight: 500;
      color: #4b5563;

      .el-icon {
        color: #667eea;
      }
    }

    :deep(.el-select),
    :deep(.el-input-number) {
      width: 100%;
    }
  }
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;

  .loading-animation {
    position: relative;
    width: 80px;
    height: 80px;
    margin-bottom: 24px;

    .loading-circle {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      border: 3px solid transparent;
      border-top-color: #667eea;
      animation: spin 1s linear infinite;
    }

    .loading-icon {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;

      .el-icon {
        font-size: 28px;
        color: #667eea;
      }
    }
  }

  p {
    color: #6b7280;
    font-size: 14px;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.review-result {
  .score-panel {
    display: flex;
    align-items: center;
    gap: 32px;
    margin-bottom: 32px;
    padding-bottom: 24px;
    border-bottom: 1px solid #f3f4f6;

    .score-circle {
      :deep(.el-progress__text) {
        display: none;
      }

      .score-content {
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .score-value {
          font-size: 36px;
          font-weight: 700;
          color: #1f2937;
        }

        .score-unit {
          font-size: 14px;
          color: #9ca3af;
        }
      }
    }

    .score-info {
      .risk-badge {
        display: inline-block;
        padding: 8px 20px;
        border-radius: 20px;
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 8px;

        &.risk-high {
          background: rgba(239, 68, 68, 0.1);
          color: #ef4444;
        }

        &.risk-medium {
          background: rgba(245, 158, 11, 0.1);
          color: #f59e0b;
        }

        &.risk-low {
          background: rgba(16, 185, 129, 0.1);
          color: #10b981;
        }
      }

      .risk-desc {
        color: #6b7280;
        font-size: 14px;
        margin: 0;
      }
    }
  }

  .dimension-section {
    h4 {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 0 0 20px 0;
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;

      .el-icon {
        color: #667eea;
      }
    }

    .dimension-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .dimension-item {
      .dim-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;

        .dim-name {
          font-size: 14px;
          font-weight: 500;
          color: #374151;
        }

        .dim-score {
          font-size: 14px;
          font-weight: 600;
        }
      }

      :deep(.el-progress-bar__outer) {
        background: #f3f4f6;
        border-radius: 4px;
      }

      .dim-comment {
        font-size: 12px;
        color: #9ca3af;
        margin-top: 6px;
      }
    }
  }
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  text-align: center;

  .empty-icon {
    width: 100px;
    height: 100px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 24px;

    .el-icon {
      font-size: 48px;
      color: #d1d5db;
    }
  }

  h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    font-weight: 600;
    color: #374151;
  }

  p {
    margin: 0;
    color: #9ca3af;
    font-size: 14px;
  }
}

.risk-details-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid #f3f4f6;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }

  .tab-label {
    display: flex;
    align-items: center;
    gap: 6px;

    &.high {
      color: #ef4444;
    }

    &.medium {
      color: #f59e0b;
    }

    &.low {
      color: #10b981;
    }
  }
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.risk-item {
  padding: 20px;
  border-radius: 16px;
  border-left: 4px solid;

  &.high {
    background: rgba(239, 68, 68, 0.05);
    border-color: #ef4444;

    .risk-header {
      color: #ef4444;
    }
  }

  &.medium {
    background: rgba(245, 158, 11, 0.05);
    border-color: #f59e0b;

    .risk-header {
      color: #f59e0b;
    }
  }

  &.low {
    background: rgba(16, 185, 129, 0.05);
    border-color: #10b981;

    .risk-header {
      color: #10b981;
    }
  }

  .risk-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;

    h4 {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
    }
  }

  .risk-desc {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: #4b5563;
    line-height: 1.6;
  }

  .risk-suggestion {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 14px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 10px;
    font-size: 13px;
    color: #6b7280;
    line-height: 1.6;

    .el-icon {
      color: #667eea;
      flex-shrink: 0;
      margin-top: 2px;
    }
  }
}

.no-risk {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: #10b981;
  font-size: 15px;
  background: rgba(16, 185, 129, 0.05);
  border-radius: 12px;
}

.overall-comment {
  margin-top: 32px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
  border-radius: 16px;

  .comment-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;

    .el-icon {
      font-size: 20px;
      color: #667eea;
    }

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }
  }

  p {
    margin: 0 0 20px 0;
    font-size: 14px;
    color: #4b5563;
    line-height: 1.8;
  }

  .actions {
    display: flex;
    gap: 12px;

    .el-button {
      border-radius: 10px;
    }
  }
}
</style>
