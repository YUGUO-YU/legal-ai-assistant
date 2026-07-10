<template>
  <slot v-if="!hasError" />

  <div v-else class="error-boundary">
    <div class="error-content">
      <div class="error-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>

      <h2 class="error-title">{{ errorTitle }}</h2>
      <p class="error-message">{{ errorMessage }}</p>

      <div class="error-actions">
        <button @click="handleRetry" class="btn-retry">
          <el-icon><Refresh /></el-icon>
          重试
        </button>
        <button @click="handleGoHome" class="btn-home">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </button>
      </div>

      <details v-if="showDetails" class="error-details">
        <summary>查看详情</summary>
        <pre>{{ errorStack }}</pre>
      </details>
    </div>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  errorTitle: {
    type: String,
    default: '出错了'
  },
  errorMessage: {
    type: String,
    default: '抱歉，页面发生了错误，请稍后重试'
  },
  showDetails: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['error', 'retry'])

const router = useRouter()
const hasError = ref(false)
const errorInfo = ref(null)
const errorStack = ref('')

onErrorCaptured((err, instance, info) => {
  hasError.value = true
  errorInfo.value = {
    message: err.message,
    name: err.name,
    info
  }
  errorStack.value = err.stack || ''

  console.error('Error captured:', err, info)

  emit('error', { err, info })

  return false
})

const handleRetry = () => {
  hasError.value = false
  errorInfo.value = null
  emit('retry')
}

const handleGoHome = () => {
  hasError.value = false
  router.push('/')
}
</script>

<style lang="scss" scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 40px 20px;
}

.error-content {
  text-align: center;
  max-width: 480px;
}

.error-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: #f56c6c;

  svg {
    width: 100%;
    height: 100%;
  }
}

.error-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 12px;
}

.error-message {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 24px;
}

.btn-retry,
.btn-home {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-retry {
  background: var(--color-primary);
  color: #fff;

  &:hover {
    background: var(--color-primary-light);
  }
}

.btn-home {
  background: var(--color-bg-soft);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);

  &:hover {
    background: var(--color-bg-mute);
  }
}

.error-details {
  text-align: left;
  margin-top: 20px;

  summary {
    cursor: pointer;
    color: var(--color-text-secondary);
    font-size: 13px;
    margin-bottom: 8px;
  }

  pre {
    background: var(--color-bg-mute);
    padding: 16px;
    border-radius: var(--radius-md);
    font-size: 12px;
    color: var(--color-text-secondary);
    overflow-x: auto;
    max-height: 200px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
