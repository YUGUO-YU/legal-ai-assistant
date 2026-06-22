<template>
  <AdminListPage
    title="案件主数据"
    subtitle="MOD-02 AI 类案 · 案件库"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod02/cases"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'case_uuid', label: 'UUID', width: 200 },
  { prop: 'case_title', label: '案件标题', width: 300 },
  { prop: 'case_cause', label: '案由', width: 160 },
  { prop: 'court_name', label: '审理法院', width: 200 },
  { prop: 'judge_date', label: '判决日期', width: 130 },
  { prop: 'case_type', label: '案件类型', width: 120 },
  { prop: 'view_count', label: '浏览量', width: 90 }
]
</script>