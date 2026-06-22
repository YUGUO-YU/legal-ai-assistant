<template>
  <AdminListPage
    title="角色权限"
    subtitle="RBAC 角色 + 数据权限范围"
    domain="基础设施域"
    domainType="primary"
    api-path="/admin/infra/roles"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'role_code', label: '角色代码', width: 160 },
  { prop: 'role_name', label: '角色名', width: 160 },
  { prop: 'data_scope', label: '数据范围', width: 100, formatter: (v) => ({ 1: '本人', 2: '本部门', 3: '本团队', 4: '全部' }[v] || v) },
  { prop: 'status', label: '状态', width: 100, formatter: 'tag' },
  { prop: 'remark', label: '备注', width: 280 }
]
</script>