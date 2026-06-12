<template>
  <div class="loading-container" v-if="loading">
    <div class="loading-spinner">
      <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    <p v-if="text" class="loading-text">{{ text }}</p>
  </div>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  text: {
    type: String,
    default: '加载中...'
  }
})
</script>

<style lang="scss" scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  min-height: 200px;
}

.loading-spinner {
  .el-icon {
    font-size: 32px;
    color: #1890ff;
  }
}

.loading-text {
  margin: 16px 0 0 0;
  font-size: 14px;
  color: #666;
}
</style>