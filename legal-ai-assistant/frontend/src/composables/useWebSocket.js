import { ref, onMounted, onUnmounted, computed } from 'vue'
import { wsService } from '@/services/websocketService'

export function useWebSocket(options = {}) {
  const {
    autoConnect = true,
    url = '/ws',
    listeners = {}
  } = options

  const isConnected = ref(false)
  const reconnectAttempts = ref(0)
  const lastMessage = ref(null)
  const messages = ref([])

  let unsubscribers = []

  const connect = async () => {
    try {
      await wsService.connect(url)
      isConnected.value = true
    } catch (error) {
      isConnected.value = false
      console.error('WebSocket connection failed:', error)
    }
  }

  const disconnect = () => {
    wsService.disconnect()
    isConnected.value = false
  }

  const send = (type, payload) => {
    return wsService.send(type, payload)
  }

  const subscribe = (event, callback) => {
    const unsubscribe = wsService.subscribe(event, (data) => {
      lastMessage.value = { event, data, timestamp: Date.now() }
      messages.value.push(lastMessage.value)
      callback(data)
    })
    unsubscribers.push(unsubscribe)
    return unsubscribe
  }

  onMounted(() => {
    if (autoConnect) {
      connect()
    }

    Object.entries(listeners).forEach(([event, callback]) => {
      subscribe(event, callback)
    })

    subscribe('connected', () => {
      isConnected.value = true
      reconnectAttempts.value = 0
    })

    subscribe('disconnected', () => {
      isConnected.value = false
    })
  })

  onUnmounted(() => {
    unsubscribers.forEach(unsub => unsub())
    unsubscribers = []
  })

  return {
    isConnected: computed(() => isConnected.value),
    reconnectAttempts,
    lastMessage,
    messages,
    connect,
    disconnect,
    send,
    subscribe
  }
}