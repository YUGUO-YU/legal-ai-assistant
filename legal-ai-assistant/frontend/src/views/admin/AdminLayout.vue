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
        <span v-if="!sidebarCollapsed" class="logo-text">LegalAI</span>
        <el-icon @click="toggleSidebar" class="collapse-icon"><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
      </div>
      <el-menu :default-active="activeMenu" :collapse="sidebarCollapsed" router class="aside-menu" background-color="var(--color-bg-dark)" text-color="var(--color-text-muted)" active-text-color="var(--color-primary)">
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

      <div class="sidebar-footer">
        <div class="sidebar-footer-toggle" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <el-icon><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
          <span v-if="!sidebarCollapsed">收起</span>
        </div>
        <div class="sidebar-footer-toggle" @click="toggleDark" :title="isDark ? '切换亮色模式' : '切换深色模式'">
          <el-icon><component :is="isDark ? Sunny : Moon" /></el-icon>
          <span v-if="!sidebarCollapsed">{{ isDark ? '深色' : '浅色' }}</span>
        </div>
      </div>
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
    document.documentElement.setAttribute('data-theme', 'dark')
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
    document.documentElement.setAttribute('data-theme', 'dark')
    localStorage.setItem('darkMode', 'true')
  } else {
    document.documentElement.removeAttribute('data-theme')
    localStorage.setItem('darkMode', 'false')
  }
}

const handleLogout = () => {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_user')
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.success('已退出后台管理')
  window.location.hash = '#/'
}

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-page);
}

// ======== 侧边栏 ========
.aside {
  width: 240px;
  min-height: 100vh;
  background: rgba(19, 17, 28, 0.95);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 1000;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &--collapsed {
    width: 64px;
  }
}

.aside-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;

  .logo-text {
    font-size: 17px;
    font-weight: 700;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    white-space: nowrap;
    letter-spacing: 0.5px;
  }

  .collapse-icon {
    cursor: pointer;
    color: var(--color-text-muted);
    font-size: 16px;
    padding: 6px;
    border-radius: var(--radius-sm);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-glass-hover);
      color: var(--color-text-primary);
    }
  }
}

.aside-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  border-right: none !important;
  padding: 8px 0;

  // 通用菜单项
  .el-menu-item,
  .el-sub-menu__title {
    border-radius: var(--radius-md);
    margin: 2px 8px;
    padding-left: 12px !important;
    height: 40px;
    line-height: 40px;
    color: var(--color-text-secondary);
    transition: all var(--transition-fast);

    .el-icon {
      color: var(--color-text-muted);
      transition: color var(--transition-fast);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
      color: var(--color-text-primary) !important;

      .el-icon {
        color: var(--color-primary-light);
      }
    }
  }

  // 激活态
  .el-menu-item.is-active {
    background: var(--gradient-glow) !important;
    border-left: 3px solid transparent;
    border-image: var(--gradient-primary) 1;
    color: var(--color-primary-light) !important;
    font-weight: 600;

    .el-icon {
      color: var(--color-primary-light);
      filter: drop-shadow(0 0 6px rgba(102, 126, 234, 0.6));
    }
  }

  // 子菜单标题
  .el-sub-menu__title {
    &:hover {
      background: rgba(255, 255, 255, 0.06) !important;
    }
  }

  // 子菜单弹出
  .el-menu--inline {
    .el-menu-item {
      padding-left: 44px !important;
      font-size: 13px;
    }
  }
}

// 折叠时菜单
.aside--collapsed {
  .aside-header {
    justify-content: center;
    padding: 0 8px;

    .logo-text { display: none; }
    .collapse-icon { margin: 0; }
  }

  .el-menu--collapse {
    width: 100% !important;
  }

  .el-menu-item,
  .el-sub-menu__title {
    justify-content: center;
    padding-left: 0 !important;
    padding-right: 0 !important;

    span, .el-sub-menu__icon-arrow { display: none !important; }
  }
}

// ======== 主内容区 ========
.right-area {
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.aside--collapsed ~ .right-area {
  margin-left: 64px;
}

// ======== 顶部栏 ========
.top-bar {
  height: 60px;
  background: rgba(15, 14, 26, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 100;

  .top-bar-title {
    font-size: 15px;
    font-weight: 600;
    background: var(--gradient-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .top-bar-right {
    display: flex;
    align-items: center;
    gap: 8px;

    .theme-toggle {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-md);
      background: var(--color-bg-glass);
      border: 1px solid var(--color-border-glass);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: var(--color-text-secondary);
      transition: all var(--transition-fast);

      &:hover {
        background: var(--color-bg-glass-hover);
        border-color: var(--color-border-glass-hover);
        color: var(--color-primary-light);
        box-shadow: var(--shadow-glow);
      }
    }

    .user-avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: var(--gradient-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      color: #fff;
      cursor: pointer;
    }

    .user-name {
      font-size: 13px;
      color: var(--color-text-secondary);
      font-weight: 500;
    }
  }
}

// ======== 主内容 ========
.main {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px;
  background: var(--color-bg-page);
}

.page-breadcrumb {
  margin-bottom: 16px;

  .el-breadcrumb__inner {
    color: var(--color-text-muted);
    font-size: 13px;
  }
  .el-breadcrumb__inner.is-link:hover {
    color: var(--color-primary-light);
  }
  .el-breadcrumb__separator {
    color: var(--color-text-muted);
  }
}

// ======== 页面过渡动画 ========
.admin-fade-enter-active {
  animation: adminPageFadeIn 0.4s ease-out;
}
.admin-fade-leave-active {
  animation: adminPageFadeOut 0.2s ease-in;
}
@keyframes adminPageFadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes adminPageFadeOut {
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-8px); }
}

// ======== 移动端 ========
.mobile-header {
  display: none;
  height: 56px;
  background: rgba(19, 17, 28, 0.95);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding: 0 16px;
  align-items: center;
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
    color: var(--color-text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);

    &:hover {
      background: var(--color-bg-glass);
      color: var(--color-text-primary);
    }
  }

  .mobile-title {
    font-weight: 600;
    font-size: 16px;
    color: var(--color-text-primary);
  }
}

.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  z-index: 999;

  @media (min-width: 768px) { display: none; }
}

.admin-layout.is-mobile {
  .aside {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 1000;
    transform: translateX(-100%);
    transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &.is-open {
      transform: translateX(0);
    }
  }

  .right-area {
    margin-left: 0 !important;
  }

  .main {
    padding-top: 72px;
  }
}

// ======== 侧边栏底部按钮 ========
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(0, 0, 0, 0.2);
  flex-shrink: 0;

  .footer-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 12px;
    border-radius: var(--radius-md);
    background: var(--color-bg-glass);
    border: 1px solid var(--color-border-glass);
    cursor: pointer;
    color: var(--color-text-muted);
    font-size: 12px;
    transition: all var(--transition-fast);
    flex: 1;
    justify-content: center;

    .el-icon { font-size: 15px; }

    &:hover {
      background: var(--color-bg-glass-hover);
      border-color: var(--color-border-glass-hover);
      color: var(--color-primary-light);
      box-shadow: var(--shadow-glow);
    }
  }
}

.aside--collapsed .sidebar-footer {
  flex-direction: column;
  gap: 4px;
  padding: 12px 8px;

  .footer-btn {
    width: 44px;
    height: 40px;
    padding: 0;
    flex: none;
    justify-content: center;
  }
}
</style>

<style>
/* 全局深色菜单覆盖 */
.el-menu {
  background: transparent !important;
  border: none !important;
}
.el-sub-menu .el-menu--inline {
  background: transparent !important;
}
.el-popper.is-dark {
  background: rgba(30, 27, 75, 0.95) !important;
  border: 1px solid var(--color-border-glass) !important;
  backdrop-filter: blur(16px);
}
</style>