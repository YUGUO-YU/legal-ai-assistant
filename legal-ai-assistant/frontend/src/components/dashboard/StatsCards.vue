<template>
  <el-row :gutter="24" class="stats-row" v-loading="loading">
    <el-col :xs="12" :sm="6" :md="4" :lg="3" v-for="(stat, index) in statsData" :key="index">
      <el-card class="kpi-card" :body-style="{ padding: '14px' }" :class="stat.class" @click="goTo(stat.path)">
        <div class="kpi-content">
          <div class="kpi-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="kpi-info">
            <span class="kpi-value">{{ stat.value }}</span>
            <span class="kpi-label">{{ stat.label }}</span>
            <div class="kpi-trend" :class="stat.trend > 0 ? 'up' : 'down'">
              <el-icon v-if="stat.trend > 0"><Top /></el-icon>
              <el-icon v-else><Bottom /></el-icon>
              <span>{{ Math.abs(stat.trend) }}%</span>
            </div>
          </div>
          <el-icon class="kpi-arrow"><Right /></el-icon>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Top, Bottom, Right, Search, Files, Document, ChatDotRound, Clock, Coin, Connection, DocumentCopy } from '@element-plus/icons-vue'

const props = defineProps({
  statsData: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['refresh'])

const router = useRouter()

const goTo = (path) => {
  if (path) {
    router.push(path)
  }
}
</script>

<style scoped>
.stats-row {
  margin-bottom: 24px;
}

.kpi-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  border-radius: 12px;
}

.kpi-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.kpi-card.up {
  border-left: 3px solid #67c23a;
}

.kpi-card.down {
  border-left: 3px solid #f56c6c;
}

.kpi-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.kpi-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.kpi-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-color-primary);
  line-height: 1.2;
}

.kpi-label {
  font-size: 12px;
  color: var(--text-color-secondary);
  margin-top: 2px;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  margin-top: 4px;
}

.kpi-trend.up {
  color: #67c23a;
}

.kpi-trend.down {
  color: #f56c6c;
}

.kpi-arrow {
  color: var(--text-color-placeholder);
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .kpi-card {
    margin-bottom: 12px;
  }

  .kpi-icon {
    width: 40px;
    height: 40px;
  }

  .kpi-value {
    font-size: 20px;
  }
}
</style>
