<template>
  <AdminListPage
    title="Token 用量统计"
    subtitle="按模块 × 模型 × 时间统计"
    domain="AI 能力域"
    domainType="warning"
    api-path="/admin/ai/token-usage"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'model_code', label: '模型', width: 160 },
  { prop: 'module', label: '模块', width: 110 },
  { prop: 'prompt_tokens', label: 'Prompt', width: 100 },
  { prop: 'completion_tokens', label: 'Completion', width: 110 },
  { prop: 'total_tokens', label: 'Total', width: 110 },
  { prop: 'cost_cny', label: '成本(元)', width: 100 },
  { prop: 'biz_id', label: '业务ID', width: 180 },
  { prop: 'created_at', label: '时间', width: 170 }
]
</script>