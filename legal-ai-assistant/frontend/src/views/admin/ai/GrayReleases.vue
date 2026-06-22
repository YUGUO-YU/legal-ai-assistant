<template>
  <AdminListPage
    title="Prompt 灰度发布"
    subtitle="灰度策略 + 回滚记录"
    domain="AI 能力域"
    domainType="warning"
    api-path="/admin/ai/prompts/gray-releases"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'prompt_id', label: 'Prompt ID', width: 110 },
  { prop: 'from_version', label: '源版本', width: 110 },
  { prop: 'to_version', label: '目标版本', width: 110 },
  { prop: 'ratio', label: '比例%', width: 90 },
  { prop: 'started_at', label: '开始', width: 170 },
  { prop: 'ended_at', label: '结束', width: 170 },
  { prop: 'rollback_reason', label: '回滚原因', width: 280 }
]
</script>