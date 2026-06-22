<template>
  <AdminListPage
    title="问答会话监控"
    subtitle="MOD-10 AI 文件问答 · 会话列表"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod10/qa-sessions"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'session_uuid', label: 'UUID', width: 200 },
  { prop: 'user_id', label: '用户', width: 120 },
  { prop: 'kb_id', label: 'KB ID', width: 100 },
  { prop: 'title', label: '会话标题', width: 280 },
  { prop: 'msg_count', label: '消息数', width: 100 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' },
  { prop: 'created_at', label: '创建时间', width: 170 }
]
</script>