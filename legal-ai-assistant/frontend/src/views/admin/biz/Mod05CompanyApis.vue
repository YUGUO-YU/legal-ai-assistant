<template>
  <AdminListPage
    title="企业查询 API"
    subtitle="MOD-05 · 信用中国/企查查/天眼查 API 配置"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod05/company-apis"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'api_name', label: 'API 名', width: 200 },
  { prop: 'provider', label: '服务商', width: 130 },
  { prop: 'endpoint', label: 'Endpoint', width: 320, formatter: (v) => v ? v.substring(0, 60) + '...' : '-' },
  { prop: 'monthly_quota', label: '月配额', width: 110 },
  { prop: 'used_count', label: '已用', width: 110 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>