import { ref, reactive, computed } from 'vue'

export function useFormValidation(rules) {
  const errors = reactive({})
  const touched = reactive({})
  const validating = reactive({})

  const validate = (field, value) => {
    const rule = rules[field]
    if (!rule) return true

    const ruleList = Array.isArray(rule) ? rule : [rule]

    for (const r of ruleList) {
      if (r.required && !value) {
        errors[field] = r.message || `${field}不能为空`
        return false
      }

      if (r.validator && typeof r.validator === 'function') {
        const result = r.validator(value)
        if (result !== true) {
          errors[field] = result
          return false
        }
      }

      if (r.pattern && !r.pattern.test(value)) {
        errors[field] = r.message || `${field}格式不正确`
        return false
      }

      if (r.minLength && value.length < r.minLength) {
        errors[field] = r.message || `${field}至少${r.minLength}个字符`
        return false
      }

      if (r.maxLength && value.length > r.maxLength) {
        errors[field] = r.message || `${field}最多${r.maxLength}个字符`
        return false
      }
    }

    errors[field] = null
    return true
  }

  const validateAll = (formData) => {
    let isValid = true
    for (const field in rules) {
      if (!validate(field, formData[field])) {
        isValid = false
      }
    }
    return isValid
  }

  const clearErrors = (field) => {
    if (field) {
      errors[field] = null
    } else {
      for (const key in errors) {
        errors[key] = null
      }
    }
  }

  const touch = (field) => {
    touched[field] = true
  }

  const getError = (field) => {
    return touched[field] ? errors[field] : null
  }

  const hasError = (field) => {
    return touched[field] && !!errors[field]
  }

  return {
    errors,
    touched,
    validate,
    validateAll,
    clearErrors,
    touch,
    getError,
    hasError
  }
}
