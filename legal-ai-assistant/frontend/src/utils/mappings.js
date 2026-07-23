export const CASE_RESULT_TYPES = {
  1: 'success',
  2: 'warning',
  3: 'danger',
  4: 'info',
  5: 'success'
}

export const CASE_RESULT_NAMES = {
  1: '全部支持',
  2: '部分支持',
  3: '驳回',
  4: '撤诉',
  5: '调解'
}

export const COURT_LEVEL_NAMES = {
  1: '最高人民法院',
  2: '高级人民法院',
  3: '中级人民法院',
  4: '基层人民法院'
}

export const CASE_TYPES = {
  1: '民事',
  2: '刑事',
  3: '行政'
}

export const TRIAL_PROCEDURES = {
  1: '一审',
  2: '二审',
  3: '再审'
}

export const STATUS_NAMES = {
  1: '现行有效',
  2: '已废止',
  3: '修订中',
  4: '尚未生效',
  5: '部分失效'
}

export const STATUS_TYPES = {
  1: 'success',
  2: 'danger',
  3: 'warning',
  4: 'info',
  5: 'warning'
}

export const getResultType = (result) => CASE_RESULT_TYPES[result] || 'info'

export const getResultName = (result) => CASE_RESULT_NAMES[result] || '未知'

export const getCourtLevel = (level) => COURT_LEVEL_NAMES[level] || '未知'

export const getCaseType = (type) => CASE_TYPES[type] || '未知'

export const getTrialProcedure = (procedure) => TRIAL_PROCEDURES[procedure] || '未知'

export const getStatusName = (status) => STATUS_NAMES[status] || '未知'

export const getStatusType = (status) => STATUS_TYPES[status] || 'info'
