<template>
  <el-card class="usage-memory-card">
    <template #header>
      <div class="card-header">
        <div class="header-title">
          <el-icon><Operation /></el-icon>
          <span>{{ title }}</span>
        </div>
        <div class="header-actions">
          <el-tag v-if="count > 0" type="info" size="small">{{ count }} 条</el-tag>
          <el-button v-if="count > 0" type="danger" size="small" link @click="$emit('clear')">
            清空
          </el-button>
        </div>
      </div>
    </template>

    <div v-if="justCleared" class="cleared-notice">
      <el-icon><CircleCheck /></el-icon>
      <span>使用记忆已清空</span>
    </div>

    <div v-else-if="count === 0" class="empty-memory">
      <el-icon><Clock /></el-icon>
      <span>暂无使用记录</span>
      <p>搜索、起草文书、审查合同等操作会被自动记录</p>
    </div>

    <div v-else class="memory-list">
      <div v-for="group in groups" :key="group.date" class="memory-group">
        <div class="memory-date">{{ formatDate(group.date) }}</div>
        <div
          v-for="item in group.items"
          :key="item.id"
          class="memory-item"
          @click="$emit('click', item)"
        >
          <div class="memory-item-icon" :style="{ background: getTypeColor(item.type) + '22' }">
            <el-icon><component :is="getTypeIcon(item.type)" /></el-icon>
          </div>
          <div class="memory-item-info">
            <span class="memory-item-title">{{ item.title }}</span>
            <span class="memory-item-desc">{{ item.desc }}</span>
          </div>
          <span class="memory-item-time">{{ formatAge(item.timestamp) }}</span>
          <el-button link size="small" type="danger" @click.stop="$emit('remove', item.id)">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { Operation, CircleCheck, Clock, Close } from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    default: '使用记忆'
  },
  count: {
    type: Number,
    default: 0
  },
  groups: {
    type: Array,
    default: () => []
  },
  justCleared: {
    type: Boolean,
    default: false
  }
})

defineEmits(['clear', 'click', 'remove'])

const formatDate = (date) => {
  const now = new Date()
  const d = new Date(date)
  if (d.toDateString() === now.toDateString()) {
    return '今天'
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  return d.toLocaleDateString('zh-CN')
}

const formatAge = (timestamp) => {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  return `${days}天前`
}

const getTypeColor = (type) => {
  const colors = {
    search: '#667eea',
    contract: '#f56c6c',
    document: '#e6a23c',
    case: '#409eff',
    law: '#67c23a'
  }
  return colors[type] || '#909399'
}

const getTypeIcon = (type) => {
  const icons = {
    search: 'Search',
    contract: 'Document',
    document: 'DocumentCopy',
    case: 'Files',
    law: 'Collection'
  }
  return icons[type] || 'Clock'
}
</script>

<style scoped>
.usage-memory-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cleared-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px;
  color: #67c23a;
}

.empty-memory {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  color: var(--text-color-secondary);
}

.empty-memory span {
  margin-top: 8px;
  font-size: 14px;
}

.empty-memory p {
  margin-top: 4px;
  font-size: 12px;
}

.memory-list {
  max-height: 400px;
  overflow-y: auto;
}

.memory-group {
  margin-bottom: 16px;
}

.memory-group:last-child {
  margin-bottom: 0;
}

.memory-date {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-bottom: 8px;
  font-weight: 500;
}

.memory-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.memory-item:hover {
  background: var(--hover-bg-color);
}

.memory-item-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.memory-item-info {
  flex: 1;
  min-width: 0;
}

.memory-item-title {
  display: block;
  font-size: 13px;
  color: var(--text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.memory-item-desc {
  display: block;
  font-size: 11px;
  color: var(--text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.memory-item-time {
  font-size: 11px;
  color: var(--text-color-placeholder);
  flex-shrink: 0;
}
</style>
