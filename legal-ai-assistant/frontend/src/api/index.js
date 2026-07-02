import axios from 'axios'

const DEFAULT_RETRY_COUNT = 2
const DEFAULT_RETRY_DELAY = 1000

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 60000
})

api.interceptors.request.use(config => {
  const isAdminCall = config.url && (config.url.includes('/admin/') || config.url.includes('/auth/admin/'))
  const token = isAdminCall
    ? localStorage.getItem('admin_token')
    : localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

api.interceptors.response.use(
  response => {
    const res = response.data
    if (response.config?.responseType === 'blob' || response.config?.responseType === 'arraybuffer') {
      return response
    }
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code !== 200) {
        console.error('API Error:', res.message)
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res
    }
    return res
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 400:
          console.error('参数错误:', error.response.data?.message)
          break
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('admin_token')
          localStorage.removeItem('admin_user')
          const reqUrl = error.config?.url || ''
          const isAdminReq = reqUrl.includes('/admin/') || reqUrl.includes('/auth/admin/')
          window.location.href = isAdminReq ? '/admin/login' : '/'
          break
        case 403:
          console.error('权限不足')
          break
        case 404:
          console.error('资源不存在')
          break
        case 500:
          console.error('服务器内部错误')
          break
        case 502:
        case 503:
        case 504:
          console.error('服务暂时不可用，请稍后重试')
          break
        default:
          console.error('请求失败:', error.message)
      }
    } else if (error.code === 'ECONNABORTED') {
      console.error('请求超时，请检查网络连接')
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

const withRetry = async (fn, retries = DEFAULT_RETRY_COUNT, delay = DEFAULT_RETRY_DELAY) => {
  let lastError
  for (let i = 0; i <= retries; i++) {
    try {
      return await fn()
    } catch (error) {
      lastError = error
      if (i < retries && shouldRetry(error)) {
        await sleep(delay * (i + 1))
      }
    }
  }
  throw lastError
}

const shouldRetry = (error) => {
  if (error.response) {
    const status = error.response.status
    return status === 502 || status === 503 || status === 504 || status === 429
  }
  if (error.code === 'ECONNABORTED' || error.code === 'ERR_NETWORK') {
    return true
  }
  return false
}

export const handleApiError = (error) => {
  if (error.response?.data?.message) {
    return error.response.data.message
  }
  if (error.message) {
    return error.message
  }
  if (error.code === 'ECONNABORTED') {
    return '请求超时，请稍后重试'
  }
  return '请求失败，请稍后重试'
}

export const requestWithRetry = (method, url, data, options = {}) => {
  const { retries = DEFAULT_RETRY_COUNT, delay = DEFAULT_RETRY_DELAY } = options
  return withRetry(() => api({ method, url, data }), retries, delay)
}

export default {
  legalSearch: {
    search: (data) => withRetry(() => api.post('/legal-search/search', data)),
    getArticle: (id) => withRetry(() => api.get(`/legal-search/articles/${id}`)),
    feedback: (data) => withRetry(() => api.post('/legal-search/feedback', data)),
    getSuggestedQueries: (query) => withRetry(() => api.get('/legal-search/suggested-queries', { params: { query } }))
  },
  caseSimilar: {
    search: (data) => withRetry(() => api.post('/case-similar/search', data))
  },
  caseSearch: {
    search: (data) => withRetry(() => api.post('/case-search/search', data)),
    getCaseDetail: (uuid) => withRetry(() => api.get(`/case-search/cases/${uuid}`)),
    analyzeCase: (uuid) => withRetry(() => api.get(`/case-search/cases/${uuid}/analysis`))
  },
  lawSearch: {
    search: (data) => withRetry(() => api.post('/law-search/search', data)),
    getCategories: () => withRetry(() => api.get('/law-search/categories')),
    getLawDetail: (uuid) => withRetry(() => api.get(`/law-search/laws/${uuid}`)),
    getLawArticles: (uuid) => withRetry(() => api.get(`/law-search/laws/${uuid}/articles`))
  },
  lawFavorite: {
    add: (lawUuid, lawTitle) => api.post('/law-favorite/add', { lawUuid, lawTitle }),
    remove: (lawUuid) => api.delete(`/law-favorite/remove/${lawUuid}`),
    list: () => api.get('/law-favorite/list'),
    check: (lawUuid) => api.get(`/law-favorite/check/${lawUuid}`)
  },
  lawAnalysis: {
    analyze: (lawUuid, lawTitle, articles) => api.post('/law-analysis/analyze', { lawUuid, lawTitle, articles })
  },
  legalResearch: {
    createTask: (data) => withRetry(() => api.post('/legal-research/tasks', data)),
    getReport: (taskId) => withRetry(() => api.get(`/legal-research/tasks/${taskId}/report`)),
    generateReport: (data) => withRetry(() => api.post('/legal-research/generate', data))
  },
  document: {
    draft: (data) => withRetry(() => api.post('/document/draft', data)),
    getTemplates: () => withRetry(() => api.get('/document/templates')),
    getTemplate: (code) => withRetry(() => api.get(`/document/templates/${code}`)),
    extractInfo: (text, templateCode) => withRetry(() => api.post('/document/extract-info', { text, templateCode }))
  },
  company: {
    query: (data) => withRetry(() => api.post('/company/query', data)),
    getRiskLevels: () => withRetry(() => api.get('/company/risk-levels')),
    getQuery: (uuid) => withRetry(() => api.get(`/company/queries/${uuid}`)),
    listQueries: (limit = 20) => withRetry(() => api.get('/company/queries', { params: { limit } }))
  },
  contract: {
    review: (data) => withRetry(() => api.post('/contract/review', data)),
    getDimensions: () => withRetry(() => api.get('/contract/dimensions')),
    getReview: (uuid) => withRetry(() => api.get(`/contract/reviews/${uuid}`)),
    listReviews: (limit = 20) => withRetry(() => api.get('/contract/reviews', { params: { limit } }))
  },
  docQa: {
    ask: (data) => withRetry(() => api.post('/doc-qa/ask', data)),
    getSessionHistory: (sessionId) => withRetry(() => api.get(`/doc-qa/sessions/${sessionId}/history`)),
    clearSession: (sessionId) => withRetry(() => api.delete(`/doc-qa/sessions/${sessionId}`)),
    getSessionList: () => withRetry(() => api.get('/doc-qa/sessions')),
    createSession: (data) => withRetry(() => api.post('/doc-qa/sessions', data))
  },
  knowledgeBase: {
    list: (params) => withRetry(() => api.get('/knowledge-base/list', { params })),
    create: (data) => withRetry(() => api.post('/knowledge-base/create', data)),
    delete: (id) => withRetry(() => api.delete(`/knowledge-base/${id}`)),
    upload: (data) => withRetry(() => api.post('/knowledge-base/upload', data)),
    detail: (id) => withRetry(() => api.get(`/knowledge-base/${id}`)),
    chunks: (id) => withRetry(() => api.get(`/knowledge-base/${id}/chunks`))
  },
  ppt: {
    generate: (data) => withRetry(() => api.post('/ppt/generate', data)),
    getById: (id) => withRetry(() => api.get(`/ppt/${id}`)),
    getByUuid: (uuid) => withRetry(() => api.get(`/ppt/uuid/${uuid}`)),
    update: (id, data) => withRetry(() => api.put(`/ppt/${id}`, data)),
    delete: (id) => withRetry(() => api.delete(`/ppt/${id}`)),
    list: (userId) => withRetry(() => api.get('/ppt/list', { params: { userId } })),
    getTemplates: () => withRetry(() => api.get('/ppt/templates')),
    recommendTemplates: (scenario) => withRetry(() => api.post('/ppt/templates/recommend', { scenario })),
    enhanceSlide: (data) => withRetry(() => api.post('/ppt/ai-enhance-slide', data))
  },
  auth: {
    login: (data) => api.post('/auth/login', data),
    adminLogin: (data) => api.post('/auth/admin/login', data),
    logout: () => api.post('/auth/logout'),
    register: (data) => api.post('/auth/register', data),
    sendVerifyCode: (username) => api.post('/auth/forgot-password', { username }),
    resetPassword: (data) => api.post('/auth/reset-password', data),
    getUserInfo: () => withRetry(() => api.get('/auth/user-info')),
    changePassword: (data) => api.put('/auth/password', data),
    updateProfile: (data) => api.put('/auth/profile', data)
  },
  health: () => api.get('/health'),
  dataImport: {
    importCivilLaw: (data) => withRetry(() => api.post('/admin/data/import-civil-law', data)),
    importLaborLaw: (data) => withRetry(() => api.post('/admin/data/import-labor-law', data)),
    importConstructionLaw: (data) => withRetry(() => api.post('/admin/data/import-construction-law', data)),
    vectorize: (data) => withRetry(() => api.post('/admin/data/vectorize', data)),
    importAll: (data) => withRetry(() => api.post('/admin/data/import-all', data))
  },
  lawImport: {
    webSearch: (data) => withRetry(() => api.post('/admin/law-import/web-search', data)),
    upload: (data) => withRetry(() => api.post('/admin/law-import/upload', data)),
    uploadFile: (formData) => withRetry(() => api.post('/admin/law-import/upload-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })),
    preset: (presetKey, operator) => withRetry(() => api.post(`/admin/law-import/preset/${presetKey}`, null, { params: { operator } })),
    presets: () => withRetry(() => api.get('/admin/law-import/presets')),
    history: (page = 1, pageSize = 20) => withRetry(() => api.get('/admin/law-import/history', { params: { page, pageSize } })),
    historyById: (id) => withRetry(() => api.get(`/admin/law-import/history/${id}`)),
    stats: () => withRetry(() => api.get('/admin/law-import/stats'))
  }
}