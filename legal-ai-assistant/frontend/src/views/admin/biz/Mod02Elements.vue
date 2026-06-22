<template>
  <AdminListPage
    title="案件要素字典"
    subtitle="MOD-02 · 案件要素模板（案由/争议焦点/法律适用等）"
    domain="数据资产域"
    domainType="success"
    api-path="/admin/biz/mod02/case-elements"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'element_code', label: '要素代码', width: 160 },
  { prop: 'element_name', label: '要素名', width: 220 },
  { prop: 'category', label: '分类', width: 160 },
  { prop: 'sort_order', label: '排序', width: 90 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>