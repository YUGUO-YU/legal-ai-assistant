import { ref } from 'vue'

export function useCountUp(endValue, duration = 1500) {
  const displayValue = ref(0)

  const start = () => {
    const startTime = performance.now()
    const startValue = 0

    const animate = (currentTime) => {
      const elapsed = currentTime - startTime
      const progress = Math.min(elapsed / duration, 1)

      const easeProgress = progress === 1
        ? 1
        : 1 - Math.pow(2, -10 * progress)

      displayValue.value = Math.floor(startValue + (endValue - startValue) * easeProgress)

      if (progress < 1) {
        requestAnimationFrame(animate)
      }
    }

    requestAnimationFrame(animate)
  }

  return {
    displayValue,
    start
  }
}
