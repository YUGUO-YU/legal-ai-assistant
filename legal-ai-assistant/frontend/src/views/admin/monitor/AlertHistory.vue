<template>
  <AdminListPage
    title="告警历史"
    subtitle="触发 / 解决记录"
    domain="监控告警域"
    domainType="danger"
    api-path="/admin/monitor/alert-history"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'rule_id', label: '规则ID', width: 100 },
  { prop: 'triggered_at', label: '触发时间', width: 170 },
  { prop: 'resolved_at', label: '解决时间', width: 170 },
  { prop: 'level', label: '级别', width: 80, formatter: (v) => ({ 1: 'P0', 2: 'P1', 3: 'P2' }[v] || v) },
  { prop: 'metric_value', label: '指标值', width: 130 },
  { prop: 'message', label: '消息', width: 320 },
  { prop: 'notify_status', label: '通知', width: 90, formatter: 'tag' }
]
</script>