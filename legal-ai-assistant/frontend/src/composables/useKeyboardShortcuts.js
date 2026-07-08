import { onMounted, onUnmounted } from 'vue'

export function useKeyboardShortcuts(shortcuts) {
  const handleKeyDown = (e) => {
    for (const shortcut of shortcuts) {
      if (shortcut.match(e)) {
        e.preventDefault()
        shortcut.handler()
        return
      }
    }
  }

  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown)
  })

  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })
}

export function matchShortcut(e, keys) {
  const ctrl = keys.ctrl || false
  const shift = keys.shift || false
  const alt = keys.alt || false
  const key = keys.key || ''

  const ctrlMatch = ctrl ? (e.ctrlKey || e.metaKey) : !e.ctrlKey && !e.metaKey
  const shiftMatch = shift ? e.shiftKey : !e.shiftKey
  const altMatch = alt ? e.altKey : !e.altKey
  const keyMatch = e.key.toLowerCase() === key.toLowerCase()

  return ctrlMatch && shiftMatch && altMatch && keyMatch
}

export function isInputFocused() {
  const tag = document.activeElement?.tagName
  return tag === 'INPUT' || tag === 'TEXTAREA' || document.activeElement?.isContentEditable
}
