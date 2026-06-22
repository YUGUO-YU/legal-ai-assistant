<template>
  <AdminListPage
    title="用户反馈"
    subtitle="跨模块反馈工作台 · SLA 24h 响应 72h 解决"
    domain="运营分析域"
    domainType="info"
    api-path="/admin/ops/user-feedback"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'biz_module', label: '模块', width: 100 },
  { prop: 'biz_id', label: '业务ID', width: 180 },
  { prop: 'feedback_type', label: '类型', width: 130 },
  { prop: 'rating', label: '评分', width: 80 },
  { prop: 'status', label: '状态', width: 110, formatter: (v) => ({ 0: '待处理', 1: '处理中', 2: '已解决', 3: '已关闭' }[v] || v) },
  { prop: 'handler_id', label: '处理人', width: 110 },
  { prop: 'sla_due_at', label: 'SLA 截止', width: 170 },
  { prop: 'created_at', label: '提交时间', width: 170 }
]
</script>