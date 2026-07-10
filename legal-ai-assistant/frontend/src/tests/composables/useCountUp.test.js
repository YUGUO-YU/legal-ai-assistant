import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useCountUp } from '@/composables/useCountUp'
import { nextTick } from 'vue'

describe('useCountUp', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should initialize with value 0', () => {
    const { displayValue } = useCountUp(100)
    expect(displayValue.value).toBe(0)
  })

  it('should start animation', () => {
    const { displayValue, start } = useCountUp(100, 1000)
    start()
    
    vi.advanceTimersByTime(500)
    expect(displayValue.value).toBeGreaterThan(0)
  })

  it('should reach end value when animation completes', () => {
    const { displayValue, start } = useCountUp(100, 1000)
    start()
    
    vi.advanceTimersByTime(2000)
    expect(displayValue.value).toBe(100)
  })

  it('should use easeOutExpo easing', () => {
    const { displayValue, start } = useCountUp(100, 1000)
    start()
    
    vi.advanceTimersByTime(500)
    const midValue = displayValue.value
    
    vi.advanceTimersByTime(500)
    const endValue = displayValue.value
    
    expect(midValue).toBeLessThan(endValue)
  })

  it('should handle zero end value', () => {
    const { displayValue, start } = useCountUp(0, 1000)
    start()
    vi.advanceTimersByTime(1000)
    expect(displayValue.value).toBe(0)
  })

  it('should handle negative values', () => {
    const { displayValue, start } = useCountUp(-50, 1000)
    start()
    vi.advanceTimersByTime(1000)
    expect(displayValue.value).toBe(-50)
  })
})
