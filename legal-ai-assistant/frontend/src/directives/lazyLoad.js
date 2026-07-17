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

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            loadImage()
            observer.unobserve(el)
            observer.disconnect()
          }
        })
      },
      { rootMargin: '50px', threshold: 0 }
    )

    observer.observe(el)

    el._lazyLoadObserver = observer
  },

  unmounted(el) {
    if (el._lazyLoadObserver) {
      el._lazyLoadObserver.unobserve(el)
      el._lazyLoadObserver.disconnect()
      el._lazyLoadObserver = null
    }
  }
}
