<template>
  <div class="shareholder-detail">
    <div class="page-header">
      <div class="header-nav">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回企业
        </el-button>
      </div>
      <div class="header-content">
        <h2>股东详情</h2>
        <p>查看股东出资、占比与关联公司</p>
      </div>
    </div>

    <loading v-if="loading" text="正在加载股东详情..." />

    <div v-else-if="shareholder" class="detail-container">
      <el-card class="summary-card">
        <div class="summary-row">
          <div class="shareholder-avatar">
            <el-icon :size="40"><UserFilled /></el-icon>
          </div>
          <div class="shareholder-meta">
            <h1>{{ shareholder.name }}</h1>
            <div class="meta-tags">
              <el-tag effect="dark" round>{{ shareholder.type || '股东' }}</el-tag>
              <el-tag type="info" effect="plain" round v-if="companyName">所属企业：{{ companyName }}</el-tag>
            </div>
          </div>
          <div class="ratio-badge">
            <el-progress
              type="circle"
              :width="110"
              :stroke-width="9"
              :percentage="ratioNumber"
              :color="getRatioColor(ratioNumber)"
            >
              <template #default>
                <span class="ratio-value">{{ shareholder.ratio || '0%' }}</span>
                <span class="ratio-label">出资比例</span>
              </template>
            </el-progress>
          </div>
        </div>
      </el-card>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="14">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Document /></el-icon>
                <span>出资信息</span>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="股东名称">{{ shareholder.name }}</el-descriptions-item>
              <el-descriptions-item label="股东类型">{{ shareholder.type || '自然人股东' }}</el-descriptions-item>
              <el-descriptions-item label="出资金额">{{ shareholder.capitalContribution || '-' }} 万元</el-descriptions-item>
              <el-descriptions-item label="出资比例">{{ shareholder.ratio || '-' }}</el-descriptions-item>
              <el-descriptions-item label="所属企业">{{ companyName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="统一社会信用代码">{{ companyInfo?.unifiedSocialCreditCode || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <el-card class="info-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <el-icon><DataAnalysis /></el-icon>
                <span>股权结构</span>
              </div>
            </template>
            <div class="shareholder-tree">
              <div class="tree-node root">
                <div class="node-tag">公司</div>
                <span class="node-name">{{ companyName || '-' }}</span>
              </div>
              <div class="tree-line"></div>
              <div
                v-for="(s, idx) in companyInfo?.shareholders || []"
                :key="idx"
                class="tree-leaf"
                :class="{ active: s.name === shareholder.name }"
              >
                <div class="node-tag" :class="{ active: s.name === shareholder.name }">股东{{ idx + 1 }}</div>
                <span class="node-name">{{ s.name }}</span>
                <span class="node-ratio">{{ s.ratio }}</span>
                <el-progress
                  :percentage="parseRatio(s.ratio)"
                  :stroke-width="4"
                  :show-text="false"
                  :color="s.name === shareholder.name ? '#667eea' : '#94a3b8'"
                  style="margin-top: 4px"
                />
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card class="info-card">
            <template #header>
              <div class="card-header">
                <el-icon><Warning /></el-icon>
                <span>风险提示</span>
              </div>
            </template>
            <div v-if="riskItems.length" class="risk-list">
              <div v-for="(r, idx) in riskItems" :key="idx" class="risk-item" :class="'risk-' + r.level.toLowerCase()">
                <el-tag :type="getRiskType(r.level)" effect="dark" size="small">{{ r.level }}</el-tag>
                <span class="risk-type">{{ r.type }}</span>
                <p class="risk-desc">{{ r.description }}</p>
              </div>
            </div>
            <empty-state v-else icon="CircleCheck" title="无明显风险" description="该股东当前无关联风险记录" />
          </el-card>

          <el-card class="info-card" style="margin-top: 20px">
            <template #header>
              <div class="card-header">
                <el-icon><Operation /></el-icon>
                <span>操作</span>
              </div>
            </template>
            <div class="action-list">
              <el-button type="primary" @click="goToCompanyDetail" style="width: 100%; margin-bottom: 10px">
                <el-icon><OfficeBuilding /></el-icon>
                查看企业完整信息
              </el-button>
              <el-button @click="copyShareholder" style="width: 100%; margin-bottom: 10px">
                <el-icon><CopyDocument /></el-icon>
                复制股东摘要
              </el-button>
              <el-button @click="exportDetail" style="width: 100%">
                <el-icon><Download /></el-icon>
                导出股东详情
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <empty-state
      v-else
      icon="User"
      title="未找到股东信息"
      description="该股东记录可能已过期或不存在"
      action-text="返回"
      @action="goBack"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  UserFilled,
  Document,
  DataAnalysis,
  Warning,
  Operation,
  OfficeBuilding,
  CopyDocument,
  Download,
  CircleCheck
} from '@element-plus/icons-vue'
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const companyInfo = ref(null)
const shareholder = ref(null)
const companyName = computed(() => companyInfo.value?.companyName || '')

const ratioNumber = computed(() => parseRatio(shareholder.value?.ratio))

const riskItems = computed(() => {
  if (!companyInfo.value?.riskWarnings) return []
  const list = companyInfo.value.riskWarnings.filter(r => {
    if (!r.description) return false
    return shareholder.value?.name && r.description.includes(shareholder.value.name)
  })
  return list.length ? list : companyInfo.value.riskWarnings.slice(0, 3)
})

const parseRatio = (ratio) => {
  if (!ratio) return 0
  const m = String(ratio).match(/(\d+(?:\.\d+)?)/)
  if (!m) return 0
  return Math.min(100, Math.max(0, parseFloat(m[1])))
}

const getRatioColor = (p) => {
  if (p >= 50) return '#ef4444'
  if (p >= 30) return '#f59e0b'
  if (p >= 10) return '#667eea'
  return '#10b981'
}

const getRiskType = (level) => {
  return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[level] || 'info'
}

const loadDetail = async () => {
  const companyUuid = route.params.companyUuid
  const shareholderName = decodeURIComponent(route.params.shareholderName || '')
  if (!companyUuid || !shareholderName) {
    ElMessage.error('参数缺失')
    router.back()
    return
  }
  loading.value = true
  try {
    const res = await api.company.getQuery(companyUuid)
    if (res.data) {
      companyInfo.value = res.data
      shareholder.value = (res.data.shareholders || []).find(s => s.name === shareholderName) || { name: shareholderName }
    } else {
      ElMessage.error('企业记录不存在')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/company')
  }
}

const goToCompanyDetail = () => {
  if (companyInfo.value?.queryUuid) {
    router.push(`/company-detail/${companyInfo.value.queryUuid}`)
  }
}

const copyShareholder = async () => {
  if (!shareholder.value) return
  const text = `【股东】${shareholder.value.name} | 类型 ${shareholder.value.type || '-'} | 出资 ${shareholder.value.capitalContribution || '-'} | 占比 ${shareholder.value.ratio || '-'} | 所属 ${companyName.value || '-'}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

const exportDetail = () => {
  if (!shareholder.value) return
  const lines = [
    `# 股东详情 - ${shareholder.value.name}`,
    '',
    `- 股东名称: ${shareholder.value.name}`,
    `- 股东类型: ${shareholder.value.type || '-'}`,
    `- 出资金额: ${shareholder.value.capitalContribution || '-'} 万元`,
    `- 出资比例: ${shareholder.value.ratio || '-'}`,
    `- 所属企业: ${companyName.value || '-'}`,
    `- 统一社会信用代码: ${companyInfo.value?.unifiedSocialCreditCode || '-'}`,
    ''
  ]
  const text = lines.join('\n')
  const blob = new Blob([text], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `股东详情_${shareholder.value.name}.md`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('已导出')
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
.shareholder-detail {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  margin-bottom: 24px;
  .header-nav :deep(.el-button) {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--color-text-secondary);
  }
  .header-content h2 {
    margin: 0 0 8px 0;
    font-size: 26px;
    font-weight: 600;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .header-content p { margin: 0; color: var(--color-text-secondary); font-size: 14px; }
}

.detail-container {
  .summary-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    :deep(.el-card__body) { padding: 24px; }

    .summary-row {
      display: flex;
      align-items: center;
      gap: 24px;
    }

    .shareholder-avatar {
      width: 72px;
      height: 72px;
      border-radius: 16px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      flex-shrink: 0;
    }

    .shareholder-meta {
      flex: 1;
      h1 { margin: 0 0 12px 0; font-size: 22px; color: var(--color-text-primary); }
    }

    .meta-tags { display: flex; gap: 8px; flex-wrap: wrap; }

    .ratio-badge {
      :deep(.ratio-value) { font-size: 24px; font-weight: 700; color: var(--color-text-primary); display: block; text-align: center; }
      :deep(.ratio-label) { font-size: 11px; color: var(--color-text-secondary); display: block; text-align: center; }
    }
  }

  .info-card {
    border: none;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    :deep(.el-card__header) { padding: 16px 20px; }
    :deep(.el-card__body) { padding: 16px 20px 20px; }
  }

  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text-primary);
    .el-icon { color: #667eea; }
  }

  .shareholder-tree {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    .tree-node, .tree-leaf {
      background: #f9fafb;
      border-radius: 10px;
      padding: 10px 14px;
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 4px;
      transition: all 0.3s;
    }

    .tree-node.root {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
      .node-tag { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
      .node-name { font-weight: 600; color: var(--color-text-primary); }
    }

    .tree-leaf {
      cursor: default;
      &.active {
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1));
        border-left: 3px solid #667eea;
      }
      .node-tag { background: #e5e7eb; color: var(--color-text-secondary); }
      .node-tag.active { background: #667eea; color: #fff; }
      .node-name { flex: 1; color: var(--color-text-primary); }
      .node-ratio { color: #667eea; font-weight: 600; font-size: 13px; }
    }

    .tree-line {
      width: 2px;
      height: 16px;
      background: #d1d5db;
      margin-left: 24px;
    }
  }

  .risk-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .risk-item {
    background: #f9fafb;
    border-radius: 10px;
    padding: 10px 14px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
    border-left: 3px solid #d1d5db;

    &.risk-high { border-color: #ef4444; }
    &.risk-medium { border-color: #f59e0b; }
    &.risk-low { border-color: #10b981; }

    .risk-type { font-weight: 500; color: var(--color-text-primary); font-size: 13px; }
    .risk-desc { width: 100%; font-size: 12px; color: var(--color-text-secondary); margin: 4px 0 0 0; }
  }

  .action-list {
    display: flex;
    flex-direction: column;
  }
}
</style>
