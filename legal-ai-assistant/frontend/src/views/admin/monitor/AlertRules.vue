<template>
  <AdminListPage
    title="告警规则"
    subtitle="开箱即用：JVM/接口RT/ES/Milvus/LLM"
    domain="监控告警域"
    domainType="danger"
    api-path="/admin/monitor/alert-rules"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'rule_name', label: '规则名', width: 220 },
  { prop: 'metric', label: '指标', width: 200 },
  { prop: 'operator', label: '比较', width: 80 },
  { prop: 'threshold', label: '阈值', width: 110 },
  { prop: 'duration_sec', label: '持续(秒)', width: 100 },
  { prop: 'level', label: '级别', width: 90, formatter: (v) => ({ 1: 'P0', 2: 'P1', 3: 'P2' }[v] || v) },
  { prop: 'channels', label: '渠道', width: 140, formatter: (v) => Array.isArray(v) ? v.join(',') : (v || '-') },
  { prop: 'biz_module', label: '模块', width: 100 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>