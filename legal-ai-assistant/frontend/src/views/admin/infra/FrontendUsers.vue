<template>
  <div class="users-page">
    <div class="page-header">
      <div class="header-content">
        <h2>前端用户管理</h2>
        <p>基础设施域 · 前端注册用户账号管理</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="用户名/姓名/邮箱/手机" clearable style="width:220px" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" clearable placeholder="全部" style="width:110px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="用户ID" width="160" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="real_name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="170">
          <template #default="{ row }">
            <div>{{ row.last_login_at || '-' }}</div>
            <div class="login-ip" v-if="row.last_login_ip">{{ row.last_login_ip }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="注册时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" size="small" @click="toggleUser(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          @current-change="handlePageChange"
        />
      </div>
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
            <el-form-item label="姓名">
              <el-input v-model="form.real_name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="密码" :required="!form.id">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空不修改' : '登录密码（至少6位）'" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="form.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱地址" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12" v-if="form.id">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const filter = reactive({ keyword: '', status: '' })
const showDialog = ref(false)
const form = reactive({ id: '', username: '', password: '', real_name: '', email: '', phone: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/frontend-users', { params: { page: currentPage.value, pageSize: pageSize.value, keyword: filter.keyword } })
    if (res.data?.error) {
      ElMessage.error('加载失败: ' + res.data.error)
      if (res.data.errorType === 'table_not_found') {
        ElMessage.warning('数据库未初始化，请联系管理员执行数据库初始化脚本')
      }
    }
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    ElMessage.error('加载失败，请检查数据库连接')
  }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { id: '', username: '', password: '', real_name: '', email: '', phone: '', status: 1 })
  showDialog.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row, password: '' })
  showDialog.value = true
}

async function handleSave() {
  if (!form.username) { ElMessage.warning('用户名必填'); return }
  if (!form.id && (!form.password || form.password.length < 6)) { ElMessage.warning('新用户必须设置密码（至少6位）'); return }
  const payload = { ...form }
  if (!payload.password) delete payload.password
  delete payload.id
  try {
    let res
    if (form.id) {
      res = await api.put('/admin/infra/frontend-users/' + form.id, payload)
    } else {
      res = await api.post('/admin/infra/frontend-users', payload)
    }
    if (res.data?.ok) { ElMessage.success('保存成功'); showDialog.value = false; load() }
    else ElMessage.error(res.data?.error || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`删除用户「${row.username}」？此操作不可恢复！`, '确认', { type: 'warning' })
    await api.delete('/admin/infra/frontend-users/' + row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function toggleUser(row) {
  try {
    await api.post('/admin/infra/frontend-users/' + row.id + '/toggle')
    row.status = row.status === 1 ? 0 : 1
    ElMessage.success(row.status === 1 ? '已启用' : '已停用')
  } catch (e) { ElMessage.error('操作失败') }
}

function handlePageChange(page) {
  currentPage.value = page
  load()
}

function reset() { filter.keyword = ''; filter.status = ''; currentPage.value = 1; load() }
onMounted(load)
</script>

<style lang="scss" scoped>
.users-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.header-actions { display:flex; gap:8px; align-items:center; }
.filter-card { margin-bottom: 16px; }
.login-ip { font-size:11px; color:#94a3b8; margin-top:2px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
