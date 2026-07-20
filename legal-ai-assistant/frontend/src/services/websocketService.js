class WebSocketService {
  constructor() {
    this.socket = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.heartbeatInterval = null
    this.listeners = new Map()
    this.isConnected = false
  }

  connect(url = '/ws') {
    this.reconnectAttempts = 0
    if (this.maxReconnectAttempts === 0) {
      this.maxReconnectAttempts = 5
    }
    return new Promise((resolve, reject) => {
      try {
        const fullUrl = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${url}`

        this.socket = new WebSocket(fullUrl)

        this.socket.onopen = () => {
          console.log('WebSocket connected')
          this.isConnected = true
          this.reconnectAttempts = 0
          this.startHeartbeat()
          this.emit('connected')
          resolve()
        }

        this.socket.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            this.handleMessage(data)
          } catch (e) {
            console.error('WebSocket message parse error:', e)
          }
        }

        this.socket.onerror = (error) => {
          console.error('WebSocket error:', error)
          this.emit('error', error)
          reject(error)
        }

        this.socket.onclose = (event) => {
          console.log('WebSocket closed:', event.code, event.reason)
          this.isConnected = false
          this.stopHeartbeat()
          this.emit('disconnected', event)

          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.scheduleReconnect()
          }
        }
      } catch (error) {
        reject(error)
      }
    })
  }

  handleMessage(data) {
    const { type, payload } = data

    this.emit(type, payload)
    this.emit('message', data)
  }

  send(type, payload) {
    if (!this.isConnected) {
      console.warn('WebSocket not connected')
      return false
    }

    const message = JSON.stringify({ type, payload, timestamp: Date.now() })
    this.socket.send(message)
    return true
  }

  subscribe(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set())
    }
    this.listeners.get(event).add(callback)

    return () => {
      this.listeners.get(event)?.delete(callback)
    }
  }

  emit(event, data) {
    const callbacks = this.listeners.get(event)
    if (callbacks) {
      callbacks.forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error(`Error in WebSocket listener for ${event}:`, e)
        }
      })
    }
  }

  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected) {
        this.send('ping', { timestamp: Date.now() })
      }
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  scheduleReconnect() {
    this.reconnectAttempts++
    console.log(`Scheduling reconnect attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)

    setTimeout(() => {
      if (!this.isConnected) {
        this.connect().catch(() => {})
      }
    }, this.reconnectDelay * this.reconnectAttempts)
  }

  disconnect() {
    this.stopHeartbeat()
    this.reconnectAttempts = this.maxReconnectAttempts

    if (this.socket) {
      this.socket.close(1000, 'Client disconnect')
      this.socket = null
    }

    this.isConnected = false
  }

  getStatus() {
    return {
      connected: this.isConnected,
      reconnectAttempts: this.reconnectAttempts
    }
  }
}

export const wsService = new WebSocketService()
export default wsService