import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api'

export const useStatsStore = defineStore('stats', () => {
  const stats = ref(null)
  const userActivity = ref([])
  const hourlyAccess = ref([])
  const lawUsage = ref([])
  const loading = ref(false)
  let cacheTime = 0
  const CACHE_TTL = 60000

  async function loadStats(force = false) {
    if (!force && stats.value && (Date.now() - cacheTime) < CACHE_TTL) return
    loading.value = true
    try {
      const [statsRes, activityRes, hourlyRes, lawRes] = await Promise.all([
        api.get('/admin/stats').catch(() => null),
        api.get('/admin/stats/user-activity').catch(() => null),
        api.get('/admin/stats/hourly-access').catch(() => null),
        api.get('/admin/stats/law-usage').catch(() => null)
      ])
      stats.value = statsRes || {}
      userActivity.value = activityRes?.data || []
      hourlyAccess.value = hourlyRes?.data || []
      lawUsage.value = lawRes?.data || []
      cacheTime = Date.now()
    } finally {
      loading.value = false
    }
  }

  return { stats, userActivity, hourlyAccess, lawUsage, loading, loadStats }
})
