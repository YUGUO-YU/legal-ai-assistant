<template>
  <div class="company-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回查询
        </el-button>
      </div>
      <div class="header-content">
        <h2>企业详情</h2>
        <p>查看工商信息、股东结构、风险预警</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载企业详情..." />

    <div v-else-if="company" class="detail-container">
      <el-card class="info-card overview-card">
        <div class="overview-row">
          <div class="logo">
            <el-icon :size="32"><OfficeBuilding /></el-icon>
          </div>
          <div class="meta">
            <h1>{{ company.companyName }}</h1>
            <div class="meta-tags">
              <el-tag :type="getStatusType(company.businessStatus)" effect="dark" round>
                {{ company.businessStatus }}
              </el-tag>
              <el-tag type="info" effect="plain" round>{{ company.unifiedSocialCreditCode }}</el-tag>
            </div>
          </div>
          <div class="risk-summary">
            <div class="risk-item high">
              <span class="value">{{ countByLevel('HIGH') }}</span>
              <span class="label">高</span>
            </div>
            <div class="risk-item medium">
              <span class="value">{{ countByLevel('MEDIUM') }}</span>
              <span class="label">中</span>
            </div>
            <div class="risk-item low">
              <span class="value">{{ countByLevel('LOW') }}</span>
              <span class="label">低</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="14">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>工商信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="法定代表人">{{ company.legalRepresentative }}</el-descriptions-item>
              <el-descriptions-item label="注册资本">{{ company.registeredCapital }} 万元</el-descriptions-item>
              <el-descriptions-item label="成立日期">{{ company.establishDate }}</el-descriptions-item>
              <el-descriptions-item label="登记机关">{{ company.registrationAuthority }}</el-descriptions-item>
              <el-descriptions-item label="经营状态" :span="2">{{ company.businessStatus }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="info-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <el-icon><Warning /></el-icon>
                <span>风险预警</span>
                <el-tag :type="getRiskType(company.riskLevel)" effect="dark" size="small">
                  {{ getRiskLabel(company.riskLevel) }}
                </el-tag>
              </div>
            </template>
            <div v-if="company.riskWarnings?.length" class="risk-list">
              <div
                v-for="(r, idx) in company.riskWarnings"
                :key="idx"
                class="risk-block"
                :class="'level-' + r.level.toLowerCase()"
              >
                <div class="risk-block-head">
                  <el-tag :type="getRiskType(r.level)" effect="dark" size="small">{{ getRiskLabel(r.level) }}</el-tag>
                  <span class="risk-type">{{ r.type }}</span>
                </div>
                <p class="risk-desc">{{ r.description }}</p>
                <span v-if="r.date" class="risk-date">{{ r.date }}</span>
              </div>
            </div>
            <empty-state v-else icon="CircleCheck" title="无风险记录" description="当前企业暂无风险预警" />
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><User /></el-icon>
                <span>股东结构</span>
                <el-button type="primary" link size="small" @click="goShareholders">查看全部</el-button>
              </div>
            </template>
            <div v-if="company.shareholders?.length" class="shareholder-list">
              <div
                v-for="(s, idx) in company.shareholders"
                :key="idx"
                class="shareholder-row"
                @click="goShareholder(s.name)"
              >
                <div class="avatar">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <div class="info">
                  <div class="name">{{ s.name }}</div>
                  <div class="meta">
                    <span>出资 {{ s.capitalContribution }} 万元</span>
                    <span class="divider">|</span>
                    <span>{{ s.ratio }}</span>
                  </div>
                </div>
                <el-icon class="arrow"><Right /></el-icon>
              </div>
            </div>
            <empty-state v-else icon="User" title="暂无股东信息" />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <empty-state v-else icon="OfficeBuilding" title="未找到企业信息" action-text="返回" @action="goBack" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  OfficeBuilding,
  Document,
  Warning,
  User,
  UserFilled,
  Right,
  CircleCheck
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const company = ref(null)

const loadDetail = async () => {
  const uuid = route.params.companyUuid
  if (!uuid) {
    ElMessage.error('参数缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await api.company.getQuery(uuid)
    if (res.data) {
      company.value = res.data
    } else {
      ElMessage.error('企业记录不存在')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/company')
}

const goShareholder = (name) => {
  if (company.value?.queryUuid) {
    router.push(`/shareholder-detail/${company.value.queryUuid}/${encodeURIComponent(name)}`)
  }
}

const goShareholders = () => {
  ElMessage.info('已在上方展示全部股东')
}

const countByLevel = (level) => {
  if (!company.value?.riskWarnings) return 0
  return company.value.riskWarnings.filter(r => r.level === level).length
}

const getStatusType = (s) => {
  if (!s) return 'info'
  if (s.includes('存续')) return 'success'
  if (s.includes('注销')) return 'info'
  if (s.includes('吊销') || s.includes('异常')) return 'danger'
  return 'warning'
}

const getRiskType = (l) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[l] || 'info')
const getRiskLabel = (l) => ({ HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[l] || '未知')

onMounted(loadDetail)
</script>

<style lang="scss" scoped>
.company-detail {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: 24px;
  .header-nav :deep(.el-button) {
    display: inline-flex; align-items: center; gap: 6px; color: #6b7280;
  }
  .header-content h2 {
    margin: 0 0 8px 0; font-size: 26px; font-weight: 600;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  }
  .header-content p { margin: 0; color: #6b7280; font-size: 14px; }
}

.detail-container {
  .info-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    :deep(.el-card__header) { padding: 16px 20px; }
    :deep(.el-card__body) { padding: 16px 20px 20px; }
  }

  .overview-card {
    :deep(.el-card__body) { padding: 24px; }
    .overview-row {
      display: flex; align-items: center; gap: 20px;
    }
    .logo {
      width: 64px; height: 64px; border-radius: 14px;
      background: linear-gradient(135deg, #fa709a, #fee140);
      display: flex; align-items: center; justify-content: center; color: #fff;
    }
    .meta { flex: 1; h1 { margin: 0 0 10px 0; font-size: 22px; color: #1f2937; } }
    .meta-tags { display: flex; gap: 8px; flex-wrap: wrap; }
    .risk-summary { display: flex; gap: 8px; }
    .risk-item {
      padding: 8px 14px; border-radius: 10px; display: flex; flex-direction: column; align-items: center;
      min-width: 56px;
      .value { font-size: 18px; font-weight: 700; }
      .label { font-size: 11px; }
      &.high { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
      &.medium { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
      &.low { background: rgba(16, 185, 129, 0.1); color: #10b981; }
    }
  }

  .card-header {
    display: flex; align-items: center; gap: 8px;
    font-size: 15px; font-weight: 600; color: #1f2937;
    .el-icon { color: #667eea; }
  }

  .risk-list { display: flex; flex-direction: column; gap: 12px; }
  .risk-block {
    background: #f9fafb; border-radius: 10px; padding: 12px 14px;
    border-left: 3px solid #d1d5db;
    &.level-high { border-color: #ef4444; }
    &.level-medium { border-color: #f59e0b; }
    &.level-low { border-color: #10b981; }
    .risk-block-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .risk-type { font-weight: 500; color: #1f2937; font-size: 13px; }
    .risk-desc { margin: 4px 0; font-size: 13px; color: #4b5563; line-height: 1.6; }
    .risk-date { font-size: 12px; color: #9ca3af; }
  }

  .shareholder-list { display: flex; flex-direction: column; gap: 8px; }
  .shareholder-row {
    display: flex; align-items: center; gap: 12px;
    padding: 12px 14px; border-radius: 10px; background: #f9fafb;
    cursor: pointer; transition: all 0.3s;
    &:hover { background: linear-gradient(135deg, rgba(102,126,234,0.08), rgba(118,75,162,0.08)); transform: translateX(2px); }
    .avatar {
      width: 40px; height: 40px; border-radius: 10px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      display: flex; align-items: center; justify-content: center; color: #fff;
    }
    .info { flex: 1; }
    .name { font-size: 14px; color: #1f2937; font-weight: 500; }
    .meta { font-size: 12px; color: #6b7280; margin-top: 2px; display: flex; gap: 6px; align-items: center; }
    .divider { color: #d1d5db; }
    .arrow { color: #9ca3af; }
  }
}
</style>
