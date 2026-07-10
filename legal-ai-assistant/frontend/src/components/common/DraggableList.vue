<template>
  <draggable
    v-model="list"
    :group="group"
    :item-key="itemKey"
    :handle="handle"
    :ghost-class="ghostClass"
    :chosen-class="chosenClass"
    :animation="animation"
    :disabled="disabled"
    class="draggable-list"
    @start="onStart"
    @end="onEnd"
  >
    <template #item="{ element, index }">
      <div
        class="draggable-item"
        :class="{ 'is-dragging': draggingIndex === index }"
      >
        <div v-if="showHandle" class="drag-handle">
          <i class="el-icon-rank"></i>
        </div>
        <div class="drag-content">
          <slot :element="element" :index="index" />
        </div>
        <div v-if="showActions" class="drag-actions">
          <slot name="actions" :element="element" :index="index" />
        </div>
      </div>
    </template>
  </draggable>
</template>

<script setup>
import draggable from 'vuedraggable'
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Array,
    required: true
  },
  itemKey: {
    type: String,
    default: 'id'
  },
  group: {
    type: [String, Object],
    default: 'default'
  },
  handle: {
    type: String,
    default: '.drag-handle'
  },
  ghostClass: {
    type: String,
    default: 'drag-ghost'
  },
  chosenClass: {
    type: String,
    default: 'drag-chosen'
  },
  animation: {
    type: Number,
    default: 200
  },
  disabled: {
    type: Boolean,
    default: false
  },
  showHandle: {
    type: Boolean,
    default: true
  },
  showActions: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const draggingIndex = ref(-1)

const list = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const onStart = (evt) => {
  draggingIndex.value = evt.oldIndex
}

const onEnd = (evt) => {
  draggingIndex.value = -1
  emit('change', {
    oldIndex: evt.oldIndex,
    newIndex: evt.newIndex,
    element: props.modelValue[evt.newIndex]
  })
}
</script>

<style scoped>
.draggable-list {
  width: 100%;
}

.draggable-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  transition: all 0.2s ease;
}

.draggable-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.draggable-item.is-dragging {
  opacity: 0.5;
  background: var(--color-bg-soft);
}

.drag-handle {
  cursor: move;
  padding: 4px;
  color: var(--color-text-muted);
  transition: color 0.2s;
}

.drag-handle:hover {
  color: var(--color-primary);
}

.drag-handle i {
  font-size: 16px;
}

.drag-content {
  flex: 1;
  min-width: 0;
}

.drag-actions {
  display: flex;
  gap: 8px;
}
</style>

<style>
.drag-ghost {
  opacity: 0.5;
  background: var(--color-primary) !important;
  color: #fff !important;
}

.drag-chosen {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}
</style>
