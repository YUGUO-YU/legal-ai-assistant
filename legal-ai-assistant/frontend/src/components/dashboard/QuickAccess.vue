<template>
  <el-card class="quick-access">
    <template #header>
      <div class="card-header">
        <div class="header-title">
          <el-icon><Lightning /></el-icon>
          <span>{{ title }}</span>
        </div>
        <el-tag v-if="showTag" type="success" size="small">{{ tagText }}</el-tag>
      </div>
    </template>
    <el-row :gutter="16">
      <el-col :span="span" v-for="item in items" :key="item.path">
        <div class="quick-item card-hover" @click="$router.push(item.path)">
          <div class="quick-icon" :style="{ background: item.gradient, boxShadow: item.shadow }">
            <el-icon :size="24"><component :is="item.icon" /></el-icon>
          </div>
          <div class="quick-text">
            <span class="quick-title">{{ item.title }}</span>
            <span class="quick-desc">{{ item.desc }}</span>
          </div>
          <el-icon class="quick-arrow"><Right /></el-icon>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { Lightning, Right } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    default: '快捷入口'
  },
  tagText: {
    type: String,
    default: '常用功能'
  },
  showTag: {
    type: Boolean,
    default: true
  },
  items: {
    type: Array,
    default: () => []
  },
  span: {
    type: Number,
    default: 8
  }
})
</script>

<style scoped>
.quick-access {
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

.quick-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
}

.quick-item:hover {
  background: var(--hover-bg-color);
}

.quick-item:last-child {
  margin-bottom: 0;
}

.quick-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.quick-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.quick-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color-primary);
}

.quick-desc {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quick-arrow {
  color: var(--text-color-placeholder);
  flex-shrink: 0;
}
</style>
