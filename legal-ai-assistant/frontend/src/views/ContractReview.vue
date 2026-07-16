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
              placeholder="请粘贴合同文本内容，系统将自动分析合同风险... (按 / 聚焦, Ctrl+Enter 审查)"
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

          <div v-if="loading" class="skeleton-review">
            <div class="skeleton-header skeleton"></div>
            <div class="skeleton-body">
              <div class="skeleton-panel skeleton"></div>
              <div class="skeleton-panel skeleton"></div>
            </div>
          </div>

          <div v-else-if="reviewResult" class="review-result fade-in-up">
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

              <div v-if="reviewResult.dimensions" class="dimension-content">
                <div class="radar-wrapper">
                  <v-chart :option="radarOption" autoresize class="radar-chart" />
                </div>

                <div class="dimensions-summary">
                  <div
                    v-for="dim in reviewResult.dimensions"
                    :key="dim.dimensionCode"
                    class="dim-card"
                    :class="'risk-' + getRiskLevel(dim.score)"
                  >
                    <span class="dim-name">{{ dim.dimensionName }}</span>
                    <span class="dim-score">{{ dim.score }}</span>
                  </div>
                </div>
              </div>

              <div v-else class="dimension-list">
                <div v-for="dim in reviewResult.dimensions" :key="dim.dimensionCode" class="dimension-item">
                  <div class="dim-header">
                    <span class="dim-name">{{ dim.dimensionName }}</span>
                    <span class="dim-score" :style="{ color: getScoreColor(dim.score) }">
                      {{ dim.score }}分
                    </span>
                  </div>
                  <el-progress
                    :percentage="dim.score"
                    :color="getScoreGradient(dim.score)"
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
                <el-icon><Star /></el-icon>
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
                <el-icon><Star /></el-icon>
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
                <el-icon><Star /></el-icon>
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
          <el-button type="primary" @click="goToDetail">
            <el-icon><View /></el-icon>
            查看详情页
          </el-button>
          <el-button @click="exportReport">
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
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
  Star,
  CircleCheck,
  ChatLineSquare,
  Download,
  Folder,
  View
} from '@element-plus/icons-vue'
import api from '../api'
import LocalLoading from '../components/Loading.vue'
import { useUsageMemory } from '@/composables/useUsageMemory'
import { useKeyboardShortcuts, matchShortcut, isInputFocused } from '@/composables/useKeyboardShortcuts'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { RadarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components'

use([CanvasRenderer, RadarChart, TitleComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const { addRecord } = useUsageMemory()

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
    reviewResult.value = res
    ElMessage.success('审查完成')
    const riskCount = res?.risks?.length || 0
    addRecord('contract', `审查"${reviewOptions.contractType}"合同`, `发现 ${riskCount} 处风险`)
  } catch (e) {
    console.error(e)
    ElMessage.error('审查失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goToDetail = () => {
  if (!reviewResult.value?.reviewUuid) {
    ElMessage.warning('请先完成合同审查')
    return
  }
  router.push(`/contract-risk/${reviewResult.value.reviewUuid}`)
}

const handleFileChange = async (file) => {
  fileList.value.push(file)
  
  if (file.raw) {
    try {
      const text = await readFileAsText(file.raw)
      contractText.value = text
      ElMessage.success('文件已读取：' + file.name)
    } catch (e) {
      console.error('读取文件失败:', e)
      ElMessage.error('读取文件失败，请手动复制内容')
    }
  }
}

const readFileAsText = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = (e) => reject(e)
    reader.readAsText(file)
  })
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

const getScoreGradient = (score) => {
  if (score >= 80) return [{ color: '#10b981', percentage: 0 }, { color: '#34d399', percentage: 100 }]
  if (score >= 60) return [{ color: '#f59e0b', percentage: 0 }, { color: '#fbbf24', percentage: 100 }]
  return [{ color: '#ef4444', percentage: 0 }, { color: '#f87171', percentage: 100 }]
}

const getRiskLevel = (score) => {
  if (score >= 80) return 'high'
  if (score >= 50) return 'medium'
  return 'low'
}

const radarOption = computed(() => {
  if (!reviewResult.value?.dimensions) return {}

  const dimensions = reviewResult.value.dimensions

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e5e7eb',
      textStyle: {
        color: '#1f2937'
      }
    },
    radar: {
      indicator: dimensions.map(d => ({
        name: d.dimensionName,
        max: 100
      })),
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: '#6b7280',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(102,126,234,0.02)', 'rgba(102,126,234,0.05)', 'rgba(102,126,234,0.08)', 'rgba(102,126,234,0.12)']
        }
      },
      axisLine: {
        lineStyle: {
          color: '#e5e7eb'
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: dimensions.map(d => d.score),
        name: '风险评分',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: {
          color: '#667eea',
          width: 2
        },
        areaStyle: {
          color: {
            type: 'radial',
            x: 0.5,
            y: 0.5,
            r: 0.5,
            colorStops: [
              { offset: 0, color: 'rgba(102,126,234,0.6)' },
              { offset: 1, color: 'rgba(102,126,234,0.1)' }
            ]
          }
        },
        itemStyle: {
          color: '#667eea'
        },
        label: {
          show: true,
          formatter: '{c}',
          color: '#667eea',
          fontWeight: 'bold'
        }
      }]
    }]
  }
})

const exportReport = () => {
  if (!reviewResult.value) {
    ElMessage.warning('请先进行合同审查')
    return
  }

  const result = reviewResult.value
  const report = generateReportHtml(result)

  const blob = new Blob([report], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `合同审查报告_${Date.now()}.html`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)

  ElMessage.success('报告已导出')
}

const generateReportHtml = (result) => {
  const riskLevelName = { LOW: '低风险', MEDIUM: '中等风险', HIGH: '高风险' }
  const riskClass = { LOW: 'low', MEDIUM: 'medium', HIGH: 'high' }

  let dimensionsHtml = ''
  if (result.dimensions && result.dimensions.length) {
    dimensionsHtml = result.dimensions.map(dim => `
      <tr>
        <td>${dim.dimensionName}</td>
        <td><span class="score">${dim.score}</span></td>
        <td>${dim.comment || '-'}</td>
      </tr>
    `).join('')
  }

  const riskLevelClass = riskClass[result.riskLevel] || 'medium'
  const riskLevelLabel = riskLevelName[result.riskLevel] || '未知'

  let highRiskHtml = ''
  if (result.highRiskItems && result.highRiskItems.length) {
    highRiskHtml = `
      <div class="risk-section high">
        <h4>高风险问题</h4>
        ${result.highRiskItems.map(item => `
          <div class="risk-item">
            <div class="risk-title">${item.title}</div>
            <div class="risk-desc">${item.description || ''}</div>
            <div class="risk-suggestion">建议：${item.suggestion || '-'}</div>
          </div>
        `).join('')}
      </div>
    `
  }

  let mediumRiskHtml = ''
  if (result.mediumRiskItems && result.mediumRiskItems.length) {
    mediumRiskHtml = `
      <div class="risk-section medium">
        <h4>中风险问题</h4>
        ${result.mediumRiskItems.map(item => `
          <div class="risk-item">
            <div class="risk-title">${item.title}</div>
            <div class="risk-desc">${item.description || ''}</div>
            <div class="risk-suggestion">建议：${item.suggestion || '-'}</div>
          </div>
        `).join('')}
      </div>
    `
  }

  let lowRiskHtml = ''
  if (result.lowRiskItems && result.lowRiskItems.length) {
    lowRiskHtml = `
      <div class="risk-section low">
        <h4>低风险问题</h4>
        ${result.lowRiskItems.map(item => `
          <div class="risk-item">
            <div class="risk-title">${item.title}</div>
            <div class="risk-desc">${item.description || ''}</div>
            <div class="risk-suggestion">建议：${item.suggestion || '-'}</div>
          </div>
        `).join('')}
      </div>
    `
  }

  return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>合同审查报告</title>
  <style>
    body { font-family: 'Microsoft YaHei', Arial, sans-serif; max-width: 900px; margin: 40px auto; padding: 20px; color: #333; }
    h1 { color: #667eea; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
    h2 { color: #444; margin-top: 30px; }
    h4 { color: #555; margin: 15px 0 10px; }
    .header { display: flex; justify-content: space-between; align-items: center; }
    .date { color: #888; font-size: 14px; }
    .summary { background: linear-gradient(135deg, rgba(102,126,234,0.1), rgba(118,75,162,0.1)); padding: 20px; border-radius: 12px; margin: 20px 0; }
    .score-circle { display: inline-flex; flex-direction: column; align-items: center; justify-content: center; width: 100px; height: 100px; border-radius: 50%; color: white; font-weight: bold; }
    .score-circle.low { background: linear-gradient(135deg, #10b981, #059669); }
    .score-circle.medium { background: linear-gradient(135deg, #f59e0b, #d97706); }
    .score-circle.high { background: linear-gradient(135deg, #ef4444, #dc2626); }
    table { width: 100%; border-collapse: collapse; margin: 15px 0; }
    th, td { padding: 12px; text-align: left; border-bottom: 1px solid rgba(102, 126, 234, 0.1); }
    th { background: rgba(102, 126, 234, 0.06); font-weight: 600; color: var(--color-text-primary); }
    .score { font-weight: bold; color: #667eea; }
    .risk-section { margin: 20px 0; padding: 15px; border-radius: 8px; }
    .risk-section.high { background: rgba(239,68,68,0.1); border-left: 4px solid #ef4444; }
    .risk-section.medium { background: rgba(245,158,11,0.1); border-left: 4px solid #f59e0b; }
    .risk-section.low { background: rgba(16,185,129,0.1); border-left: 4px solid #10b981; }
    .risk-item { margin-bottom: 15px; }
    .risk-title { font-weight: 600; color: #333; }
    .risk-desc { color: #666; margin: 5px 0; }
    .risk-suggestion { color: #667eea; font-size: 14px; }
    .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; color: #888; font-size: 12px; }
    .disclaimer { background: rgba(102, 126, 234, 0.06); padding: 15px; border-radius: 8px; margin-top: 20px; font-size: 13px; color: var(--color-text-secondary); border: 1px solid rgba(102, 126, 234, 0.12); }
  </style>
</head>
<body>
  <h1>合同审查报告</h1>
  <div class="date">生成时间：${new Date().toLocaleString('zh-CN')}</div>

  <div class="summary">
    <div class="header">
      <div>
        <h2 style="margin: 0;">综合评分：<span class="score">${result.totalScore}</span> 分</h2>
        <p style="margin: 5px 0 0;">风险等级：<span class="risk-badge ${riskLevelClass}">${riskLevelLabel}</span></p>
      </div>
    </div>
    <p style="margin-top: 15px;">${result.overallComment || ''}</p>
  </div>

  <h2>维度评分</h2>
  <table>
    <thead>
      <tr>
        <th>维度</th>
        <th>评分</th>
        <th>评价</th>
      </tr>
    </thead>
    <tbody>
      ${dimensionsHtml}
    </tbody>
  </table>

  ${highRiskHtml}
  ${mediumRiskHtml}
  ${lowRiskHtml}

  <div class="disclaimer">
    <strong>免责声明：</strong>本报告由AI辅助生成，仅供参考。报告内容不构成正式法律意见，如需针对具体合同的法律建议，请咨询具有执业资格的专业律师。
  </div>

  <div class="footer">
    <p>本报告由法律AI助手自动生成</p>
  </div>
</body>
</html>
  `
}

const saveDraft = async () => {
  if (!reviewResult.value) {
    ElMessage.warning('请先完成合同审查')
    return
  }
  try {
    const draft = {
      id: reviewResult.value.reviewUuid || Date.now().toString(36),
      contractText: contractText.value,
      contractType: reviewOptions.contractType,
      contractAmount: reviewOptions.amount,
      riskLevel: reviewResult.value.riskLevel,
      risks: reviewResult.value.risks,
      summary: reviewResult.value.summary,
      savedAt: Date.now()
    }
    const drafts = JSON.parse(localStorage.getItem('contract_drafts') || '[]')
    const existing = drafts.findIndex(d => d.id === draft.id)
    if (existing >= 0) {
      drafts[existing] = draft
    } else {
      drafts.unshift(draft)
    }
    localStorage.setItem('contract_drafts', JSON.stringify(drafts.slice(0, 20)))
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

useKeyboardShortcuts([
  {
    match: (e) => matchShortcut(e, { ctrl: true, key: 'enter' }) && !loading.value && contractText.value.trim(),
    handler: () => handleReview()
  },
  {
    match: (e) => e.key === '/' && !isInputFocused(),
    handler: () => {
      const textarea = document.querySelector('.textarea-wrapper textarea')
      if (textarea) textarea.focus()
    }
  }
])
</script>

<style lang="scss" scoped>
.contract-review {
  animation: fadeIn 0.4s ease;
}

.skeleton-review {
  padding: 20px 0;

  .skeleton-header {
    height: 120px;
    margin-bottom: 20px;
    border-radius: 12px;
  }

  .skeleton-body {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .skeleton-panel {
      height: 80px;
      border-radius: 12px;
    }
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
      color: var(--color-text-secondary);
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
  border: 1px solid rgba(102, 126, 234, 0.15);
  border-radius: 20px;
  background: rgba(19, 17, 28, 0.7);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  height: 100%;

  :deep(.el-card__header) {
    padding: 20px 24px;
    border-bottom: 1px solid rgba(102, 126, 234, 0.12);
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
    color: var(--color-text-primary);

    .el-icon {
      font-size: 20px;
      color: var(--color-primary-light);
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
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(102, 126, 234, 0.15);
    color: var(--color-text-primary);

    &::placeholder {
      color: var(--color-text-muted);
    }
  }
}

.upload-wrapper {
  margin-bottom: 20px;

  .upload-area {
    :deep(.el-upload-dragger) {
      padding: 40px;
      border-radius: 16px;
      border: 2px dashed rgba(102, 126, 234, 0.3);
      background: rgba(102, 126, 234, 0.06);
      transition: all 0.3s;
      position: relative;

      &:hover {
        border-color: rgba(102, 126, 234, 0.6);
        background: rgba(102, 126, 234, 0.1);

        &::after {
          content: '';
          position: absolute;
          inset: -2px;
          border-radius: 16px;
          border: 2px dashed rgba(102, 126, 234, 0.6);
          animation: dashFlow 0.8s linear infinite;
          pointer-events: none;
        }
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
        color: var(--color-text-primary);
        margin-bottom: 4px;
      }

      .sub-text {
        font-size: 13px;
        color: var(--color-text-secondary);
      }
    }

    .upload-hint {
      font-size: 12px;
      color: var(--color-text-secondary);
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
      color: var(--color-text-secondary);

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
    color: var(--color-text-secondary);
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

@keyframes dashFlow {
  0% {
    border-color: #667eea;
  }
  50% {
    border-color: rgba(102, 126, 234, 0.3);
  }
  100% {
    border-color: #667eea;
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
          color: var(--color-text-primary);
        }

        .score-unit {
          font-size: 14px;
          color: var(--color-text-secondary);
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
        color: var(--color-text-secondary);
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
      color: var(--color-text-primary);

      .el-icon {
        color: #667eea;
      }
    }

    .dimension-content {
      display: flex;
      gap: 24px;

      .radar-wrapper {
        flex: 0 0 300px;
        height: 300px;

        .radar-chart {
          width: 100%;
          height: 100%;
        }
      }

      .dimensions-summary {
        flex: 1;
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;
        align-content: start;
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
          color: var(--color-text-secondary);
        }

        .dim-score {
          font-size: 14px;
          font-weight: 600;
        }
      }

      :deep(.el-progress-bar__outer) {
        background: rgba(102, 126, 234, 0.12) !important;
        border-radius: 4px;
      }

      .dim-comment {
        font-size: 12px;
        color: var(--color-text-secondary);
        margin-top: 6px;
      }
    }

    .dim-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-radius: 10px;
      border-left: 3px solid;
      background: #f9fafb;

      .dim-name {
        font-size: 13px;
        font-weight: 500;
        color: var(--color-text-secondary);
      }

      .dim-score {
        font-size: 16px;
        font-weight: 700;
      }

      &.risk-high {
        background: rgba(245, 108, 108, 0.1);
        border-left-color: #f56c6c;

        .dim-score { color: #f56c6c; }
      }

      &.risk-medium {
        background: rgba(230, 162, 60, 0.1);
        border-left-color: #e6a23c;

        .dim-score { color: #e6a23c; }
      }

      &.risk-low {
        background: rgba(103, 194, 58, 0.1);
        border-left-color: #67c23a;

        .dim-score { color: #67c23a; }
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
    color: var(--color-text-secondary);
  }

  p {
    margin: 0;
    color: var(--color-text-secondary);
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
    color: var(--color-text-secondary);
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
    color: var(--color-text-secondary);
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
      color: var(--color-text-primary);
    }
  }

  p {
    margin: 0 0 20px 0;
    font-size: 14px;
    color: var(--color-text-secondary);
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
