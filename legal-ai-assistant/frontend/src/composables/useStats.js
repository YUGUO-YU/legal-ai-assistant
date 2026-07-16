import { ref, computed } from 'vue'

const STORAGE_KEY = 'user_stats'

function loadStats() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return getDefaultStats()
    return { ...getDefaultStats(), ...JSON.parse(raw) }
  } catch {
    return getDefaultStats()
  }
}

function saveStats(stats) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(stats))
}

function getDefaultStats() {
  return {
    searchCount: 0,
    caseAnalysisCount: 0,
    documentDraftCount: 0,
    sessionCount: 0,
    lastUpdated: null
  }
}

export function useStats() {
  const stats = ref(loadStats())

  function increment(key) {
    if (key in stats.value) {
      stats.value[key]++
      stats.value.lastUpdated = Date.now()
      saveStats(stats.value)
    }
  }

  function get(key) {
    return stats.value[key] || 0
  }

  return {
    stats,
    increment,
    get
  }
}
