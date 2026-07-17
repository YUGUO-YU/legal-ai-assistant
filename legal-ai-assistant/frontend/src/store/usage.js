import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useUsageStore = defineStore('usage', () => {
  const records = ref([])
  const total = ref(0)
  const loading = ref(false)

  async function loadRecords(params = {}) {
    loading.value = true
    try {
      const res = await api.get('/usage/records', { params })
      if (res?.data) {
        records.value = res.data.list || []
        total.value = res.data.total || 0
      }
    } finally {
      loading.value = false
    }
  }

  return { records, total, loading, loadRecords }
})
