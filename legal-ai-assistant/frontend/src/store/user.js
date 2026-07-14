import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const isLoggedIn = computed(() => !!token.value)

  const login = async (username, password) => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    const data = await res.json()
    if (data.code === 200) {
      token.value = data.data.token
      userInfo.value = data.data.userInfo
      localStorage.setItem('token', data.data.token)
      return true
    }
    return false
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    localStorage.removeItem('userInfo')
  }

  const setUserInfo = (info) => {
    userInfo.value = info
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    setUserInfo
  }
})