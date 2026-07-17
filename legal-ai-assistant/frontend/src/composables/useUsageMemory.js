import { ref, computed } from 'vue'
import api from '@/api'

const STORAGE_KEY = 'usage_memory'
const EXPIRE_MS = 24 * 60 * 60 * 1000
const USER_ID_KEY = 'usage_user_id'

function genId() {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 7)
}

function getUserId() {
  let userId = localStorage.getItem(USER_ID_KEY)
  if (!userId) {
    userId = 'user_' + Math.random().toString(36).substring(2, 10)
    localStorage.setItem(USER_ID_KEY, userId)
  }
  return userId
}

function loadRecords() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
  } catch {
    return []
  }
}

function saveRecords(records) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records))
}

function cleanExpired(records) {
  const now = Date.now()
  return records.filter(r => now - r.timestamp < EXPIRE_MS)
}

async function syncFromBackend(userId) {
  try {
    const res = await api.usage.getRecords(userId, 200)
    if (res.data && Array.isArray(res.data)) {
      return res.data.map(r => ({
        id: r.id,
        type: r.type,
        title: r.title,
        desc: r.desc,
        timestamp: r.timestamp
      }))
    }
  } catch (e) {
    console.warn('Failed to sync from backend:', e)
  }
  return null
}

async function addRecordToBackend(record, userId) {
  try {
    await api.usage.addRecord({
      userId,
      type: record.type,
      title: record.title,
      desc: record.desc
    })
    return true
  } catch (e) {
    console.warn('Failed to add record to backend:', e)
    return false
  }
}

export function useUsageMemory() {
  const records = ref([])
  const justCleared = ref(false)
  const isLoading = ref(false)
  const cleanedCount = ref(0)
  let userId = getUserId()
  loadRecordsAsync()

  async function loadRecordsAsync() {
    isLoading.value = true
    try {
      const backendRecords = await syncFromBackend(userId)
      if (backendRecords && backendRecords.length > 0) {
        records.value = backendRecords
        saveRecords(backendRecords)
      } else {
        const localRecords = loadRecords()
        records.value = cleanExpired(localRecords)
      }
    } catch (e) {
      const localRecords = loadRecords()
      records.value = cleanExpired(localRecords)
    } finally {
      isLoading.value = false
    }
  }

  const expiredRecords = computed(() => {
    const now = Date.now()
    return records.value.filter(r => now - r.timestamp >= EXPIRE_MS)
  })

  const validRecords = computed(() => {
    return records.value
  })

  const groupByDate = computed(() => {
    const groups = {}
    for (const r of validRecords.value) {
      const d = new Date(r.timestamp)
      const dateKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      if (!groups[dateKey]) groups[dateKey] = []
      groups[dateKey].push(r)
    }
    return Object.entries(groups)
      .sort(([a], [b]) => b.localeCompare(a))
      .map(([date, items]) => ({ date, items }))
  })

  function checkAndClean() {
    const before = records.value.length
    const cleaned = cleanExpired(records.value)
    if (cleaned.length !== before) {
      cleanedCount.value = before - cleaned.length
      records.value = cleaned
      saveRecords(records.value)
      return true
    }
    return false
  }

  async function addRecord(type, title, desc, extra = {}) {
    checkAndClean()

    const record = {
      id: genId(),
      type,
      title,
      desc,
      timestamp: Date.now(),
      ...extra
    }

    records.value = [record, ...records.value].slice(0, 200)
    saveRecords(records.value)

    await addRecordToBackend(record, userId)

    return false
  }

  async function clearAll() {
    records.value = []
    justCleared.value = true
    saveRecords([])
    try {
      await api.usage.clearAll(userId)
    } catch (e) {
      console.warn('Failed to clear records from backend:', e)
    }
    setTimeout(() => { justCleared.value = false }, 3000)
  }

  async function removeRecord(id) {
    records.value = records.value.filter(r => r.id !== id)
    saveRecords(records.value)
    try {
      await api.usage.deleteRecord(id, userId)
    } catch (e) {
      console.warn('Failed to delete record from backend:', e)
    }
  }

  function getRecordCount() {
    return validRecords.value.length
  }

  function getTypeLabel(type) {
    const map = {
      search: '检索',
      document: '文书',
      contract: '合同审查',
      company: '企业查询',
      docqa: '文档问答',
      law: '法规搜索',
      case: '案例搜索',
      ppt: 'PPT生成',
      other: '其他'
    }
    return map[type] || type
  }

  function getTypeColor(type) {
    const map = {
      search: '#667eea',
      document: '#4facfe',
      contract: '#a18cd1',
      company: '#f5576c',
      docqa: '#43e97b',
      law: '#ff9a56',
      case: '#fbc2eb',
      ppt: '#11998e',
      other: '#64748b'
    }
    return map[type] || '#64748b'
  }

  function formatAge(timestamp) {
    const diff = Date.now() - timestamp
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return '刚刚'
    if (mins < 60) return `${mins}分钟前`
    const hours = Math.floor(mins / 60)
    if (hours < 24) return `${hours}小时前`
    const days = Math.floor(hours / 24)
    return `${days}天前`
  }

  function formatDate(timestamp) {
    const d = new Date(timestamp)
    return d.toLocaleString('zh-CN', {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    })
  }

  return {
    records,
    validRecords,
    groupByDate,
    justCleared,
    cleanedCount,
    expiredRecords,
    addRecord,
    clearAll,
    removeRecord,
    checkAndClean,
    getRecordCount,
    getTypeLabel,
    getTypeColor,
    formatAge,
    formatDate,
    loadRecords: loadRecordsAsync
  }
}
