<template>
  <div class="company-query">
    <div class="page-header">
      <div class="header-content">
        <h2>企业查询</h2>
        <p>查询企业工商信息、股东结构、司法风险等</p>
      </div>
    </div>

    <el-card class="search-card">
      <div v-if="queryHistory.length > 0" class="query-history">
        <div class="history-label">最近查询</div>
        <div class="history-items">
          <el-tag v-for="h in queryHistory.slice(0, 5)" :key="h.timestamp" @click="restoreQuery(h)" class="history-item">
            {{ h.name }}
          </el-tag>
        </div>
      </div>
      <div class="search-box">
        <div class="search-input-wrapper">
          <el-icon class="search-icon"><OfficeBuilding /></el-icon>
          <el-input
            v-model="companyName"
            placeholder="请输入企业名称或统一社会信用代码"
            size="large"
            @keyup.enter="handleQuery"
          />
          <el-button type="primary" class="search-btn" :loading="loading" @click="handleQuery">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-if="loading" class="search-progress">
      <el-steps v-if="loading" :active="queryStep" finish-status="success" simple style="margin: 20px 0;">
        <el-step title="输入验证" />
        <el-step title="API查询" />
        <el-step title="数据分析" />
        <el-step title="结果展示" />
      </el-steps>
    </div>

    <template v-else-if="companyInfo">
      <el-card class="info-card company-header-card">
        <div class="company-header">
          <div class="company-main">
            <div class="company-logo">
              <el-icon :size="32"><OfficeBuilding /></el-icon>
            </div>
            <div class="company-detail">
              <h3>{{ companyInfo.companyName }}</h3>
              <div class="company-tags">
                <el-tag :type="getStatusType(companyInfo.businessStatus)" effect="dark" round>
                  {{ companyInfo.businessStatus }}
                </el-tag>
                <el-tag type="info" effect="plain" round>
                  {{ companyInfo.unifiedSocialCreditCode }}
                </el-tag>
              </div>
            </div>
          </div>
          <div class="risk-overview">
            <div class="risk-item" :class="getRiskClass('HIGH')">
              <span class="risk-value">{{ getRiskCount('HIGH') }}</span>
              <span class="risk-label">高风险</span>
            </div>
            <div class="risk-item" :class="getRiskClass('MEDIUM')">
              <span class="risk-value">{{ getRiskCount('MEDIUM') }}</span>
              <span class="risk-label">中风险</span>
            </div>
            <div class="risk-item" :class="getRiskClass('LOW')">
              <span class="risk-value">{{ getRiskCount('LOW') }}</span>
              <span class="risk-label">低风险</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-tabs v-model="activeTab" class="result-tabs">
        <el-tab-pane label="基本信息" name="basic">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>基本信息</span>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <label>法定代表人</label>
                <span>{{ companyInfo.legalRepresentative }}</span>
              </div>
              <div class="info-item">
                <label>注册资本</label>
                <span>{{ companyInfo.registeredCapital }}万元</span>
              </div>
              <div class="info-item">
                <label>成立日期</label>
                <span>{{ companyInfo.establishDate }}</span>
              </div>
              <div class="info-item">
                <label>登记机关</label>
                <span>{{ companyInfo.registrationAuthority }}</span>
              </div>
              <div class="info-item">
                <label>企业类型</label>
                <span>{{ companyInfo.companyType || '有限责任公司' }}</span>
              </div>
              <div class="info-item">
                <label>经营状态</label>
                <span>{{ companyInfo.businessStatus }}</span>
              </div>
            </div>
          </el-card>

          <el-card class="info-card" v-if="companyInfo.riskWarnings?.length">
            <template #header>
              <div class="card-header">
                <el-icon><Warning /></el-icon>
                <span>风险预警</span>
              </div>
            </template>
            <div class="risk-list">
              <div v-for="warning in companyInfo.riskWarnings" :key="warning.type" class="risk-item" :class="getRiskItemClass(warning.level)">
                <div class="risk-icon">
                  <el-icon v-if="warning.level === 'HIGH'"><WarningFilled /></el-icon>
                  <el-icon v-else-if="warning.level === 'MEDIUM'"><Warning /></el-icon>
                  <el-icon v-else><InfoFilled /></el-icon>
                </div>
                <div class="risk-content">
                  <div class="risk-header">
                    <el-tag :type="getRiskType(warning.level)" size="small" effect="dark">
                      {{ getRiskLabel(warning.level) }}
                    </el-tag>
                    <span class="risk-type">{{ warning.type }}</span>
                  </div>
                  <p class="risk-desc">{{ warning.description }}</p>
                  <span class="risk-date">{{ warning.date }}</span>
                </div>
              </div>
            </div>
          </el-card>

          <el-card class="info-card" v-if="companyInfo.shareholders?.length">
            <template #header>
              <div class="card-header">
                <el-icon><User /></el-icon>
                <span>股东信息</span>
                <el-button type="primary" link size="small" @click="goCompanyDetail">查看详情</el-button>
              </div>
            </template>
            <div class="shareholder-list">
              <div
                v-for="shareholder in companyInfo.shareholders"
                :key="shareholder.name"
                class="shareholder-item"
                @click="goShareholder(shareholder.name)"
              >
                <div class="shareholder-avatar">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <div class="shareholder-info">
                  <span class="shareholder-name">{{ shareholder.name }}</span>
                  <div class="shareholder-detail">
                    <span>出资 {{ shareholder.capitalContribution }}万元</span>
                    <span class="divider">|</span>
                    <span>{{ shareholder.ratio }}</span>
                  </div>
                </div>
                <el-icon class="shareholder-arrow"><Right /></el-icon>
              </div>
            </div>
          </el-card>

          <el-card class="info-card data-source">
            <template #header>
              <div class="card-header">
                <el-icon><Link /></el-icon>
                <span>数据来源</span>
              </div>
            </template>
            <div class="source-info">
              <div class="source-main">
                <el-icon><OfficeBuilding /></el-icon>
                <span>{{ companyInfo.dataSource }}</span>
              </div>
              <div v-if="companyInfo.searchSources?.length" class="source-list">
                <div v-for="(src, idx) in companyInfo.searchSources" :key="idx" class="source-item">
                  <el-tag size="small" type="info" effect="plain">{{ src }}</el-tag>
                </div>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="司法风险" name="legal">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Warning /></el-icon>
                <span>司法风险</span>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <label>失信被执行数</label>
                <span>{{ companyInfo.dishonestCount ?? '暂无数据' }}</span>
              </div>
              <div class="info-item">
                <label>法律诉讼数</label>
                <span>{{ companyInfo.litigationCount ?? '暂无数据' }}</span>
              </div>
              <div class="info-item">
                <label>限消数据</label>
                <span>{{ companyInfo.restrictedConsumerCount ?? '暂无数据' }}</span>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="经营状况" name="business">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><OfficeBuilding /></el-icon>
                <span>经营状况</span>
              </div>
            </template>
            <div class="info-grid">
              <div class="info-item">
                <label>商标数</label>
                <span>{{ companyInfo.trademarkCount ?? '暂无数据' }}</span>
              </div>
              <div class="info-item">
                <label>专利数</label>
                <span>{{ companyInfo.patentCount ?? '暂无数据' }}</span>
              </div>
              <div class="info-item">
                <label>招聘趋势</label>
                <span>{{ companyInfo.recruitmentTrend ?? '暂无数据' }}</span>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="经营分析" name="analysis" v-if="companyInfo.businessAnalysis">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><DataAnalysis /></el-icon>
                <span>经营分析</span>
              </div>
            </template>
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="analysis-item">
                  <div class="analysis-label">员工规模</div>
                  <div class="analysis-value">{{ companyInfo.businessAnalysis.employeeCount || '-' }} 人</div>
                  <div class="analysis-trend" :class="getTrendClass(companyInfo.businessAnalysis.employeeTrend)">
                    <el-icon v-if="companyInfo.businessAnalysis.employeeTrend === '上升'"><Top /></el-icon>
                    <el-icon v-else-if="companyInfo.businessAnalysis.employeeTrend === '下降'"><Bottom /></el-icon>
                    {{ companyInfo.businessAnalysis.employeeTrend || '-' }}
                  </div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="analysis-item">
                  <div class="analysis-label">实缴资本</div>
                  <div class="analysis-value">{{ companyInfo.businessAnalysis.paidInCapital || '-' }} 万元</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="analysis-item">
                  <div class="analysis-label">所属行业</div>
                  <div class="analysis-value">{{ companyInfo.businessAnalysis.industry || '-' }}</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="analysis-item">
                  <div class="analysis-label">行业水平</div>
                  <div class="analysis-value">{{ companyInfo.businessAnalysis.industryAvgRatio || '-' }}</div>
                </div>
              </el-col>
            </el-row>
            <el-row :gutter="20" style="margin-top: 20px">
              <el-col :span="8">
                <div class="ip-item">
                  <el-icon><Document /></el-icon>
                  <span>专利 {{ companyInfo.businessAnalysis.patentCount || 0 }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="ip-item">
                  <el-icon><Collection /></el-icon>
                  <span>商标 {{ companyInfo.businessAnalysis.trademarkCount || 0 }}</span>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="ip-item">
                  <el-icon><Reading /></el-icon>
                  <span>著作权 {{ companyInfo.businessAnalysis.copyrightCount || 0 }}</span>
                </div>
              </el-col>
            </el-row>
            <div class="business-scope" v-if="companyInfo.businessAnalysis.businessScope">
              <div class="scope-label">经营范围</div>
              <div class="scope-content">{{ companyInfo.businessAnalysis.businessScope }}</div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="股权穿透" name="equity" v-if="companyInfo.equityChain">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Connection /></el-icon>
                <span>股权穿透</span>
              </div>
            </template>
            <div class="equity-chain">
              <div
                v-for="(node, idx) in companyInfo.equityChain"
                :key="idx"
                class="equity-node"
                :class="{ 'is-target': node.isTarget }"
              >
                <div class="node-header">
                  <span class="node-name">{{ node.name }}</span>
                  <el-tag size="small" :type="node.isTarget ? 'primary' : 'info'">
                    {{ node.isTarget ? '目标公司' : node.holdingRatio }}
                  </el-tag>
                </div>
                <div class="node-meta" v-if="node.actualControl">
                  实际控制人：{{ node.actualControl }}
                </div>
                <div v-if="idx < companyInfo.equityChain.length - 1" class="node-arrow">
                  <el-icon><Bottom /></el-icon>
                </div>
              </div>
            </div>
          </el-card>
        </el-tab-pane>

        <el-tab-pane label="关联方" name="related" v-if="companyInfo.relatedCompanies?.length">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><OfficeBuilding /></el-icon>
                <span>关联方</span>
              </div>
            </template>
            <div class="related-list">
              <div
                v-for="(rc, idx) in companyInfo.relatedCompanies"
                :key="idx"
                class="related-item"
              >
                <div class="related-icon">
                  <el-icon><OfficeBuilding /></el-icon>
                </div>
                <div class="related-info">
                  <div class="related-name">{{ rc.name }}</div>
                  <div class="related-meta">
                    <el-tag size="small" type="info">{{ rc.relation }}</el-tag>
                    <span class="divider">|</span>
                    <span>{{ rc.businessStatus }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </template>

    <empty-state
      v-else-if="hasSearched"
      icon="OfficeBuilding"
      title="未找到相关企业"
      description="未找到该企业信息，请检查输入是否正确"
      action-text="清除搜索"
      @action="companyName = ''; companyInfo = null; hasSearched = false; queryStep = 0"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  OfficeBuilding,
  Search,
  Document,
  Warning,
  WarningFilled,
  InfoFilled,
  User,
  UserFilled,
  Link,
  Right,
  Top,
  Bottom,
  DataAnalysis,
  Reading,
  Collection,
  Connection
} from '@element-plus/icons-vue'
import api from '../api'
import EmptyState from '../components/EmptyState.vue'
import { useUsageMemory } from '@/composables/useUsageMemory'

const { addRecord } = useUsageMemory()

const router = useRouter()
const companyName = ref('')
const loading = ref(false)
const searchStep = ref(0)
const queryStep = ref(0)
const companyInfo = ref(null)
const hasSearched = ref(false)
const activeTab = ref('basic')
const queryHistory = ref(JSON.parse(localStorage.getItem('companyQueryHistory') || '[]'))

const restoreQuery = (item) => {
  companyName.value = item.name
  handleQuery()
}

const goShareholder = (name) => {
  if (!companyInfo.value?.queryUuid) {
    ElMessage.warning('请先完成查询')
    return
  }
  router.push(`/shareholder-detail/${companyInfo.value.queryUuid}/${encodeURIComponent(name)}`)
}

const goCompanyDetail = () => {
  if (!companyInfo.value?.queryUuid) {
    ElMessage.warning('请先完成查询')
    return
  }
  router.push(`/company-detail/${companyInfo.value.queryUuid}`)
}

const handleQuery = async () => {
  if (!companyName.value.trim()) {
    ElMessage.warning('请输入企业名称')
    return
  }

  loading.value = true
  queryStep.value = 0
  hasSearched.value = true
  try {
    queryStep.value = 1
    const res = await api.company.query({ companyName: companyName.value })
    queryStep.value = 2
    companyInfo.value = res.data
    queryStep.value = 3
    const historyItem = {
      name: companyName.value,
      timestamp: Date.now(),
      riskLevel: res.data.riskLevel || 'unknown'
    }
    const existing = queryHistory.value.filter(h => h.name !== companyName.value)
    queryHistory.value = [historyItem, ...existing].slice(0, 10)
    localStorage.setItem('companyQueryHistory', JSON.stringify(queryHistory.value))
    addRecord('company', `查询"${companyName.value}"`, `风险等级：${res.data.riskLevel || '未知'}`)
  } catch (e) {
    console.error(e)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
    queryStep.value = 0
  }
}

const getStatusType = (status) => {
  return status === '存续' ? 'success' : 'warning'
}

const getRiskType = (level) => {
  return level === 'HIGH' ? 'danger' : level === 'MEDIUM' ? 'warning' : 'success'
}

const getRiskLabel = (level) => {
  return level === 'HIGH' ? '高风险' : level === 'MEDIUM' ? '中风险' : '低风险'
}

const getRiskCount = (level) => {
  if (!companyInfo.value?.riskWarnings) return 0
  return companyInfo.value.riskWarnings.filter(w => w.level === level).length
}

const getRiskClass = (level) => {
  const count = getRiskCount(level)
  if (level === 'HIGH') return count > 0 ? 'high-active' : ''
  if (level === 'MEDIUM') return count > 0 ? 'medium-active' : ''
  return count > 0 ? 'low-active' : ''
}

const getRiskItemClass = (level) => {
  return level === 'HIGH' ? 'risk-high' : level === 'MEDIUM' ? 'risk-medium' : 'risk-low'
}

const getTrendClass = (trend) => {
  if (trend === '上升') return 'trend-up'
  if (trend === '下降') return 'trend-down'
  return 'trend-stable'
}
</script>

<style lang="scss" scoped>
.company-query {
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

.search-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.query-history {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f3f4f6;

  .history-label {
    font-size: 12px;
    color: #9ca3af;
    margin-bottom: 10px;
  }

  .history-items {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .history-item {
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        opacity: 0.8;
      }
    }
  }
}

.search-box {
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
        font-size: 15px;
      }
    }

    .search-btn {
      height: 44px;
      padding: 0 28px;
      border-radius: 10px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
      }
    }
  }
}

.info-card {
  border: none;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;

  :deep(.el-card__header) {
    padding: 18px 24px;
    border-bottom: 1px solid #f3f4f6;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.result-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    height: 2px;
  }

  :deep(.el-tabs__item) {
    font-size: 16px;
    font-weight: 600;
    padding: 0 24px;
    height: 48px;
    line-height: 48px;

    &.is-active {
      color: #667eea;
    }
  }

  :deep(.el-tabs__active-bar) {
    height: 3px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 3px;
  }

  :deep(.el-tabs__nav) {
    &::before {
      display: none;
    }
  }
}

.company-header-card {
  :deep(.el-card__body) {
    padding: 24px;
  }

  .company-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .company-main {
      display: flex;
      gap: 20px;

      .company-logo {
        width: 72px;
        height: 72px;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
        border-radius: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #667eea;
      }

      .company-detail {
        h3 {
          margin: 0 0 12px 0;
          font-size: 22px;
          font-weight: 600;
          color: #1f2937;
        }

        .company-tags {
          display: flex;
          gap: 10px;
        }
      }
    }

    .risk-overview {
      display: flex;
      gap: 24px;

      .risk-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 16px 24px;
        background: #f9fafb;
        border-radius: 14px;
        min-width: 80px;

        .risk-value {
          font-size: 28px;
          font-weight: 700;
          color: #1f2937;
        }

        .risk-label {
          font-size: 12px;
          color: #6b7280;
          margin-top: 4px;
        }

        &.high-active {
          background: rgba(239, 68, 68, 0.1);

          .risk-value {
            color: #ef4444;
          }
        }

        &.medium-active {
          background: rgba(245, 158, 11, 0.1);

          .risk-value {
            color: #f59e0b;
          }
        }

        &.low-active {
          background: rgba(16, 185, 129, 0.1);

          .risk-value {
            color: #10b981;
          }
        }
      }
    }
  }
}

.card-header {
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

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 6px;

    label {
      font-size: 13px;
      color: #6b7280;
    }

    span {
      font-size: 15px;
      font-weight: 500;
      color: #1f2937;
    }
  }
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .risk-item {
    display: flex;
    gap: 16px;
    padding: 16px;
    border-radius: 14px;
    border-left: 4px solid;

    &.risk-high {
      background: rgba(239, 68, 68, 0.05);
      border-color: #ef4444;

      .risk-icon {
        color: #ef4444;
      }
    }

    &.risk-medium {
      background: rgba(245, 158, 11, 0.05);
      border-color: #f59e0b;

      .risk-icon {
        color: #f59e0b;
      }
    }

    &.risk-low {
      background: rgba(16, 185, 129, 0.05);
      border-color: #10b981;

      .risk-icon {
        color: #10b981;
      }
    }

    .risk-icon {
      font-size: 20px;
      margin-top: 2px;
    }

    .risk-content {
      flex: 1;

      .risk-header {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 8px;

        .risk-type {
          font-size: 15px;
          font-weight: 500;
          color: #1f2937;
        }
      }

      .risk-desc {
        margin: 0 0 8px 0;
        font-size: 14px;
        color: #6b7280;
        line-height: 1.6;
      }

      .risk-date {
        font-size: 12px;
        color: #9ca3af;
      }
    }
  }
}

.shareholder-list {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .shareholder-item {
    display: flex;
    gap: 14px;
    padding: 12px;
    background: #f9fafb;
    border-radius: 12px;
    cursor: pointer;
    align-items: center;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.08), rgba(118, 75, 162, 0.08));
      transform: translateX(2px);

      .shareholder-arrow { color: #667eea; transform: translateX(0); opacity: 1; }
    }

    .shareholder-arrow {
      color: #d1d5db;
      opacity: 0;
      transform: translateX(-4px);
      transition: all 0.3s;
    }

    .shareholder-avatar {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .shareholder-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: center;

      .shareholder-name {
        font-size: 15px;
        font-weight: 500;
        color: #1f2937;
        margin-bottom: 4px;
      }

      .shareholder-detail {
        font-size: 13px;
        color: #6b7280;

        .divider {
          margin: 0 8px;
          color: #d1d5db;
        }
      }
    }
  }
}

.data-source {
  .source-info {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
    border-radius: 12px;
    color: #667eea;
    font-size: 14px;

    .source-main {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .source-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      .source-item {
        :deep(.el-tag) {
          font-size: 12px;
        }
      }
    }
  }
}

.search-progress {
  animation: fadeIn 0.3s ease;

  .progress-card {
    padding: 24px;
  }

  .progress-steps {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0;
    margin-bottom: 16px;
  }

  .progress-step {
    display: flex;
    align-items: center;
    gap: 10px;
    opacity: 0.35;
    transition: opacity 0.4s ease;

    &.active {
      opacity: 1;

      .step-number {
        background: linear-gradient(135deg, #667eea, #764ba2);
        color: #fff;
        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
      }

      span {
        color: #667eea;
        font-weight: 500;
      }
    }

    .step-number {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      background: #e5e7eb;
      color: #9ca3af;
      transition: all 0.4s ease;
    }

    span {
      font-size: 14px;
      color: #9ca3af;
      transition: color 0.4s ease;
    }
  }

  .step-connector {
    width: 60px;
    height: 2px;
    background: #e5e7eb;
    margin: 0 12px;
    transition: background 0.4s ease;

    &.active {
      background: linear-gradient(90deg, #667eea, #764ba2);
    }
  }

  .progress-tip {
    text-align: center;
    font-size: 12px;
    color: #9ca3af;
  }

  .analysis-item {
    background: #f9fafb; border-radius: 12px; padding: 16px; text-align: center;
    .analysis-label { font-size: 12px; color: #9ca3af; margin-bottom: 8px; }
    .analysis-value { font-size: 20px; font-weight: 700; color: #1f2937; }
    .analysis-trend {
      font-size: 12px; margin-top: 6px; display: flex; align-items: center; justify-content: center; gap: 4px;
      &.trend-up { color: #10b981; }
      &.trend-down { color: #ef4444; }
      &.trend-stable { color: #9ca3af; }
    }
  }

  .ip-item {
    display: flex; align-items: center; gap: 8px; padding: 12px;
    background: #f9fafb; border-radius: 10px; justify-content: center;
    color: #6b7280; font-size: 14px;
    .el-icon { color: #667eea; font-size: 18px; }
  }

  .business-scope {
    margin-top: 20px; padding: 16px; background: #f9fafb; border-radius: 12px;
    .scope-label { font-size: 13px; color: #9ca3af; margin-bottom: 8px; }
    .scope-content { font-size: 14px; color: #4b5563; line-height: 1.8; }
  }

  .equity-chain {
    display: flex; flex-direction: column; align-items: center; gap: 0;
    .equity-node {
      background: #f9fafb; border-radius: 12px; padding: 16px 24px; min-width: 300px;
      text-align: center; position: relative;
      &.is-target { background: linear-gradient(135deg, rgba(102,126,234,0.1), rgba(118,75,162,0.1)); border: 2px solid #667eea; }
      .node-header { display: flex; align-items: center; justify-content: center; gap: 10px; }
      .node-name { font-size: 16px; font-weight: 600; color: #1f2937; }
      .node-meta { font-size: 13px; color: #6b7280; margin-top: 8px; }
      .node-arrow { color: #667eea; margin-top: 8px; }
    }
  }

  .related-list { display: flex; flex-direction: column; gap: 10px; }
  .related-item {
    display: flex; align-items: center; gap: 12px;
    padding: 12px 14px; border-radius: 10px; background: #f9fafb;
    .related-icon {
      width: 40px; height: 40px; border-radius: 10px;
      background: linear-gradient(135deg, #10b981, #059669);
      display: flex; align-items: center; justify-content: center; color: #fff;
    }
    .related-info { flex: 1; }
    .related-name { font-size: 14px; color: #1f2937; font-weight: 500; }
    .related-meta { font-size: 12px; color: #6b7280; margin-top: 4px; display: flex; align-items: center; gap: 6px; }
  }
}
</style>
