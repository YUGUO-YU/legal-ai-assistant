import { ref, onMounted, onUnmounted, nextTick } from 'vue'

export function useFocusTrap(containerRef, isActive) {
  const focusableSelectors = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])'
  ].join(', ')

  const getFocusableElements = () => {
    if (!containerRef.value) return []
    return Array.from(containerRef.value.querySelectorAll(focusableSelectors))
  }

  const handleKeyDown = (e) => {
    if (!isActive.value || e.key !== 'Tab') return

    const focusable = getFocusableElements()
    if (focusable.length === 0) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown)

    if (isActive.value) {
      nextTick(() => {
        const focusable = getFocusableElements()
        if (focusable.length > 0) {
          focusable[0].focus()
        }
      })
    }
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })

  return {
    getFocusableElements
  }
}
