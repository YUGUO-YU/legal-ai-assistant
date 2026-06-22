<template>
  <AdminListPage
    title="文书模板"
    subtitle="MOD-03 AI 文书起草 · 模板 CRUD + 版本"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod03/templates"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'template_code', label: '代码', width: 200 },
  { prop: 'template_name', label: '模板名', width: 200 },
  { prop: 'category', label: '分类', width: 110 },
  { prop: 'version', label: '版本', width: 90 },
  { prop: 'review_required', label: '需复核', width: 90, formatter: (v) => v === 1 ? '✓' : '—' },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>