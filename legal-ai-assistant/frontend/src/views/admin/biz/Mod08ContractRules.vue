<template>
  <AdminListPage
    title="合同审查规则"
    subtitle="MOD-08 AI 合同审查 · 8 维度权重 + 风险阈值"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod08/contract-rules"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'dimension', label: '维度', width: 220 },
  { prop: 'weight', label: '权重', width: 100 },
  { prop: 'threshold_high', label: '高风险阈值', width: 130 },
  { prop: 'threshold_low', label: '低风险阈值', width: 130 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>