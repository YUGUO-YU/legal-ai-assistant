<template>
  <div class="error-boundary">
    <slot v-if="!hasError" />
    <el-card v-else class="error-card">
      <div class="error-content">
        <el-icon class="error-icon"><Warning /></el-icon>
        <h2>页面出错了</h2>
        <p>抱歉，页面发生了错误，请尝试刷新页面</p>
        <el-button type="primary" @click="resetError">
          重新加载
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'

const hasError = ref(false)

const resetError = () => {
  hasError.value = false
  window.location.reload()
}

onErrorCaptured((err, instance, info) => {
  console.error('[ErrorBoundary]', err, info)
  hasError.value = true
  return false
})
</script>

<style lang="scss" scoped>
.error-boundary {
  width: 100%;
  min-height: 100%;
}

.error-card {
  .error-content {
    text-align: center;
    padding: 48px 24px;
  }

  .error-icon {
    font-size: 64px;
    color: #ff4d4f;
    margin-bottom: 16px;
  }

  h2 {
    margin: 0 0 8px 0;
    font-size: 20px;
    color: #333;
  }

  p {
    margin: 0 0 24px 0;
    font-size: 14px;
    color: #666;
  }
}
</style>