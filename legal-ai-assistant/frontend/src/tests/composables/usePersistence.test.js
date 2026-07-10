import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { usePersistence } from '@/composables/usePersistence'
import { nextTick } from 'vue'

describe('usePersistence', () => {
  const originalLocalStorage = global.localStorage
  const originalSessionStorage = global.sessionStorage

  beforeEach(() => {
    vi.useFakeTimers()
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    const sessionStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    global.localStorage = localStorageMock
    global.sessionStorage = sessionStorageMock
  })

  afterEach(() => {
    vi.useRealTimers()
    global.localStorage = originalLocalStorage
    global.sessionStorage = originalSessionStorage
  })

  describe('load', () => {
    it('should return default value when storage is empty', () => {
      global.localStorage.getItem.mockReturnValue(null)
      const { data } = usePersistence('test-key', 'default')
      expect(data.value).toBe('default')
    })

    it('should load value from localStorage', () => {
      global.localStorage.getItem.mockReturnValue(JSON.stringify({
        value: 'stored-value',
        updatedAt: Date.now()
      }))
      const { data } = usePersistence('test-key', 'default')
      expect(data.value).toBe('stored-value')
    })

    it('should handle JSON parse error', () => {
      global.localStorage.getItem.mockReturnValue('invalid-json')
      const { data } = usePersistence('test-key', 'default')
      expect(data.value).toBe('default')
    })

    it('should respect TTL expiration', () => {
      global.localStorage.getItem.mockReturnValue(JSON.stringify({
        value: 'expired-value',
        updatedAt: Date.now() - 20000,
        expiresAt: Date.now() - 10000
      }))
      const { data } = usePersistence('test-key', 'default', { ttl: 5000 })
      expect(data.value).toBe('default')
    })
  })

  describe('session storage', () => {
    it('should use sessionStorage when specified', () => {
      global.sessionStorage.getItem.mockReturnValue(JSON.stringify({
        value: 'session-value',
        updatedAt: Date.now()
      }))
      const { data } = usePersistence('test-key', 'default', { storage: 'session' })
      expect(data.value).toBe('session-value')
      expect(global.sessionStorage.getItem).toHaveBeenCalled()
    })
  })

  describe('save', () => {
    it('should save to localStorage on data change', async () => {
      vi.useRealTimers()
      global.localStorage.getItem.mockReturnValue(null)
      const { data } = usePersistence('test-key', 'default')
      
      data.value = 'new-value'
      await nextTick()
      
      expect(global.localStorage.setItem).toHaveBeenCalled()
    })
  })

  describe('reset', () => {
    it('should reset data to default and clear storage', async () => {
      vi.useRealTimers()
      global.localStorage.getItem.mockReturnValue(JSON.stringify({
        value: 'stored-value',
        updatedAt: Date.now()
      }))
      const { data, reset } = usePersistence('test-key', 'default')
      
      reset()
      
      expect(data.value).toBe('default')
      expect(global.localStorage.removeItem).toHaveBeenCalledWith('test-key')
    })
  })

  describe('complex data', () => {
    it('should handle object values', () => {
      global.localStorage.getItem.mockReturnValue(JSON.stringify({
        value: { name: 'test', age: 25 },
        updatedAt: Date.now()
      }))
      const { data } = usePersistence('test-key', { name: '', age: 0 })
      expect(data.value).toEqual({ name: 'test', age: 25 })
    })

    it('should handle array values', () => {
      global.localStorage.getItem.mockReturnValue(JSON.stringify({
        value: [1, 2, 3],
        updatedAt: Date.now()
      }))
      const { data } = usePersistence('test-key', [])
      expect(data.value).toEqual([1, 2, 3])
    })
  })
})
