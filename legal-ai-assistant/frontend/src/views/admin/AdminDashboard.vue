<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <div class="header-content">
        <h2>后台管理 · 概览</h2>
        <p>6 大域 / 27 张管理表 / 跨业务模块 10 个的统一治理入口</p>
      </div>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <el-row :gutter="16" class="metrics">
      <el-col :span="6" v-for="m in metricCards" :key="m.label">
        <el-card class="metric-card" :body-style="{ padding: '20px' }">
          <div class="metric-label">{{ m.label }}</div>
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-foot">{{ m.foot }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="domains">
      <el-col :span="8" v-for="d in domains" :key="d.path">
        <el-card class="domain-card" shadow="hover" @click="$router.push(d.path)">
          <div class="domain-head">
            <el-icon :size="22"><component :is="d.icon" /></el-icon>
            <span class="domain-title">{{ d.title }}</span>
            <el-tag :type="d.tagType" size="small">{{ d.subModules }} 子模块</el-tag>
          </div>
          <div class="domain-desc">{{ d.desc }}</div>
          <ul class="domain-items">
            <li v-for="it in d.items" :key="it">{{ it }}</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="counts-card">
      <template #header>
        <span>核心表数据量</span>
      </template>
      <el-descriptions :column="3" border>
        <el-descriptions-item v-for="(v, k) in counts" :key="k" :label="k">
          <el-tag :type="v >= 0 ? 'success' : 'danger'" size="small">
            {{ v >= 0 ? v : '缺失' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh, Tools, Document, MagicStick, DataAnalysis, Bell, Setting, Odometer } from '@element-plus/icons-vue'
import api from '../../api'

const counts = ref({})
const loading = ref(false)

const metricCards = computed(() => [
  { label: '管理用户', value: counts.value.admin_user ?? '-', foot: '基础设施域' },
  { label: '审计日志', value: counts.value.admin_audit_log ?? '-', foot: '最近 24h 操作' },
  { label: '文书模板', value: counts.value.doc_template ?? '-', foot: 'MOD-03' },
  { label: 'Prompt 模板', value: counts.value.prompt_template ?? '-', foot: 'AI 域' },
  { label: '法规主数据', value: counts.value.law_document ?? '-', foot: 'MOD-01' },
  { label: '告警规则', value: counts.value.alert_rule ?? '-', foot: '监控域' }
])

const domains = [
  { title: '基础设施域', path: '/admin/infra/users', icon: Tools, tagType: 'primary', subModules: 8, desc: '用户/角色/菜单/审计/SSO/字典', items: ['用户管理', '角色权限', '菜单权限', '操作审计', '数据字典'] },
  { title: '数据资产域', path: '/admin/biz/mod01', icon: Document, tagType: 'success', subModules: 22, desc: '10 个业务模块的主数据与审核', items: ['法规主数据', '文书模板', '草稿复核', '案件要素', '企业 API'] },
  { title: 'AI 能力域', path: '/admin/ai/prompts', icon: MagicStick, tagType: 'warning', subModules: 6, desc: 'Prompt 治理 + 模型切换 + 用量统计', items: ['Prompt 管理', '灰度发布', '模型配置', 'Token 用量'] },
  { title: '运营分析域', path: '/admin/ops/feedback', icon: DataAnalysis, tagType: 'info', subModules: 3, desc: '用户反馈 + 搜索日志', items: ['用户反馈', '搜索日志', 'AI 调用统计'] },
  { title: '监控告警域', path: '/admin/monitor/rules', icon: Bell, tagType: 'danger', subModules: 5, desc: 'Spring Boot Actuator + 告警规则', items: ['告警规则', '告警历史', '依赖监控', '慢接口 TOP'] },
  { title: '系统配置域', path: '/admin/sys/configs', icon: Setting, tagType: '', subModules: 5, desc: '动态参数 + 通知模板 + 限流', items: ['系统参数', '数据字典', '限流规则', '通知模板'] }
]

async function load() {
  loading.value = true
  try {
    const res = await api.get('/admin/stats')
    counts.value = res.data?.counts || {}
  } catch (e) {
    counts.value = {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style lang="scss" scoped>
.admin-dashboard {
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .header-content h2 { margin: 0 0 6px; font-size: 22px; font-weight: 600; }
  .header-content p { margin: 0; color: #64748b; font-size: 13px; }
}

.metrics { margin-bottom: 16px; }

.metric-card {
  .metric-label { font-size: 13px; color: #64748b; }
  .metric-value { font-size: 26px; font-weight: 700; color: #1e293b; margin: 6px 0; }
  .metric-foot { font-size: 12px; color: #94a3b8; }
}

.domains { margin-bottom: 16px; }

.domain-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s;

  &:hover { transform: translateY(-3px); }

  .domain-head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 10px;

    .domain-title {
      font-size: 16px;
      font-weight: 600;
      flex: 1;
    }
  }

  .domain-desc {
    color: #64748b;
    font-size: 13px;
    margin-bottom: 10px;
  }

  .domain-items {
    margin: 0;
    padding: 0;
    list-style: none;
    color: #475569;
    font-size: 13px;

    li {
      padding: 3px 0;
      &::before { content: '· '; color: #94a3b8; }
    }
  }
}

.counts-card { margin-top: 8px; }
</style>