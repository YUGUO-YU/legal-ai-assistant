import { describe, it, expect, vi } from 'vitest'
import { ref } from 'vue'
import { createMount } from '../utils'
import NotificationToast from '@/components/common/NotificationToast.vue'

vi.mock('@/composables/useNotification', () => ({
  useNotification: () => ({
    notifications: ref([
      {
        id: 1,
        type: 'success',
        message: 'Test message',
        title: 'Test',
        duration: 3000,
        dismissible: true,
        actions: []
      }
    ]),
    remove: vi.fn(),
    success: vi.fn(),
    error: vi.fn()
  })
}))

describe('NotificationToast', () => {
  it('should render notifications', () => {
    const wrapper = createMount(NotificationToast)
    expect(wrapper.find('.notification').exists()).toBe(true)
  })

  it('should display correct message', () => {
    const wrapper = createMount(NotificationToast)
    expect(wrapper.text()).toContain('Test message')
  })
})
