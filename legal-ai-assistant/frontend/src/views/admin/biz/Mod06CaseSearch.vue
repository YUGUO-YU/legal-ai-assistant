<template>
  <AdminListPage
    title="案例查询日志"
    subtitle="MOD-06 · 案例库访问日志"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod06/case-search-logs"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'user_id', label: '用户', width: 120 },
  { prop: 'query_text', label: '查询词', width: 320 },
  { prop: 'result_count', label: '结果数', width: 100 },
  { prop: 'duration_ms', label: '耗时ms', width: 100 },
  { prop: 'created_at', label: '时间', width: 170 }
]
</script>