<template>
  <AdminListPage
    title="知识库主数据"
    subtitle="MOD-09 案例法规库 · 团队知识库 CRUD"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod09/kb-bases"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'kb_code', label: 'KB 代码', width: 170 },
  { prop: 'name', label: '知识库名', width: 240 },
  { prop: 'description', label: '简介', width: 320 },
  { prop: 'owner_id', label: '所有者', width: 120 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' },
  { prop: 'doc_count', label: '文档数', width: 100 }
]
</script>