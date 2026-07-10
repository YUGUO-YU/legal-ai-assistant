import { ref } from 'vue'

export function useContextMenu() {
  const visible = ref(false)
  const x = ref(0)
  const y = ref(0)
  const currentRow = ref(null)
  const menus = ref([])

  const show = (event, row, menuList) => {
    event.preventDefault()
    event.stopPropagation()

    x.value = event.clientX
    y.value = event.clientY
    currentRow.value = row
    menus.value = menuList
    visible.value = true
  }

  const hide = () => {
    visible.value = false
  }

  const handleSelect = (item) => {
    if (item.handler && typeof item.handler === 'function') {
      item.handler(currentRow.value)
    } else if (item.action) {
      item.action(currentRow.value)
    }
  }

  return {
    visible,
    x,
    y,
    currentRow,
    menus,
    show,
    hide,
    handleSelect
  }
}
