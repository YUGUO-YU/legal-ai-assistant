<template>
  <AdminListPage
    title="LLM 模型配置"
    subtitle="主备模型 · 温度 · 健康探测"
    domain="AI 能力域"
    domainType="warning"
    api-path="/admin/ai/llm-models"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'model_code', label: '模型代码', width: 160 },
  { prop: 'model_name', label: '模型名', width: 200 },
  { prop: 'provider', label: '服务商', width: 110 },
  { prop: 'endpoint', label: 'Endpoint', width: 240, formatter: (v) => v ? v.replace(/^https?:\/\//, '').substring(0, 40) : '-' },
  { prop: 'temperature', label: '温度', width: 80 },
  { prop: 'max_tokens', label: 'Max', width: 90 },
  { prop: 'is_primary', label: '主', width: 60, formatter: (v) => v === 1 ? '✓' : '—' },
  { prop: 'is_fallback', label: '备', width: 60, formatter: (v) => v === 1 ? '✓' : '—' },
  { prop: 'health_status', label: '健康', width: 90, formatter: 'tag' },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>