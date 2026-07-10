export interface User {
  id: number
  username: string
  email: string
  phone?: string
  role: 'admin' | 'user' | 'guest'
  status: 'pending' | 'approved' | 'rejected' | 'disabled'
  department?: string
  avatar?: string
  createdAt: string
  updatedAt: string
  lastLoginAt?: string
}

export interface LoginHistory {
  id: number
  userId: number
  username: string
  ip: string
  location?: string
  device: string
  browser?: string
  os?: string
  loginAt: string
  loginStatus: 'success' | 'failed'
  failureReason?: string
}

export interface AuditLog {
  id: number
  userId: number
  username: string
  action: string
  module: string
  method: string
  url: string
  ip: string
  userAgent?: string
  requestParams?: Record<string, any>
  responseData?: any
  statusCode?: number
  errorMessage?: string
  duration?: number
  createdAt: string
}

export interface Law {
  id: number
  title: string
  category: string
  subcategory?: string
  lawNumber?: string
  effectiveDate?: string
  issuer?: string
  status: 'effective' | 'abolished' | 'amended'
  keywords?: string[]
  summary?: string
  content: string
  attachedFiles?: string[]
  viewCount: number
  searchCount: number
  collectCount: number
  createdAt: string
  updatedAt: string
}

export interface Case {
  id: number
  caseNumber: string
  title: string
  caseType: string
  court?: string
  judge?: string
  date?: string
  parties?: string[]
  lawyers?: string[]
  claims?: string
  defense?: string
  judgment?: string
  legalBasis?: string[]
  keywords?: string[]
  summary?: string
  content: string
  similarCases?: number[]
  viewCount: number
  searchCount: number
  collectCount: number
  createdAt: string
  updatedAt: string
}

export interface SearchLog {
  id: number
  userId: number
  username: string
  searchType: 'law' | 'case' | 'contract' | 'draft' | 'all'
  keyword: string
  filters?: Record<string, any>
  resultCount: number
  responseTime: number
  ip: string
  createdAt: string
}

export interface SearchFeedback {
  id: number
  searchLogId: number
  userId: number
  caseId?: number
  lawId?: number
  helpful: boolean
  comment?: string
  createdAt: string
}

export interface ContractReview {
  id: number
  userId: number
  contractType: string
  contractContent: string
  riskLevel: 'low' | 'medium' | 'high'
  riskScore?: number
  dimensions?: RiskDimension[]
  riskClauses?: RiskClause[]
  suggestions?: string[]
  status: 'pending' | 'completed' | 'failed'
  createdAt: string
  completedAt?: string
}

export interface RiskDimension {
  name: string
  score: number
  description?: string
}

export interface RiskClause {
  clause: string
  riskLevel: 'low' | 'medium' | 'high'
  description: string
  suggestion: string
  position?: { start: number; end: number }
}

export interface Alert {
  id: number
  type: 'system' | 'security' | 'performance' | 'business'
  level: 'info' | 'warning' | 'error' | 'critical'
  title: string
  message: string
  source?: string
  details?: Record<string, any>
  status: 'pending' | 'processing' | 'resolved' | 'ignored'
  handler?: number
  handledAt?: string
  handledComment?: string
  createdAt: string
}

export interface AlertRule {
  id: number
  name: string
  type: string
  condition: string
  threshold?: number
  enabled: boolean
  notifyChannels: string[]
  createdAt: string
  updatedAt: string
}

export interface Dict {
  id: number
  type: string
  label: string
  value: string
  sort?: number
  remark?: string
  status: 'normal' | 'disabled'
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface PageParams {
  page?: number
  pageSize?: number
  keyword?: string
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface DashboardStats {
  totalUsers: number
  todayLogins: number
  totalSearches: number
  todaySearches: number
  totalLaws: number
  totalCases: number
  activeAlerts: number
  totalTokens: number
}

export interface Menu {
  id: number
  name: string
  path: string
  component?: string
  icon?: string
  parentId?: number
  orderNum?: number
  permission?: string
  type: 'menu' | 'button' | 'api'
  visible: boolean
  status: 'normal' | 'disabled'
  children?: Menu[]
}
