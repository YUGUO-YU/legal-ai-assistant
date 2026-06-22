<template>
  <AdminListPage
    title="研究任务监控"
    subtitle="MOD-04 AI 法律研究 · 任务列表与状态"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod04/research-tasks"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'task_uuid', label: 'UUID', width: 200 },
  { prop: 'topic', label: '主题', width: 320 },
  { prop: 'user_id', label: '用户', width: 120 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'created_at', label: '创建时间', width: 170 }
]
</script>