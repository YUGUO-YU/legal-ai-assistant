<template>
  <div class="company-query">
    <div class="page-header">
      <div class="header-content">
        <h2>企业查询</h2>
        <p>查询企业工商信息、股东结构、司法风险等</p>
      </div>
    </div>

    <el-card class="search-card">
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

    <loading v-if="loading" text="正在查询企业信息..." />

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

      <el-row :gutter="24">
        <el-col :span="16">
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
        </el-col>

        <el-col :span="8">
          <el-card class="info-card" v-if="companyInfo.shareholders?.length">
            <template #header>
              <div class="card-header">
                <el-icon><User /></el-icon>
                <span>股东信息</span>
              </div>
            </template>
            <div class="shareholder-list">
              <div v-for="shareholder in companyInfo.shareholders" :key="shareholder.name" class="shareholder-item">
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
              <el-icon><OfficeBuilding /></el-icon>
              <span>{{ companyInfo.dataSource }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <empty-state
      v-else-if="hasSearched"
      icon="OfficeBuilding"
      title="未找到相关企业"
      description="未找到该企业信息，请检查输入是否正确"
      action-text="清除搜索"
      @action="companyName = ''; companyInfo = null; hasSearched = false"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
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
  Link
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const companyName = ref('')
const loading = ref(false)
const companyInfo = ref(null)
const hasSearched = ref(false)

const handleQuery = async () => {
  if (!companyName.value.trim()) {
    ElMessage.warning('请输入企业名称')
    return
  }

  loading.value = true
  hasSearched.value = true
  try {
    const res = await api.company.query({ companyName: companyName.value })
    companyInfo.value = res.data
  } catch (e) {
    console.error(e)
    ElMessage.error('查询失败，请稍后重试')
  } finally {
    loading.value = false
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
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05));
    border-radius: 12px;
    color: #667eea;
    font-size: 14px;
  }
}
</style>
