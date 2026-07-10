<template>
  <div class="table-empty-state">
    <EmptyState
      :title="title"
      :description="description"
      :illustration="illustration"
      :size="size"
      :action-text="actionText"
      :secondary-action-text="secondaryActionText"
      @action="handleAction"
      @secondary-action="handleSecondaryAction"
    >
      <template #action v-if="$slots.action">
        <slot name="action"></slot>
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import EmptyState from './EmptyState.vue'

defineProps({
  title: {
    type: String,
    default: '暂无记录'
  },
  description: {
    type: String,
    default: '没有找到符合条件的数据'
  },
  illustration: {
    type: String,
    default: 'data'
  },
  size: {
    type: String,
    default: 'medium'
  },
  actionText: {
    type: String,
    default: ''
  },
  secondaryActionText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['action', 'secondary-action'])

const handleAction = () => {
  emit('action')
}

const handleSecondaryAction = () => {
  emit('secondary-action')
}
</script>

<style scoped>
.table-empty-state {
  padding: 60px 20px;
}
</style>
