import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ref, nextTick } from 'vue'
import { createMount } from '../utils'
import NotificationToast from '@/components/common/NotificationToast.vue'

const mockNotifications = ref([
  {
    id: 1,
    type: 'success',
    message: 'Test message',
    title: 'Test',
    duration: 3000,
    dismissible: true,
    actions: []
  }
])

vi.mock('@/composables/useNotification', () => ({
  useNotification: () => ({
    notifications: mockNotifications,
    remove: vi.fn(),
    success: vi.fn(),
    error: vi.fn()
  })
}))

describe('NotificationToast', () => {
  beforeEach(() => {
    mockNotifications.value = [{
      id: 1,
      type: 'success',
      message: 'Test message',
      title: 'Test',
      duration: 3000,
      dismissible: true,
      actions: []
    }]
  })

  it('should render notifications', async () => {
    const wrapper = createMount(NotificationToast, {
      global: {
        stubs: {
          Teleport: true
        }
      }
    })
    await nextTick()
    expect(wrapper.find('.notification').exists()).toBe(true)
  })

  it('should display correct message', async () => {
    const wrapper = createMount(NotificationToast, {
      global: {
        stubs: {
          Teleport: true
        }
      }
    })
    await nextTick()
    expect(wrapper.text()).toContain('Test message')
  })
})
