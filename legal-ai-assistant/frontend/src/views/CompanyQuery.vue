<template>
  <div class="page-card">
    <div class="page-header">
      <h2>企业查询</h2>
      <p>查询企业工商信息、股东结构、司法风险等</p>
    </div>

    <div class="search-box">
      <el-input
        v-model="companyName"
        placeholder="请输入企业名称或统一社会信用代码"
        size="large"
        @keyup.enter="handleQuery"
      >
        <template #append>
          <el-button :icon="Search" @click="handleQuery" :loading="loading">查询</el-button>
        </template>
      </el-input>
    </div>

    <loading v-if="loading" text="正在查询企业信息..." />

    <template v-else-if="companyInfo">
      <el-card class="info-card">
        <template #header>
          <div class="card-header">
            <span>{{ companyInfo.companyName }}</span>
            <el-tag :type="getStatusType(companyInfo.businessStatus)">
              {{ companyInfo.businessStatus }}
            </el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="统一社会信用代码">
            {{ companyInfo.unifiedSocialCreditCode }}
          </el-descriptions-item>
          <el-descriptions-item label="法定代表人">
            {{ companyInfo.legalRepresentative }}
          </el-descriptions-item>
          <el-descriptions-item label="注册资本">
            {{ companyInfo.registeredCapital }}万元
          </el-descriptions-item>
          <el-descriptions-item label="成立日期">
            {{ companyInfo.establishDate }}
          </el-descriptions-item>
          <el-descriptions-item label="登记机关">
            {{ companyInfo.registrationAuthority }}
          </el-descriptions-item>
          <el-descriptions-item label="数据来源">
            {{ companyInfo.dataSource }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="info-card" v-if="companyInfo.shareholders?.length">
        <template #header>
          <span>股东信息</span>
        </template>
        <el-table :data="companyInfo.shareholders">
          <el-table-column prop="name" label="股东" />
          <el-table-column prop="capitalContribution" label="出资额" />
          <el-table-column prop="ratio" label="持股比例" />
        </el-table>
      </el-card>

      <el-card class="info-card" v-if="companyInfo.riskWarnings?.length">
        <template #header>
          <span>风险预警</span>
        </template>
        <div v-for="warning in companyInfo.riskWarnings" :key="warning.type" class="risk-item">
          <el-tag :type="getRiskType(warning.level)" size="small">
            {{ warning.level === 'HIGH' ? '高风险' : warning.level === 'MEDIUM' ? '中风险' : '低风险' }}
          </el-tag>
          <span class="risk-type">{{ warning.type }}</span>
          <span class="risk-desc">{{ warning.description }}</span>
          <span class="risk-date">{{ warning.date }}</span>
        </div>
      </el-card>
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
import api from '../api'
import Loading from '../components/Loading.vue'
import EmptyState from '../components/EmptyState.vue'

const companyName = ref('')
const loading = ref(false)
const companyInfo = ref(null)
const hasSearched = ref(false)

const handleQuery = async () => {
  if (!companyName.value.trim()) return

  loading.value = true
  hasSearched.value = true
  try {
    const res = await api.company.query({ companyName: companyName.value })
    companyInfo.value = res.data
  } catch (e) {
    console.error(e)
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
</script>

<style lang="scss" scoped>
.company-detail {
  .info-card {
    margin-bottom: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.risk-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  .risk-type {
    font-weight: 500;
  }
  .risk-desc {
    flex: 1;
    color: #666;
  }
  .risk-date {
    color: #999;
    font-size: 13px;
  }
}
</style>