import { ref } from 'vue'

export function useDraggable(options = {}) {
  const {
    handle: dragHandle = null,
    disabled = false,
    ghostClass = 'drag-ghost',
    chosenClass = 'drag-chosen',
    animation = 200,
    group = 'default'
  } = options

  const isDragging = ref(false)
  const dragState = ref({
    index: -1,
    oldIndex: -1,
    newIndex: -1
  })

  const onStart = (evt) => {
    isDragging.value = true
    dragState.value.index = evt.oldIndex
    dragState.value.oldIndex = evt.oldIndex
  }

  const onEnd = (evt) => {
    isDragging.value = false
    dragState.value.newIndex = evt.newIndex
    dragState.value.index = -1

    if (evt.oldIndex !== evt.newIndex) {
      options.onChange?.(evt)
    }
  }

  const getSortableOptions = () => ({
    animation,
    ghostClass,
    chosenClass,
    disabled,
    group,
    handle: dragHandle,
    onStart,
    onEnd
  })

  return {
    isDragging,
    dragState,
    getSortableOptions
  }
}
