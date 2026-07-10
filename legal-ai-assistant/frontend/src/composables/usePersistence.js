import { ref, watch, onMounted } from 'vue'

export function usePersistence(key, defaultValue, options = {}) {
  const { storage = 'local', ttl } = options
  
  const getStorage = () => {
    return storage === 'session' ? sessionStorage : localStorage
  }
  
  const load = () => {
    try {
      const raw = getStorage().getItem(key)
      if (!raw) return defaultValue
      
      const data = JSON.parse(raw)
      
      if (ttl && data.expiresAt && Date.now() > data.expiresAt) {
        getStorage().removeItem(key)
        return defaultValue
      }
      
      return data.value
    } catch {
      return defaultValue
    }
  }
  
  const data = ref(load())
  
  const save = () => {
    try {
      const payload = {
        value: data.value,
        updatedAt: Date.now(),
        expiresAt: ttl ? Date.now() + ttl : null
      }
      getStorage().setItem(key, JSON.stringify(payload))
    } catch (e) {
      console.warn('Persistence save failed:', e)
    }
  }
  
  watch(data, save, { deep: true })
  
  const reset = () => {
    data.value = defaultValue
    getStorage().removeItem(key)
  }
  
  return {
    data,
    reset
  }
}
