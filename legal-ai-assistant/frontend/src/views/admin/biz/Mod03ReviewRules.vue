<template>
  <AdminListPage
    title="复核规则"
    subtitle="MOD-03 · 金额/类型触发复核阈值"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod03/review-rules"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'template_code', label: '模板', width: 180 },
  { prop: 'rule_type', label: '规则类型', width: 130 },
  { prop: 'operator', label: '比较', width: 90 },
  { prop: 'threshold', label: '阈值', width: 130 },
  { prop: 'trigger_action', label: '触发动作', width: 140 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>