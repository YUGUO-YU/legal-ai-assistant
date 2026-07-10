<template>
  <el-card class="advanced-search-panel" v-loading="loading">
    <template #header>
      <div class="panel-header">
        <div class="header-title">
          <el-icon><Search /></el-icon>
          <span>{{ title }}</span>
        </div>
        <el-button v-if="showSave" type="primary" link size="small" @click="showSaveDialog = true">
          保存筛选
        </el-button>
      </div>
    </template>

    <div class="search-filters">
      <div class="filter-section" v-for="(filter, index) in filters" :key="index">
        <div class="filter-label">{{ filter.label }}</div>
        <div class="filter-values">
          <el-select
            v-if="filter.type === 'select'"
            v-model="filterValues[filter.key]"
            :placeholder="filter.placeholder || '请选择'"
            clearable
            @change="handleFilterChange"
          >
            <el-option
              v-for="opt in filter.options"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>

          <el-input
            v-else-if="filter.type === 'text'"
            v-model="filterValues[filter.key]"
            :placeholder="filter.placeholder || '请输入'"
            clearable
            @input="handleFilterChange"
          />

          <el-date-picker
            v-else-if="filter.type === 'date'"
            v-model="filterValues[filter.key]"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="handleFilterChange"
          />

          <el-input-number
            v-else-if="filter.type === 'number'"
            v-model="filterValues[filter.key]"
            :min="filter.min"
            :max="filter.max"
            :placeholder="filter.placeholder"
            @change="handleFilterChange"
          />
        </div>
      </div>
    </div>

    <div class="active-filters" v-if="activeFilters.length > 0">
      <el-tag
        v-for="(tag, index) in activeFilters"
        :key="index"
        closable
        size="small"
        @close="removeFilter(tag)"
      >
        {{ tag.label }}: {{ tag.value }}
      </el-tag>
      <el-button type="primary" link size="small" @click="clearAllFilters">
        清除全部
      </el-button>
    </div>

    <div class="search-actions">
      <el-button type="primary" @click="handleSearch" :icon="Search">
        搜索
      </el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-dialog v-model="showSaveDialog" title="保存筛选方案" width="400px">
      <el-form>
        <el-form-item label="方案名称">
          <el-input v-model="saveName" placeholder="请输入方案名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <div class="saved-filters" v-if="savedFilters.length > 0 && showSaved">
      <div class="saved-title">已保存的筛选方案</div>
      <div class="saved-list">
        <div
          v-for="sf in savedFilters"
          :key="sf.id"
          class="saved-item"
          @click="applySavedFilter(sf)"
        >
          <span>{{ sf.name }}</span>
          <el-icon><Close /></el-icon>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { Search, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  title: {
    type: String,
    default: '高级搜索'
  },
  filters: {
    type: Array,
    default: () => []
  },
  showSave: {
    type: Boolean,
    default: true
  },
  showSaved: {
    type: Boolean,
    default: true
  },
  initialValues: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['search', 'reset', 'save'])

const loading = ref(false)
const filterValues = reactive({ ...props.initialValues })
const showSaveDialog = ref(false)
const saveName = ref('')
const savedFilters = ref([])

const activeFilters = computed(() => {
  return Object.entries(filterValues)
    .filter(([key, value]) => {
      if (value === null || value === undefined || value === '') return false
      if (Array.isArray(value) && value.length === 0) return false
      return true
    })
    .map(([key, value]) => {
      const filter = props.filters.find(f => f.key === key)
      return {
        key,
        label: filter?.label || key,
        value: Array.isArray(value) ? value.join(',') : String(value)
      }
    })
})

const handleFilterChange = () => {
  // Auto-search can be enabled here if needed
}

const handleSearch = () => {
  emit('search', { ...filterValues })
}

const handleReset = () => {
  Object.keys(filterValues).forEach(key => {
    filterValues[key] = null
  })
  emit('reset')
}

const removeFilter = (tag) => {
  filterValues[tag.key] = null
}

const clearAllFilters = () => {
  handleReset()
}

const handleSave = () => {
  if (!saveName.value.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  emit('save', {
    name: saveName.value,
    filters: { ...filterValues }
  })
  savedFilters.value.push({
    id: Date.now(),
    name: saveName.value,
    filters: { ...filterValues }
  })
  saveName.value = ''
  showSaveDialog.value = false
  ElMessage.success('筛选方案已保存')
}

const applySavedFilter = (sf) => {
  Object.assign(filterValues, sf.filters)
  handleSearch()
}

const loadSavedFilters = () => {
  try {
    const saved = localStorage.getItem('savedFilters')
    if (saved) {
      savedFilters.value = JSON.parse(saved)
    }
  } catch (e) {
    console.warn('Failed to load saved filters')
  }
}

defineExpose({
  getFilters: () => ({ ...filterValues })
})
</script>

<style scoped>
.advanced-search-panel {
  border-radius: 12px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.search-filters {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-label {
  font-size: 12px;
  color: var(--text-color-secondary);
}

.filter-values {
  display: flex;
}

.active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-color);
  border-radius: 8px;
}

.active-filters .el-tag {
  margin-right: 4px;
}

.search-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.saved-filters {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.saved-title {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-bottom: 8px;
}

.saved-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.saved-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-color-light);
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.saved-item:hover {
  background: var(--hover-bg-color);
}
</style>
