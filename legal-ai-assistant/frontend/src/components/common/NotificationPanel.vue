<template>
  <div class="notification-panel" v-if="visible" ref="panelRef">
    <div class="panel-header">
      <span class="panel-title">通知中心</span>
      <div class="panel-actions">
        <el-button text size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
          全标已读
        </el-button>
        <el-button text size="small" type="danger" @click="handleClearAll" :disabled="notifications.length === 0">
          清空
        </el-button>
      </div>
    </div>

    <div class="panel-body" v-if="notifications.length > 0">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.read, [`type-${item.type}`]: true }"
        @click="handleItemClick(item)"
      >
        <div class="item-icon">
          <el-icon><component :is="getIcon(item.type)" /></el-icon>
        </div>
        <div class="item-content">
          <div class="item-title">{{ item.title }}</div>
          <div class="item-message">{{ item.message }}</div>
          <div class="item-time">{{ formatTime(item.createdAt) }}</div>
        </div>
        <div class="item-actions">
          <el-button text size="small" @click.stop="handleRemove(item.id)">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <div class="panel-empty" v-else>
      <el-icon :size="40"><Bell /></el-icon>
      <p>暂无通知</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Close, Bell, CircleCheck, WarningFilled, CircleCloseFilled, InfoFilled } from '@element-plus/icons-vue'
import { useNotificationCenter } from '@/composables/useNotificationCenter'

defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])
const router = useRouter()
const { notifications, unreadCount, markRead, markAllRead, remove, clearAll, formatTime } = useNotificationCenter()

function getIcon(type) {
  const map = {
    success: CircleCheck,
    warning: WarningFilled,
    error: CircleCloseFilled,
    info: InfoFilled,
    system: Bell
  }
  return map[type] || Bell
}

function handleItemClick(item) {
  if (!item.read) {
    markRead(item.id)
  }
  if (item.link) {
    router.push(item.link)
    emit('close')
  }
}

function handleMarkAllRead() {
  markAllRead()
}

function handleRemove(id) {
  remove(id)
}

function handleClearAll() {
  clearAll()
}
</script>

<style scoped>
.notification-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 360px;
  max-height: 480px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color);
  flex-shrink: 0;
}

.panel-title {
  font-weight: 600;
  font-size: 14px;
}

.panel-actions {
  display: flex;
  gap: 4px;
}

.panel-body {
  overflow-y: auto;
  flex: 1;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-fill-color-light);
  transition: background 0.15s;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--el-fill-color-light);
}

.notification-item.unread {
  background: var(--el-color-primary-light-9);
}

.notification-item.unread:hover {
  background: var(--el-color-primary-light-8);
}

.item-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color);
}

.type-success .item-icon { color: var(--el-color-success); }
.type-warning .item-icon { color: var(--el-color-warning); }
.type-error .item-icon { color: var(--el-color-danger); }
.type-info .item-icon { color: var(--el-color-primary); }
.type-system .item-icon { color: var(--el-color-primary); }

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 2px;
}

.item-message {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-time {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

.item-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}

.notification-item:hover .item-actions {
  opacity: 1;
}

.panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-placeholder);
  gap: 8px;
}

.panel-empty p {
  margin: 0;
  font-size: 13px;
}
</style>
