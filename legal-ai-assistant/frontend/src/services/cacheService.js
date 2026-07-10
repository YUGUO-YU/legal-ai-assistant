const DEFAULT_TTL = 5 * 60 * 1000
const MEMORY_CACHE = new Map()

export const cacheService = {
  set(key, value, ttl = DEFAULT_TTL) {
    const expiresAt = ttl > 0 ? Date.now() + ttl : null
    MEMORY_CACHE.set(key, {
      value,
      expiresAt,
      createdAt: Date.now()
    })
  },
  
  get(key) {
    const item = MEMORY_CACHE.get(key)
    if (!item) return null
    
    if (item.expiresAt && Date.now() > item.expiresAt) {
      MEMORY_CACHE.delete(key)
      return null
    }
    
    return item.value
  },
  
  has(key) {
    return this.get(key) !== null
  },
  
  delete(key) {
    MEMORY_CACHE.delete(key)
  },
  
  clear() {
    MEMORY_CACHE.clear()
  },
  
  setStorage(key, value, ttlSeconds = 3600) {
    const data = {
      value,
      expiresAt: ttlSeconds > 0 ? Date.now() + ttlSeconds * 1000 : null
    }
    try {
      localStorage.setItem(key, JSON.stringify(data))
    } catch (e) {
      console.warn('LocalStorage cache set failed:', e)
    }
  },
  
  getStorage(key) {
    try {
      const raw = localStorage.getItem(key)
      if (!raw) return null
      
      const data = JSON.parse(raw)
      if (data.expiresAt && Date.now() > data.expiresAt) {
        localStorage.removeItem(key)
        return null
      }
      
      return data.value
    } catch (e) {
      return null
    }
  },
  
  deleteStorage(key) {
    localStorage.removeItem(key)
  },
  
  clearStorage(prefix = '') {
    if (!prefix) {
      localStorage.clear()
      return
    }
    
    const keys = Object.keys(localStorage)
    keys.forEach(key => {
      if (key.startsWith(prefix)) {
        localStorage.removeItem(key)
      }
    })
  }
}

export async function cachedRequest(key, fetchFn, options = {}) {
  const { ttl = DEFAULT_TTL, forceRefresh = false } = options
  
  if (!forceRefresh) {
    const cached = cacheService.get(key)
    if (cached !== null) {
      return cached
    }
  }
  
  const data = await fetchFn()
  cacheService.set(key, data, ttl)
  
  return data
}

export function clearCacheByPattern(pattern) {
  const regex = new RegExp(pattern)
  
  for (const key of MEMORY_CACHE.keys()) {
    if (regex.test(key)) {
      MEMORY_CACHE.delete(key)
    }
  }
}
