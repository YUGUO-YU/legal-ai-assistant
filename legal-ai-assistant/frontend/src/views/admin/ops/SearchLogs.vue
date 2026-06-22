<template>
  <AdminListPage
    title="搜索日志"
    subtitle="MOD-01/06/07 · 用户检索行为"
    domain="运营分析域"
    domainType="info"
    api-path="/admin/ops/search-logs"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'user_id', label: '用户', width: 120 },
  { prop: 'module', label: '模块', width: 100 },
  { prop: 'query_text', label: '查询词', width: 320 },
  { prop: 'result_count', label: '结果数', width: 100 },
  { prop: 'clicked_id', label: '点击ID', width: 180 },
  { prop: 'duration_ms', label: '耗时ms', width: 100 },
  { prop: 'created_at', label: '时间', width: 170 }
]
</script>