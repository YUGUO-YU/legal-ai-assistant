<template>
  <Teleport to="body">
    <TransitionGroup name="notification" tag="div" class="notification-container">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        class="notification"
        :class="[`notification-${notification.type}`]"
        @click="notification.onClick && notification.onClick()"
      >
        <div class="notification-icon">
          <i :class="getIcon(notification.type)"></i>
        </div>
        <div class="notification-content">
          <div v-if="notification.title" class="notification-title">
            {{ notification.title }}
          </div>
          <div class="notification-message">{{ notification.message }}</div>
          <div v-if="notification.actions.length" class="notification-actions">
            <button
              v-for="action in notification.actions"
              :key="action.label"
              class="notification-action-btn"
              @click.stop="action.handler()"
            >
              {{ action.label }}
            </button>
          </div>
        </div>
        <button
          v-if="notification.dismissible"
          class="notification-close"
          @click.stop="remove(notification.id)"
        >
          <i class="el-icon-close"></i>
        </button>
        <div class="notification-progress" v-if="notification.duration > 0">
          <div
            class="notification-progress-bar"
            :style="{ animationDuration: notification.duration + 'ms' }"
          ></div>
        </div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { useNotification } from '@/composables/useNotification'

const { notifications, remove } = useNotification()

const getIcon = (type) => {
  const icons = {
    success: 'el-icon-success',
    error: 'el-icon-error',
    warning: 'el-icon-warning',
    info: 'el-icon-info'
  }
  return icons[type] || icons.info
}
</script>

<style lang="scss" scoped>
.notification-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 400px;

  @media (max-width: 480px) {
    left: 16px;
    right: 16px;
    max-width: none;
  }
}

.notification {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  border-left: 4px solid;
  cursor: pointer;
  overflow: hidden;

  &.notification-success {
    border-left-color: #67c23a;
    .notification-icon { color: #67c23a; }
  }

  &.notification-error {
    border-left-color: #f56c6c;
    .notification-icon { color: #f56c6c; }
  }

  &.notification-warning {
    border-left-color: #e6a23c;
    .notification-icon { color: #e6a23c; }
  }

  &.notification-info {
    border-left-color: #409eff;
    .notification-icon { color: #409eff; }
  }
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
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.notification-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.notification-action-btn {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-primary);
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-primary);
    color: #fff;
  }
}

.notification-close {
  padding: 4px;
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: var(--color-text-primary);
  }
}

.notification-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: rgba(0, 0, 0, 0.1);
}

.notification-progress-bar {
  height: 100%;
  background: currentColor;
  opacity: 0.3;
  animation: progress linear forwards;
}

@keyframes progress {
  from { width: 100%; }
  to { width: 0%; }
}

.notification-enter-active {
  animation: notificationIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-leave-active {
  animation: notificationOut 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes notificationIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes notificationOut {
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
