<template>
  <div class="admin-list-page">
    <div class="page-header">
      <div class="header-content">
        <h2>{{ title }}</h2>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="domain" :type="domainType" size="small">{{ domain }}</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter">
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="搜索 ID / 标题 / 名称" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item v-if="showModuleFilter" label="模块">
          <el-select v-model="filter.module" placeholder="全部模块" clearable style="width: 180px">
            <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column type="index" label="#" width="60" />
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 120" :show-overflow-tooltip="true">
          <template #default="{ row }">
            <span v-if="!col.formatter">{{ row[col.prop] ?? '-' }}</span>
            <el-tag v-else-if="col.formatter === 'tag'" :type="tagType(row[col.prop])" size="small">{{ row[col.prop] }}</el-tag>
            <span v-else>{{ col.formatter(row[col.prop], row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="filter.page"
        v-model:page-size="filter.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pager"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-drawer v-model="showDetail" :title="`详情：${detail?.id ?? ''}`" size="50%" direction="rtl">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item v-for="key in detailKeys" :key="key" :label="key">
          <span style="white-space: pre-wrap; word-break: break-all;">{{ formatDetailValue(detail[key]) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="无数据" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  domain: { type: String, default: '' },
  domainType: { type: String, default: 'primary' },
  apiPath: { type: String, required: true },
  detailPath: { type: String, default: '' },
  columns: { type: Array, required: true },
  moduleOptions: { type: Array, default: () => [] },
  showModuleFilter: { type: Boolean, default: false }
})

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const showDetail = ref(false)
const detail = ref(null)

const filter = reactive({
  page: 1,
  pageSize: 20,
  keyword: '',
  module: ''
})

const detailKeys = computed(() => detail.value ? Object.keys(detail.value) : [])

function tagType(val) {
  if (val === 1 || val === '1' || val === '已通过' || val === '成功' || val === 'active' || val === 'enabled') return 'success'
  if (val === 0 || val === '0' || val === '停用' || val === '失败' || val === 'inactive') return 'info'
  if (val === 2 || val === '2' || val === '驳回' || val === '锁定') return 'warning'
  if (val === 3 || val === '3' || val === '高风险') return 'danger'
  return ''
}

function formatDetailValue(v) {
  if (v === null || v === undefined) return '-'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

async function load() {
  loading.value = true
  try {
    const res = await api.get(props.apiPath, {
      params: {
        page: filter.page,
        pageSize: filter.pageSize,
        keyword: filter.keyword || undefined,
        module: filter.module || undefined
      }
    })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败：' + (e.message || '未知错误'))
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filter.page = 1
  load()
}

function handleReset() {
  filter.keyword = ''
  filter.module = ''
  filter.page = 1
  load()
}

async function handleView(row) {
  const detailPath = props.detailPath || props.apiPath.replace(/\?.*$/, '').replace(/\/list$/, `/${row.id}`)
  try {
    const res = await api.get(detailPath)
    detail.value = res.data?.data || row
    showDetail.value = true
  } catch (e) {
    detail.value = row
    showDetail.value = true
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.admin-list-page {
  animation: fadeIn 0.4s ease;
  padding: 0 4px;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .header-content h2 {
    margin: 0 0 6px;
    font-size: 22px;
    font-weight: 600;
  }

  .header-content p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 13px;
  }

  .header-actions {
    display: flex;
    gap: 8px;
    align-items: center;
  }
}

.filter-card { margin-bottom: 16px; }
.table-card { margin-bottom: 16px; }

.pager {
  margin-top: 16px;
  justify-content: flex-end;
  display: flex;
}
</style>