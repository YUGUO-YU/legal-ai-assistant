import axios from 'axios'
import { ElMessage } from 'element-plus'

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
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message || '请求失败'))
      }
      return res.data
    }
    return res
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 400:
          ElMessage.error('参数错误：' + (error.response.data?.message || ''))
          break
        case 401:
          localStorage.removeItem('token')
          localStorage.removeItem('admin_token')
          localStorage.removeItem('admin_user')
          const reqUrl = error.config?.url || ''
          const isAdminReq = reqUrl.includes('/admin/') || reqUrl.includes('/auth/admin/')
          window.location.hash = isAdminReq ? '#/admin/login' : '#/'
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('资源不存在或接口未定义')
          break
        case 429:
          ElMessage.warning('请求过于频繁，请稍后重试')
          break
        case 500:
          ElMessage.error('服务器内部错误：' + (error.response.data?.message || ''))
          break
        case 502:
        case 503:
        case 504:
          ElMessage.error('服务暂时不可用，请稍后重试')
          break
        default:
          ElMessage.error('请求失败：' + (error.response.data?.message || error.message))
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('无法连接到服务器，请确认后端服务已启动')
    } else if (error.request) {
      ElMessage.error('网络错误，请检查网络连接和后端服务状态')
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
        await sleep(delay * Math.pow(2, i))
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

const apiClient = api

export default {
  get: (url, config) => apiClient.get(url, config),
  post: (url, data, config) => apiClient.post(url, data, config),
  put: (url, data, config) => apiClient.put(url, data, config),
  delete: (url, config) => apiClient.delete(url, config),
  v2: {
    laws: (params) => withRetry(() => apiClient.get('/v2/laws', { params })),
    law: (id, params) => withRetry(() => apiClient.get(`/v2/laws/${id}`, { params })),
    lawArticles: (id, params) => withRetry(() => apiClient.get(`/v2/laws/${id}/articles`, { params })),
    categories: (type) => withRetry(() => apiClient.get('/v2/laws/categories', { params: { type } })),
    stats: () => withRetry(() => apiClient.get('/v2/laws/stats'))
  },
  legalSearch: {
    search: (data) => withRetry(() => apiClient.post('/legal-search/search', data)),
    getArticle: (id) => withRetry(() => apiClient.get(`/legal-search/articles/${id}`)),
    feedback: (data) => withRetry(() => apiClient.post('/legal-search/feedback', data)),
    getSuggestedQueries: (query) => withRetry(() => apiClient.get('/legal-search/suggested-queries', { params: { query } }))
  },
  caseSimilar: {
    search: (data) => withRetry(() => apiClient.post('/case-similar/search', data))
  },
  caseSearch: {
    search: (data) => withRetry(() => apiClient.post('/case-search/search', data)),
    getCaseDetail: (uuid) => withRetry(() => apiClient.get(`/case-search/cases/${uuid}`)),
    analyzeCase: (uuid) => withRetry(() => apiClient.get(`/case-search/cases/${uuid}/analysis`))
  },
  lawSearch: {
    search: (data) => withRetry(() => apiClient.post('/law-search/search', data)),
    getCategories: () => withRetry(() => apiClient.get('/law-search/categories')),
    getLawDetail: (uuid) => withRetry(() => apiClient.get(`/law-search/laws/${uuid}`)),
    getLawArticles: (uuid) => withRetry(() => apiClient.get(`/law-search/laws/${uuid}/articles`))
  },
  lawFavorite: {
    add: (lawUuid, lawTitle) => withRetry(() => apiClient.post('/law-favorite/add', { lawUuid, lawTitle })),
    remove: (lawUuid) => withRetry(() => apiClient.delete(`/law-favorite/remove/${lawUuid}`)),
    list: () => withRetry(() => apiClient.get('/law-favorite/list')),
    check: (lawUuid) => withRetry(() => apiClient.get(`/law-favorite/check/${lawUuid}`))
  },
  lawAnalysis: {
    analyze: (lawUuid, lawTitle, articles) => withRetry(() => apiClient.post('/law-analysis/analyze', { lawUuid, lawTitle, articles }))
  },
  legalResearch: {
    createTask: (data) => withRetry(() => apiClient.post('/legal-research/tasks', data)),
    getReport: (taskId) => withRetry(() => apiClient.get(`/legal-research/tasks/${taskId}/report`)),
    generateReport: (data) => withRetry(() => apiClient.post('/legal-research/generate', data))
  },
  document: {
    draft: (data) => withRetry(() => apiClient.post('/document/draft', data)),
    getTemplates: () => withRetry(() => apiClient.get('/document/templates')),
    getTemplate: (code) => withRetry(() => apiClient.get(`/document/templates/${code}`)),
    extractInfo: (text, templateCode) => withRetry(() => apiClient.post('/document/extract-info', { text, templateCode }))
  },
  company: {
    query: (data) => withRetry(() => apiClient.post('/company/query', data)),
    getRiskLevels: () => withRetry(() => apiClient.get('/company/risk-levels')),
    getQuery: (uuid) => withRetry(() => apiClient.get(`/company/queries/${uuid}`)),
    listQueries: (limit = 20) => withRetry(() => apiClient.get('/company/queries', { params: { limit } }))
  },
  contract: {
    review: (data) => withRetry(() => apiClient.post('/contract/review', data)),
    getDimensions: () => withRetry(() => apiClient.get('/contract/dimensions')),
    getReview: (uuid) => withRetry(() => apiClient.get(`/contract/reviews/${uuid}`)),
    listReviews: (limit = 20) => withRetry(() => apiClient.get('/contract/reviews', { params: { limit } }))
  },
  docQa: {
    ask: (data) => withRetry(() => apiClient.post('/doc-qa/ask', data)),
    getSessionHistory: (sessionId) => withRetry(() => apiClient.get(`/doc-qa/sessions/${sessionId}/history`)),
    clearSession: (sessionId) => withRetry(() => apiClient.delete(`/doc-qa/sessions/${sessionId}`)),
    getSessionList: () => withRetry(() => apiClient.get('/doc-qa/sessions')),
    createSession: (data) => withRetry(() => apiClient.post('/doc-qa/sessions', data))
  },
  knowledgeBase: {
    list: (params) => withRetry(() => apiClient.get('/knowledge-base/list', { params })),
    create: (data) => withRetry(() => apiClient.post('/knowledge-base/create', data)),
    delete: (id) => withRetry(() => apiClient.delete(`/knowledge-base/${id}`)),
    upload: (data) => withRetry(() => apiClient.post('/knowledge-base/upload', data)),
    detail: (id) => withRetry(() => apiClient.get(`/knowledge-base/${id}`)),
    chunks: (id) => withRetry(() => apiClient.get(`/knowledge-base/${id}/chunks`))
  },
  ppt: {
    generate: (data) => withRetry(() => apiClient.post('/ppt/generate', data)),
    getById: (id) => withRetry(() => apiClient.get(`/ppt/${id}`)),
    getByUuid: (uuid) => withRetry(() => apiClient.get(`/ppt/uuid/${uuid}`)),
    update: (id, data) => withRetry(() => apiClient.put(`/ppt/${id}`, data)),
    delete: (id) => withRetry(() => apiClient.delete(`/ppt/${id}`)),
    list: (userId) => withRetry(() => apiClient.get('/ppt/list', { params: { userId } })),
    getTemplates: () => withRetry(() => apiClient.get('/ppt/templates')),
    recommendTemplates: (scenario) => withRetry(() => apiClient.post('/ppt/templates/recommend', { scenario })),
    enhanceSlide: (data) => withRetry(() => apiClient.post('/ppt/ai-enhance-slide', data))
  },
  auth: {
    login: (data) => withRetry(() => apiClient.post('/auth/login', data)),
    adminLogin: (data) => withRetry(() => apiClient.post('/auth/admin/login', data)),
    logout: () => withRetry(() => apiClient.post('/auth/logout')),
    register: (data) => withRetry(() => apiClient.post('/auth/register', data)),
    sendVerifyCode: (username) => withRetry(() => apiClient.post('/auth/forgot-password', { username })),
    resetPassword: (data) => withRetry(() => apiClient.post('/auth/reset-password', data)),
    getUserInfo: () => withRetry(() => apiClient.get('/auth/user-info')),
    changePassword: (data) => withRetry(() => apiClient.put('/auth/password', data)),
    updateProfile: (data) => withRetry(() => apiClient.put('/auth/profile', data))
  },
  usage: {
    addRecord: (data) => apiClient.post('/usage/records', data),
    getRecords: (userId, limit = 50) => apiClient.get('/usage/records', { params: { userId, limit } }),
    deleteRecord: (recordId, userId) => apiClient.delete('/usage/records/' + recordId, { params: { userId } }),
    clearAll: (userId) => apiClient.delete('/usage/records', { params: { userId } })
  },
  health: () => apiClient.get('/health'),
  dataImport: {
    importCivilLaw: (data) => withRetry(() => apiClient.post('/admin/data/import-civil-law', data)),
    importLaborLaw: (data) => withRetry(() => apiClient.post('/admin/data/import-labor-law', data)),
    importConstructionLaw: (data) => withRetry(() => apiClient.post('/admin/data/import-construction-law', data)),
    vectorize: (data) => withRetry(() => apiClient.post('/admin/data/vectorize', data)),
    importAll: (data) => withRetry(() => apiClient.post('/admin/data/import-all', data))
  },
  judgmentImport: {
    preview: (formData) => withRetry(() => apiClient.post('/admin/data-import/judgments/preview', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })),
    confirm: (data) => withRetry(() => apiClient.post('/admin/data-import/judgments/confirm', data))
  },
  lawImport: {
    webSearch: (data) => withRetry(() => apiClient.post('/admin/law-import/web-search', data)),
    upload: (data) => withRetry(() => apiClient.post('/admin/law-import/upload', data)),
    uploadFile: (formData) => withRetry(() => apiClient.post('/admin/law-import/upload-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })),
    preset: (presetKey, operator) => withRetry(() => apiClient.post(`/admin/law-import/preset/${presetKey}`, null, { params: { operator } })),
    presets: () => withRetry(() => apiClient.get('/admin/law-import/presets')),
    history: (page = 1, pageSize = 20) => withRetry(() => apiClient.get('/admin/law-import/history', { params: { page, pageSize } })),
    historyById: (id) => withRetry(() => apiClient.get(`/admin/law-import/history/${id}`)),
    stats: () => withRetry(() => apiClient.get('/admin/law-import/stats'))
  },
  categoryTypes: () => withRetry(() => apiClient.get('/admin/law/category-types')),
  getCategoryType: (id) => withRetry(() => apiClient.get(`/admin/law/category-types/${id}`)),
  createCategoryType: (data) => withRetry(() => apiClient.post('/admin/law/category-types', data)),
  updateCategoryType: (id, data) => withRetry(() => apiClient.put(`/admin/law/category-types/${id}`, data)),
  deleteCategoryType: (id) => withRetry(() => apiClient.delete(`/admin/law/category-types/${id}`)),
  categories: (typeId) => withRetry(() => apiClient.get('/admin/law/categories', { params: { typeId } })),
  createCategory: (data) => withRetry(() => apiClient.post('/admin/law/categories', data)),
  updateCategory: (id, data) => withRetry(() => apiClient.put(`/admin/law/categories/${id}`, data)),
  deleteCategory: (id) => withRetry(() => apiClient.delete(`/admin/law/categories/${id}`)),
  getDocumentCategories: (lawId) => withRetry(() => apiClient.get(`/admin/law/document-categories/${lawId}`)),
  setDocumentCategories: (lawId, categoryIds) => withRetry(() => apiClient.post(`/admin/law/document-categories/${lawId}`, { categoryIds })),
  importPreview: (formData) => withRetry(() => apiClient.post('/admin/law-import/preview', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })),
  importConfirm: (data) => withRetry(() => apiClient.post('/admin/law-import/confirm', data)),
  importDirect: (formData) => withRetry(() => apiClient.post('/admin/law-import/direct', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })),
  lawImportHistory: () => withRetry(() => apiClient.get('/admin/law-import/history')),
  lawDocument: {
    create: (data) => withRetry(() => apiClient.post('/admin/law-document/create', data)),
    update: (id, data) => withRetry(() => apiClient.post(`/admin/law-document/${id}/update`, data)),
    delete: (id) => withRetry(() => apiClient.post(`/admin/law-document/${id}/delete`)),
    batchDelete: (ids) => withRetry(() => apiClient.post('/admin/law-document/batch-delete', { ids })),
    addArticle: (lawId, data) => withRetry(() => apiClient.post(`/admin/law-document/${lawId}/articles/add`, data)),
    updateArticle: (lawId, articleId, data) => withRetry(() => apiClient.post(`/admin/law-document/${lawId}/articles/${articleId}/update`, data)),
    deleteArticle: (lawId, articleId) => withRetry(() => apiClient.post(`/admin/law-document/${lawId}/articles/${articleId}/delete`)),
    export: (params) => withRetry(() => apiClient.get('/admin/law-document/export', { params })),
    dataQuality: () => withRetry(() => apiClient.get('/admin/law-document/data-quality'))
  },
  stats: {
    userActivity: (params) => withRetry(() => apiClient.get('/admin/stats/user-activity', { params })),
    lawUsage: (params) => withRetry(() => apiClient.get('/admin/stats/law-usage', { params })),
    hourlyAccess: () => withRetry(() => apiClient.get('/admin/stats/hourly-access'))
  },
  mod10: {
    sessionDetail: (sessionId) => withRetry(() => apiClient.get(`/admin/biz/mod10/qa-sessions/${sessionId}`)),
    sessionMessages: (sessionId, params) => withRetry(() => apiClient.get(`/admin/biz/mod10/qa-sessions/${sessionId}/messages`, { params })),
    deleteSession: (sessionId) => withRetry(() => apiClient.delete(`/admin/biz/mod10/qa-sessions/${sessionId}`)),
    exportSession: (sessionId) => apiClient.post(`/admin/biz/mod10/qa-sessions/${sessionId}/export`, {}, { responseType: 'blob' })
  }
}