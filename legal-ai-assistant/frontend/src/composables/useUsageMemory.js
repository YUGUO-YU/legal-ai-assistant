import { ref, computed } from 'vue'

const STORAGE_KEY = 'usage_memory'
const EXPIRE_MS = 24 * 60 * 60 * 1000

function genId() {
  return Date.now().toString(36) + Math.random().toString(36).substring(2, 7)
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

export function useUsageMemory() {
  const records = ref(loadRecords())
  const justCleared = ref(false)

  const cleanedCount = ref(0)

  const expiredRecords = computed(() => {
    const now = Date.now()
    return records.value.filter(r => now - r.timestamp >= EXPIRE_MS)
  })

  const validRecords = computed(() => {
    const now = Date.now()
    return records.value.filter(r => now - r.timestamp < EXPIRE_MS)
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

  function addRecord(type, title, desc, extra = {}) {
    const wasExpired = checkAndClean()

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
    return wasExpired
  }

  function clearAll() {
    records.value = []
    justCleared.value = true
    saveRecords(records.value)
    setTimeout(() => { justCleared.value = false }, 3000)
  }

  function removeRecord(id) {
    records.value = records.value.filter(r => r.id !== id)
    saveRecords(records.value)
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
    formatDate
  }
}
