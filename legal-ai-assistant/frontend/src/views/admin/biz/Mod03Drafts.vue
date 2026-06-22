<template>
  <AdminListPage
    title="草稿复核工作台"
    subtitle="MOD-03 · 待复核/已通过/已驳回"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod03/drafts"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'draft_uuid', label: 'UUID', width: 200 },
  { prop: 'template_code', label: '模板', width: 180 },
  { prop: 'title', label: '草稿标题', width: 240 },
  { prop: 'claim_amount', label: '金额', width: 120 },
  { prop: 'risk_level', label: '风险', width: 100, formatter: 'tag' },
  { prop: 'review_status', label: '复核状态', width: 100, formatter: 'tag' },
  { prop: 'user_id', label: '用户', width: 110 },
  { prop: 'reviewed_at', label: '复核时间', width: 170 },
  { prop: 'created_at', label: '创建时间', width: 170 }
]
</script>