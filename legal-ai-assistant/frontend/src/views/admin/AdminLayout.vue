<template>
  <div class="admin-layout">
    <el-aside width="240px" class="aside">
      <div class="aside-header">
        <el-icon><Setting /></el-icon>
        <span>后台管理系统</span>
      </div>
      <el-menu :default-active="activeMenu" router class="aside-menu" background-color="#1e293b" text-color="#cbd5e1" active-text-color="#60a5fa">
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
        </el-sub-menu>

        <el-sub-menu index="law">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>法规管理</span>
          </template>
          <el-menu-item index="/admin/law/category-types">分类维度</el-menu-item>
          <el-menu-item index="/admin/law/categories">分类管理</el-menu-item>
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
          <el-menu-item index="/admin/ai/milvus">Milvus 集合</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ops">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>运营分析</span>
          </template>
          <el-menu-item index="/admin/ops/feedback">用户反馈</el-menu-item>
          <el-menu-item index="/admin/ops/search-logs">搜索日志</el-menu-item>
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
        </el-sub-menu>
      </el-menu>
    </el-aside>

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
        <router-view />
      </el-main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Setting, Odometer, Tools, Document, MagicStick, DataAnalysis, Bell, SwitchButton, Sunny, Moon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const adminName = ref('管理员')
const isDark = ref(false)

onMounted(() => {
  try {
    const u = JSON.parse(localStorage.getItem('admin_user') || '{}')
    adminName.value = u.nickname || u.username || '管理员'
  } catch (e) { /* ignore */ }
  const saved = localStorage.getItem('darkMode')
  if (saved === 'true') {
    isDark.value = true
    document.documentElement.classList.add('dark')
  }
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
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.aside {
  background: #1e293b;
  color: #fff;

  .aside-header {
    height: 56px;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 0 20px;
    font-size: 15px;
    font-weight: 600;
    color: #fff;
    background: #0f172a;
    border-bottom: 1px solid #334155;
  }

  .aside-menu {
    border-right: none;
  }
}

.right-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.top-bar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .top-bar-title {
    font-size: 14px;
    color: #64748b;
    font-weight: 500;
  }

  .top-bar-right {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-name {
      font-size: 13px;
      color: #334155;
      font-weight: 500;
    }
  }
}

.main {
  flex: 1;
  padding: 20px;
  background: #f1f5f9;
  overflow-x: hidden;
}
</style>

<style>
html.dark .aside {
  background: #1e293b !important;
}
html.dark .aside-header {
  background: #0f172a !important;
  border-bottom-color: #334155 !important;
}
html.dark .top-bar {
  background: #1e293b !important;
  border-bottom-color: #334155 !important;
}
html.dark .top-bar-title {
  color: #94a3b8 !important;
}
html.dark .user-name {
  color: #e2e8f0 !important;
}
html.dark .main {
  background: #0f172a !important;
}
html.dark .right-area {
  background: #0f172a !important;
}
html.dark .el-menu {
  background: transparent !important;
}
html.dark .el-menu-item {
  color: #cbd5e1 !important;
}
html.dark .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}
html.dark .el-menu-item.is-active {
  background: rgba(99, 102, 241, 0.2) !important;
  color: #818cf8 !important;
}
html.dark .el-sub-menu__title {
  color: #cbd5e1 !important;
}
</style>