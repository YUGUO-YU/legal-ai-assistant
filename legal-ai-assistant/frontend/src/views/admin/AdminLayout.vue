<template>
  <div :class="['admin-layout', { 'sidebar-collapsed': sidebarCollapsed, 'is-mobile': isMobile }]">
    <!-- 移动端顶部导航 -->
    <header class="mobile-header" v-if="isMobile">
      <div class="mobile-menu-btn" @click="toggleSidebar">
        <el-icon><component :is="sidebarOpen ? 'Close' : 'Menu'" /></el-icon>
      </div>
      <div class="mobile-title">{{ currentPage || '后台管理' }}</div>
    </header>

    <!-- 侧边栏 -->
    <el-aside :class="['aside', { 'aside--collapsed': sidebarCollapsed, 'is-open': sidebarOpen }]" v-show="!isMobile || sidebarOpen">
      <div class="aside-header">
        <span v-if="!sidebarCollapsed">法律AI助手</span>
        <el-icon @click="toggleSidebar" class="collapse-icon"><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
      </div>
      <el-menu :default-active="activeMenu" :collapse="sidebarCollapsed" router class="aside-menu" background-color="#1e293b" text-color="#cbd5e1" active-text-color="#60a5fa">
        <el-menu-item index="/admin">
          <el-icon><Odometer /></el-icon>
          <span>概览</span>
        </el-menu-item>

        <el-sub-menu index="infra">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>基础设施</span>
          </template>
          <el-menu-item index="/admin/infra/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/infra/roles">角色权限</el-menu-item>
          <el-menu-item index="/admin/infra/menus">菜单权限</el-menu-item>
          <el-menu-item index="/admin/infra/audit">操作审计</el-menu-item>
          <el-menu-item index="/admin/infra/service-health">服务健康</el-menu-item>
          <el-menu-item index="/admin/infra/search-feedback">搜索反馈</el-menu-item>
          <el-menu-item index="/admin/infra/law-favorites">法规收藏</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="law">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>法规管理</span>
          </template>
          <el-menu-item index="/admin/law/category-types">分类维度</el-menu-item>
          <el-menu-item index="/admin/law/categories">分类管理</el-menu-item>
          <el-menu-item index="/admin/law/relations">关联管理</el-menu-item>
          <el-menu-item index="/admin/law/import">AI导入</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="biz">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>数据资产</span>
          </template>
          <el-menu-item index="/admin/biz/mod01">法规主数据</el-menu-item>
          <el-menu-item index="/admin/biz/mod01-revisions">法规修订</el-menu-item>
          <el-menu-item index="/admin/biz/mod01-crawl">爬虫任务</el-menu-item>
          <el-menu-item index="/admin/biz/mod02">案件主数据</el-menu-item>
          <el-menu-item index="/admin/biz/mod02-elements">案件要素</el-menu-item>
          <el-menu-item index="/admin/biz/mod03-templates">文书模板</el-menu-item>
          <el-menu-item index="/admin/biz/mod03-drafts">草稿复核</el-menu-item>
          <el-menu-item index="/admin/biz/mod03-rules">复核规则</el-menu-item>
          <el-menu-item index="/admin/biz/mod04">研究任务</el-menu-item>
          <el-menu-item index="/admin/biz/mod05">企业 API</el-menu-item>
          <el-menu-item index="/admin/biz/mod06">案例查询日志</el-menu-item>
          <el-menu-item index="/admin/biz/mod07">法规查询</el-menu-item>
          <el-menu-item index="/admin/biz/mod08">合同规则</el-menu-item>
          <el-menu-item index="/admin/biz/mod09-kb">知识库</el-menu-item>
          <el-menu-item index="/admin/biz/mod09-strategy">分块策略</el-menu-item>
          <el-menu-item index="/admin/biz/mod10">问答会话</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ai">
          <template #title>
            <el-icon><MagicStick /></el-icon>
            <span>AI 能力</span>
          </template>
          <el-menu-item index="/admin/ai/prompts">Prompt 管理</el-menu-item>
          <el-menu-item index="/admin/ai/gray">灰度发布</el-menu-item>
          <el-menu-item index="/admin/ai/llm">模型配置</el-menu-item>
          <el-menu-item index="/admin/ai/token">Token 用量</el-menu-item>
          <el-menu-item index="/admin/ai/kb-chunks">分块管理</el-menu-item>
          <el-menu-item index="/admin/ai/milvus">Milvus 集合</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ops">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>运营分析</span>
          </template>
          <el-menu-item index="/admin/ops/feedback">用户反馈</el-menu-item>
          <el-menu-item index="/admin/ops/search-logs">搜索日志</el-menu-item>
          <el-menu-item index="/admin/ops/app-logs">应用日志</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="monitor">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>监控告警</span>
          </template>
          <el-menu-item index="/admin/monitor/rules">告警规则</el-menu-item>
          <el-menu-item index="/admin/monitor/history">告警历史</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="sys">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </template>
          <el-menu-item index="/admin/sys/configs">系统参数</el-menu-item>
          <el-menu-item index="/admin/sys/dicts">数据字典</el-menu-item>
          <el-menu-item index="/admin/sys/announcements">系统公告</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 遮罩层 -->
    <div class="sidebar-overlay" v-if="isMobile && sidebarOpen" @click="closeSidebar"></div>

    <div class="right-area">
      <div class="top-bar">
        <span class="top-bar-title">Legal AI 后台</span>
        <div class="top-bar-right">
          <el-button text size="small" @click="toggleDark">
            <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
          </el-button>
          <span class="user-name">{{ adminName }}</span>
          <el-button text size="small" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出
          </el-button>
        </div>
      </div>
      <el-main class="main">
        <el-breadcrumb separator="/" class="page-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentGroup">{{ currentGroup }}</el-breadcrumb-item>
          <el-breadcrumb-item v-if="currentPage">{{ currentPage }}</el-breadcrumb-item>
        </el-breadcrumb>
        <router-view v-slot="{ Component, route }">
          <transition :name="route.meta.transition || 'admin-fade'" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </el-main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting, Odometer, Tools, Document, MagicStick, DataAnalysis, Bell, SwitchButton, Sunny, Moon, Fold, Expand, Close, Menu } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const adminName = ref('管理员')
const isDark = ref(false)
const sidebarCollapsed = ref(false)
const isMobile = ref(false)
const sidebarOpen = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const toggleSidebar = () => {
  if (isMobile.value) {
    sidebarOpen.value = !sidebarOpen.value
  } else {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('sidebar_collapsed', sidebarCollapsed.value)
  }
}

const closeSidebar = () => {
  sidebarOpen.value = false
}

const menuMap = {
  'infra': '基础设施',
  'law': '法规管理',
  'biz': '数据资产',
  'ai': 'AI 能力',
  'ops': '运营分析',
  'monitor': '监控告警',
  'sys': '系统配置'
}

const pageNameMap = {
  'users': '用户管理',
  'roles': '角色权限',
  'menus': '菜单权限',
  'audit': '操作审计',
  'service-health': '服务健康',
  'search-feedback': '搜索反馈',
  'law-favorites': '法规收藏',
  'frontend-users': '前端用户',
  'category-types': '分类维度',
  'categories': '分类管理',
  'relations': '关联管理',
  'import': 'AI导入',
  'mod01': '法规主数据',
  'mod01-revisions': '法规修订',
  'mod01-crawl': '爬虫任务',
  'mod02': '案件主数据',
  'mod02-elements': '案件要素',
  'mod03-templates': '文书模板',
  'mod03-drafts': '草稿复核',
  'mod03-rules': '复核规则',
  'mod04': '研究任务',
  'mod05': '企业 API',
  'mod06': '案例查询日志',
  'mod07': '法规查询',
  'mod08': '合同规则',
  'mod09-kb': '知识库',
  'mod09-strategy': '分块策略',
  'mod10': '问答会话',
  'prompts': 'Prompt 管理',
  'gray': '灰度发布',
  'llm': '模型配置',
  'token': 'Token 用量',
  'milvus': 'Milvus 集合',
  'kb-chunks': '分块管理',
  'feedback': '用户反馈',
  'search-logs': '搜索日志',
  'app-logs': '应用日志',
  'rules': '告警规则',
  'history': '告警历史',
  'configs': '系统参数',
  'dicts': '数据字典'
}

const currentGroup = computed(() => {
  const path = route.path
  for (const key in menuMap) {
    if (path.includes('/' + key)) return menuMap[key]
  }
  return ''
})

const currentPage = computed(() => {
  const path = route.path
  const segments = path.split('/')
  const lastSegment = segments[segments.length - 1]
  return pageNameMap[lastSegment] || ''
})

onMounted(() => {
  try {
    const u = JSON.parse(localStorage.getItem('admin_user') || '{}')
    adminName.value = u.nickname || u.username || '管理员'
  } catch (e) { /* ignore */ }
  const savedDark = localStorage.getItem('darkMode')
  if (savedDark === 'true') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
  const savedCollapsed = localStorage.getItem('sidebar_collapsed')
  if (savedCollapsed === 'true') {
    sidebarCollapsed.value = true
  }
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

const toggleDark = () => {
  isDark.value = !isDark.value
  if (isDark.value) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('darkMode', 'true')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('darkMode', 'false')
  }
}

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_user')
  ElMessage.success('已退出后台管理')
  router.push('/admin/login')
}

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.aside {
  width: 240px;
  background: var(--color-bg-secondary);
  color: #fff;
  transition: width 0.3s ease;
  overflow: hidden;

  &--collapsed {
    width: 64px;

    .aside-menu {
      width: 64px;
    }
  }

  .aside-header {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    padding: 0 16px;
    font-size: 15px;
    font-weight: 600;
    color: #fff;
    background: var(--color-bg);
    border-bottom: 1px solid var(--color-border);
    white-space: nowrap;

    .collapse-icon {
      cursor: pointer;
      flex-shrink: 0;
    }
  }

  .aside-menu {
    border-right: none;
    transition: width 0.3s ease;
  }
}

.right-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 240px;
  transition: margin-left 0.3s ease;

  .aside--collapsed + & {
    margin-left: 64px;
  }
}

.top-bar {
  height: 56px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .top-bar-title {
    font-size: 14px;
    color: var(--color-text-secondary);
    font-weight: 500;
  }

  .top-bar-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-name {
      font-size: 13px;
      color: var(--color-text-primary);
      font-weight: 500;
    }
  }
}

.main {
  flex: 1;
  padding: 16px 20px;
  background: var(--color-bg-secondary);
  overflow-x: hidden;
}

.page-breadcrumb {
  margin-bottom: 16px;
}

.admin-fade-enter-active,
.admin-fade-leave-active {
  transition: all 0.25s ease;
}

.admin-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.admin-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

// 移动端顶部导航
.mobile-header {
  display: none;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--color-border-light);
  padding: 0 16px;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1001;

  @media (max-width: 768px) {
    display: flex;
  }

  .mobile-menu-btn {
    font-size: 20px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
  }

  .mobile-title {
    font-weight: 600;
    font-size: 16px;
    color: var(--color-text-primary);
  }
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  z-index: 999;

  @media (min-width: 768px) {
    display: none;
  }
}

// 移动端布局适配
.admin-layout {
  &.is-mobile {
    .aside {
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      z-index: 1000;
      transform: translateX(-100%);
      transition: transform 0.3s ease;

      &.is-open {
        transform: translateX(0);
      }
    }

    .right-area {
      margin-left: 0 !important;
    }

    .main {
      padding-top: 56px;
    }
  }
}
</style>

<style>
html.dark .aside {
  background: var(--color-bg-secondary) !important;
}
html.dark .aside-header {
  background: var(--color-bg) !important;
  border-bottom-color: var(--color-border) !important;
}
html.dark .top-bar {
  background: var(--color-bg-secondary) !important;
  border-bottom-color: var(--color-border) !important;
}
html.dark .top-bar-title {
  color: var(--color-text-placeholder) !important;
}
html.dark .user-name {
  color: var(--color-border-light) !important;
}
html.dark .main {
  background: var(--color-bg) !important;
}
html.dark .right-area {
  background: var(--color-bg) !important;
}
html.dark .el-menu {
  background: transparent !important;
}
html.dark .el-menu-item {
  color: var(--color-border-dark) !important;
}
html.dark .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}
html.dark .el-menu-item.is-active {
  background: rgba(99, 102, 241, 0.2) !important;
  color: #818cf8 !important;
}
html.dark .el-sub-menu__title {
  color: var(--color-border-dark) !important;
}
</style>