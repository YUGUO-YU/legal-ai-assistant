import { ref, readonly } from 'vue'

const notifications = ref([])
let idCounter = 0

export function useNotification() {
  const add = (options) => {
    const id = ++idCounter
    const notification = {
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
  
  const remove = (id) => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      const notification = notifications.value[index]
      notifications.value.splice(index, 1)
      if (notification.onClose) {
        notification.onClose()
      }
    }
  }
  
  const clear = () => {
    notifications.value = []
  }
  
  const success = (message, options = {}) => add({ type: 'success', message, ...options })
  const error = (message, options = {}) => add({ type: 'error', message, ...options })
  const warning = (message, options = {}) => add({ type: 'warning', message, ...options })
  const info = (message, options = {}) => add({ type: 'info', message, ...options })
  
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
