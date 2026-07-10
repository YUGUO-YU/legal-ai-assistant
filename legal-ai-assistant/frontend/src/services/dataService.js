import { cacheService, cachedRequest, clearCacheByPattern } from './cacheService'
import api from '@/api'

const request = api

export const userService = {
  async getCurrentUser(forceRefresh = false) {
    return cachedRequest(
      'user:current',
      () => request.auth.getUserInfo(),
      { ttl: 5 * 60 * 1000, forceRefresh }
    )
  },
  
  async getUserList(params, forceRefresh = false) {
    const key = `users:list:${JSON.stringify(params)}`
    return cachedRequest(key, () => request.get('/users', { params }), { forceRefresh })
  },
  
  clearUserCache() {
    cacheService.delete('user:current')
    clearCacheByPattern(/^users:list:/)
  }
}

export const lawService = {
  async getLaws(params, forceRefresh = false) {
    const key = `laws:${JSON.stringify(params)}`
    return cachedRequest(key, () => request.lawSearch.search(params), { ttl: 10 * 60 * 1000, forceRefresh })
  },
  
  async getLawDetail(id, forceRefresh = false) {
    return cachedRequest(
      `law:${id}`,
      () => request.lawSearch.getLawDetail(id),
      { ttl: 30 * 60 * 1000, forceRefresh }
    )
  },
  
  async getCategories(forceRefresh = false) {
    return cachedRequest(
      'law:categories',
      () => request.lawSearch.getCategories(),
      { ttl: 60 * 60 * 1000, forceRefresh }
    )
  }
}

export const dictService = {
  async getDict(type, forceRefresh = false) {
    return cachedRequest(
      `dict:${type}`,
      () => request.get(`/dicts/${type}`),
      { ttl: 60 * 60 * 1000, forceRefresh }
    )
  }
}

export const caseService = {
  async searchCases(params, forceRefresh = false) {
    const key = `cases:${JSON.stringify(params)}`
    return cachedRequest(key, () => request.caseSearch.search(params), { ttl: 5 * 60 * 1000, forceRefresh })
  },
  
  async getCaseDetail(uuid, forceRefresh = false) {
    return cachedRequest(
      `case:${uuid}`,
      () => request.caseSearch.getCaseDetail(uuid),
      { ttl: 30 * 60 * 1000, forceRefresh }
    )
  }
}

export const knowledgeBaseService = {
  async getList(params, forceRefresh = false) {
    const key = `kb:list:${JSON.stringify(params)}`
    return cachedRequest(key, () => request.knowledgeBase.list(params), { ttl: 5 * 60 * 1000, forceRefresh })
  },
  
  async getDetail(id, forceRefresh = false) {
    return cachedRequest(
      `kb:detail:${id}`,
      () => request.knowledgeBase.detail(id),
      { ttl: 30 * 60 * 1000, forceRefresh }
    )
  }
}

export async function initializeApp() {
  const tasks = []
  
  tasks.push(
    userService.getCurrentUser().catch(() => null)
  )
  
  tasks.push(
    Promise.all([
      lawService.getCategories(),
      dictService.getDict('law_category'),
      dictService.getDict('case_type')
    ]).catch(() => ({}))
  )
  
  await Promise.allSettled(tasks)
  
  cacheService.clear()
}
