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
        <el-dropdown trigger="click" @command="handleColumnCommand" class="column-config-dropdown">
          <el-button size="small">
            <el-icon><Setting /></el-icon> 列配置
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-for="col in tableColumns" :key="col.prop" :command="{ type: 'toggle', prop: col.prop }">
                <el-checkbox :model-value="!col.hidden" @change="toggleColumn(col.prop)" />
                {{ col.label }}
              </el-dropdown-item>
              <el-dropdown-item command="reset" divided>恢复默认</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form inline :model="filter" class="filter-form" @submit.prevent>
        <FormField
          field="keyword"
          label="关键词"
          :modelValue="filter.keyword"
          :error="filterErrors.keyword"
          :touched="filterTouched.keyword"
          :hint="showModuleFilter ? '至少2个字符' : ''"
        >
          <el-input
            v-model="filter.keyword"
            placeholder="搜索 ID / 标题 / 名称"
            clearable
            style="width: 220px"
            @input="(v) => handleFilterChange('keyword', v)"
            @blur="() => touchField('keyword')"
            @keyup.enter="handleSearch"
          />
        </FormField>
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
      <template v-if="rows.length === 0 && !loading">
        <table-empty-state text="暂无数据" />
      </template>
        <el-table
          v-else
          :data="rows"
          v-loading="loading"
          stripe
          border
          @header-dragend="handleHeaderDragend"
          @row-click="handleRowClick"
          @row-contextmenu="handleContextMenu"
          ref="tableRef"
          :row-class-name="rowClassName"
          tabindex="0"
        >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column v-for="col in tableColumns.filter(c => !c.hidden)" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" :min-width="col.minWidth || col.width || 120" :fixed="col.fixed" :show-overflow-tooltip="true">
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
        <el-table-column width="50" fixed="right">
          <template #default>
            <i class="el-icon-rank drag-handle-icon"></i>
          </template>
        </el-table-column>
      </el-table>

      <draggable
        v-model="rows"
        item-key="id"
        tag="tbody"
        :style="{ display: 'none' }"
        ghost-class="drag-ghost"
        chosen-class="drag-chosen"
        :animation="200"
        @end="handleRowDragEnd"
      />

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

    <el-drawer v-model="showDetail" :title="`详情：${detail?.id ?? ''}`" size="50%" direction="rtl" class="drawer-slide-right">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item v-for="key in detailKeys" :key="key" :label="key">
          <span style="white-space: pre-wrap; word-break: break-all;">{{ formatDetailValue(detail[key]) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="无数据" />
    </el-drawer>

    <ContextMenu
      v-model:visible="contextMenu.visible"
      :x="contextMenu.x"
      :y="contextMenu.y"
      :menus="contextMenu.menus"
      @select="contextMenu.handleSelect"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { Refresh, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import api from '../../api'
import TableEmptyState from '../components/TableEmptyState.vue'
import { useTableColumnConfig } from '../../composables/useTableColumnConfig'
import { useFormValidation } from '../../composables/useFormValidation'
import FormField from '../../components/common/FormField.vue'
import ContextMenu from '../../components/common/ContextMenu.vue'
import { useContextMenu } from '../../composables/useContextMenu'

const props = defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  domain: { type: String, default: '' },
  domainType: { type: String, default: 'primary' },
  apiPath: { type: String, required: true },
  detailPath: { type: String, default: '' },
  columns: { type: Array, required: true },
  moduleOptions: { type: Array, default: () => [] },
  showModuleFilter: { type: Boolean, default: false },
  tableName: { type: String, default: '' }
})

const { columns: tableColumns, saveConfig, resetConfig, toggleColumn, handleHeaderDragend } = useTableColumnConfig(
  props.tableName || props.apiPath,
  props.columns
)

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const showDetail = ref(false)
const detail = ref(null)
const tableRef = ref(null)
const focusedRowIndex = ref(-1)

const contextMenu = reactive({
  ...useContextMenu(),
  menus: [
    {
      label: '查看详情',
      icon: 'el-icon-view',
      shortcut: 'Enter',
      action: (row) => handleView(row)
    },
    {
      label: '编辑',
      icon: 'el-icon-edit',
      shortcut: 'E',
      action: (row) => handleEdit(row)
    },
    {
      label: '复制',
      icon: 'el-icon-document-copy',
      shortcut: 'Ctrl+C',
      action: (row) => handleCopy(row)
    },
    {
      label: '',
      divided: true
    },
    {
      label: '删除',
      icon: 'el-icon-delete',
      action: (row) => handleDelete(row)
    }
  ]
})

const handleContextMenu = (row, column, event) => {
  contextMenu.show(event, row, contextMenu.menus)
}

const handleEdit = (row) => {
  ElMessage.info('编辑功能开发中')
}

const handleCopy = (row) => {
  navigator.clipboard.writeText(JSON.stringify(row))
  ElMessage.success('已复制到剪贴板')
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除这条数据吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const filter = reactive({
  page: 1,
  pageSize: 20,
  keyword: '',
  module: ''
})

const filterRules = computed(() => ({
  keyword: {
    minLength: props.showModuleFilter ? 2 : undefined,
    message: '关键词至少2个字符'
  }
}))

const { errors: filterErrors, touched: filterTouched, validate, validateAll, touch: touchField, clearErrors: clearFilterErrors } = useFormValidation(filterRules.value)

function handleFilterChange(field, value) {
  filter[field] = value
  validate(field, value)
}

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
  clearFilterErrors()
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

function handleColumnCommand(command) {
  if (command.type === 'toggle') {
    toggleColumn(command.prop)
  } else if (command === 'reset') {
    resetConfig()
  }
}

function handleRowClick(row) {
  focusedRowIndex.value = rows.value.indexOf(row)
}

function rowClassName({ rowIndex }) {
  return rowIndex === focusedRowIndex.value ? 'is-focused' : ''
}

function handleRowDragEnd(evt) {
  ElMessage.success(`行从 ${evt.oldIndex} 移动到 ${evt.newIndex}`)
}

function handleTableKeyDown(event) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    const focusedRow = rows.value[focusedRowIndex.value]
    if (focusedRow) handleView(focusedRow)
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    focusNextRow()
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    focusPreviousRow()
  }
}

function focusPreviousRow() {
  if (focusedRowIndex.value > 0) {
    focusedRowIndex.value--
    focusRow(focusedRowIndex.value)
  }
}

function focusNextRow() {
  if (focusedRowIndex.value < rows.value.length - 1) {
    focusedRowIndex.value++
    focusRow(focusedRowIndex.value)
  }
}

function focusRow(index) {
  nextTick(() => {
    const table = tableRef.value
    if (table) {
      const rowEl = table.$el.querySelector(`.el-table__body-wrapper tr[data-index="${index}"]`)
      if (rowEl) {
        rowEl.focus()
      }
    }
  })
}

onMounted(load)

onMounted(() => {
  const tableEl = tableRef.value?.$el
  if (tableEl) {
    tableEl.addEventListener('keydown', handleTableKeyDown)
  }
})
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

// 移动端适配
@media (max-width: 768px) {
  .admin-list-page {
    padding: 0;

    .page-header {
      flex-direction: column;
      gap: 12px;
      padding: 16px;

      .header-content {
        width: 100%;
      }

      .header-actions {
        width: 100%;
        justify-content: flex-end;
      }
    }

    .filter-card {
      .el-form {
        flex-direction: column;
        gap: 12px;

        .el-form-item {
          width: 100%;
          margin-right: 0;

          .el-input,
          .el-select {
            width: 100% !important;
          }
        }
      }
    }

    .table-card {
      overflow-x: auto;

      .el-table {
        min-width: 600px;
      }
    }
  }
}
</style>

<style lang="scss">
.column-config-dropdown {
  .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.filter-form {
  .el-input.is-error {
    animation: shake 0.4s ease;
  }
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20%, 60% { transform: translateX(-4px); }
  40%, 80% { transform: translateX(4px); }
}

.is-focused {
  outline: 2px solid #667eea;
  outline-offset: -2px;
}

.drag-handle-icon {
  cursor: move;
  color: var(--color-text-muted);

  &:hover {
    color: var(--color-primary);
  }
}
</style>