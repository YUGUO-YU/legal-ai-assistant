import { vi } from 'vitest'
import { config } from '@vue/test-utils'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
    info: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn().mockResolvedValue(true),
    alert: vi.fn().mockResolvedValue(true),
    prompt: vi.fn().mockResolvedValue('input')
  }
}))

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  go: vi.fn(),
  back: vi.fn(),
  currentRoute: { value: { path: '/' } }
}

const mockRoute = {
  path: '/',
  name: 'home',
  params: {},
  query: {},
  fullPath: '/',
  matched: []
}

config.global.mocks = {
  $router: mockRouter,
  $route: mockRoute
}

config.global.plugins = []

config.global.provide = {
  $router: mockRouter,
  $route: mockRoute
}
