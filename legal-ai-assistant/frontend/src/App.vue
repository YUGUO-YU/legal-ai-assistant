<template>
  <div id="app" class="app-container">
    <error-boundary>
      <toast-manager>
        <el-container v-if="isLoggedIn" class="main-layout">
          <el-aside width="260px" class="sidebar">
            <div class="logo">
              <div class="logo-icon">
                <el-icon :size="28"><Sunny /></el-icon>
              </div>
              <div class="logo-text">
                <h2>法律AI助手</h2>
                <span>Legal AI Assistant</span>
              </div>
            </div>

            <div class="menu-container">
              <el-menu
                :default-active="activeMenu"
                router
                class="sidebar-menu"
                :collapse="false"
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

                <el-menu-item index="/">
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
              <div class="ai-badge">
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

              <div class="header-actions">
                <el-button :icon="Bell" circle class="header-btn" />
                <el-button :icon="Setting" circle class="header-btn" />

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

            <el-main class="main-content">
              <router-view v-slot="{ Component }">
                <transition name="fade-slide" mode="out-in">
                  <component :is="Component" />
                </transition>
              </router-view>
            </el-main>
          </el-container>
        </el-container>

        <transition name="fade" v-else>
          <router-view />
        </transition>
      </toast-manager>
    </error-boundary>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
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
  TrendCharts,
  MagicStick
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const isLoggedIn = computed(() => !!localStorage.getItem('token'))
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
    '/profile': '个人设置'
  }
  return titles[route.path] || '法律AI助手'
})

const handleCommand = (command) => {
  switch (command) {
    case 'logout':
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      ElMessage.success('已退出登录')
      router.push('/login')
      break
    case 'profile':
      router.push('/profile')
      break
    case 'help':
      ElMessage.info('帮助功能开发中...')
      break
  }
}

watch(() => route.path, (newPath) => {
  if (!isLoggedIn.value && newPath !== '/login') {
    router.push('/login')
  }
})

onMounted(() => {
  if (!isLoggedIn.value && route.path !== '/login') {
    router.push('/login')
  }
})
</script>

<style lang="scss" scoped>
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

  .logo {
    padding: 24px 20px;
    display: flex;
    align-items: center;
    gap: 14px;
    position: relative;
    z-index: 1;

    .logo-icon {
      width: 52px;
      height: 52px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
    }

    .logo-text {
      h2 {
        color: #fff;
        font-size: 18px;
        font-weight: 600;
        margin: 0 0 4px 0;
      }

      span {
        color: rgba(255, 255, 255, 0.5);
        font-size: 11px;
        letter-spacing: 1px;
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
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  z-index: 1;

  .ai-badge {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    border: 1px solid rgba(255, 255, 255, 0.1);

    .ai-badge-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #10b981, #059669);
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 18px;
    }

    .ai-badge-info {
      display: flex;
      flex-direction: column;

      .ai-badge-title {
        color: #fff;
        font-size: 13px;
        font-weight: 500;
      }

      .ai-badge-status {
        display: flex;
        align-items: center;
        gap: 6px;
        color: rgba(255, 255, 255, 0.5);
        font-size: 11px;

        .status-dot {
          width: 6px;
          height: 6px;
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

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
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
