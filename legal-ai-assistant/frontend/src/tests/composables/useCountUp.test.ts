import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'

const add = (a: number, b: number) => a + b
const multiply = (a: number, b: number) => a * b

describe('Math utilities', () => {
  describe('add', () => {
    it('should add two numbers correctly', () => {
      expect(add(1, 2)).toBe(3)
      expect(add(-1, 1)).toBe(0)
      expect(add(0, 0)).toBe(0)
    })

    it('should handle negative numbers', () => {
      expect(add(-5, -3)).toBe(-8)
    })
  })

  describe('multiply', () => {
    it('should multiply two numbers correctly', () => {
      expect(multiply(2, 3)).toBe(6)
      expect(multiply(0, 5)).toBe(0)
      expect(multiply(-2, 3)).toBe(-6)
    })
  })
})

describe('CacheService', () => {
  const cacheService = {
    set: (key: string, value: any) => {
      localStorage.setItem(key, JSON.stringify({ value }))
    },
    get: (key: string) => {
      const raw = localStorage.getItem(key)
      if (!raw) return null
      const data = JSON.parse(raw)
      return data.value
    },
    delete: (key: string) => {
      localStorage.removeItem(key)
    },
    clear: () => {
      localStorage.clear()
    }
  }

  beforeEach(() => {
    cacheService.clear()
  })

  it('should set and get cache correctly', () => {
    cacheService.set('test', 'value')
    expect(cacheService.get('test')).toBe('value')
  })

  it('should return null for non-existent key', () => {
    expect(cacheService.get('non-existent')).toBeNull()
  })

  it('should delete cache correctly', () => {
    cacheService.set('test', 'value')
    cacheService.delete('test')
    expect(cacheService.get('test')).toBeNull()
  })
})
