<template>
  <div class="loading-container" v-if="loading">
    <div v-if="type === 'skeleton'" class="loading-skeleton">
      <div class="skeleton-line" style="width: 60%"></div>
      <div class="skeleton-line" style="width: 80%"></div>
      <div class="skeleton-line" style="width: 45%"></div>
      <div class="skeleton-line" style="width: 70%"></div>
      <div class="skeleton-line" style="width: 55%"></div>
    </div>
    <div v-else class="loading-animation">
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
  },
  type: {
    type: String,
    default: 'spin',
    validator: (val) => ['spin', 'skeleton'].includes(val)
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
    border-top-color: var(--color-primary);
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
      border-top-color: var(--color-primary-dark);
      animation: spin 1.5s linear infinite reverse;
    }

    &::after {
      inset: 12px;
      border: 3px solid transparent;
      border-top-color: var(--color-primary-light);
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
      color: var(--color-primary);
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

.loading-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  max-width: 320px;
}

.skeleton-line {
  height: 16px;
  border-radius: var(--radius-sm);
  background: linear-gradient(
    90deg,
    var(--color-border) 25%,
    var(--color-bg-secondary) 50%,
    var(--color-border) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.loading-text {
  margin: 24px 0 0 0;
  font-size: 15px;
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
}
</style>
