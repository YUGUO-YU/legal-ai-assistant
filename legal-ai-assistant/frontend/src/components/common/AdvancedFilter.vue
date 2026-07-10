<template>
  <div class="advanced-filter">
    <div class="filter-bar">
      <div class="filter-tags" v-if="activeFilters.length > 0">
        <el-tag
          v-for="(filter, index) in activeFilters"
          :key="index"
          closable
          @close="removeFilter(index)"
          :type="getTagType(filter.operator)"
        >
          <span class="tag-label">{{ filter.label }}:</span>
          <span class="tag-value">{{ formatFilterValue(filter) }}</span>
        </el-tag>
        <el-button type="text" @click="clearAll" class="clear-btn">
          清除全部
        </el-button>
      </div>
      
      <div class="filter-actions">
        <el-dropdown trigger="click" @command="handleAddFilter">
          <el-button type="primary" plain size="small">
            <i class="el-icon-plus"></i>
            添加条件
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item 
                v-for="field in filterFields" 
                :key="field.key"
                :command="field.key"
                :disabled="isFieldUsed(field.key)"
              >
                {{ field.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        
        <el-button 
          v-if="showSearchBtn"
          type="primary" 
          size="small" 
          @click="handleSearch"
          :loading="searching"
        >
          搜索
        </el-button>
      </div>
    </div>
    
    <Transition name="filter-expand">
      <div v-if="showFilterPanel" class="filter-panel">
        <div class="filter-row">
          <div class="filter-item">
            <el-select 
              v-model="currentField" 
              placeholder="选择字段"
              size="default"
              @change="handleFieldChange"
            >
              <el-option
                v-for="field in availableFields"
                :key="field.key"
                :label="field.label"
                :value="field.key"
              />
            </el-select>
          </div>
          
          <div class="filter-item">
            <el-select 
              v-model="currentOperator" 
              placeholder="选择操作符"
              size="default"
            >
              <el-option
                v-for="op in currentFieldConfig?.operators || []"
                :key="op.value"
                :label="op.label"
                :value="op.value"
              />
            </el-select>
          </div>
          
          <div class="filter-item filter-value">
            <component 
              :is="getValueComponent(currentFieldConfig)"
              v-model="currentValue"
              v-bind="getValueProps(currentFieldConfig)"
            />
          </div>
          
          <div class="filter-item">
            <el-button @click="addFilter" type="primary">
              添加
            </el-button>
            <el-button @click="cancelFilter">
              取消
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
    
    <div v-if="savedFilters.length > 0" class="saved-filters">
      <el-dropdown trigger="click">
        <el-button type="text" size="small">
          <i class="el-icon-document"></i>
          保存的方案
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item 
              v-for="(sf, index) in savedFilters"
              :key="index"
            >
              <span @click="applySavedFilter(sf)">{{ sf.name }}</span>
              <el-button 
                type="text" 
                size="small" 
                @click.stop="deleteSavedFilter(index)"
                class="delete-saved"
              >
                删除
              </el-button>
            </el-dropdown-item>
            <el-dropdown-item divided @click="showSaveDialog = true">
              保存当前方案
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    
    <el-dialog v-model="showSaveDialog" title="保存筛选方案" width="400px">
      <el-input v-model="filterName" placeholder="请输入方案名称" />
      <template #footer>
        <el-button @click="showSaveDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCurrentFilter">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  fields: {
    type: Array,
    required: true
  },
  savedFilters: {
    type: Array,
    default: () => []
  },
  searching: {
    type: Boolean,
    default: false
  },
  showSearchBtn: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['search', 'update:savedFilters'])

const activeFilters = ref([])
const showFilterPanel = ref(false)
const currentField = ref('')
const currentOperator = ref('')
const currentValue = ref(null)
const showSaveDialog = ref(false)
const filterName = ref('')

const filterFields = computed(() => props.fields)

const availableFields = computed(() => {
  return filterFields.value.filter(f => !isFieldUsed(f.key))
})

const currentFieldConfig = computed(() => {
  return filterFields.value.find(f => f.key === currentField.value)
})

const isFieldUsed = (key) => {
  return activeFilters.value.some(f => f.field === key)
}

const getTagType = (operator) => {
  const types = {
    '=': 'success',
    '!=': 'danger',
    '>': 'warning',
    '<': 'warning',
    '>=': 'warning',
    '<=': 'warning',
    'contains': 'info',
    'startsWith': 'info',
    'endsWith': 'info',
    'between': 'warning'
  }
  return types[operator] || 'info'
}

const formatFilterValue = (filter) => {
  if (filter.operator === 'between' && Array.isArray(filter.value)) {
    return `${filter.value[0]} ~ ${filter.value[1]}`
  }
  if (Array.isArray(filter.value)) {
    return filter.value.join(', ')
  }
  return filter.value
}

const handleAddFilter = (fieldKey) => {
  currentField.value = fieldKey
  currentOperator.value = ''
  currentValue.value = null
  showFilterPanel.value = true
}

const handleFieldChange = () => {
  currentOperator.value = ''
  currentValue.value = null
}

const getValueComponent = (config) => {
  if (!config) return 'el-input'
  
  switch (config.type) {
    case 'select':
    case 'radio':
      return 'el-select'
    case 'date':
    case 'daterange':
      return 'el-date-picker'
    case 'number':
      return 'el-input-number'
    default:
      return 'el-input'
  }
}

const getValueProps = (config) => {
  if (!config) return {}
  
  const baseProps = {
    placeholder: config.placeholder || '请输入'
  }
  
  if (config.type === 'select' || config.type === 'radio') {
    return {
      ...baseProps,
      options: config.options || []
    }
  }
  
  if (config.type === 'daterange') {
    return {
      ...baseProps,
      type: 'daterange',
      rangeSeparator: '至',
      startPlaceholder: '开始日期',
      endPlaceholder: '结束日期'
    }
  }
  
  if (config.type === 'number') {
    return {
      ...baseProps,
      min: 0,
      precision: 0
    }
  }
  
  return baseProps
}

const addFilter = () => {
  if (!currentField.value || !currentOperator.value || !currentValue.value) {
    return
  }
  
  const field = filterFields.value.find(f => f.key === currentField.value)
  
  activeFilters.value.push({
    field: currentField.value,
    label: field?.label || currentField.value,
    operator: currentOperator.value,
    value: currentValue.value
  })
  
  cancelFilter()
  handleSearch()
}

const cancelFilter = () => {
  showFilterPanel.value = false
  currentField.value = ''
  currentOperator.value = ''
  currentValue.value = null
}

const removeFilter = (index) => {
  activeFilters.value.splice(index, 1)
  handleSearch()
}

const clearAll = () => {
  activeFilters.value = []
  handleSearch()
}

const handleSearch = () => {
  emit('search', {
    filters: activeFilters.value,
    query: buildQuery()
  })
}

const buildQuery = () => {
  const query = {}
  activeFilters.value.forEach(f => {
    query[f.field] = {
      operator: f.operator,
      value: f.value
    }
  })
  return query
}

const saveCurrentFilter = () => {
  if (!filterName.value) return
  
  const newSaved = {
    name: filterName.value,
    filters: [...activeFilters.value]
  }
  
  emit('update:savedFilters', [...props.savedFilters, newSaved])
  filterName.value = ''
  showSaveDialog.value = false
}

const applySavedFilter = (saved) => {
  activeFilters.value = [...saved.filters]
  handleSearch()
}

const deleteSavedFilter = (index) => {
  const updated = props.savedFilters.filter((_, i) => i !== index)
  emit('update:savedFilters', updated)
}
</script>

<style scoped>
.advanced-filter {
  background: var(--color-bg);
  border-radius: var(--radius-md);
  padding: 16px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  flex: 1;
  
  :deep(.el-tag) {
    border-radius: 20px;
    padding: 0 12px;
    height: 32px;
    line-height: 30px;
    
    .tag-label {
      color: var(--color-text-muted);
      margin-right: 4px;
    }
    
    .tag-value {
      font-weight: 500;
    }
  }
}

.clear-btn {
  color: var(--color-text-muted);
  font-size: 13px;
  
  &:hover {
    color: var(--color-primary);
  }
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.filter-panel {
  margin-top: 16px;
  padding: 16px;
  background: var(--color-bg-soft);
  border-radius: var(--radius-md);
  border: 1px dashed var(--color-border);
}

.filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.filter-item {
  min-width: 150px;
  
  &.filter-value {
    flex: 1;
    min-width: 200px;
  }
}

.saved-filters {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-light);
}

.delete-saved {
  margin-left: 12px;
  color: var(--el-color-danger);
}

.filter-expand-enter-active,
.filter-expand-leave-active {
  transition: all 0.3s ease;
}

.filter-expand-enter-from,
.filter-expand-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
