<template>
  <el-card class="empty-state">
    <div class="empty-content">
      <el-icon class="empty-icon"><component :is="icon" /></el-icon>
      <h3 class="empty-title">{{ title }}</h3>
      <p class="empty-desc">{{ description }}</p>
      <slot name="action">
        <el-button v-if="actionText" type="primary" @click="$emit('action')">
          {{ actionText }}
        </el-button>
      </slot>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  icon: {
    type: String,
    default: 'Files'
  },
  title: {
    type: String,
    default: '暂无数据'
  },
  description: {
    type: String,
    default: '当前没有可显示的内容'
  },
  actionText: {
    type: String,
    default: ''
  }
})

defineEmits(['action'])
</script>

<style lang="scss" scoped>
.empty-state {
  .empty-content {
    text-align: center;
    padding: 48px 24px;
  }

  .empty-icon {
    font-size: 64px;
    color: #d9d9d9;
    margin-bottom: 16px;
  }

  .empty-title {
    margin: 0 0 8px 0;
    font-size: 16px;
    color: #333;
  }

  .empty-desc {
    margin: 0 0 24px 0;
    font-size: 14px;
    color: #999;
  }
}
</style>