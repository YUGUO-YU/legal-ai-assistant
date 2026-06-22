<template>
  <AdminListPage
    title="爬虫任务管理"
    subtitle="MOD-01/02/05 · 数据源爬虫调度"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod01/crawl-tasks"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'task_name', label: '任务名', width: 220 },
  { prop: 'source', label: '数据源', width: 110 },
  { prop: 'crawl_type', label: '类型', width: 100 },
  { prop: 'target_url', label: '目标 URL', width: 320, formatter: (v) => v ? v.substring(0, 80) + '...' : '-' },
  { prop: 'cron_expression', label: 'Cron', width: 130 },
  { prop: 'status', label: '状态', width: 100, formatter: (v) => ({ 0: '待启动', 1: '运行中', 2: '暂停', 3: '已停止' }[v] || v) },
  { prop: 'last_run_at', label: '上次执行', width: 170 },
  { prop: 'total_crawled', label: '累计', width: 90 },
  { prop: 'success_count', label: '成功', width: 90 },
  { prop: 'fail_count', label: '失败', width: 90 }
]
</script>