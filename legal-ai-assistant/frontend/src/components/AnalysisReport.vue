<template>
  <div class="analysis-report">
    <el-tabs v-model="activeTab" class="analysis-tabs">
      <el-tab-pane label="胜诉率预测" name="winRate">
        <div class="win-rate-section">
          <el-row :gutter="24">
            <el-col :span="8">
              <div class="prediction-card">
                <div class="prediction-header">
                  <span class="prediction-label">预测胜诉率</span>
                  <el-tag :type="getWinRateTagType" size="small">{{ getWinRateLevel }}</el-tag>
                </div>
                <div class="prediction-value">
                  <el-progress
                    type="circle"
                    :percentage="Math.round((statistics.winRatePrediction?.predictedWinRate || 0) * 100)"
                    :color="getWinRateColor"
                    :width="140"
                    :stroke-width="12"
                  >
                    <template #default>
                      <span class="percentage-text">{{ Math.round((statistics.winRatePrediction?.predictedWinRate || 0) * 100) }}%</span>
                    </template>
                  </el-progress>
                </div>
                <div class="confidence">
                  置信度：{{ Math.round((statistics.winRatePrediction?.confidence || 0) * 100) }}%
                </div>
              </div>
            </el-col>
            <el-col :span="16">
              <div class="factors-card">
                <h4>影响因素分析</h4>
                <div class="factors-list">
                  <div v-if="favorableFactors.length > 0" class="factor-group favorable">
                    <div class="factor-title">
                      <el-icon><CircleCheck /></el-icon>
                      <span>有利因素</span>
                    </div>
                    <ul>
                      <li v-for="(factor, idx) in favorableFactors" :key="'f-' + idx">{{ factor }}</li>
                    </ul>
                  </div>
                  <div v-if="unfavorableFactors.length > 0" class="factor-group unfavorable">
                    <div class="factor-title">
                      <el-icon><CircleClose /></el-icon>
                      <span>不利因素</span>
                    </div>
                    <ul>
                      <li v-for="(factor, idx) in unfavorableFactors" :key="'u-' + idx">{{ factor }}</li>
                    </ul>
                  </div>
                  <div v-if="neutralFactors.length > 0" class="factor-group neutral">
                    <div class="factor-title">
                      <el-icon><InfoFilled /></el-icon>
                      <span>中性因素</span>
                    </div>
                    <ul>
                      <li v-for="(factor, idx) in neutralFactors" :key="'n-' + idx">{{ factor }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="24" class="result-prob-row">
            <el-col :span="24">
              <div class="result-prob-card">
                <h4>各类结果概率分布</h4>
                <div class="result-prob-bars">
                  <div v-for="(prob, key) in resultProbabilities" :key="key" class="prob-item">
                    <span class="prob-label">{{ key }}</span>
                    <el-progress :percentage="Math.round(prob * 100)" :stroke-width="20" :color="getResultColor(key)" />
                    <span class="prob-value">{{ Math.round(prob * 100) }}%</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="判决分布" name="judgment">
        <div class="judgment-section">
          <el-row :gutter="24">
            <el-col :span="12">
              <div class="chart-card">
                <h4>判决结果分布</h4>
                <div ref="judgmentChartRef" class="chart-container"></div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="distribution-detail">
                <h4>详细数据</h4>
                <div class="detail-list">
                  <div v-for="(count, key) in statistics.judgmentDistribution" :key="key" class="detail-item">
                    <span class="detail-label">{{ key }}</span>
                    <span class="detail-value">{{ count }} 件</span>
                    <span class="detail-percent">{{ getPercent(count) }}%</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="赔偿分析" name="compensation">
        <div class="compensation-section">
          <el-row :gutter="24">
            <el-col :span="12">
              <div class="chart-card">
                <h4>赔偿金额区间分布</h4>
                <div ref="compensationChartRef" class="chart-container"></div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="compensation-detail">
                <h4>金额区间统计</h4>
                <div class="range-list">
                  <div class="range-item">
                    <div class="range-header">
                      <span class="range-label">0-5万</span>
                      <span class="range-count">{{ statistics.compensationDistribution?.range0to5w || 0 }} 件</span>
                    </div>
                    <el-progress :percentage="getRangePercent(statistics.compensationDistribution?.range0to5w || 0)" :stroke-width="16" color="#67C23A" />
                  </div>
                  <div class="range-item">
                    <div class="range-header">
                      <span class="range-label">5-20万</span>
                      <span class="range-count">{{ statistics.compensationDistribution?.range5to20w || 0 }} 件</span>
                    </div>
                    <el-progress :percentage="getRangePercent(statistics.compensationDistribution?.range5to20w || 0)" :stroke-width="16" color="#409EFF" />
                  </div>
                  <div class="range-item">
                    <div class="range-header">
                      <span class="range-label">20-50万</span>
                      <span class="range-count">{{ statistics.compensationDistribution?.range20to50w || 0 }} 件</span>
                    </div>
                    <el-progress :percentage="getRangePercent(statistics.compensationDistribution?.range20to50w || 0)" :stroke-width="16" color="#E6A23C" />
                  </div>
                  <div class="range-item">
                    <div class="range-header">
                      <span class="range-label">50万以上</span>
                      <span class="range-count">{{ statistics.compensationDistribution?.rangeAbove50w || 0 }} 件</span>
                    </div>
                    <el-progress :percentage="getRangePercent(statistics.compensationDistribution?.rangeAbove50w || 0)" :stroke-width="16" color="#F56C6C" />
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="策略建议" name="strategy">
        <div class="strategy-section">
          <el-card class="strategy-card">
            <template #header>
              <div class="strategy-header">
                <el-icon><MagicStick /></el-icon>
                <span>诉讼策略建议</span>
              </div>
            </template>
            <div class="strategy-list">
              <div
                v-for="(rec, idx) in statistics.strategyRecommendations"
                :key="idx"
                class="strategy-item"
              >
                <div class="strategy-index">{{ idx + 1 }}</div>
                <div class="strategy-content">{{ rec }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="时间线分析" name="timeline">
        <div class="timeline-section">
          <el-row :gutter="24">
            <el-col :span="16">
              <div class="chart-card">
                <h4>类案审结时间分布</h4>
                <div ref="timelineChartRef" class="chart-container timeline-chart"></div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="timeline-summary">
                <h4>时间线摘要</h4>
                <div class="summary-item">
                  <span class="summary-label">平均审理周期</span>
                  <span class="summary-value">{{ statistics.timelineAnalysis?.avgDuration || 0 }} 天</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">趋势方向</span>
                  <el-tag :type="getTrendTagType" size="small">{{ getTrendText }}</el-tag>
                </div>
                <div class="summary-item">
                  <span class="summary-label">法院级别分布</span>
                </div>
                <div class="court-dist-list">
                  <div v-for="(count, level) in statistics.timelineAnalysis?.courtLevelDistribution" :key="level" class="court-dist-item">
                    <span>{{ level }}</span>
                    <span>{{ count }} 件</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  statistics: {
    type: Object,
    default: () => ({})
  }
})

const activeTab = ref('winRate')
const judgmentChartRef = ref(null)
const compensationChartRef = ref(null)
const timelineChartRef = ref(null)

let judgmentChart = null
let compensationChart = null
let timelineChart = null

const favorableFactors = computed(() => props.statistics.winRatePrediction?.factorAnalysis?.favorableFactors || [])
const unfavorableFactors = computed(() => props.statistics.winRatePrediction?.factorAnalysis?.unfavorableFactors || [])
const neutralFactors = computed(() => props.statistics.winRatePrediction?.factorAnalysis?.neutralFactors || [])
const resultProbabilities = computed(() => props.statistics.winRatePrediction?.resultProbabilities || {})

const getWinRateColor = computed(() => {
  const rate = props.statistics.winRatePrediction?.predictedWinRate || 0
  if (rate >= 0.7) return '#67C23A'
  if (rate >= 0.4) return '#E6A23C'
  return '#F56C6C'
})

const getWinRateTagType = computed(() => {
  const rate = props.statistics.winRatePrediction?.predictedWinRate || 0
  if (rate >= 0.7) return 'success'
  if (rate >= 0.4) return 'warning'
  return 'danger'
})

const getWinRateLevel = computed(() => {
  const rate = props.statistics.winRatePrediction?.predictedWinRate || 0
  if (rate >= 0.7) return '较高'
  if (rate >= 0.4) return '中等'
  return '较低'
})

const getResultColor = (key) => {
  if (key === '全部支持') return '#67C23A'
  if (key === '部分支持') return '#E6A23C'
  return '#F56C6C'
}

const getTrendTagType = computed(() => {
  const trend = props.statistics.timelineAnalysis?.trendDirection || 'stable'
  if (trend === 'increasing') return 'success'
  if (trend === 'decreasing') return 'danger'
  return 'info'
})

const getTrendText = computed(() => {
  const trend = props.statistics.timelineAnalysis?.trendDirection || 'stable'
  if (trend === 'increasing') return '上升'
  if (trend === 'decreasing') return '下降'
  return '稳定'
})

const getPercent = (count) => {
  const total = Object.values(props.statistics.judgmentDistribution || {}).reduce((a, b) => a + b, 0)
  return total > 0 ? Math.round(count / total * 100) : 0
}

const getRangePercent = (count) => {
  const total = (props.statistics.compensationDistribution?.range0to5w || 0) +
    (props.statistics.compensationDistribution?.range5to20w || 0) +
    (props.statistics.compensationDistribution?.range20to50w || 0) +
    (props.statistics.compensationDistribution?.rangeAbove50w || 0)
  return total > 0 ? Math.round(count / total * 100) : 0
}

const initJudgmentChart = () => {
  if (!judgmentChartRef.value) return
  const dist = props.statistics.judgmentDistribution || {}
  const data = Object.entries(dist).map(([name, value]) => ({ name, value }))

  judgmentChart = echarts.init(judgmentChartRef.value)
  judgmentChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { bottom: 0 },
    color: ['#67C23A', '#E6A23C', '#F56C6C'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}件' },
      data
    }]
  })
}

const initCompensationChart = () => {
  if (!compensationChartRef.value) return
  const dist = props.statistics.compensationDistribution || {}

  compensationChart = echarts.init(compensationChartRef.value)
  compensationChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['0-5万', '5-20万', '20-50万', '50万以上'] },
    yAxis: { type: 'value', name: '案件数' },
    color: ['#67C23A', '#409EFF', '#E6A23C', '#F56C6C'],
    series: [{
      type: 'bar',
      data: [
        dist.range0to5w || 0,
        dist.range5to20w || 0,
        dist.range20to50w || 0,
        dist.rangeAbove50w || 0
      ],
      itemStyle: { borderRadius: [8, 8, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  })
}

const initTimelineChart = () => {
  if (!timelineChartRef.value) return
  const dist = props.statistics.timelineAnalysis?.caseDistribution || []

  timelineChart = echarts.init(timelineChartRef.value)
  timelineChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dist.map(d => d.year) },
    yAxis: { type: 'value', name: '案件数' },
    color: ['#409EFF'],
    series: [{
      type: 'line',
      data: dist.map(d => d.count),
      smooth: true,
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(64, 158, 255, 0.4)' }, { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }] } },
      label: { show: true, position: 'top' }
    }]
  })
}

const initCharts = () => {
  nextTick(() => {
    initJudgmentChart()
    initCompensationChart()
    initTimelineChart()
  })
}

const resizeCharts = () => {
  judgmentChart?.resize()
  compensationChart?.resize()
  timelineChart?.resize()
}

watch(() => props.statistics, () => {
  initCharts()
}, { deep: true })

watch(activeTab, () => {
  setTimeout(resizeCharts, 100)
})

onMounted(() => {
  initCharts()
  window.addEventListener('resize', resizeCharts)
})
</script>

<style lang="scss" scoped>
.analysis-report {
  padding: 20px 0;
}

.analysis-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }
}

.win-rate-section {
  .prediction-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    padding: 24px;
    color: #fff;
    text-align: center;

    .prediction-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .prediction-label {
        font-size: 14px;
        opacity: 0.9;
      }
    }

    .prediction-value {
      display: flex;
      justify-content: center;
      margin-bottom: 12px;

      .percentage-text {
        font-size: 32px;
        font-weight: bold;
      }
    }

    .confidence {
      font-size: 12px;
      opacity: 0.8;
    }
  }

  .factors-card {
    background: #f5f7fa;
    border-radius: 16px;
    padding: 20px;

    h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #303133;
    }

    .factors-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .factor-group {
      ul {
        margin: 8px 0 0 0;
        padding-left: 24px;

        li {
          color: #606266;
          font-size: 13px;
          line-height: 1.6;
        }
      }

      &.favorable .factor-title {
        color: #67C23A;
      }

      &.unfavorable .factor-title {
        color: #F56C6C;
      }

      &.neutral .factor-title {
        color: #909399;
      }

      .factor-title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        font-weight: 500;
      }
    }
  }

  .result-prob-row {
    margin-top: 20px;
  }

  .result-prob-card {
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #303133;
    }

    .result-prob-bars {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .prob-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .prob-label {
        width: 80px;
        font-size: 14px;
        color: #606266;
      }

      .prob-value {
        width: 50px;
        text-align: right;
        font-size: 14px;
        font-weight: 500;
        color: #303133;
      }

      :deep(.el-progress) {
        flex: 1;
      }
    }
  }
}

.judgment-section,
.compensation-section,
.timeline-section {
  .chart-card {
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

    h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #303133;
    }

    .chart-container {
      height: 280px;
    }

    .timeline-chart {
      height: 260px;
    }
  }

  .distribution-detail,
  .compensation-detail,
  .timeline-summary {
    background: #fff;
    border-radius: 16px;
    padding: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
    height: 100%;

    h4 {
      margin: 0 0 16px 0;
      font-size: 16px;
      color: #303133;
    }

    .detail-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .detail-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .detail-label {
        flex: 1;
        font-size: 14px;
        color: #606266;
      }

      .detail-value {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
      }

      .detail-percent {
        width: 50px;
        text-align: right;
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .range-list {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .range-item {
    .range-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;

      .range-label {
        font-size: 14px;
        color: #606266;
      }

      .range-count {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
      }
    }
  }

  .timeline-summary {
    .summary-item {
      margin-bottom: 16px;

      .summary-label {
        display: block;
        font-size: 13px;
        color: #909399;
        margin-bottom: 4px;
      }

      .summary-value {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }

    .court-dist-list {
      margin-top: 12px;
    }

    .court-dist-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
      font-size: 13px;

      &:last-child {
        border-bottom: none;
      }

      span:first-child {
        color: #606266;
      }

      span:last-child {
        color: #303133;
        font-weight: 500;
      }
    }
  }
}

.strategy-section {
  .strategy-card {
    border-radius: 16px;

    :deep(.el-card__header) {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border: none;

      .strategy-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 500;
      }
    }
  }

  .strategy-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .strategy-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 12px;
    transition: all 0.3s;

    &:hover {
      background: #ecf5ff;
      transform: translateX(4px);
    }

    .strategy-index {
      width: 28px;
      height: 28px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: bold;
      flex-shrink: 0;
    }

    .strategy-content {
      font-size: 14px;
      color: #606266;
      line-height: 1.6;
    }
  }
}
</style>
