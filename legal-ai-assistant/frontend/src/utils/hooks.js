import { ref } from 'vue'

export function usePagination(fetchFn, options = {}) {
  const { pageSize = 10, immediate = true } = options

  const data = ref([])
  const total = ref(0)
  const page = ref(1)
  const loading = ref(false)
  const error = ref(null)

  const load = async (pageNum = 1) => {
    loading.value = true
    error.value = null
    page.value = pageNum

    try {
      const res = await fetchFn({ page: pageNum, pageSize })
      data.value = res.data?.items || res.data || []
      total.value = res.data?.total || 0
    } catch (e) {
      error.value = e.message
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    page.value = 1
    data.value = []
    total.value = 0
    error.value = null
  }

  if (immediate) {
    load(1)
  }

  return {
    data,
    total,
    page,
    loading,
    error,
    load,
    reset
  }
}

export function useSearch(debounceMs = 300) {
  const keyword = ref('')
  const loading = ref(false)
  const results = ref([])
  const searched = ref(false)

  let debounceTimer = null

  const search = async (fn) => {
    if (debounceTimer) {
      clearTimeout(debounceTimer)
    }

    debounceTimer = setTimeout(async () => {
      if (!keyword.value.trim()) {
        results.value = []
        return
      }

      loading.value = true
      searched.value = true

      try {
        results.value = await fn(keyword.value)
      } catch (e) {
        console.error(e)
      } finally {
        loading.value = false
      }
    }, debounceMs)
  }

  const clear = () => {
    keyword.value = ''
    results.value = []
    searched.value = false
  }

  return {
    keyword,
    loading,
    results,
    searched,
    search,
    clear
  }
}

export function useSelection() {
  const selected = ref(new Set())
  const isAllSelected = ref(false)

  const toggle = (id) => {
    if (selected.value.has(id)) {
      selected.value.delete(id)
    } else {
      selected.value.add(id)
    }
  }

  const selectAll = (ids) => {
    ids.forEach(id => selected.value.add(id))
  }

  const clearSelection = () => {
    selected.value.clear()
    isAllSelected.value = false
  }

  const isSelected = (id) => selected.value.has(id)

  return {
    selected,
    isAllSelected,
    toggle,
    selectAll,
    clearSelection,
    isSelected
  }
}