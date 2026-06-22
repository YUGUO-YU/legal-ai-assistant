<template>
  <AdminListPage
    title="菜单权限"
    subtitle="后台菜单树（按业务模块分组）"
    domain="基础设施域"
    domainType="primary"
    api-path="/admin/infra/menus"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'menu_name', label: '菜单名', width: 160 },
  { prop: 'menu_type', label: '类型', width: 90, formatter: (v) => ({ 1: '目录', 2: '菜单', 3: '按钮' }[v] || v) },
  { prop: 'path', label: '路径', width: 200 },
  { prop: 'component', label: '组件', width: 200 },
  { prop: 'permission', label: '权限标识', width: 200 },
  { prop: 'biz_module', label: '业务模块', width: 110 },
  { prop: 'icon', label: '图标', width: 120 },
  { prop: 'sort_order', label: '排序', width: 80 },
  { prop: 'status', label: '状态', width: 90, formatter: 'tag' }
]
</script>