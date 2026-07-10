import { ref, onMounted } from 'vue'

export function usePrefetch() {
  const prefetched = ref(new Set())
  
  const prefetch = (url) => {
    if (prefetched.value.has(url)) return
    
    const link = document.createElement('link')
    link.rel = 'prefetch'
    link.href = url
    document.head.appendChild(link)
    
    prefetched.value.add(url)
  }
  
  const prefetchRoute = (router, routeName) => {
    const route = router.resolve({ name: routeName })
    if (route) {
      prefetch(route.href)
    }
  }
  
  return {
    prefetch,
    prefetchRoute,
    prefetched
  }
}
