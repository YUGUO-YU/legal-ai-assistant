<template>
  <AdminListPage
    title="系统参数"
    subtitle="动态配置 · LLM/Cache/RateLimit/Feature Flag"
    domain="系统配置域"
    api-path="/admin/sys/configs"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'config_key', label: 'Key', width: 280 },
  { prop: 'config_value', label: 'Value', width: 280 },
  { prop: 'config_group', label: '分组', width: 130 },
  { prop: 'value_type', label: '类型', width: 100 },
  { prop: 'remark', label: '备注', width: 280 },
  { prop: 'updated_at', label: '更新时间', width: 170 }
]
</script>