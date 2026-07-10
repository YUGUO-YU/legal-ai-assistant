import { ref, watch } from 'vue'

export function useTableColumnConfig(tableName, defaultColumns) {
  const STORAGE_KEY = `table_columns_${tableName}`

  const columns = ref(JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null') || [...defaultColumns])

  const saveConfig = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(columns.value))
  }

  const resetConfig = () => {
    columns.value = [...defaultColumns]
    localStorage.removeItem(STORAGE_KEY)
  }

  const updateColumn = (index, newColumn) => {
    columns.value[index] = { ...columns.value[index], ...newColumn }
    saveConfig()
  }

  const toggleColumn = (prop) => {
    const index = columns.value.findIndex(c => c.prop === prop)
    if (index !== -1) {
      columns.value[index].hidden = !columns.value[index].hidden
      saveConfig()
    }
  }

  const handleHeaderDragend = (newWidth, oldWidth, column) => {
    const index = columns.value.findIndex(c => c.prop === column.property)
    if (index !== -1) {
      columns.value[index].width = newWidth
      saveConfig()
    }
  }

  watch(columns, saveConfig, { deep: true })

  return {
    columns,
    saveConfig,
    resetConfig,
    updateColumn,
    toggleColumn,
    handleHeaderDragend
  }
}
