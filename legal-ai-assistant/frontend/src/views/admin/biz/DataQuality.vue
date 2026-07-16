<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="header-content">
        <h2 class="gradient-text">数据质量监控</h2>
        <p>业务域 · 跨库数据一致性校验、数据统计分析</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadReport" :loading="loading" type="primary">
          <el-icon><Refresh /></el-icon>
          刷新报告
        </el-button>
      </div>
    </div>
        <p>跨库数据一致性校验、数据统计分析</p>
      </div>
      <el-button @click="loadReport" :loading="loading" type="primary">
        <el-icon><Refresh /></el-icon>
        刷新报告
      </el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-purple)">
              <el-icon :size="24"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ report.mysql?.lawCount || 0 }}</span>
              <span class="stat-label">MySQL 法规数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: var(--gradient-pink)">
              <el-icon :size="24"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ report.mysql?.articleCount || 0 }}</span>
              <span class="stat-label">MySQL 条款数</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="esAvailable ? 'background: var(--gradient-blue)' : 'background: var(--gradient-pink)'">
              <el-icon :size="24"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ report.elasticsearch?.available !== false ? (report.elasticsearch?.articleCount || 0) : 'N/A' }}</span>
              <span class="stat-label">ES 索引数</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>法规状态分布</span>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <span>分类分布 TOP 10</span>
          </template>
          <div ref="categoryChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="report-card">
      <template #header>
        <div class="card-header">
          <span>数据一致性报告</span>
          <el-tag :type="reportStatusType">{{ reportStatusText }}</el-tag>
        </div>
      </template>
      <el-descriptions :column="2" border v-if="report.mysql">
        <el-descriptions-item label="MySQL 法规数量">
          {{ report.mysql.lawCount }}
        </el-descriptions-item>
        <el-descriptions-item label="MySQL 条款数量">
          {{ report.mysql.articleCount }}
        </el-descriptions-item>
        <el-descriptions-item label="ES 可用状态">
          <el-tag :type="report.elasticsearch?.available !== false ? 'success' : 'danger'" size="small">
            {{ report.elasticsearch?.available !== false ? '可用' : '不可用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="ES 文档数量">
          {{ report.elasticsearch?.articleCount || 'N/A' }}
        </el-descriptions-item>
        <el-descriptions-item label="报告生成时间" :span="2">
          {{ report.generatedAt }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" />
    </el-card>

    <el-card class="actions-card">
      <template #header>
        <span>数据操作</span>
      </template>
      <el-space wrap>
        <el-button type="primary" @click="handleExport" :loading="exporting">
          <el-icon><Download /></el-icon>
          导出法规数据 (JSON)
        </el-button>
        <el-button @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          手动创建法规
        </el-button>
        <el-button type="danger" @click="showBatchDeleteDialog = true" :disabled="selectedLaws.length === 0">
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedLaws.length }})
        </el-button>
      </el-space>
    </el-card>

    <el-card class="glass table-card laws-card">
      <template #header>
        <div class="card-header">
          <span>法规列表</span>
          <el-input v-model="searchKeyword" placeholder="搜索法规..." style="width: 200px;" clearable @change="loadLaws">
            <template #append>
              <el-button :icon="Search" @click="loadLaws" />
            </template>
          </el-input>
        </div>
      </template>
      <el-table :data="laws" v-loading="tableLoading" @selection-change="handleSelectionChange" stripe>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="法规名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryL1" label="一级分类" width="120" />
        <el-table-column prop="categoryL2" label="二级分类" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issuingAuthority" label="发布机关" width="150" show-overflow-tooltip />
        <el-table-column prop="issueDate" label="发布日期" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="editLaw(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="deleteLaw(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="total > 0"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
        style="margin-top: 16px; justify-content: center;"
      />
    </el-card>

    <el-dialog v-model="showCreateDialog" title="创建法规" width="600px">
      <el-form :model="lawForm" label-width="100px">
        <el-form-item label="法规标题" required>
          <el-input v-model="lawForm.title" placeholder="请输入法规完整标题" />
        </el-form-item>
        <el-form-item label="法规简称">
          <el-input v-model="lawForm.shortTitle" placeholder="如：民法典" />
        </el-form-item>
        <el-form-item label="一级分类">
          <el-select v-model="lawForm.categoryL1" placeholder="请选择" style="width: 100%;">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="二级分类">
          <el-select v-model="lawForm.categoryL2" placeholder="请选择" style="width: 100%;">
            <el-option v-for="c in categoryL2Options" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布机关">
          <el-input v-model="lawForm.issuingAuthority" placeholder="如：全国人民代表大会" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="lawForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="lawForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="lawForm.status" style="width: 100%;">
            <el-option :value="1" label="现行有效" />
            <el-option :value="2" label="已废止" />
            <el-option :value="3" label="修订中" />
            <el-option :value="4" label="尚未生效" />
            <el-option :value="5" label="部分失效" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源URL">
          <el-input v-model="lawForm.sourceUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="来源名称">
          <el-input v-model="lawForm.sourceName" placeholder="国家法律法规信息库" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditDialog" title="编辑法规" width="600px">
      <el-form :model="lawForm" label-width="100px">
        <el-form-item label="法规标题" required>
          <el-input v-model="lawForm.title" />
        </el-form-item>
        <el-form-item label="法规简称">
          <el-input v-model="lawForm.shortTitle" />
        </el-form-item>
        <el-form-item label="一级分类">
          <el-select v-model="lawForm.categoryL1" style="width: 100%;">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="二级分类">
          <el-select v-model="lawForm.categoryL2" style="width: 100%;">
            <el-option v-for="c in categoryL2Options" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布机关">
          <el-input v-model="lawForm.issuingAuthority" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker v-model="lawForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="生效日期">
          <el-date-picker v-model="lawForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="lawForm.status" style="width: 100%;">
            <el-option :value="1" label="现行有效" />
            <el-option :value="2" label="已废止" />
            <el-option :value="3" label="修订中" />
            <el-option :value="4" label="尚未生效" />
            <el-option :value="5" label="部分失效" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源URL">
          <el-input v-model="lawForm.sourceUrl" />
        </el-form-item>
        <el-form-item label="来源名称">
          <el-input v-model="lawForm.sourceName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate" :loading="updating">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBatchDeleteDialog" title="批量删除确认" width="400px">
      <p style="color: var(--color-danger);">确定要删除选中的 {{ selectedLaws.length }} 条法规吗？此操作不可恢复。</p>
      <template #footer>
        <el-button @click="showBatchDeleteDialog = false">取消</el-button>
        <el-button type="danger" @click="handleBatchDelete" :loading="batchDeleting">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Download, Plus, Delete, Search, Document, Collection, Connection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import api from '@/api'
import { chartBorderColor } from '@/utils/adminChartPalette'

const loading = ref(false)
const exporting = ref(false)
const creating = ref(false)
const updating = ref(false)
const batchDeleting = ref(false)
const tableLoading = ref(false)

const report = ref({})
const laws = ref([])
const selectedLaws = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showBatchDeleteDialog = ref(false)
const editingLawId = ref(null)

const statusChartRef = ref(null)
const categoryChartRef = ref(null)
let statusChartInstance = null
let categoryChartInstance = null

const lawForm = ref({
  title: '',
  shortTitle: '',
  categoryL1: '',
  categoryL2: '',
  issuingAuthority: '',
  issueDate: '',
  effectiveDate: '',
  status: 1,
  sourceUrl: '',
  sourceName: ''
})

const categoryOptions = ['法律', '行政法规', '部门规章', '地方性法规', '司法解释']
const categoryL2Options = ['民法', '商法', '刑法', '行政法', '劳动法', '知识产权法', '诉讼法', '经济法', '社会法']

const reportStatusType = computed(() => {
  if (!report.value.elasticsearch) return 'info'
  return report.value.elasticsearch.available !== false ? 'success' : 'warning'
})

const reportStatusText = computed(() => {
  if (!report.value.elasticsearch) return '未知'
  return report.value.elasticsearch.available !== false ? '数据一致' : 'ES 不可用'
})

const esAvailable = computed(() => report.value.elasticsearch?.available !== false)

const getStatusType = (status) => {
  const types = { 1: 'success', 2: 'danger', 3: 'warning', 4: 'info', 5: 'warning' }
  return types[status] || 'info'
}

const getStatusName = (status) => {
  const names = { 1: '现行有效', 2: '已废止', 3: '修订中', 4: '尚未生效', 5: '部分失效' }
  return names[status] || '未知'
}

const loadReport = async () => {
  loading.value = true
  try {
    const res = await api.lawDocument.dataQuality()
    if (res.data) {
      report.value = res.data
      nextTick(() => {
        updateStatusChart()
        updateCategoryChart()
      })
    }
  } catch (e) {
    ElMessage.error('加载数据质量报告失败')
  } finally {
    loading.value = false
  }
}

function updateStatusChart() {
  if (!statusChartRef.value) return

  if (!statusChartInstance) {
    statusChartInstance = echarts.init(statusChartRef.value)
  }

  const statusData = [
    { name: '现行有效', value: laws.value.filter(l => l.status === 1).length },
    { name: '已废止', value: laws.value.filter(l => l.status === 2).length },
    { name: '修订中', value: laws.value.filter(l => l.status === 3).length },
    { name: '尚未生效', value: laws.value.filter(l => l.status === 4).length },
    { name: '部分失效', value: laws.value.filter(l => l.status === 5).length }
  ]

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
      label: { show: true, formatter: '{b}: {c}' },
      data: statusData.length > 0 ? statusData : [{ name: '无数据', value: 0 }]
    }],
    color: ['#67c23a', '#f56c6c', '#e6a23c', '#909399', '#f0c060']
  }

  statusChartInstance.setOption(option)
}

function updateCategoryChart() {
  if (!categoryChartRef.value) return

  if (!categoryChartInstance) {
    categoryChartInstance = echarts.init(categoryChartRef.value)
  }

  const categoryCount = {}
  laws.value.forEach(law => {
    const cat = law.categoryL1 || '未分类'
    categoryCount[cat] = (categoryCount[cat] || 0) + 1
  })

  const sortedCategories = Object.entries(categoryCount)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 10)

  const option = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: sortedCategories.map(c => c[0]),
      axisLabel: { rotate: 30, interval: 0 }
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: sortedCategories.map(c => c[1]),
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      },
      barWidth: '60%'
    }]
  }

  categoryChartInstance.setOption(option)
}

function handleResize() {
  if (statusChartInstance) statusChartInstance.resize()
  if (categoryChartInstance) categoryChartInstance.resize()
}

const loadLaws = async () => {
  tableLoading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      search: searchKeyword.value || undefined
    }
    const res = await api.v2?.laws ? await api.v2.laws(params) : await api.lawSearch.search({ keyword: searchKeyword.value, page: currentPage.value, pageSize: pageSize.value })
    if (res.data?.items) {
      laws.value = res.data.items
      total.value = res.data.total || 0
    } else if (res.data?.laws) {
      laws.value = res.data.laws
      total.value = res.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载法规列表失败')
  } finally {
    tableLoading.value = false
  }
}

const handleSelectionChange = (selection) => {
  selectedLaws.value = selection
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadLaws()
}

const handleCreate = async () => {
  if (!lawForm.value.title) {
    ElMessage.warning('请输入法规标题')
    return
  }
  creating.value = true
  try {
    await api.lawDocument.create(lawForm.value)
    ElMessage.success('法规创建成功')
    showCreateDialog.value = false
    resetForm()
    loadLaws()
    loadReport()
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

const editLaw = (law) => {
  editingLawId.value = law.id
  lawForm.value = {
    title: law.title,
    shortTitle: law.shortTitle || '',
    categoryL1: law.categoryL1 || '',
    categoryL2: law.categoryL2 || '',
    issuingAuthority: law.issuingAuthority || '',
    issueDate: law.issueDate || '',
    effectiveDate: law.effectiveDate || '',
    status: law.status || 1,
    sourceUrl: law.sourceUrl || '',
    sourceName: law.sourceName || ''
  }
  showEditDialog.value = true
}

const handleUpdate = async () => {
  if (!lawForm.value.title) {
    ElMessage.warning('请输入法规标题')
    return
  }
  updating.value = true
  try {
    await api.lawDocument.update(editingLawId.value, lawForm.value)
    ElMessage.success('法规更新成功')
    showEditDialog.value = false
    loadLaws()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    updating.value = false
  }
}

const deleteLaw = async (law) => {
  try {
    await ElMessageBox.confirm(`确定要删除 "${law.title}" 吗？`, '删除确认', {
      type: 'warning'
    })
    await api.lawDocument.delete(law.id)
    ElMessage.success('删除成功')
    loadLaws()
    loadReport()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedLaws.value.length === 0) {
    ElMessage.warning('请选择要删除的法规')
    return
  }
  batchDeleting.value = true
  try {
    const ids = selectedLaws.value.map(l => l.id)
    await api.lawDocument.batchDelete(ids)
    ElMessage.success(`成功删除 ${ids.length} 条法规`)
    showBatchDeleteDialog.value = false
    selectedLaws.value = []
    loadLaws()
    loadReport()
  } catch (e) {
    ElMessage.error('批量删除失败')
  } finally {
    batchDeleting.value = false
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await api.lawDocument.export({ includeArticles: true })
    if (res.data) {
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `laws_export_${Date.now()}.json`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    }
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const resetForm = () => {
  lawForm.value = {
    title: '',
    shortTitle: '',
    categoryL1: '',
    categoryL2: '',
    issuingAuthority: '',
    issueDate: '',
    effectiveDate: '',
    status: 1,
    sourceUrl: '',
    sourceName: ''
  }
}

onMounted(() => {
  loadReport()
  loadLaws()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (statusChartInstance) {
    statusChartInstance.dispose()
    statusChartInstance = null
  }
  if (categoryChartInstance) {
    categoryChartInstance.dispose()
    categoryChartInstance = null
  }
})
</script>

<style scoped>
.data-quality {
  padding: 24px;
  animation: adminFadeIn 0.4s ease;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;

  h2 {
    margin: 0 0 4px;
    font-size: 22px;
    font-weight: 600;
    color: var(--color-text-primary);
  }
  p {
    margin: 0;
    color: var(--color-text-secondary);
    font-size: 14px;
  }
}

.stats-row {
  margin-bottom: 24px;
}

.chart-row {
  margin-bottom: 24px;
}

.chart-card {
  border-radius: 12px;
}

.chart-container {
  height: 280px;
}

.stat-card {
  border-radius: 16px;
  border: none;
  transition: transform 0.2s;

  &:hover {
    transform: translateY(-2px);
  }

  .stat-content {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 14px;
    color: var(--color-text-inverse);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .stat-info {
    display: flex;
    flex-direction: column;
  }

  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: var(--color-text-primary);
  }

  .stat-label {
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-top: 2px;
  }
}

.report-card, .actions-card, .laws-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
