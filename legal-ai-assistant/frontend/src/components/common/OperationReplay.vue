<template>
  <div class="operation-replay">
    <div class="replay-header">
      <h4>操作回放</h4>
      <div class="replay-controls">
        <button @click="toggleRecording">
          {{ isRecording ? '停止录制' : '开始录制' }}
        </button>
        <button @click="playRecording" :disabled="!hasRecording">
          播放
        </button>
        <button @click="clearRecording" :disabled="!hasRecording">
          清空
        </button>
      </div>
    </div>
    
    <div v-if="hasRecording" class="replay-info">
      <span>{{ operations.length }} 个操作</span>
      <span>{{ formatDuration(duration) }}</span>
    </div>
    
    <div v-if="isPlaying" class="replay-player">
      <div class="player-progress">
        <div class="progress-bar" :style="{ width: progress + '%' }"></div>
      </div>
      <div class="player-info">
        <span>正在执行: {{ currentOperation?.type }}</span>
        <span>{{ currentIndex + 1 }} / {{ operations.length }}</span>
      </div>
    </div>
    
    <div class="replay-list" v-if="operations.length > 0">
      <div
        v-for="(op, index) in operations"
        :key="op.id"
        class="replay-item"
        :class="{ 'is-active': index === currentIndex }"
        @click="jumpTo(index)"
      >
        <span class="op-index">{{ index + 1 }}</span>
        <span class="op-type">{{ op.type }}</span>
        <span class="op-detail">{{ getOpDetail(op) }}</span>
        <span class="op-time">{{ formatElapsed(op.elapsed) }}</span>
      </div>
    </div>
    
    <div v-else class="replay-empty">
      暂无录制数据
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { operationRecorder } from '@/services/operationRecorder'

const isRecording = ref(false)
const hasRecording = ref(false)
const isPlaying = ref(false)
const currentIndex = ref(0)
const operations = ref([])

const duration = computed(() => {
  if (operations.value.length < 2) return 0
  const last = operations.value[operations.value.length - 1]
  return last.elapsed
})

const progress = computed(() => {
  if (!duration.value) return 0
  const current = operations.value[currentIndex.value]
  if (!current) return 100
  return (current.elapsed / duration.value) * 100
})

const currentOperation = computed(() => {
  return operations.value[currentIndex.value]
})

const toggleRecording = () => {
  if (isRecording.value) {
    const data = operationRecorder.stop()
    operations.value = data.operations
    hasRecording.value = true
    isRecording.value = false
  } else {
    operationRecorder.start()
    isRecording.value = true
  }
}

const playRecording = async () => {
  if (!hasRecording.value || isPlaying.value) return
  
  isPlaying.value = true
  currentIndex.value = 0
  
  for (let i = 0; i < operations.value.length; i++) {
    if (!isPlaying.value) break
    
    currentIndex.value = i
    const op = operations.value[i]
    
    await executeOperation(op)
    
    if (i < operations.value.length - 1) {
      const nextOp = operations.value[i + 1]
      const delay = nextOp.elapsed - op.elapsed
      if (delay > 0 && delay < 5000) {
        await new Promise(r => setTimeout(r, delay))
      }
    }
  }
  
  isPlaying.value = false
}

const executeOperation = async (op) => {
  switch (op.type) {
    case 'click':
      const element = document.querySelector(op.selector)
      if (element) {
        element.click()
      }
      break
    case 'input':
      const inputEl = document.querySelector(op.selector)
      if (inputEl) {
        inputEl.value = op.data.value
        inputEl.dispatchEvent(new Event('input', { bubbles: true }))
      }
      break
    case 'submit':
      const form = document.querySelector(op.selector)
      if (form) {
        form.submit()
      }
      break
  }
}

const jumpTo = (index) => {
  currentIndex.value = index
}

const clearRecording = () => {
  operationRecorder.clear()
  operations.value = []
  hasRecording.value = false
  isPlaying.value = false
  currentIndex.value = 0
}

const getOpDetail = (op) => {
  switch (op.type) {
    case 'click':
      return op.data.action || op.data.text || op.selector
    case 'input':
      return `${op.data.name}: ${op.data.value?.substring(0, 20)}`
    case 'submit':
      return op.data.action || 'form submit'
    case 'navigation':
      return op.data.to
    default:
      return ''
  }
}

const formatDuration = (ms) => {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  return `${minutes}:${(seconds % 60).toString().padStart(2, '0')}`
}

const formatElapsed = (ms) => {
  return (ms / 1000).toFixed(1) + 's'
}
</script>

<style scoped>
.operation-replay {
  position: fixed;
  bottom: 20px;
  right: 20px;
  width: 400px;
  max-height: 500px;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  z-index: 8000;
  display: flex;
  flex-direction: column;
}

.replay-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--color-border-light);
  
  h4 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
  }
}

.replay-controls {
  display: flex;
  gap: 8px;
  
  button {
    padding: 6px 12px;
    font-size: 12px;
    border: 1px solid var(--color-border);
    background: var(--color-bg);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover:not(:disabled) {
      border-color: var(--color-primary);
      color: var(--color-primary);
    }
    
    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.replay-info {
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: var(--color-bg-soft);
}

.replay-player {
  padding: 12px 16px;
  background: var(--color-bg-soft);
}

.player-progress {
  height: 4px;
  background: var(--color-border);
  border-radius: 2px;
  overflow: hidden;
  
  .progress-bar {
    height: 100%;
    background: var(--color-primary);
    transition: width 0.3s;
  }
}

.player-info {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.replay-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.replay-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
  
  &:hover {
    background: var(--color-bg-soft);
  }
  
  &.is-active {
    background: rgba(102, 126, 234, 0.1);
  }
  
  .op-index {
    width: 20px;
    font-size: 11px;
    color: var(--color-text-muted);
  }
  
  .op-type {
    padding: 2px 6px;
    font-size: 10px;
    font-weight: 500;
    background: var(--color-bg-soft);
    border-radius: 3px;
    color: var(--color-text-secondary);
    text-transform: uppercase;
  }
  
  .op-detail {
    flex: 1;
    font-size: 12px;
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .op-time {
    font-size: 11px;
    color: var(--color-text-muted);
  }
}

.replay-empty {
  padding: 40px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 14px;
}
</style>
