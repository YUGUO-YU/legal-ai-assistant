<template>
  <div id="app" class="app-container">
    <error-boundary>
      <toast-manager>
        <el-container v-if="isLoggedIn">
          <el-aside width="240px" class="sidebar">
            <div class="logo">
              <router-link to="/dashboard">
                <h2>法律AI助手</h2>
              </router-link>
            </div>
            <el-menu
              :default-active="activeMenu"
              router
              class="sidebar-menu"
            >
              <el-menu-item index="/dashboard">
                <el-icon><HomeFilled /></el-icon>
                <span>工作台</span>
              </el-menu-item>
              <el-menu-item index="/">
                <el-icon><Document /></el-icon>
                <span>AI搜法</span>
              </el-menu-item>
              <el-menu-item index="/case-similar">
                <el-icon><Connection /></el-icon>
                <span>AI类案</span>
              </el-menu-item>
              <el-menu-item index="/document">
                <el-icon><DocumentCopy /></el-icon>
                <span>AI文书起草</span>
              </el-menu-item>
              <el-menu-item index="/legal-research">
                <el-icon><Search /></el-icon>
                <span>AI法律研究</span>
              </el-menu-item>
              <el-menu-item index="/company">
                <el-icon><OfficeBuilding /></el-icon>
                <span>企业查询</span>
              </el-menu-item>
              <el-menu-item index="/case-search">
                <el-icon><Files /></el-icon>
                <span>案例查询</span>
              </el-menu-item>
              <el-menu-item index="/law-search">
                <el-icon><Collection /></el-icon>
                <span>法规查询</span>
              </el-menu-item>
              <el-menu-item index="/contract-review">
                <el-icon><Stamp /></el-icon>
                <span>AI合同审查</span>
              </el-menu-item>
              <el-menu-item index="/knowledge-base">
                <el-icon><Box /></el-icon>
                <span>案例法规库</span>
              </el-menu-item>
              <el-menu-item index="/doc-qa">
                <el-icon><ChatDotRound /></el-icon>
                <span>AI文件问答</span>
              </el-menu-item>
            </el-menu>
          </el-aside>
          <el-container>
            <el-header class="header">
              <div class="header-title">{{ pageTitle }}</div>
              <div class="header-actions">
                <el-dropdown @command="handleCommand">
                  <span class="user-info">
                    <el-avatar :size="32" icon="User" />
                    <span class="username">{{ username }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </span>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                      <el-dropdown-item command="help">使用帮助</el-dropdown-item>
                      <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </el-header>
            <el-main class="main-content">
              <router-view />
            </el-main>
          </el-container>
        </el-container>
        <router-view v-else />
      </toast-manager>
    </error-boundary>
  </div>
</template>

<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErrorBoundary from './components/ErrorBoundary.vue'
import ToastManager from './components/ToastManager.vue'

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
}

.sidebar {
  background: #001529;
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid rgba(255,255,255,0.1);
    h2 {
      color: #fff;
      font-size: 18px;
      margin: 0;
    }
  }
  .sidebar-menu {
    border-right: none;
    background: transparent;
    .el-menu-item {
      color: rgba(255,255,255,0.65);
      &:hover, &.is-active {
        background: #001529;
        color: #fff;
      }
    }
  }
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #f0f0f0;
  .header-title {
    font-size: 16px;
    font-weight: 500;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  .username {
    color: #333;
  }
}

.main-content {
  background: #f5f5f5;
  padding: 24px;
}
</style>