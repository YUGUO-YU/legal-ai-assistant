<template>
  <el-card class="feature-detail-card">
    <template #header>
      <div class="card-header">
        <div class="header-title">
          <el-icon><DataBoard /></el-icon>
          <span>{{ title }}</span>
        </div>
        <el-tag type="info" size="small">{{ tagText }}</el-tag>
      </div>
    </template>

    <div class="detail-cards-grid">
      <div
        v-for="card in firstRowCards"
        :key="card.key"
        class="detail-card card-hover"
        :class="card.class"
        @click="$emit('cardClick', card)"
        @contextmenu.prevent="$emit('cardContextMenu', card, $event)"
      >
        <div class="detail-card-header">
          <div class="detail-card-icon" :style="{ background: card.gradient }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <el-tag size="small" :type="card.tagType" effect="plain">{{ card.status }}</el-tag>
        </div>
        <div class="detail-card-body">
          <span class="detail-card-title">{{ card.title }}</span>
          <span class="detail-card-desc">{{ card.desc }}</span>
        </div>
        <div class="detail-card-footer">
          <span class="detail-card-meta">{{ card.meta }}</span>
          <el-icon class="detail-card-arrow"><Right /></el-icon>
        </div>
      </div>
    </div>

    <div class="detail-cards-grid second-row">
      <div
        v-for="card in secondRowCards"
        :key="card.key"
        class="detail-card card-hover"
        :class="card.class"
        @click="$emit('cardClick', card)"
        @contextmenu.prevent="$emit('cardContextMenu', card, $event)"
      >
        <div class="detail-card-header">
          <div class="detail-card-icon" :style="{ background: card.gradient }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <el-tag size="small" :type="card.tagType" effect="plain">{{ card.status }}</el-tag>
        </div>
        <div class="detail-card-body">
          <span class="detail-card-title">{{ card.title }}</span>
          <span class="detail-card-desc">{{ card.desc }}</span>
        </div>
        <div class="detail-card-footer">
          <span class="detail-card-meta">{{ card.meta }}</span>
          <el-icon class="detail-card-arrow"><Right /></el-icon>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { DataBoard, Right } from '@element-plus/icons-vue'

const props = defineProps({
  title: {
    type: String,
    default: '功能详情卡'
  },
  tagText: {
    type: String,
    default: '点击查看详情'
  },
  cards: {
    type: Array,
    default: () => []
  }
})

defineEmits(['cardClick', 'cardContextMenu'])

const firstRowCards = computed(() => props.cards.slice(0, 4))
const secondRowCards = computed(() => props.cards.slice(4, 8))
</script>

<style scoped>
.feature-detail-card {
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

.detail-cards-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.detail-cards-grid.second-row {
  margin-top: 20px;
}

.detail-card {
  border-radius: 12px;
  padding: 16px;
  background: var(--bg-color-light);
  cursor: pointer;
  transition: all 0.3s ease;
}

.detail-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.detail-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.detail-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.detail-card-body {
  display: flex;
  flex-direction: column;
  margin-bottom: 12px;
}

.detail-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color-primary);
}

.detail-card-desc {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-top: 4px;
  line-height: 1.4;
}

.detail-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-card-meta {
  font-size: 11px;
  color: var(--text-color-placeholder);
}

.detail-card-arrow {
  color: var(--text-color-placeholder);
}

@media (max-width: 1200px) {
  .detail-cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .detail-cards-grid {
    grid-template-columns: 1fr;
  }
}
</style>
