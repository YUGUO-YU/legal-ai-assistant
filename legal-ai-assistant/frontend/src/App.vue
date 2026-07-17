<template>
  <div id="app" class="app-container">
    <a href="#main-content" class="skip-link">跳转到主要内容</a>

    <SRAnnouncer />

    <el-container v-if="isLoggedIn" class="main-layout">
      <el-aside :width="sidebarCollapsed ? '64px' : '260px'" class="sidebar">
        <div class="sidebar-header">
          <div class="logo">
            <div class="logo-icon">
              <el-icon :size="28"><Sunny /></el-icon>
            </div>
            <div class="logo-text" v-show="!sidebarCollapsed">
              <h2>法律AI助手</h2>
              <span>Legal AI Assistant</span>
            </div>
          </div>
          <el-icon class="collapse-btn" @click="toggleSidebar">
            <component :is="sidebarCollapsed ? Expand : Fold" />
          </el-icon>
        </div>

        <div class="menu-container">
          <el-menu
            :default-active="activeMenu"
            router
            class="sidebar-menu"
            :collapse="sidebarCollapsed"
          >
            <el-menu-item index="/dashboard">
              <el-icon><HomeFilled /></el-icon>
              <template #title>
                <span>工作台</span>
              </template>
            </el-menu-item>

            <el-divider class="menu-divider">
              <span>核心功能</span>
            </el-divider>

            <el-menu-item index="/legal-search">
              <el-icon><Search /></el-icon>
              <template #title>
                <span>AI搜法</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/case-similar">
              <el-icon><Connection /></el-icon>
              <template #title>
                <span>AI类案</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/document">
              <el-icon><DocumentCopy /></el-icon>
              <template #title>
                <span>AI文书起草</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/legal-research">
              <el-icon><TrendCharts /></el-icon>
              <template #title>
                <span>AI法律研究</span>
              </template>
            </el-menu-item>

            <el-divider class="menu-divider">
              <span>查询服务</span>
            </el-divider>

            <el-menu-item index="/company">
              <el-icon><OfficeBuilding /></el-icon>
              <template #title>
                <span>企业查询</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/case-search">
              <el-icon><Files /></el-icon>
              <template #title>
                <span>案例查询</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/law-search">
              <el-icon><Collection /></el-icon>
              <template #title>
                <span>法规查询</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/contract-review">
              <el-icon><Stamp /></el-icon>
              <template #title>
                <span>AI合同审查</span>
              </template>
            </el-menu-item>

            <el-divider class="menu-divider">
              <span>知识管理</span>
            </el-divider>

            <el-menu-item index="/knowledge-base">
              <el-icon><Box /></el-icon>
              <template #title>
                <span>案例法规库</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/doc-qa">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>
                <span>AI文件问答</span>
              </template>
            </el-menu-item>

            <el-divider class="menu-divider">
              <span>工具</span>
            </el-divider>

            <el-menu-item index="/ppt-editor">
              <el-icon><Memo /></el-icon>
              <template #title>
                <span>PPT生成器</span>
              </template>
            </el-menu-item>

            <el-menu-item index="/ppt-files">
              <el-icon><FolderOpened /></el-icon>
              <template #title>
                <span>PPT文件管理</span>
              </template>
            </el-menu-item>
          </el-menu>
        </div>

        <div class="sidebar-footer">
          <div class="sidebar-footer-toggle" @click="toggleTheme" :title="isDark ? '切换亮色模式' : '切换深色模式'">
            <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
            <span v-show="!sidebarCollapsed">{{ isDark ? '深色' : '浅色' }}</span>
          </div>
          <div class="sidebar-footer-toggle" @click="$router.push('/profile')" title="个人设置" v-show="!sidebarCollapsed">
            <el-icon><User /></el-icon>
            <span>个人设置</span>
          </div>
          <div class="sidebar-footer-toggle" @click="$router.push('/help')" title="使用帮助" v-show="!sidebarCollapsed">
            <el-icon><QuestionFilled /></el-icon>
            <span>使用帮助</span>
          </div>
          <div class="ai-badge" v-show="!sidebarCollapsed">
            <div class="ai-badge-icon">
              <el-icon><MagicStick /></el-icon>
            </div>
            <div class="ai-badge-info">
              <span class="ai-badge-title">MiniMax M3</span>
              <span class="ai-badge-status">
                <span class="status-dot"></span>
                运行中
              </span>
            </div>
          </div>
        </div>
      </el-aside>

      <el-container class="main-container">
        <el-header class="header">
          <div class="header-left">
            <h3 class="page-title">{{ pageTitle }}</h3>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>

            <div class="header-actions" style="position: relative;">
              <el-button :icon="Bell" circle class="header-btn" @click="toggleNotificationPanel" />
              <sup v-if="unreadCount > 0" class="notification-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</sup>
              <NotificationPanel
                v-if="showNotificationPanel"
                :visible="showNotificationPanel"
                @close="closeNotificationPanel"
                style="position: absolute; top: calc(100% + 8px); right: 40px;"
              />
              <el-button :icon="Setting" circle class="header-btn" @click="$router.push('/profile')" />

              <el-dropdown @command="handleCommand" trigger="click">
                <div class="user-info">
                  <el-avatar :size="36" class="user-avatar">
                    <el-icon><UserFilled /></el-icon>
                  </el-avatar>
                  <div class="user-detail">
                    <span class="username">{{ username }}</span>
                    <span class="user-role">法律顾问</span>
                  </div>
                  <el-icon class="user-arrow"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>
                      个人设置
                    </el-dropdown-item>
                    <el-dropdown-item command="help">
                      <el-icon><QuestionFilled /></el-icon>
                      使用帮助
                    </el-dropdown-item>
                    <el-dropdown-item divided command="logout">
                      <el-icon><SwitchButton /></el-icon>
                      退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </el-header>

        <el-main id="main-content" class="main-content">
          <router-view v-slot="{ Component, route }">
            <transition :name="route.meta.transition || 'page-fade'" mode="out-in">
              <component :is="Component" :key="route.path" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>

    <router-view v-else />
  </div>
  <NotificationToast />
  <OperationReplay v-if="showReplay && isDev" />
  <QuickActions ref="quickActionsRef" />
  <CommandPalette ref="commandPaletteRef" />
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  HomeFilled,
  Document,
  Connection,
  DocumentCopy,
  Search,
  OfficeBuilding,
  Files,
  Collection,
  Stamp,
  Box,
  ChatDotRound,
  Bell,
  Setting,
  User,
  UserFilled,
  QuestionFilled,
  SwitchButton,
  ArrowDown,
  Sunny,
  Moon,
  TrendCharts,
  MagicStick,
  Fold,
  Expand
} from '@element-plus/icons-vue'
import { useKeyboardShortcuts, isInputFocused } from './composables/useKeyboardShortcuts'
import NotificationPanel from '@/components/common/NotificationPanel.vue'
import { useNotificationCenter } from '@/composables/useNotificationCenter'
import NotificationToast from '@/components/common/NotificationToast.vue'
import SRAnnouncer from '@/components/common/SRAnnouncer.vue'
import OperationReplay from '@/components/common/OperationReplay.vue'
import QuickActions from '@/components/common/QuickActions.vue'
import CommandPalette from '@/components/common/CommandPalette.vue'
import { initializeApp } from '@/services/dataService'
import { wsService } from '@/services/websocketService'
import { useNotificationWs } from '@/composables/useNotificationWs'

const route = useRoute()
const router = useRouter()

const isDark = ref(false)
const showReplay = ref(false)
const sidebarCollapsed = ref(false)
const isDev = import.meta.env.DEV
let themeTimer = null
const quickActionsRef = ref(null)
const commandPaletteRef = ref(null)

if (import.meta.env.DEV) {
  document.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.shiftKey && e.key === 'R') {
      e.preventDefault()
      showReplay.value = !showReplay.value
    }
  })
}

document.addEventListener('keydown', (e) => {
  if (e.ctrlKey && e.shiftKey && e.key === 'A') {
    e.preventDefault()
    quickActionsRef.value?.show()
  }
})

const toggleTheme = () => {
  const newTheme = isDark.value ? 'light' : 'dark'
  applyTheme(newTheme)
  isDark.value = !isDark.value
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', sidebarCollapsed.value)
}

const getInitialTheme = () => {
  const saved = localStorage.getItem('theme')
  if (saved === 'dark' || saved === 'light') return saved
  const hour = new Date().getHours()
  return (hour >= 18 || hour < 6) ? 'dark' : 'light'
}

const applyTheme = (theme) => {
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem('theme', theme)
}

const scheduleNextThemeSwitch = () => {
  if (themeTimer) clearTimeout(themeTimer)
  const now = new Date()
  const currentHour = now.getHours()
  let nextSwitchHour
  if (currentHour >= 18 || currentHour < 6) {
    nextSwitchHour = 6
  } else {
    nextSwitchHour = 18
  }
  const next = new Date(now)
  next.setHours(nextSwitchHour, 0, 0, 0)
  if (next <= now) next.setDate(next.getDate() + 1)
  const ms = next - now
  themeTimer = setTimeout(() => {
    const theme = getInitialTheme()
    applyTheme(theme)
    scheduleNextThemeSwitch()
  }, ms)
}

const isLoggedIn = ref(!!localStorage.getItem('token'))

onMounted(() => {
  const theme = getInitialTheme()
  applyTheme(theme)
  isDark.value = theme === 'dark'
  scheduleNextThemeSwitch()

  const savedCollapsed = localStorage.getItem('sidebar_collapsed')
  if (savedCollapsed === 'true') {
    sidebarCollapsed.value = true
  }

  if (isLoggedIn.value) {
    initializeApp()
    const { connect: connectNotificationWs } = useNotificationWs()
    connectNotificationWs()
  }

  if (!import.meta.env.DEV) {
    wsService.connect().catch(console.error)
  }

  document.addEventListener('click', handleDocClick)
})

onUnmounted(() => {
  if (themeTimer) clearTimeout(themeTimer)
  document.removeEventListener('click', handleDocClick)
})

function handleDocClick(e) {
  if (showNotificationPanel.value) {
    const panel = document.querySelector('.notification-panel')
    const btn = e.target.closest('.header-btn')
    if (panel && !panel.contains(e.target) && !btn) {
      showNotificationPanel.value = false
    }
  }
}

const { unreadCount } = useNotificationCenter()
const showNotificationPanel = ref(false)
const notificationPanelRef = ref(null)

function toggleNotificationPanel() {
  showNotificationPanel.value = !showNotificationPanel.value
}

function closeNotificationPanel() {
  showNotificationPanel.value = false
}

window.addEventListener('login-state-change', () => {
  isLoggedIn.value = !!localStorage.getItem('token')
})

const username = computed(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    const info = JSON.parse(userInfo)
    return info.nickname || info.username || '用户'
  }
  return '用户'
})

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const titles = {
    '/dashboard': '工作台',
    '/': 'AI搜法',
    '/case-similar': 'AI类案',
    '/document': 'AI文书起草',
    '/legal-research': 'AI法律研究',
    '/company': '企业查询',
    '/case-search': '案例查询',
    '/law-search': '法规查询',
    '/contract-review': 'AI合同审查',
    '/knowledge-base': '案例法规库',
    '/doc-qa': 'AI文件问答',
    '/ppt-editor': 'PPT编辑器',
    '/ppt-files': 'PPT文件管理',
    '/profile': '个人设置',
    '/help': '使用帮助'
  }
  return titles[route.path] || '法律AI助手'
})

const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_user')
      isLoggedIn.value = false
      ElMessage.success('已退出登录')
      router.push('/')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'help':
      router.push('/help')
      break
  }
}

watch(() => route.path, (newPath) => {
  if (!isLoggedIn.value && newPath !== '/' && newPath !== '/login' && !newPath.startsWith('/admin')) {
    router.push('/')
  }
})

onMounted(() => {
  if (!isLoggedIn.value && route.path !== '/' && !route.path.startsWith('/admin')) {
    router.push('/')
  }
})

useKeyboardShortcuts([
  {
    match: (e) => e.key === '/' && !isInputFocused(),
    handler: () => {
      const searchInput = document.querySelector('.search-input-wrapper input')
      if (searchInput) searchInput.focus()
    }
  },
  {
    match: (e) => e.key === 'Escape',
    handler: () => {
      const dialog = document.querySelector('.el-dialog__wrapper:not(.is-hidden)')
      if (dialog) {
        const closeBtn = dialog.querySelector('.el-dialog__headerbtn')
        if (closeBtn) closeBtn.click()
      }
    }
  }
])
</script>

<style lang="scss" scoped>
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #667eea;
  color: #fff;
  padding: 8px 16px;
  z-index: 10000;
  transition: top 0.3s;
  text-decoration: none;
  border-radius: 0 0 8px 0;
}

.skip-link:focus {
  top: 0;
}

.app-container {
  height: 100vh;
  overflow: hidden;
}

.main-layout {
  height: 100vh;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  transition: width 0.3s ease;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 200px;
    background: linear-gradient(135deg, rgba(102, 126, 234, 0.15), rgba(118, 75, 162, 0.15));
    pointer-events: none;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    position: relative;
    z-index: 1;

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
      min-width: 0;

      .logo-icon {
        width: 36px;
        height: 36px;
        background: linear-gradient(135deg, #667eea, #764ba2);
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        flex-shrink: 0;
      }

      .logo-text {
        h2 {
          color: #fff;
          font-size: 16px;
          font-weight: 600;
          margin: 0 0 2px 0;
          white-space: nowrap;
        }

        span {
          color: rgba(255, 255, 255, 0.5);
          font-size: 10px;
          letter-spacing: 1px;
          white-space: nowrap;
        }
      }
    }

    .collapse-btn {
      color: rgba(255, 255, 255, 0.6);
      font-size: 18px;
      cursor: pointer;
      padding: 6px;
      border-radius: 6px;
      transition: all 0.2s;
      flex-shrink: 0;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #fff;
      }
    }
  }
}

.menu-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 12px;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

.sidebar-menu {
  background: transparent;
  border: none;

  :deep(.el-menu-item) {
    height: 48px;
    line-height: 48px;
    color: rgba(255, 255, 255, 0.65);
    border-radius: 12px;
    margin-bottom: 4px;
    transition: all 0.3s;
    padding-left: 16px !important;

    .el-icon {
      font-size: 18px;
      margin-right: 12px;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      color: #fff;
    }

    &.is-active {
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.8), rgba(118, 75, 162, 0.8));
      color: #fff;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 24px;
        background: #fff;
        border-radius: 0 4px 4px 0;
      }
    }
  }
}

.menu-divider {
  margin: 16px 8px;
  border-color: rgba(255, 255, 255, 0.1);

  :deep(.el-divider__text) {
    color: rgba(255, 255, 255, 0.35);
    font-size: 11px;
    padding: 0 8px;
    background: transparent;
  }
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .sidebar-footer-toggle {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 8px;
    cursor: pointer;
    color: rgba(255, 255, 255, 0.6);
    font-size: 13px;
    transition: all 0.2s;

    .el-icon {
      font-size: 16px;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      color: #fff;
    }
  }

  .ai-badge {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .ai-badge-icon {
      width: 32px;
      height: 32px;
      background: linear-gradient(135deg, #10b981, #059669);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 16px;
      flex-shrink: 0;
    }

    .ai-badge-info {
      display: flex;
      flex-direction: column;
      min-width: 0;

      .ai-badge-title {
        color: #fff;
        font-size: 12px;
        font-weight: 500;
        white-space: nowrap;
      }

      .ai-badge-status {
        display: flex;
        align-items: center;
        gap: 4px;
        color: rgba(255, 255, 255, 0.5);
        font-size: 10px;

        .status-dot {
          width: 5px;
          height: 5px;
          background: #10b981;
          border-radius: 50%;
          animation: pulse 2s infinite;
        }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.main-container {
  background: #f8fafc;
  flex-direction: column;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  border-bottom: 1px solid #f0f0f0;
  height: 72px;

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #1f2937;
      margin: 0;
    }

    :deep(.el-breadcrumb) {
      font-size: 12px;

      .el-breadcrumb__inner {
        color: #9ca3af;

        &.is-link:hover {
          color: #667eea;
        }
      }

      .el-breadcrumb__separator {
        color: #d1d5db;
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .header-btn {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      border: 1px solid #f0f0f0;
      background: #fff;
      color: #6b7280;
      transition: all 0.3s;

      &:hover {
        border-color: #667eea;
        color: #667eea;
        background: rgba(102, 126, 234, 0.05);
      }
    }

    .notification-badge {
      position: absolute;
      top: -2px;
      right: -2px;
      min-width: 18px;
      height: 18px;
      padding: 0 4px;
      background: #f56c6c;
      color: #fff;
      font-size: 10px;
      font-weight: 600;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid #fff;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 6px 12px 6px 6px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
    border: 1px solid transparent;

    &:hover {
      background: #f9fafb;
      border-color: #f0f0f0;
    }

    .user-avatar {
      background: linear-gradient(135deg, #667eea, #764ba2);
      border: none;
    }

    .user-detail {
      display: flex;
      flex-direction: column;

      .username {
        font-size: 14px;
        font-weight: 500;
        color: #1f2937;
      }

      .user-role {
        font-size: 11px;
        color: #9ca3af;
      }
    }

    .user-arrow {
      color: #9ca3af;
      font-size: 12px;
    }
  }
}

.main-content {
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  padding: 24px 28px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-thumb {
    background: #e5e7eb;
    border-radius: 4px;

    &:hover {
      background: #d1d5db;
    }
  }
}

.page-slide-enter-active,
.page-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.25s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.page-zoom-enter-active,
.page-zoom-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-zoom-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.page-zoom-leave-to {
  opacity: 0;
  transform: scale(1.02);
}

:deep(.el-dropdown-menu) {
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  border: 1px solid #f0f0f0;

  .el-dropdown-menu__item {
    padding: 10px 16px;
    border-radius: 8px;
    margin-bottom: 2px;
    font-size: 14px;

    .el-icon {
      margin-right: 10px;
    }

    &:last-child {
      margin-bottom: 0;
    }

    &:hover {
      background: rgba(102, 126, 234, 0.08);
      color: #667eea;
    }
  }
}
</style>

<style>
[data-theme="dark"] {
  .sidebar {
    background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%) !important;
  }
  .aside-header {
    background: #0f172a !important;
    border-bottom-color: #334155 !important;
  }
  .top-bar {
    background: #1e293b !important;
    border-bottom-color: #334155 !important;
  }
  .top-bar-title {
    color: #e2e8f0 !important;
  }
  .user-name {
    color: #e2e8f0 !important;
  }
  .main {
    background: #0f172a !important;
  }
  .main-container {
    background: #0f172a !important;
  }
  .header {
    background: #1e293b !important;
    border-bottom-color: #334155 !important;
  }
  .header .page-title {
    color: #f1f5f9 !important;
  }
  .header-btn {
    background: #334155 !important;
    border-color: #475569 !important;
    color: #94a3b8 !important;
    &:hover {
      border-color: #667eea !important;
      color: #667eea !important;
      background: rgba(102, 126, 234, 0.1) !important;
    }
  }
  .user-info {
    background: #334155 !important;
    &:hover {
      background: #475569 !important;
    }
    .username {
      color: #f1f5f9 !important;
    }
    .user-role {
      color: #94a3b8 !important;
    }
  }
  .page-card {
    background: #1e293b !important;
    border-color: #334155 !important;
  }
  .page-header h2 {
    color: #f1f5f9 !important;
  }
  .page-header p {
    color: #94a3b8 !important;
  }
  .result-item {
    background: #1e293b !important;
    border-color: #334155 !important;
  }
  .el-card {
    background: #1e293b !important;
    border-color: #334155 !important;
    --el-card-bg-color: #1e293b;
  }
  .el-table {
    background: #1e293b !important;
    --el-table-bg-color: #1e293b;
    --el-table-tr-bg-color: #1e293b;
    --el-table-header-bg-color: #0f172a;
    color: #f1f5f9 !important;
  }
  .el-input__wrapper {
    background: #334155 !important;
    box-shadow: none !important;
  }
  .el-input__inner {
    color: #f1f5f9 !important;
  }
  .el-input__inner::placeholder {
    color: #64748b !important;
  }
  .el-select__wrapper {
    background: #334155 !important;
    box-shadow: none !important;
  }
  .el-textarea__inner {
    background: #334155 !important;
    color: #f1f5f9 !important;
    box-shadow: none !important;
  }
  .el-dialog {
    --el-dialog-bg-color: #1e293b;
  }
  .el-message-box {
    --el-messagebox-bg-color: #1e293b;
  }
  .el-dropdown-menu {
    background: #1e293b !important;
    border-color: #334155 !important;
  }
  .el-dropdown-menu__item {
    color: #f1f5f9 !important;
    &:hover {
      background: #334155 !important;
    }
  }
  .el-tabs__item {
    color: #94a3b8 !important;
    &.is-active {
      color: #667eea !important;
    }
  }
  .el-tabs__nav-wrap::after {
    background: #334155 !important;
  }
  .el-tag {
    --el-tag-bg-color: #334155;
    --el-tag-border-color: #475569;
    --el-tag-text-color: #f1f5f9;
  }
  .el-alert {
    --el-alert-bg-color: #1e293b;
    border-color: #334155 !important;
  }
  .el-pagination {
    --el-pagination-bg-color: #1e293b;
    --el-pagination-button-bg-color: #334155;
    color: #f1f5f9 !important;
  }
  .empty-state {
    background: #1e293b !important;
    .empty-title {
      color: #f1f5f9 !important;
    }
    .empty-desc {
      color: #94a3b8 !important;
    }
  }
  .loading-container {
    background: #0f172a !important;
  }
  .analysis-report {
    .el-tabs__content {
      background: #1e293b !important;
    }
  }
}
</style>
