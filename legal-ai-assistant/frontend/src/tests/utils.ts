import { mount, shallowMount, MountingOptions } from '@vue/test-utils'
import type { Component } from 'vue'

export const createMount = (component: Component, options?: MountingOptions<any>) => {
  return mount(component, {
    global: {
      mocks: {
        $router: {
          push: vi.fn(),
          replace: vi.fn()
        },
        $route: {
          path: '/',
          query: {}
        }
      },
      stubs: {
        'el-button': true,
        'el-input': true,
        'el-table': true,
        'el-form': true,
        'el-form-item': true,
        'el-select': true,
        'el-option': true,
        'el-dialog': true,
        'el-pagination': true
      }
    },
    ...options
  })
}

export const createShallowMount = (component: Component, options?: MountingOptions<any>) => {
  return shallowMount(component, options)
}
