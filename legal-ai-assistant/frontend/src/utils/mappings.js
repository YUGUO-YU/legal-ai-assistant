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

export const getResultType = (result) => CASE_RESULT_TYPES[result] || 'info'

export const getResultName = (result) => CASE_RESULT_NAMES[result] || '未知'

export const getCourtLevel = (level) => COURT_LEVEL_NAMES[level] || '未知'
