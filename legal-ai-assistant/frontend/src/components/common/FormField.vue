<template>
  <div class="form-field" :class="{ 'has-error': hasError, 'is-required': isRequired }">
    <label v-if="label" :for="inputId" class="form-field-label">
      {{ label }}
      <span v-if="isRequired" class="required-mark">*</span>
    </label>

    <div class="form-field-control">
      <slot :hasError="hasError" :isValid="isValid" :errorMessage="errorMessage" />
      <div v-if="isValid && touched" class="valid-icon">
        <i class="el-icon-success"></i>
      </div>
    </div>

    <Transition name="field-error">
      <div v-if="hasError && errorMessage" class="form-field-error">
        <i class="el-icon-error"></i>
        {{ errorMessage }}
      </div>
    </Transition>

    <div v-if="hint && !hasError" class="form-field-hint">
      {{ hint }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: String,
  field: String,
  rules: Object,
  modelValue: [String, Number, Array, Object],
  error: String,
  touched: Boolean,
  required: Boolean,
  hint: String
})

const inputId = computed(() => `field-${props.field}-${Math.random().toString(36).substr(2, 9)}`)

const isRequired = computed(() => {
  if (props.required) return true
  if (props.rules?.required) return true
  return false
})

const hasError = computed(() => props.touched && props.error)
const errorMessage = computed(() => props.error)
const isValid = computed(() => {
  return props.touched && !props.error && props.modelValue
})
</script>

<style scoped>
.form-field {
  margin-bottom: 20px;
}

.form-field.has-error .form-field-control :deep(.el-input__wrapper),
.form-field.has-error .form-field-control :deep(.el-textarea__inner),
.form-field.has-error .form-field-control :deep(.el-select__wrapper) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}

.form-field.has-error .form-field-control :deep(.el-input__wrapper:focus),
.form-field.has-error .form-field-control :deep(.el-textarea__inner:focus),
.form-field.has-error .form-field-control :deep(.el-select__wrapper:focus) {
  box-shadow: 0 0 0 1px var(--el-color-danger) inset !important;
}

.form-field-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.form-field-label .required-mark {
  color: #f56c6c;
  margin-left: 2px;
}

.form-field-control {
  position: relative;
}

.form-field-control :deep(.el-input),
.form-field-control :deep(.el-textarea),
.form-field-control :deep(.el-select) {
  width: 100%;
}

.form-field-error {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-color-danger);
}

.form-field-error i {
  font-size: 14px;
}

.form-field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.valid-icon {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #67c23a;
  font-size: 16px;
  animation: scaleIn 0.2s ease;
}

@keyframes scaleIn {
  from { transform: translateY(-50%) scale(0); }
  to { transform: translateY(-50%) scale(1); }
}

.field-error-enter-active,
.field-error-leave-active {
  transition: all 0.2s ease;
}

.field-error-enter-from,
.field-error-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
