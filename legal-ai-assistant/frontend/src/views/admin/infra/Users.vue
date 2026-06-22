<template>
  <AdminListPage
    title="用户管理"
    subtitle="后台用户、飞书 SSO 绑定、状态管理"
    domain="基础设施域"
    domainType="primary"
    api-path="/admin/infra/users"
    :columns="columns"
  />
</template>

<script setup>
import AdminListPage from '../AdminListPage.vue'

const columns = [
  { prop: 'id', label: 'ID', width: 70 },
  { prop: 'username', label: '用户名', width: 140 },
  { prop: 'real_name', label: '真实姓名', width: 120 },
  { prop: 'mobile', label: '手机', width: 130 },
  { prop: 'email', label: '邮箱', width: 200 },
  { prop: 'user_type', label: '类型', width: 100, formatter: 'tag' },
  { prop: 'status', label: '状态', width: 100, formatter: 'tag' },
  { prop: 'last_login_at', label: '最近登录', width: 170 },
  { prop: 'feishu_union_id', label: '飞书 UnionID', width: 200 }
]
</script>