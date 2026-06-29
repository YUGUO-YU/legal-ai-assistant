<template>
  <div class="admin-list-page">
    <div class="page-header">
      <div class="header-content">
        <h2>操作审计</h2>
        <p>所有 admin 写操作 + 敏感读，自动入库</p>
      </div>
      <el-tag type="primary" size="small">基础设施域</el-tag>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="用户ID"><el-input v-model="filter.userId" clearable style="width:160px" /></el-form-item>
        <el-form-item label="操作"><el-select v-model="filter.operation" clearable style="width:160px"><el-option v-for="o in operations" :key="o" :label="o" :value="o" /></el-select></el-form-item>
        <el-form-item label="业务模块"><el-select v-model="filter.module" clearable style="width:160px"><el-option v-for="m in modules" :key="m" :label="m" :value="m" /></el-select></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="exportData" :loading="exporting">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="user_id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="operation" label="操作" width="120" />
        <el-table-column prop="biz_module" label="模块" width="110" />
        <el-table-column prop="biz_type" label="对象类型" width="160" />
        <el-table-column prop="biz_id" label="对象ID" width="140" />
        <el-table-column prop="request_method" label="方法" width="80" />
        <el-table-column prop="request_url" label="URL" width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="duration_ms" label="耗时ms" width="90" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="时间" width="170" />
      </el-table>
      <el-pagination
        v-model:current-page="filter.page"
        v-model:page-size="filter.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        class="pager"
        @current-change="load"
        @size-change="load"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const filter = reactive({ page: 1, pageSize: 20, userId: '', operation: '', module: '' })
const operations = ['CREATE', 'UPDATE', 'DELETE', 'EXPORT', 'LOGIN', 'AUDIT', 'LIST', 'DETAIL']
const modules = ['MOD-01', 'MOD-02', 'MOD-03', 'MOD-04', 'MOD-05', 'MOD-06', 'MOD-07', 'MOD-08', 'MOD-09', 'MOD-10', 'ADMIN']

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/infra/audit-logs', {
      params: { page: filter.page, pageSize: filter.pageSize, userId: filter.userId || undefined, operation: filter.operation || undefined, module: filter.module || undefined }
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function reset() {
  filter.userId = ''; filter.operation = ''; filter.module = ''
  filter.page = 1; load()
}

function exportData() {
  exporting.value = true
  const headers = ['ID', '用户ID', '用户名', '操作', '模块', '对象类型', '对象ID', '方法', 'URL', 'IP', '耗时ms', '状态', '时间']
  const fields = ['id', 'user_id', 'username', 'operation', 'biz_module', 'biz_type', 'biz_id', 'request_method', 'request_url', 'ip', 'duration_ms', 'status', 'created_at']
  const csv = [headers.join(',')]
  for (const row of rows.value) {
    const values = fields.map(f => {
      let v = row[f] == null ? '' : String(row[f])
      if (v.includes(',') || v.includes('"') || v.includes('\n')) {
        v = '"' + v.replace(/"/g, '""') + '"'
      }
      return v
    })
    csv.push(values.join(','))
  }
  const blob = new Blob(['\ufeff' + csv.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'audit_logs_' + new Date().toISOString().slice(0, 10) + '.csv'
  a.click()
  URL.revokeObjectURL(url)
  exporting.value = false
}

onMounted(load)
</script>

<style lang="scss" scoped>
.admin-list-page { animation: fadeIn 0.4s ease; padding: 0 4px; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: #64748b; font-size: 13px; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }
</style>