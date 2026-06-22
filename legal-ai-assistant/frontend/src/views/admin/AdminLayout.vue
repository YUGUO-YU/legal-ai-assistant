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

    <el-main class="main">
      <router-view />
    </el-main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Setting, Odometer, Tools, Document, MagicStick, DataAnalysis, Bell } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style lang="scss" scoped>
.admin-layout {
  display: flex;
  min-height: calc(100vh - 60px);
}

.aside {
  background: #1e293b;
  color: #fff;

  .aside-header {
    height: 60px;
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

.main {
  flex: 1;
  padding: 20px;
  background: #f1f5f9;
  overflow-x: hidden;
}
</style>