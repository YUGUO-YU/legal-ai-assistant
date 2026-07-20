import { ref, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useNotificationCenter } from './useNotificationCenter'

export function useNotificationWs() {
  const wsStatus = ref('disconnected')
  let ws = null
  let reconnectAttempts = 0
  let reconnectTimer = null
  let pingTimer = null
  const maxReconnectAttempts = 5

  function getToken() {
    return localStorage.getItem('admin_token') || localStorage.getItem('token')
  }

  function connect() {
    const token = getToken()
    if (!token) {
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}/ws/notifications?token=${encodeURIComponent(token)}`

    try {
      ws = new WebSocket(url)

      ws.onopen = () => {
        wsStatus.value = 'connected'
        reconnectAttempts = 0
        startPing()
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'pong') {
            return
          }
          const { add } = useNotificationCenter()
          add({
            type: data.type || 'system',
            title: data.title || '',
            message: data.message || ''
          })
        } catch (e) {
          ElMessage.warning('[WS] 消息解析失败')
        }
      }

      ws.onclose = () => {
        wsStatus.value = 'disconnected'
        stopPing()
        scheduleReconnect()
      }

      ws.onerror = (err) => {
        ElMessage.warning('[WS] 连接错误')
        wsStatus.value = 'error'
      }
    } catch (e) {
      ElMessage.warning('[WS] 连接失败')
      scheduleReconnect()
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    stopPing()
    if (ws) {
      ws.close()
      ws = null
    }
    wsStatus.value = 'disconnected'
    reconnectAttempts = 0
  }

  function scheduleReconnect() {
    if (reconnectAttempts >= maxReconnectAttempts) {
      wsStatus.value = 'failed'
      return
    }

    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
    reconnectAttempts++

    reconnectTimer = setTimeout(() => {
      if (getToken()) {
        wsStatus.value = 'reconnecting'
        connect()
      }
    }, delay)
  }

  function startPing() {
    stopPing()
    pingTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ action: 'ping' }))
      }
    }, 30000)
  }

  function stopPing() {
    if (pingTimer) {
      clearInterval(pingTimer)
      pingTimer = null
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    wsStatus,
    connect,
    disconnect
  }
}
