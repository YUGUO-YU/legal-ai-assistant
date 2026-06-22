<template>
  <AdminListPage
    title="法规查询管理"
    subtitle="MOD-07 · 法规库（与 MOD-01 联动）"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod07/laws"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'title', label: '法规标题', width: 320 },
  { prop: 'category_l1', label: '效力级别', width: 110 },
  { prop: 'category_l2', label: '分类', width: 140 },
  { prop: 'effective_date', label: '生效日期', width: 130 },
  { prop: 'status', label: '状态', width: 100, formatter: (v) => ({ 1: '现行', 2: '废止', 3: '修订中' }[v] || v) }
]
</script>