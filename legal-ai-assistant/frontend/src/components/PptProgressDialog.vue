<template>
  <el-dialog
    v-model="visible"
    title="正在生成PPT"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="ppt-progress-dialog"
  >
    <div class="progress-body">
      <div class="progress-icon">
        <el-icon :size="48" class="pulse-icon" :class="{ done: currentStep >= steps.length }">
          <component :is="currentStep >= steps.length ? 'CircleCheck' : 'Loading'" />
        </el-icon>
      </div>
      <el-progress
        :percentage="percentage"
        :stroke-width="8"
        :color="progressColor"
        :show-text="true"
        class="progress-bar"
      />
      <div class="steps-list">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="step-item"
          :class="{
            active: index === currentStep,
            done: index < currentStep,
            error: index === errorStep
          }"
        >
          <span class="step-dot">
            <el-icon v-if="index < currentStep"><Check /></el-icon>
            <el-icon v-else-if="index === errorStep"><Close /></el-icon>
            <span v-else class="dot-empty"></span>
          </span>
          <span class="step-text">{{ step }}</span>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Loading, CircleCheck, Check, Close } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  steps: { type: Array, default: () => ['分析检索结果', '组织幻灯片结构', '生成PPT内容', '保存到服务器'] }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(props.modelValue)
const currentStep = ref(0)
const errorStep = ref(-1)
let timer = null

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    startProgress()
  } else {
    stopProgress()
  }
})

const percentage = computed(() => {
  const total = props.steps.length
  return Math.round((currentStep.value / total) * 100)
})

const progressColor = computed(() => {
  if (errorStep.value >= 0) return '#ef4444'
  return { color: '#667eea', percentage: 100 }
})

const startProgress = () => {
  currentStep.value = 0
  errorStep.value = -1
  advanceStep()
}

const advanceStep = () => {
  if (!visible.value) return
  if (currentStep.value < props.steps.length) {
    const delay = 600 + Math.random() * 1200
    timer = setTimeout(() => {
      currentStep.value++
      advanceStep()
    }, delay)
  }
}

const stopProgress = () => {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

const markError = (stepIndex) => {
  errorStep.value = stepIndex
  stopProgress()
}

const markComplete = () => {
  stopProgress()
  currentStep.value = props.steps.length
  setTimeout(() => {
    visible.value = false
    emit('update:modelValue', false)
  }, 800)
}

defineExpose({ markError, markComplete })
</script>

<style lang="scss" scoped>
.ppt-progress-dialog {
  :deep(.el-dialog__body) {
    padding: 32px 40px;
  }
}

.progress-body {
  text-align: center;
}

.progress-icon {
  margin-bottom: 24px;

  .pulse-icon {
    color: #667eea;

    &.done {
      color: #10b981;
    }
  }
}

.progress-bar {
  margin-bottom: 28px;

  :deep(.el-progress__text) {
    font-size: 14px;
    font-weight: 600;
    color: #667eea;
  }
}

.steps-list {
  text-align: left;
  padding: 0 20px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  color: #9ca3af;
  transition: all 0.3s;

  &.active {
    color: #667eea;

    .dot-empty {
      background: #667eea;
      border-color: #667eea;
    }
  }

  &.done {
    color: #10b981;
  }

  &.error {
    color: #ef4444;
  }

  .step-dot {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    font-size: 14px;
  }

  .dot-empty {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    border: 2px solid #d1d5db;
    display: inline-block;
  }

  .step-text {
    font-size: 14px;
    font-weight: 500;
  }
}
</style>
