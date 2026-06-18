import axios from 'axios'

const DEFAULT_RETRY_COUNT = 2
const DEFAULT_RETRY_DELAY = 1000

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 60000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
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
    if (res.code !== 200) {
      console.error('API Error:', res.message)
      return Promise.reject(new Error(res.message || '请求失败'))
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
          window.location.href = '/login'
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
    getCaseDetail: (uuid) => withRetry(() => api.get(`/case-search/cases/${uuid}`))
  },
  lawSearch: {
    search: (data) => withRetry(() => api.post('/law-search/search', data)),
    getCategories: () => withRetry(() => api.get('/law-search/categories')),
    getLawDetail: (uuid) => withRetry(() => api.get(`/law-search/laws/${uuid}`)),
    getLawArticles: (uuid) => withRetry(() => api.get(`/law-search/laws/${uuid}/articles`))
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
    getByUuid: (id, uuid) => withRetry(() => api.get(`/ppt/${id}/uuid/${uuid}`)),
    update: (id, data) => withRetry(() => api.put(`/ppt/${id}`, data)),
    delete: (id) => withRetry(() => api.delete(`/ppt/${id}`)),
    list: (userId) => withRetry(() => api.get('/ppt/list', { params: { userId } })),
    getTemplates: () => withRetry(() => api.get('/ppt/templates')),
    recommendTemplates: (scenario) => withRetry(() => api.post('/ppt/templates/recommend', { scenario })),
    enhanceSlide: (data) => withRetry(() => api.post('/ppt/ai-enhance-slide', data))
  },
  auth: {
    login: (data) => api.post('/auth/login', data),
    logout: () => api.post('/auth/logout'),
    getUserInfo: () => withRetry(() => api.get('/auth/user-info'))
  },
  health: () => api.get('/health'),
  dataImport: {
    importCivilLaw: (data) => withRetry(() => api.post('/admin/data/import-civil-law', data)),
    importLaborLaw: (data) => withRetry(() => api.post('/admin/data/import-labor-law', data)),
    importConstructionLaw: (data) => withRetry(() => api.post('/admin/data/import-construction-law', data)),
    vectorize: (data) => withRetry(() => api.post('/admin/data/vectorize', data)),
    importAll: (data) => withRetry(() => api.post('/admin/data/import-all', data))
  }
}