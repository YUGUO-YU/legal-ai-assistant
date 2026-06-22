<template>
  <AdminListPage
    title="数据字典"
    subtitle="通用枚举字典 · 审核状态/风险等级/LLM 服务商"
    domain="系统配置域"
    api-path="/admin/sys/dicts"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'dict_type', label: '字典类型', width: 200 },
  { prop: 'dict_label', label: '显示名', width: 200 },
  { prop: 'dict_value', label: '值', width: 200 },
  { prop: 'sort_order', label: '排序', width: 90 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>