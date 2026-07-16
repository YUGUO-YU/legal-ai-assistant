import { ref, computed, readonly } from 'vue'

const STORAGE_KEY = 'notification_center'

export interface CenterNotification {
  id: string
  type: 'info' | 'success' | 'warning' | 'error' | 'system'
  title: string
  message: string
  read: boolean
  createdAt: number
  link?: string
}

function loadFromStorage(): CenterNotification[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    return JSON.parse(raw)
  } catch {
    return []
  }
}

function saveToStorage(items: CenterNotification[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items.slice(0, 100)))
}

let idCounter = Date.now()

const DEMO_NOTIFIED_KEY = 'notification_center_demo_shown'

export function useNotificationCenter() {
  const stored = loadFromStorage()
  if (stored.length === 0 && !localStorage.getItem(DEMO_NOTIFIED_KEY)) {
    const demo: CenterNotification[] = [{
      id: (++idCounter).toString(36) + Math.random().toString(36).substring(2, 6),
      type: 'system',
      title: '欢迎使用法律AI助手',
      message: '您可以点击铃铛图标查看系统通知和最新消息。各项功能均已就绪，开始使用吧！',
      read: false,
      createdAt: Date.now()
    }]
    saveToStorage(demo)
    localStorage.setItem(DEMO_NOTIFIED_KEY, '1')
    stored.push(...demo)
  }

  const notifications = ref<CenterNotification[]>(stored)

  const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

  function add(options: Omit<CenterNotification, 'id' | 'read' | 'createdAt'>): string {
    const id = (++idCounter).toString(36) + Math.random().toString(36).substring(2, 6)
    const item: CenterNotification = {
      ...options,
      id,
      read: false,
      createdAt: Date.now()
    }
    notifications.value = [item, ...notifications.value].slice(0, 100)
    saveToStorage(notifications.value)
    return id
  }

  function markRead(id: string) {
    const item = notifications.value.find(n => n.id === id)
    if (item) {
      item.read = true
      saveToStorage(notifications.value)
    }
  }

  function markAllRead() {
    notifications.value.forEach(n => { n.read = true })
    saveToStorage(notifications.value)
  }

  function remove(id: string) {
    notifications.value = notifications.value.filter(n => n.id !== id)
    saveToStorage(notifications.value)
  }

  function clearAll() {
    notifications.value = []
    saveToStorage([])
  }

  function formatTime(timestamp: number): string {
    const diff = Date.now() - timestamp
    const mins = Math.floor(diff / 60000)
    if (mins < 1) return '刚刚'
    if (mins < 60) return `${mins}分钟前`
    const hours = Math.floor(mins / 60)
    if (hours < 24) return `${hours}小时前`
    const days = Math.floor(hours / 24)
    if (days < 7) return `${days}天前`
    return new Date(timestamp).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }

  return {
    notifications: readonly(notifications),
    unreadCount,
    add,
    markRead,
    markAllRead,
    remove,
    clearAll,
    formatTime
  }
}
