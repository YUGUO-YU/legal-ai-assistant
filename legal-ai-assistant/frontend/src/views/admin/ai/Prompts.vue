<template>
  <AdminListPage
    title="Prompt 模板管理"
    subtitle="AI 域核心资产 · 按模块 × 场景管理 · 支持灰度与回滚"
    domain="AI 能力域"
    domainType="warning"
    api-path="/admin/ai/prompts"
    :columns="columns"
    :module-options="modules"
    :show-module-filter="true"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const modules = ['MOD-01', 'MOD-02', 'MOD-03', 'MOD-04', 'MOD-05', 'MOD-06', 'MOD-07', 'MOD-08', 'MOD-09', 'MOD-10']

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'prompt_code', label: 'Prompt 代码', width: 200 },
  { prop: 'module', label: '模块', width: 100 },
  { prop: 'scene', label: '场景', width: 130 },
  { prop: 'version', label: '版本', width: 100 },
  { prop: 'is_active', label: '已激活', width: 90, formatter: (v) => v === 1 ? '✓' : '—' },
  { prop: 'is_gray', label: '灰度中', width: 90, formatter: (v) => v === 1 ? '✓' : '—' },
  { prop: 'gray_ratio', label: '灰度%', width: 90 },
  { prop: 'adopt_rate', label: '采纳率', width: 110 },
  { prop: 'feedback_score', label: '评分', width: 90 },
  { prop: 'created_at', label: '创建时间', width: 170 }
]
</script>