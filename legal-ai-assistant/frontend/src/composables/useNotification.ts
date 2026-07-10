import { ref, readonly } from 'vue'

export interface NotificationOptions {
  type?: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration?: number
  dismissible?: boolean
  actions?: NotificationAction[]
  onClose?: () => void
  onClick?: () => void
}

export interface NotificationAction {
  label: string
  handler: () => void
}

export interface Notification {
  id: number
  type: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration: number
  dismissible: boolean
  actions: NotificationAction[]
  onClose?: () => void
  onClick?: () => void
  createdAt: number
}

const notifications = ref<Notification[]>([])
let idCounter = 0

export function useNotification() {
  const add = (options: NotificationOptions): number => {
    const id = ++idCounter
    const notification: Notification = {
      id,
      type: options.type || 'info',
      title: options.title || '',
      message: options.message || '',
      duration: options.duration || 4000,
      dismissible: options.dismissible !== false,
      actions: options.actions || [],
      onClose: options.onClose,
      onClick: options.onClick,
      createdAt: Date.now()
    }
    
    notifications.value.push(notification)
    
    if (notification.duration > 0) {
      setTimeout(() => {
        remove(id)
      }, notification.duration)
    }
    
    return id
  }
  
  const remove = (id: number): void => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      const notification = notifications.value[index]
      notifications.value.splice(index, 1)
      if (notification.onClose) {
        notification.onClose()
      }
    }
  }
  
  const clear = (): void => {
    notifications.value = []
  }
  
  const success = (message: string, options?: Omit<NotificationOptions, 'message' | 'type'>): number => add({ type: 'success', message, ...options })
  const error = (message: string, options?: Omit<NotificationOptions, 'message' | 'type'>): number => add({ type: 'error', message, ...options })
  const warning = (message: string, options?: Omit<NotificationOptions, 'message' | 'type'>): number => add({ type: 'warning', message, ...options })
  const info = (message: string, options?: Omit<NotificationOptions, 'message' | 'type'>): number => add({ type: 'info', message, ...options })
  
  return {
    notifications: readonly(notifications),
    add,
    remove,
    clear,
    success,
    error,
    warning,
    info
  }
}
