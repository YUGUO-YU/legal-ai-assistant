<template>
  <AdminListPage
    title="法规主数据"
    subtitle="MOD-01 AI 搜法 · 法规主表（含审核状态机）"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod01/laws"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'law_uuid', label: 'UUID', width: 200 },
  { prop: 'title', label: '法规标题', width: 320 },
  { prop: 'category_l1', label: '效力级别', width: 110 },
  { prop: 'category_l2', label: '分类', width: 140 },
  { prop: 'issuing_authority', label: '制定机关', width: 200 },
  { prop: 'effective_date', label: '生效日期', width: 120 },
  { prop: 'status', label: '效力状态', width: 100, formatter: (v) => ({ 1: '现行', 2: '废止', 3: '修订中', 4: '未生效', 5: '部分失效' }[v] || v) },
  { prop: 'audit_status', label: '审核状态', width: 100, formatter: 'tag' },
  { prop: 'view_count', label: '浏览量', width: 90 }
]
</script>