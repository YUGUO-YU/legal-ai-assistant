<template>
  <div class="roles-page">
    <div class="page-header">
      <div class="header-content">
        <h2>角色权限管理</h2>
        <p>基础设施域 · RBAC 角色 · 数据范围 · 菜单授权</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
      </div>
    </div>

    <el-card>
      <template v-if="rows.length === 0 && !loading">
        <table-empty-state text="暂无数据" />
      </template>
      <el-table v-else :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="role_code" label="角色代码" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.role_code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role_name" label="角色名称" min-width="150" />
        <el-table-column label="数据范围" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="scopeTag(row.data_scope)">{{ scopeLabel(row.data_scope) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑角色' : '新增角色'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="角色代码" required>
              <el-input v-model="form.role_code" :disabled="!!form.id" placeholder="例：admin" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色名称" required>
              <el-input v-model="form.role_name" placeholder="例：超级管理员" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="数据范围">
          <el-select v-model="form.data_scope" style="width:100%">
            <el-option label="仅本人" :value="1" />
            <el-option label="本部门" :value="2" />
            <el-option label="本团队" :value="3" />
            <el-option label="全部数据" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="menuTreeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'nodeLabel' }"
            :default-expand-all="true"
            :check-strictly="false"
            style="max-height:300px;overflow-y:auto;border:1px solid #e2e8f0;border-radius:6px;padding:8px"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusOn" active-text="启用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'
import TableEmptyState from '../components/TableEmptyState.vue'

const rows = ref([])
const loading = ref(false)
const showDialog = ref(false)
const statusOn = ref(true)
const menuTree = ref([])
const menuTreeRef = ref(null)
const allMenus = ref([])
const form = reactive({ id: null, role_code: '', role_name: '', data_scope: 4, status: 1, remark: '' })

function scopeLabel(s) { return ({ 1: '仅本人', 2: '本部门', 3: '本团队', 4: '全部' }[s] || s) }
function scopeTag(s) { return ({ 1: 'info', 2: 'warning', 3: 'primary', 4: 'success' }[s] || '') }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/roles')
    rows.value = res.data?.list || []
  } catch (e) { rows.value = [] }
  finally { loading.value = false }
}

async function loadAllMenus() {
  try {
    const res = await api.get('/admin/infra/menus')
    allMenus.value = res.data?.list || []
    buildMenuTree()
  } catch (e) { allMenus.value = [] }
}

function buildMenuTree() {
  const map = {}
  const roots = []
  allMenus.value.forEach(m => {
    const node = { ...m, nodeLabel: `${m.menu_name} ${m.permission ? '(' + m.permission + ')' : ''}` }
    map[m.id] = node
    node.children = []
  })
  allMenus.value.forEach(m => {
    if (m.parent_id && map[m.parent_id]) {
      map[m.parent_id].children.push(map[m.id])
    } else if (m.parent_id === 0) {
      roots.push(map[m.id])
    }
  })
  menuTree.value = roots
}

async function loadRoleMenus(roleId) {
  try {
    const res = await api.get(`/admin/infra/roles/${roleId}/menus`)
    return res.data?.menu_ids || []
  } catch (e) { return [] }
}

function openCreate() {
  Object.assign(form, { id: null, role_code: '', role_name: '', data_scope: 4, status: 1, remark: '' })
  statusOn.value = true
  showDialog.value = true
  nextTick(() => {
    if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys([])
  })
}

async function openEdit(row) {
  Object.assign(form, { ...row })
  statusOn.value = row.status === 1
  showDialog.value = true
  const menuIds = await loadRoleMenus(row.id)
  nextTick(() => {
    if (menuTreeRef.value) menuTreeRef.value.setCheckedKeys(menuIds)
  })
}

async function handleSave() {
  if (!form.role_code || !form.role_name) { ElMessage.warning('角色代码和名称必填'); return }
  form.status = statusOn.value ? 1 : 0
  const payload = { ...form }; delete payload.id
  try {
    const res = form.id
      ? await api.post(`/admin/admin_role/${form.id}/update`, payload)
      : await api.post('/admin/admin_role/create', payload)
    if (res.data?.ok) {
      if (menuTreeRef.value) {
        const checkedKeys = menuTreeRef.value.getCheckedKeys()
        const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys()
        const allChecked = [...checkedKeys, ...halfCheckedKeys]
        await api.post(`/admin/infra/roles/${form.id || res.data?.id || rows.value[0]?.id}/menus`, { menu_ids: allChecked })
      }
      ElMessage.success('保存成功'); showDialog.value = false; load()
    } else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除角色「${row.role_name}」？`, '确认', { type: 'warning' })
    await api.post(`/admin/admin_role/${row.id}/delete`)
    ElMessage.success('已删除'); load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => { load(); loadAllMenus() })
</script>

<style lang="scss" scoped>
.roles-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
 to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-secondary); font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
</style>