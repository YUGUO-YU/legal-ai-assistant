<template>
  <el-card class="hot-topics">
    <template #header>
      <div class="card-header">
        <div class="header-title">
          <el-icon><TrendCharts /></el-icon>
          <span>{{ title }}</span>
        </div>
        <el-tag v-if="showTag" type="warning" size="small">{{ tagText }}</el-tag>
      </div>
    </template>
    <div class="topic-list">
      <div v-for="(topic, index) in topics" :key="topic.title" class="topic-item">
        <div class="topic-rank" :class="{ top: index < 3 }">
          {{ index + 1 }}
        </div>
        <div class="topic-info">
          <span class="topic-title">{{ topic.title }}</span>
          <el-progress
            :percentage="topic.percentage"
            :stroke-width="4"
            :show-text="false"
            :color="topic.color"
          />
        </div>
        <span class="topic-count">{{ topic.count }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { TrendCharts } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '热门检索'
  },
  tagText: {
    type: String,
    default: '实时更新'
  },
  showTag: {
    type: Boolean,
    default: true
  },
  topics: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.hot-topics {
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

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topic-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-color-secondary);
  flex-shrink: 0;
}

.topic-rank.top {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.topic-info {
  flex: 1;
  min-width: 0;
}

.topic-title {
  display: block;
  font-size: 13px;
  color: var(--text-color-primary);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topic-count {
  font-size: 12px;
  color: var(--text-color-secondary);
  flex-shrink: 0;
}
</style>
