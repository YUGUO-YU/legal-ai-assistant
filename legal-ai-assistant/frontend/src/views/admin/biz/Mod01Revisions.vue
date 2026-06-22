<template>
  <AdminListPage
    title="法规修订追溯"
    subtitle="MOD-01 · 法规修订历史（admin 侧新增表）"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod01/laws/0/revisions"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'law_id', label: '法规ID', width: 100 },
  { prop: 'revision_no', label: '修订号', width: 130 },
  { prop: 'revision_type', label: '类型', width: 110, formatter: (v) => ({ 1: '首次发布', 2: '修订', 3: '废止', 4: '部分修订' }[v] || v) },
  { prop: 'revision_date', label: '修订日期', width: 130 },
  { prop: 'revision_note', label: '备注', width: 380 }
]
</script>