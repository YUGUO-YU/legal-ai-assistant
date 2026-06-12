import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSearchStore = defineStore('search', () => {
  const recentSearches = ref(JSON.parse(localStorage.getItem('recentSearches') || '[]'))
  const searchHistory = ref([])

  const addRecentSearch = (query) => {
    const searches = recentSearches.value.filter(s => s !== query)
    searches.unshift(query)
    if (searches.length > 20) {
      searches.pop()
    }
    recentSearches.value = searches
    localStorage.setItem('recentSearches', JSON.stringify(searches))
  }

  const clearRecentSearches = () => {
    recentSearches.value = []
    localStorage.removeItem('recentSearches')
  }

  const addToHistory = (record) => {
    searchHistory.value.unshift({
      ...record,
      timestamp: Date.now()
    })
    if (searchHistory.value.length > 100) {
      searchHistory.value.pop()
    }
  }

  return {
    recentSearches,
    searchHistory,
    addRecentSearch,
    clearRecentSearches,
    addToHistory
  }
})