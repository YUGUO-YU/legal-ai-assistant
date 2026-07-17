import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useSessionStore = defineStore('session', () => {
  const adminToken = ref(localStorage.getItem('admin_token') || '')
  const adminUser = ref(null)
  const wsConnected = ref(false)

  const isLoggedIn = computed(() => !!adminToken.value)

  function setAdmin(token, user) {
    adminToken.value = token
    adminUser.value = user
    localStorage.setItem('admin_token', token)
  }

  function clearSession() {
    adminToken.value = ''
    adminUser.value = null
    wsConnected.value = false
    localStorage.removeItem('admin_token')
  }

  function setWsConnected(connected) {
    wsConnected.value = connected
  }

  return { adminToken, adminUser, wsConnected, isLoggedIn, setAdmin, clearSession, setWsConnected }
})
