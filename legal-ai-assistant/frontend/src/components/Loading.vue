<template>
  <div class="loading-container" v-if="loading">
    <div class="loading-animation">
      <div class="loading-circle"></div>
      <div class="loading-icon">
        <el-icon class="is-loading"><Loading /></el-icon>
      </div>
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
  padding: 64px;
  min-height: 300px;
}

.loading-animation {
  position: relative;
  width: 80px;
  height: 80px;

  .loading-circle {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    border: 3px solid transparent;
    border-top-color: #667eea;
    animation: spin 1s linear infinite;

    &::before,
    &::after {
      content: '';
      position: absolute;
      border-radius: 50%;
    }

    &::before {
      inset: 6px;
      border: 3px solid transparent;
      border-top-color: #764ba2;
      animation: spin 1.5s linear infinite reverse;
    }

    &::after {
      inset: 12px;
      border: 3px solid transparent;
      border-top-color: #667eea;
      animation: spin 2s linear infinite;
    }
  }

  .loading-icon {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;

    .el-icon {
      font-size: 28px;
      color: #667eea;
    }
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin: 24px 0 0 0;
  font-size: 15px;
  color: #6b7280;
  letter-spacing: 0.5px;
}
</style>
