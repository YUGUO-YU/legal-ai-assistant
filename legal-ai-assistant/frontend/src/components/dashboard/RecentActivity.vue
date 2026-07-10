<template>
  <el-card class="recent-activity">
    <template #header>
      <div class="card-header">
        <div class="header-title">
          <el-icon><Clock /></el-icon>
          <span>{{ title }}</span>
        </div>
        <el-button v-if="showViewAll" type="primary" link size="small" @click="$emit('viewAll')">
          {{ viewAllText }}
          <el-icon><Right /></el-icon>
        </el-button>
      </div>
    </template>
    <div class="activity-list">
      <div v-for="activity in activities" :key="activity.id" class="activity-item stagger-item">
        <div class="activity-icon" :style="{ background: activity.gradient }">
          <el-icon><component :is="activity.icon" /></el-icon>
        </div>
        <div class="activity-info">
          <span class="activity-title">{{ activity.title }}</span>
          <span class="activity-desc">{{ activity.desc }}</span>
        </div>
        <div class="activity-time">
          <span>{{ activity.time }}</span>
        </div>
      </div>
      <div v-if="activities.length === 0" class="empty-activity">
        <el-icon><Clock /></el-icon>
        <span>暂无最近活动</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { Clock, Right } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '最近活动'
  },
  viewAllText: {
    type: String,
    default: '查看全部'
  },
  showViewAll: {
    type: Boolean,
    default: true
  },
  activities: {
    type: Array,
    default: () => []
  }
})

defineEmits(['viewAll'])
</script>

<style scoped>
.recent-activity {
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

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.activity-item:hover {
  background: var(--hover-bg-color);
}

.activity-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.activity-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.activity-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-desc {
  font-size: 12px;
  color: var(--text-color-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.activity-time {
  flex-shrink: 0;
}

.activity-time span {
  font-size: 11px;
  color: var(--text-color-placeholder);
}

.empty-activity {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: var(--text-color-secondary);
  gap: 8px;
}

.empty-activity span {
  font-size: 13px;
}
</style>
