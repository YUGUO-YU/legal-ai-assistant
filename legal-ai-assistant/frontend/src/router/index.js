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
    path: '/case-search',
    component: () => import('../views/CaseSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/law-search',
    component: () => import('../views/LawSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contract-review',
    component: () => import('../views/ContractReview.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/knowledge-base',
    component: () => import('../views/KnowledgeBase.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/doc-qa',
    component: () => import('../views/DocQa.vue'),
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
  } else if (to.path === '/' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router