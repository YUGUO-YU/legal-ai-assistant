<template>
  <div class="realtime-notification">
    <TransitionGroup name="notification-slide" tag="div" class="notification-list">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-item"
        :class="[`notification-${notification.level}`]"
      >
        <div class="notification-icon">
          <i :class="getIcon(notification.level)"></i>
        </div>
        <div class="notification-content">
          <div class="notification-title">{{ notification.title }}</div>
          <div class="notification-message">{{ notification.message }}</div>
          <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
        </div>
        <button class="notification-close" @click="remove(notification.id)">
          <i class="el-icon-close"></i>
        </button>
      </div>
    </TransitionGroup>

    <div v-if="notifications.length > 0" class="notification-footer">
      <button @click="clearAll">清除全部</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { wsService } from '@/services/websocketService'
import { useNotification } from '@/composables/useNotification'

const { success, error, warning, info } = useNotification()

const notifications = ref([])
let idCounter = 0

const getIcon = (level) => {
  const icons = {
    info: 'el-icon-info',
    success: 'el-icon-success',
    warning: 'el-icon-warning',
    error: 'el-icon-error',
    critical: 'el-icon-warning'
  }
  return icons[level] || icons.info
}

const formatTime = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const add = (notification) => {
  const id = ++idCounter
  notifications.value.unshift({
    id,
    ...notification,
    createdAt: Date.now()
  })

  if (notifications.value.length > 10) {
    notifications.value.pop()
  }

  switch (notification.level) {
    case 'success':
      success(notification.message, { title: notification.title })
      break
    case 'error':
    case 'critical':
      error(notification.message, { title: notification.title })
      break
    case 'warning':
      warning(notification.message, { title: notification.title })
      break
    default:
      info(notification.message, { title: notification.title })
  }
}

const remove = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

const clearAll = () => {
  notifications.value = []
}

const handleAlert = (payload) => {
  add({
    level: payload.level || 'warning',
    title: payload.title || '告警通知',
    message: payload.message || '您有一条新的告警',
    data: payload
  })
}

const handleMessage = (payload) => {
  add({
    level: 'info',
    title: '新消息',
    message: payload.content || '您有一条新消息',
    data: payload
  })
}

onMounted(() => {
  wsService.subscribe('alert', handleAlert)
  wsService.subscribe('notification', handleMessage)
  wsService.subscribe('system', (payload) => {
    add({
      level: payload.level || 'info',
      title: '系统通知',
      message: payload.message,
      data: payload
    })
  })
})
</script>

<style scoped>
.realtime-notification {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 360px;
  z-index: 9000;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  border-left: 4px solid;
}

.notification-item.notification-info {
  border-left-color: #409eff;
}

.notification-item.notification-info .notification-icon {
  color: #409eff;
}

.notification-item.notification-success {
  border-left-color: #67c23a;
}

.notification-item.notification-success .notification-icon {
  color: #67c23a;
}

.notification-item.notification-warning {
  border-left-color: #e6a23c;
}

.notification-item.notification-warning .notification-icon {
  color: #e6a23c;
}

.notification-item.notification-error,
.notification-item.notification-critical {
  border-left-color: #f56c6c;
}

.notification-item.notification-error .notification-icon,
.notification-item.notification-critical .notification-icon {
  color: #f56c6c;
}

.notification-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.notification-message {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.4;
}

.notification-time {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.notification-close {
  padding: 4px;
  background: transparent;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
}

.notification-close:hover {
  background: var(--color-bg-soft);
  color: var(--color-text-primary);
}

.notification-footer {
  text-align: center;
  margin-top: 12px;
}

.notification-footer button {
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: 13px;
  cursor: pointer;
}

.notification-footer button:hover {
  color: var(--color-primary);
}

.notification-slide-enter-active {
  animation: slideIn 0.3s ease;
}

.notification-slide-leave-active {
  animation: slideOut 0.25s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(100%);
  }
}
</style>