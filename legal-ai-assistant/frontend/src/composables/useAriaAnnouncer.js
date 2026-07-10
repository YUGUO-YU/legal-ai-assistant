import { ref } from 'vue'

export function useAriaAnnouncer() {
  const announcement = ref('')
  const politeness = ref('polite')

  let timeoutId = null

  const announce = (message, options = {}) => {
    politeness.value = options.politeness || 'polite'

    if (timeoutId) {
      clearTimeout(timeoutId)
    }

    announcement.value = ''

    timeoutId = setTimeout(() => {
      announcement.value = message
    }, 100)
  }

  const announceAssertive = (message) => {
    announce(message, { politeness: 'assertive' })
  }

  return {
    announcement,
    politeness,
    announce,
    announceAssertive
  }
}
