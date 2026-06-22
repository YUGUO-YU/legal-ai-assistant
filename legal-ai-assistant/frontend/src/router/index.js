import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    redirect: '/'
  },
  {
    path: '/',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/legal-search',
    component: () => import('../views/LegalSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/case-similar',
    component: () => import('../views/CaseSimilar.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/document',
    component: () => import('../views/Document.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/legal-research',
    component: () => import('../views/LegalResearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/company',
    component: () => import('../views/CompanyQuery.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/company-detail/:companyUuid',
    component: () => import('../views/CompanyDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/shareholder-detail/:companyUuid/:shareholderName',
    component: () => import('../views/ShareholderDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/case-search',
    component: () => import('../views/CaseSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/case-detail/:caseUuid',
    component: () => import('../views/CaseDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/law-search',
    component: () => import('../views/LawSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/law-detail/:lawUuid',
    component: () => import('../views/LawDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contract-review',
    component: () => import('../views/ContractReview.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contract-risk/:reviewUuid',
    component: () => import('../views/ContractRiskDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/knowledge-base',
    component: () => import('../views/KnowledgeBase.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/kb-detail/:kbId',
    component: () => import('../views/KbDocumentDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/doc-qa',
    component: () => import('../views/DocQa.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/qa-session/:sessionId',
    component: () => import('../views/SessionDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ppt-editor',
    component: () => import('../views/PptEditor.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ppt-files',
    component: () => import('../views/FileManager.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/data-manager',
    component: () => import('../views/DataManager.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('../views/admin/AdminDashboard.vue') },

      { path: 'infra/users', component: () => import('../views/admin/infra/Users.vue') },
      { path: 'infra/roles', component: () => import('../views/admin/infra/Roles.vue') },
      { path: 'infra/menus', component: () => import('../views/admin/infra/Menus.vue') },
      { path: 'infra/audit', component: () => import('../views/admin/infra/AuditLogs.vue') },
      { path: 'infra/service-health', component: () => import('../views/admin/infra/ServiceHealth.vue') },

      { path: 'biz/mod01', component: () => import('../views/admin/biz/Mod01Laws.vue') },
      { path: 'biz/mod01-revisions', component: () => import('../views/admin/biz/Mod01Revisions.vue') },
      { path: 'biz/mod01-crawl', component: () => import('../views/admin/biz/Mod01Crawl.vue') },
      { path: 'biz/mod02', component: () => import('../views/admin/biz/Mod02Cases.vue') },
      { path: 'biz/mod02-elements', component: () => import('../views/admin/biz/Mod02Elements.vue') },
      { path: 'biz/mod03-templates', component: () => import('../views/admin/biz/Mod03Templates.vue') },
      { path: 'biz/mod03-drafts', component: () => import('../views/admin/biz/Mod03Drafts.vue') },
      { path: 'biz/mod03-rules', component: () => import('../views/admin/biz/Mod03ReviewRules.vue') },
      { path: 'biz/mod04', component: () => import('../views/admin/biz/Mod04Tasks.vue') },
      { path: 'biz/mod05', component: () => import('../views/admin/biz/Mod05CompanyApis.vue') },
      { path: 'biz/mod06', component: () => import('../views/admin/biz/Mod06CaseSearch.vue') },
      { path: 'biz/mod07', component: () => import('../views/admin/biz/Mod07Laws.vue') },
      { path: 'biz/mod08', component: () => import('../views/admin/biz/Mod08ContractRules.vue') },
      { path: 'biz/mod09-kb', component: () => import('../views/admin/biz/Mod09KbBases.vue') },
      { path: 'biz/mod09-strategy', component: () => import('../views/admin/biz/Mod09Strategy.vue') },
      { path: 'biz/mod10', component: () => import('../views/admin/biz/Mod10QaSessions.vue') },

      { path: 'ai/prompts', component: () => import('../views/admin/ai/Prompts.vue') },
      { path: 'ai/gray', component: () => import('../views/admin/ai/GrayReleases.vue') },
      { path: 'ai/llm', component: () => import('../views/admin/ai/LlmModels.vue') },
      { path: 'ai/token', component: () => import('../views/admin/ai/TokenUsage.vue') },
      { path: 'ai/milvus', component: () => import('../views/admin/ai/MilvusCollections.vue') },

      { path: 'ops/feedback', component: () => import('../views/admin/ops/UserFeedback.vue') },
      { path: 'ops/search-logs', component: () => import('../views/admin/ops/SearchLogs.vue') },

      { path: 'monitor/rules', component: () => import('../views/admin/monitor/AlertRules.vue') },
      { path: 'monitor/history', component: () => import('../views/admin/monitor/AlertHistory.vue') },

      { path: 'sys/configs', component: () => import('../views/admin/sys/SysConfigs.vue') },
      { path: 'sys/dicts', component: () => import('../views/admin/sys/SysDicts.vue') }
    ]
  },
  {
    path: '/profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/')
  } else {
    next()
  }
})

export default router