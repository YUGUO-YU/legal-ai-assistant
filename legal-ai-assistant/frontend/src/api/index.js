import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000
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
        default:
          console.error('请求失败:', error.message)
      }
    } else if (error.request) {
      console.error('网络错误，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export const handleApiError = (error) => {
  if (error.response?.data?.message) {
    return error.response.data.message
  }
  if (error.message) {
    return error.message
  }
  return '请求失败，请稍后重试'
}

export default {
  legalSearch: {
    search: (data) => api.post('/legal-search/search', data),
    getArticle: (id) => api.get(`/legal-search/articles/${id}`),
    feedback: (data) => api.post('/legal-search/feedback', data),
    getSuggestedQueries: (query) => api.get('/legal-search/suggested-queries', { params: { query } })
  },
  caseSimilar: {
    search: (data) => api.post('/case-similar/search', data)
  },
  caseSearch: {
    search: (data) => api.post('/case-search/search', data),
    getCaseDetail: (uuid) => api.get(`/case-search/cases/${uuid}`)
  },
  lawSearch: {
    search: (data) => api.post('/law-search/search', data),
    getCategories: () => api.get('/law-search/categories'),
    getLawDetail: (uuid) => api.get(`/law-search/laws/${uuid}`)
  },
  legalResearch: {
    createTask: (data) => api.post('/legal-research/tasks', data),
    getReport: (taskId) => api.get(`/legal-research/tasks/${taskId}/report`),
    generateReport: (data) => api.post('/legal-research/generate', data)
  },
  document: {
    draft: (data) => api.post('/document/draft', data),
    getTemplates: () => api.get('/document/templates'),
    getTemplate: (code) => api.get(`/document/templates/${code}`)
  },
  company: {
    query: (data) => api.post('/company/query', data),
    getRiskLevels: () => api.get('/company/risk-levels')
  },
  contract: {
    review: (data) => api.post('/contract/review', data),
    getDimensions: () => api.get('/contract/dimensions')
  },
  docQa: {
    ask: (data) => api.post('/doc-qa/ask', data)
  },
  knowledgeBase: {
    list: (params) => api.get('/knowledge-base/list', { params }),
    create: (data) => api.post('/knowledge-base/create', data),
    delete: (id) => api.delete(`/knowledge-base/${id}`),
    upload: (data) => api.post('/knowledge-base/upload', data)
  },
  auth: {
    login: (data) => api.post('/auth/login', data),
    logout: () => api.post('/auth/logout'),
    getUserInfo: () => api.get('/auth/user-info')
  },
  health: () => api.get('/health'),
  dataImport: {
    importCivilLaw: () => api.post('/admin/data/import-civil-law'),
    importLaborLaw: () => api.post('/admin/data/import-labor-law'),
    importConstructionLaw: () => api.post('/admin/data/import-construction-law'),
    vectorize: () => api.post('/admin/data/vectorize'),
    importAll: () => api.post('/admin/data/import-all')
  }
}