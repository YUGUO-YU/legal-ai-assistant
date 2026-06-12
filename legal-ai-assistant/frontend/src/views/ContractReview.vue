<template>
  <div class="page-card">
    <div class="page-header">
      <h2>AI合同审查</h2>
      <p>上传合同文本，智能分析8大维度风险</p>
    </div>

    <el-row :gutter="24">
      <el-col :span="12">
        <div class="upload-area">
          <el-tabs v-model="inputMode">
            <el-tab-pane label="粘贴文本" name="paste">
              <el-input
                v-model="contractText"
                type="textarea"
                :rows="12"
                placeholder="请粘贴合同文本内容..."
              />
            </el-tab-pane>
            <el-tab-pane label="上传文件" name="upload">
              <el-upload
                drag
                action="#"
                :auto-upload="false"
                :on-change="handleFileChange"
                :file-list="fileList"
              >
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
              </el-upload>
            </el-tab-pane>
          </el-tabs>

          <div class="review-options">
            <el-form :inline="true" :model="reviewOptions">
              <el-form-item label="合同类型">
                <el-select v-model="reviewOptions.contractType" placeholder="请选择">
                  <el-option label="买卖合同" value="sale" />
                  <el-option label="租赁合同" value="lease" />
                  <el-option label="建设工程合同" value="construction" />
                  <el-option label="借款合同" value="loan" />
                  <el-option label="劳动合同" value="labor" />
                  <el-option label="其他" value="other" />
                </el-select>
              </el-form-item>
              <el-form-item label="合同金额">
                <el-input-number v-model="reviewOptions.amount" :min="0" :precision="2" />
              </el-form-item>
            </el-form>
          </div>

          <div class="upload-actions">
            <el-button type="primary" @click="handleReview" :loading="loading">
              <el-icon><DocumentChecked /></el-icon>
              开始审查
            </el-button>
            <el-button @click="resetForm">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </div>
          <loading v-if="loading" text="正在分析合同风险..." />
        </div>
      </el-col>
      <el-col :span="12">
        <div v-if="reviewResult" class="review-result">
          <div class="score-panel">
            <div class="score-circle" :class="getRiskClass(reviewResult.riskLevel)">
              <div class="score-inner">
                <span class="score-value">{{ reviewResult.totalScore }}</span>
                <span class="score-unit">分</span>
              </div>
            </div>
            <div class="score-info">
              <div class="risk-level" :class="getRiskClass(reviewResult.riskLevel)">
                {{ reviewResult.riskLevel }}
              </div>
              <div class="risk-desc">综合风险评估</div>
            </div>
          </div>

          <div class="dimension-chart">
            <h4>各维度评分</h4>
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
                :stroke-width="8"
              />
             <div class="dim-comment">{{ dim.comment }}</div>
            </div>
          </div>
        </div>
        <div v-else class="empty-result">
          <el-empty description="请上传或粘贴合同文本，开始智能审查">
            <template #image>
              <el-icon :size="80" color="#ddd"><Document /></el-icon>
            </template>
          </el-empty>
        </div>
      </el-col>
    </el-row>

    <div v-if="reviewResult" class="risk-details">
      <el-tabs>
        <el-tab-pane label="高风险" name="high">
          <template #label>
            <span>高风险 <el-badge :value="reviewResult.highRiskItems?.length ||0" type="danger" /></span>
          </template>
          <div v-for="item in reviewResult.highRiskItems" :key="item.title" class="risk-item high">
            <div class="risk-header">
              <el-icon type="danger"><WarningFilled /></el-icon>
              <h4>{{ item.title }}</h4>
            </div>
            <p class="risk-desc">{{ item.description }}</p>
            <div class="risk-suggestion">
              <el-icon><InfoFilled /></el-icon>
              <span>修改建议：{{ item.suggestion }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="中风险" name="medium">
          <template #label>
            <span>中风险 <el-badge :value="reviewResult.mediumRiskItems?.length || 0" type="warning" /></span>
          </template>
          <div v-for="item in reviewResult.mediumRiskItems" :key="item.title" class="risk-item medium">
            <div class="risk-header">
              <el-icon type="warning"><Warning /></el-icon>
              <h4>{{ item.title }}</h4>
            </div>
            <p class="risk-desc">{{ item.description }}</p>
            <div class="risk-suggestion">
              <el-icon><InfoFilled /></el-icon>
              <span>修改建议：{{ item.suggestion }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="低风险" name="low">
          <template #label>
            <span>低风险 <el-badge :value="reviewResult.lowRiskItems?.length || 0" type="success" /></span>
          </template>
          <div v-for="item in reviewResult.lowRiskItems" :key="item.title" class="risk-item low">
            <div class="risk-header">
              <el-icon type="success"><SuccessFilled /></el-icon>
              <h4>{{ item.title }}</h4>
            </div>
            <p class="risk-desc">{{ item.description }}</p>
            <div class="risk-suggestion">
              <el-icon><InfoFilled /></el-icon>
              <span>优化建议：{{ item.suggestion }}</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <div class="overall-comment">
        <h4>
          <el-icon><ChatLineSquare /></el-icon>
          综合评价
        </h4>
        <p>{{ reviewResult.overallComment }}</p>
        <div class="actions">
          <el-button type="primary" @click="exportReport">导出审查报告</el-button>
          <el-button @click="saveDraft">保存草稿</el-button>
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
  if (score >= 80) return '#52c41a'
  if (score >= 60) return '#faad14'
  return '#ff4d4f'
}

const exportReport = () => {
  ElMessage.info('导出功能开发中...')
}

const saveDraft = () => {
  ElMessage.success('草稿已保存')
}
</script>

<style lang="scss" scoped>
.upload-area {
  .review-options {
    margin: 16px 0;
    padding: 16px;
    background: #f5f5f5;
    border-radius: 8px;
  }

  .upload-actions {
    margin-top: 16px;
    display: flex;
    gap: 12px;
  }
}

.review-result {
  background: #fafafa;
  padding: 24px;
  border-radius: 8px;
  height: 100%;
}

.score-panel {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #f0f0f0;
}

.score-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 6px solid;
  &.risk-high { border-color: #ff4d4f; }
  &.risk-medium { border-color: #faad14; }
  &.risk-low { border-color: #52c41a; }

  .score-inner {
    display: flex;
    flex-direction: column;
    align-items: center;
    .score-value {
      font-size: 32px;
      font-weight: bold;
    }
    .score-unit {
      font-size: 12px;
      color: #999;
    }
  }
}

.score-info {
  .risk-level {
    font-size: 24px;
    font-weight: bold;
    &.risk-high { color: #ff4d4f; }
    &.risk-medium { color: #faad14; }
    &.risk-low { color: #52c41a; }
  }
  .risk-desc {
    color: #666;
    font-size: 14px;
  }
}

.dimension-chart {
  h4 {
    margin: 0 0 16px 0;
    font-size: 14px;
    color: #666;
  }
}

.dimension-item {
  margin-bottom: 16px;
  .dim-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
    .dim-name {
      font-size: 14px;
    }
    .dim-score {
      font-weight: bold;
    }
  }
  .dim-comment {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.empty-result {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
}

.risk-details {
  margin-top: 32px;
}

.risk-item {
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;

  &.high {
    background: #fff2f0;
    border-left: 4px solid #ff4d4f;
    .risk-header { color: #ff4d4f; }
  }
  &.medium {
    background: #fffbe6;
    border-left: 4px solid #faad14;
    .risk-header { color: #faad14; }
  }
  &.low {
    background: #f6ffed;
    border-left: 4px solid #52c41a;
    .risk-header { color: #52c41a; }
  }

  .risk-header {
    display: flex;
    align-items: center;
    gap: 8px;
    h4 { margin: 0; }
  }
  .risk-desc {
    margin: 8px 0;
    color: #333;
  }
  .risk-suggestion {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 8px 12px;
    background: rgba(255,255,255,0.5);
    border-radius: 4px;
    font-size: 13px;
    color: #666;
  }
}

.overall-comment {
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-top: 24px;

  h4 {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 12px 0;
  }
  p {
    margin: 0 0 16px 0;
    line-height: 1.6;
  }
  .actions {
    display: flex;
    gap: 12px;
  }
}
</style>