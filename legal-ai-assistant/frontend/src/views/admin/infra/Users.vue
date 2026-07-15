<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-content">
        <h2>用户管理</h2>
        <p>基础设施域 · 账号 / 角色 / 状态 / 登录记录</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="用户名/姓名/手机" clearable style="width:220px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" clearable placeholder="全部" style="width:110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
            <el-option label="锁定" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template v-if="rows.length === 0 && !loading">
        <table-empty-state :text="loadError || '暂无数据'" />
      </template>
      <template v-if="selectedRows.length > 0">
        <div class="batch-actions">
          <span>已选择 {{ selectedRows.length }} 项</span>
          <el-button size="small" type="danger" @click="handleBatchDelete">批量删除</el-button>
          <el-button size="small" type="success" @click="handleBatchToggle(1)">批量启用</el-button>
          <el-button size="small" type="warning" @click="handleBatchToggle(0)">批量停用</el-button>
          <el-button size="small" @click="selectedRows = []">取消选择</el-button>
        </div>
      </template>
      <el-table v-else :data="rows" v-loading="loading" stripe border @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="real_name" label="姓名" width="120" />
        <el-table-column prop="mobile" label="手机" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.user_type === 1 ? 'primary' : 'info'">{{ row.user_type === 1 ? '后台' : '只读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">
            <div>{{ row.last_login_at || '-' }}</div>
            <div class="login-ip" v-if="row.last_login_ip">{{ row.last_login_ip }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openAssignRoles(row)">分配角色</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleUser(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="用户名" required>
              <el-input v-model="form.username" :disabled="!!form.id" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" required>
              <el-input v-model="form.real_name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="密码" :required="!form.id">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空不修改' : '登录密码'" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="form.mobile" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="用户类型">
              <el-select v-model="form.user_type" style="width:100%">
                <el-option label="后台管理" :value="1" />
                <el-option label="业务只读" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="飞书ID">
              <el-input v-model="form.feishu_union_id" placeholder="飞书 Union ID" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRoleDialog" title="分配角色" width="480px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :value="roleForm.username + '（' + roleForm.realName + '）'" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="roleForm.selectedRoles">
            <el-checkbox v-for="r in allRoles" :key="r.id" :label="r.id">{{ r.role_name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRoles">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'
import TableEmptyState from '../components/TableEmptyState.vue'

const rows = ref([])
const loading = ref(false)
const loadError = ref('')
const filter = reactive({ keyword: '', status: '' })
const showDialog = ref(false)
const showRoleDialog = ref(false)
const form = reactive({ id: null, username: '', password: '', real_name: '', mobile: '', email: '', user_type: 1, feishu_union_id: '', status: 1 })
const roleForm = reactive({ userId: null, username: '', realName: '', selectedRoles: [] })
const allRoles = ref([])
const selectedRows = ref([])

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

const handleBatchDelete = async () => {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 项？`, '批量删除', { type: 'warning' })
    const ids = selectedRows.value.map(r => r.id)
    await api.post('/admin/admin_user/batch-delete', { ids })
    ElMessage.success('删除成功')
    selectedRows.value = []
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const handleBatchToggle = async (status) => {
  if (!selectedRows.value.length) return
  const ids = selectedRows.value.map(r => r.id)
  await api.post('/admin/admin_user/batch-toggle', { ids, status })
  ElMessage.success('操作成功')
  selectedRows.value = []
  load()
}

function statusLabel(s) { return ({ 1: '启用', 0: '停用', 2: '锁定' }[s] || s) }
function statusTag(s) { return ({ 1: 'success', 0: 'info', 2: 'danger' }[s] || '') }

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await api.get('/admin/infra/users', { params: { ...filter } })
    if (res.data?.error) {
      loadError.value = '数据加载失败: ' + res.data.error
      if (res.data.errorType === 'table_not_found') {
        loadError.value = '数据库表未初始化，请联系管理员执行数据库初始化脚本'
      }
      rows.value = []
    } else {
      rows.value = res.data?.list || []
    }
  } catch (e) {
    rows.value = []
    loadError.value = '网络错误，无法加载数据'
  }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: null, username: '', password: '', real_name: '', mobile: '', email: '', user_type: 1, feishu_union_id: '', status: 1 })
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row, password: '' })
  showDialog.value = true
}

async function handleSave() {
  if (!form.username || !form.real_name) { ElMessage.warning('用户名和姓名必填'); return }
  if (!form.id && !form.password) { ElMessage.warning('新用户必须设置密码'); return }
  const payload = { ...form }
  if (!payload.password) delete payload.password
  delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.post(`/admin/admin_user/${form.id}/update`, payload)
    } else {
      res = await api.post('/admin/admin_user/create', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除用户「${row.real_name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/admin_user/${row.id}/delete`)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function toggleUser(row) {
  const action = row.status === 1 ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${row.username}（${row.real_name}）」？${action === '停用' ? '停用后该用户将无法登录。' : '启用后该用户将恢复登录权限。'}`, `确认${action}`, { type: 'warning' })
    const newStatus = row.status === 1 ? 0 : 1
    await api.post(`/admin/admin_user/${row.id}/toggle`, { status: newStatus })
    row.status = newStatus
    ElMessage.success(statusLabel(newStatus))
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

async function openAssignRoles(row) {
  roleForm.userId = row.id
  roleForm.username = row.username
  roleForm.realName = row.real_name
  roleForm.selectedRoles = []
  showRoleDialog.value = true
  try {
    const [rolesRes, userRolesRes] = await Promise.all([
      api.get('/admin/infra/roles', { params: { page: 1, pageSize: 100 } }),
      api.get(`/admin/infra/users/${row.id}/roles`)
    ])
    allRoles.value = rolesRes.data?.list || []
    roleForm.selectedRoles = userRolesRes.data?.roleIds || []
  } catch (e) {
    allRoles.value = []
  }
}

async function handleAssignRoles() {
  try {
    const res = await api.post(`/admin/infra/users/${roleForm.userId}/roles`, {
      role_ids: roleForm.selectedRoles
    })
    if (res.data?.ok) {
      ElMessage.success('角色分配成功')
      showRoleDialog.value = false
    } else {
      ElMessage.error(res.data?.error || '分配失败')
    }
  } catch (e) { ElMessage.error('分配失败') }
}

function reset() { filter.keyword = ''; filter.status = ''; load() }
onMounted(load)
</script>

<style lang="scss" scoped>
.users-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.login-ip { font-size:11px; color:var(--color-text-placeholder); margin-top:2px; }

.batch-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-primary-light);
  border-radius: var(--radius-md);
  margin-bottom: 12px;

  span {
    color: var(--color-primary);
    font-weight: 500;
  }
}
</style>