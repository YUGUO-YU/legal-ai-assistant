import { useIntersectionObserver } from '@/composables/useIntersectionObserver'

export const lazyLoad = {
  mounted(el, binding) {
    const { src, placeholder = '/placeholder.png' } = binding.value || {}

    el.src = placeholder
    el.style.opacity = '0'
    el.style.transition = 'opacity 0.3s ease'

    const loadImage = () => {
      const img = new Image()
      img.onload = () => {
        el.src = src
        el.style.opacity = '1'
      }
      img.onerror = () => {
        el.src = placeholder
        el.style.opacity = '1'
      }
      img.src = src
    }

    const { stop } = useIntersectionObserver(el, (isIntersecting) => {
      if (isIntersecting) {
        loadImage()
        stop()
      }
    })

    el._lazyLoadStop = stop
  },

  unmounted(el) {
    if (el._lazyLoadStop) {
      el._lazyLoadStop()
    }
  }
}
