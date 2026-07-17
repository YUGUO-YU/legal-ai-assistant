import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useLawStore = defineStore('law', () => {
  const categoryTypes = ref([])
  const categories = ref([])
  const loading = ref(false)

  async function loadCategoryTypes() {
    if (categoryTypes.value.length > 0) return
    loading.value = true
    try {
      const res = await api.get('/admin/law/category-types')
      categoryTypes.value = res || []
    } finally {
      loading.value = false
    }
  }

  async function loadCategories(typeId) {
    loading.value = true
    try {
      const res = await api.get('/admin/law/categories', { params: { typeId } })
      categories.value = res || []
    } finally {
      loading.value = false
    }
  }

  return { categoryTypes, categories, loading, loadCategoryTypes, loadCategories }
})
