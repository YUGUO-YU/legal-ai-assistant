<template>
  <AdminListPage
    title="分块策略"
    subtitle="MOD-09 · 知识库文档分块参数（512 tokens / 64 overlap）"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod09/kb-strategies"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'kb_id', label: 'KB ID', width: 100 },
  { prop: 'chunk_size', label: '块大小', width: 110 },
  { prop: 'chunk_overlap', label: '重叠', width: 100 },
  { prop: 'splitter', label: '切分器', width: 160 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' },
  { prop: 'updated_at', label: '更新时间', width: 170 }
]
</script>