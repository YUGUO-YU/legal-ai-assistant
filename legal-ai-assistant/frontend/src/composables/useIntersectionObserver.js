import { onMounted, onUnmounted, ref } from 'vue'

export function useIntersectionObserver(target, callback, options = {}) {
  let observer = null
  const isIntersecting = ref(false)

  const defaultOptions = {
    root: null,
    rootMargin: '50px',
    threshold: 0,
    ...options
  }

  onMounted(() => {
    if (!target.value) return

    observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        isIntersecting.value = entry.isIntersecting
        callback(entry.isIntersecting, entry)
      })
    }, defaultOptions)

    observer.observe(target.value)
  })

  onUnmounted(() => {
    if (observer) {
      observer.disconnect()
      observer = null
    }
  })

  const stop = () => {
    if (observer && target.value) {
      observer.unobserve(target.value)
      observer.disconnect()
      observer = null
    }
  }

  return {
    isIntersecting,
    stop
  }
}
