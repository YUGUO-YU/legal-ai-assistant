<template>
  <teleport to="body">
    <transition name="toast-fade">
      <div v-if="visible" class="toast-container" :class="`toast-${type}`">
        <div class="toast-content">
          <el-icon v-if="type === 'success'" class="toast-icon"><CircleCheck /></el-icon>
          <el-icon v-else-if="type === 'error'" class="toast-icon"><CircleClose /></el-icon>
          <el-icon v-else-if="type === 'warning'" class="toast-icon"><Warning /></el-icon>
          <el-icon v-else class="toast-icon"><InfoFilled /></el-icon>
          <span class="toast-message">{{ message }}</span>
          <el-icon v-if="showClose" class="toast-close" @click="close"><Close /></el-icon>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  message: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'info',
    validator: (val) => ['success', 'error', 'warning', 'info'].includes(val)
  },
  duration: {
    type: Number,
    default: 3000
  },
  showClose: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])

const visible = ref(false)
let timer = null

const show = () => {
  visible.value = true
  if (props.duration > 0) {
    timer = setTimeout(() => {
      close()
    }, props.duration)
  }
}

const close = () => {
  visible.value = false
  emit('close')
}

watch(() => props.message, (newVal) => {
  if (newVal) {
    show()
  }
}, { immediate: true })

defineExpose({ show, close })
</script>

<style lang="scss" scoped>
.toast-container {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  min-width: 280px;
  max-width: 500px;
  padding: 12px 16px;
  background: var(--color-bg-card);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  color: var(--color-text);

  &.toast-success {
    border-left: 4px solid var(--color-success);
    .toast-icon { color: var(--color-success); }
  }

  &.toast-error {
    border-left: 4px solid var(--color-danger);
    .toast-icon { color: var(--color-danger); }
  }

  &.toast-warning {
    border-left: 4px solid var(--color-warning);
    .toast-icon { color: var(--color-warning); }
  }

  &.toast-info {
    border-left: 4px solid var(--color-info);
    .toast-icon { color: var(--color-info); }
  }
}

.toast-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toast-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.toast-message {
  flex: 1;
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.5;
}

.toast-close {
  font-size: 14px;
  color: var(--color-text-muted);
  cursor: pointer;
  flex-shrink: 0;
  &:hover {
    color: var(--color-text);
  }
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.3s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-20px);
}
</style>
