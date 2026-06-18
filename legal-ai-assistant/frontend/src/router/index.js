import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/',
    component: () => import('../views/LegalSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/legal-search',
    redirect: '/'
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
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router