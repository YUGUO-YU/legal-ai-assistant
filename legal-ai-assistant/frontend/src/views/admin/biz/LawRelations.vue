<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">法规关联管理</h2>
        <p>管理法规之间的关联关系，支持参照、援引、修改、废止、配套等关联类型</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openDialog()">
          <el-icon><Plus /></el-icon>
          新增关联
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-purple)">
              <el-icon :size="20"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.total }}</span>
              <span class="stat-label">总关联数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-pink)">
              <el-icon :size="20"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.referenceCount }}</span>
              <span class="stat-label">参照</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-blue)">
              <el-icon :size="20"><Link /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.citeCount }}</span>
              <span class="stat-label">援引</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-mint)">
              <el-icon :size="20"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stats.amendCount }}</span>
              <span class="stat-label">修改/废止</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="chart-card">
      <template #header>
        <span>关联类型分布</span>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <el-card class="glass filter-card" style="margin-bottom: 14px;">
      <el-form inline :model="filter">
        <el-form-item label="来源法规">
          <el-select
            v-model="filter.sourceLawId"
            placeholder="选择法规"
            clearable
            filterable
            style="width:200px"
            @change="load"
          >
            <el-option
              v-for="law in lawList"
              :key="law.id"
              :label="law.title"
              :value="law.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联类型">
          <el-select v-model="filter.relationType" placeholder="请选择" clearable style="width:140px" @change="load">
            <el-option label="参照" value="参照" />
            <el-option label="援引" value="援引" />
            <el-option label="修改" value="修改" />
            <el-option label="废止" value="废止" />
            <el-option label="配套" value="配套" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="glass table-card">
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="来源法规" min-width="250">
          <template #default="{ row }">
            <div class="law-cell">
              <span class="law-title" :title="row.source_article_title">{{ row.source_article_title || '未知法规' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="relation_type" label="关联类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="relationTypeTag(row.relation_type)">{{ row.relation_type || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标法规" min-width="250">
          <template #default="{ row }">
            <div class="law-cell">
              <span class="law-title" :title="row.target_article_title">{{ row.target_article_title || '未知法规' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="80">
          <template #default="{ row }">
            {{ row.weight?.toFixed(2) || '1.00' }}
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="filter.page"
          v-model:page-size="filter.pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="来源法规" prop="sourceArticleId">
          <el-select
            v-model="form.sourceArticleId"
            placeholder="选择法规"
            filterable
            style="width:100%"
          >
            <el-option
              v-for="law in lawList"
              :key="law.id"
              :label="law.title"
              :value="law.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标法规" prop="targetArticleId">
          <el-select
            v-model="form.targetArticleId"
            placeholder="选择法规"
            filterable
            style="width:100%"
          >
            <el-option
              v-for="law in lawList"
              :key="law.id"
              :label="law.title"
              :value="law.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联类型" prop="relationType">
          <el-select v-model="form.relationType" placeholder="请选择关联类型" style="width:100%">
            <el-option label="参照" value="参照" />
            <el-option label="援引" value="援引" />
            <el-option label="修改" value="修改" />
            <el-option label="废止" value="废止" />
            <el-option label="配套" value="配套" />
          </el-select>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-input-number v-model="form.weight" :min="0.01" :max="10" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Connection, Document, Link, Warning } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '../../../api'
import { chartBorderColor } from '@/utils/adminChartPalette'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增关联')
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const lawList = ref([])
const chartRef = ref(null)
let chartInstance = null

const stats = reactive({
  total: 0,
  referenceCount: 0,
  citeCount: 0,
  amendCount: 0
})

const filter = reactive({
  page: 1,
  pageSize: 20,
  sourceLawId: null,
  relationType: ''
})

const form = reactive({
  sourceArticleId: null,
  targetArticleId: null,
  relationType: '',
  weight: 1.0
})

const rules = {
  sourceArticleId: [{ required: true, message: '请选择来源法规', trigger: 'change' }],
  targetArticleId: [{ required: true, message: '请选择目标法规', trigger: 'change' }],
  relationType: [{ required: true, message: '请选择关联类型', trigger: 'change' }]
}

function relationTypeTag(type) {
  const map = {
    '参照': 'primary',
    '援引': 'success',
    '修改': 'warning',
    '废止': 'danger',
    '配套': 'info'
  }
  return map[type] || 'info'
}

async function loadLaws() {
  try {
    const res = await api.lawDocument.export({ includeArticles: false })
    const laws = res.data?.laws || []
    lawList.value = laws.map(l => ({ id: l.id, title: l.title }))
  } catch (e) {
    lawList.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const params = {
      page: filter.page,
      pageSize: filter.pageSize
    }
    if (filter.sourceLawId) params.sourceArticleId = filter.sourceLawId
    if (filter.relationType) params.relationType = filter.relationType

    const res = await api.get('/admin/biz/mod01/law-relations', { params })
    rows.value = res.data?.list || []
    total.value = res.data?.total || 0

    updateStats()
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function updateStats() {
  stats.total = total.value
  const typeCount = {}
  rows.value.forEach(r => {
    const type = r.relation_type || '其他'
    typeCount[type] = (typeCount[type] || 0) + 1
  })
  stats.referenceCount = typeCount['参照'] || 0
  stats.citeCount = typeCount['援引'] || 0
  stats.amendCount = (typeCount['修改'] || 0) + (typeCount['废止'] || 0) + (typeCount['配套'] || 0)
  updateChart(typeCount)
}

function updateChart(typeCount) {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const data = Object.entries(typeCount).map(([name, value]) => ({ name, value }))

  const option = {
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%', left: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: chartBorderColor,
        borderWidth: 2
      },
      label: { show: true, formatter: '{b}: {c} ({d}%)' },
      data: data.length > 0 ? data : [{ name: '无数据', value: 0 }]
    }],
    color: ['#667eea', '#f5576c', '#4facfe', '#43e97b', '#f093fb']
  }

  chartInstance.setOption(option)
}

function reset() {
  filter.page = 1
  filter.sourceLawId = null
  filter.relationType = ''
  load()
}

function openDialog(row) {
  if (row) {
    dialogTitle.value = '编辑关联'
    isEdit.value = true
    editingId.value = row.id
    form.sourceArticleId = row.source_article_id ? parseInt(row.source_article_id) : null
    form.targetArticleId = row.target_article_id ? parseInt(row.target_article_id) : null
    form.relationType = row.relation_type || ''
    form.weight = parseFloat(row.weight) || 1.0
  } else {
    dialogTitle.value = '新增关联'
    isEdit.value = false
    editingId.value = null
    form.sourceArticleId = null
    form.targetArticleId = null
    form.relationType = ''
    form.weight = 1.0
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  if (form.sourceArticleId === form.targetArticleId) {
    ElMessage.error('来源法规和目标法规不能相同')
    return
  }

  try {
    const payload = {
      sourceArticleId: form.sourceArticleId,
      targetArticleId: form.targetArticleId,
      relationType: form.relationType,
      weight: form.weight
    }

    if (isEdit.value) {
      const res = await api.put(`/admin/biz/mod01/law-relations/${editingId.value}`, payload)
      if (res.data?.ok) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.data?.error || '更新失败')
      }
    } else {
      const res = await api.post('/admin/biz/mod01/law-relations', payload)
      if (res.data?.ok) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        load()
      } else {
        ElMessage.error(res.data?.error || '创建失败')
      }
    }
  } catch (e) {
    // error handled by interceptor
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除该关联关系吗？`, '删除确认', { type: 'warning' })
    const res = await api.delete(`/admin/biz/mod01/law-relations/${row.id}`)
    if (res.data?.ok) {
      ElMessage.success('删除成功')
      load()
    } else {
      ElMessage.error(res.data?.error || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

function handleResize() {
  if (chartInstance) {
    chartInstance.resize()
  }
}

onMounted(() => {
  loadLaws()
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style lang="scss" scoped>
.law-relations-page { animation: adminFadeIn 0.4s ease; padding: 0 4px; }
.page-header { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; flex-wrap: wrap; gap: 10px; }
.header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
.header-content p { margin: 0; color: var(--color-text-muted); font-size: 13px; }
.header-actions { display:flex; gap:8px; flex-wrap: wrap; }
.filter-card { margin-bottom: 16px; }
.pager { margin-top: 16px; justify-content: flex-end; display: flex; }

.stats-row { margin-bottom: 16px; }
.stat-card {
  .stat-content { display: flex; align-items: center; gap: 12px; }
  .stat-icon {
    width: 48px; height: 48px; border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    color: var(--color-text-inverse);
  }
  .stat-info { display: flex; flex-direction: column; }
  .stat-value { font-size: 24px; font-weight: 600; line-height: 1.2; }
  .stat-label { font-size: 12px; color: var(--color-text-muted); margin-top: 2px; }
}

.chart-card { margin-bottom: 16px; }
.chart-container { height: 200px; }

.law-cell {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}
.law-title {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
