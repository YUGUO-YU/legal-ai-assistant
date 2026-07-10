import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useFormValidation } from '@/composables/useFormValidation'

describe('useFormValidation', () => {
  describe('validate', () => {
    it('should return true for field without rules', () => {
      const { validate } = useFormValidation({})
      expect(validate('name', 'test')).toBe(true)
    })

    it('should validate required fields', () => {
      const { validate } = useFormValidation({
        name: { required: true, message: '姓名不能为空' }
      })
      expect(validate('name', '')).toBe(false)
      expect(validate('name', 'test')).toBe(true)
    })

    it('should validate pattern', () => {
      const { validate } = useFormValidation({
        email: { 
          pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
          message: '邮箱格式不正确'
        }
      })
      expect(validate('email', 'invalid')).toBe(false)
      expect(validate('email', 'test@example.com')).toBe(true)
    })

    it('should validate minLength', () => {
      const { validate } = useFormValidation({
        password: { minLength: 6, message: '密码至少6个字符' }
      })
      expect(validate('password', '123')).toBe(false)
      expect(validate('password', '123456')).toBe(true)
    })

    it('should validate maxLength', () => {
      const { validate } = useFormValidation({
        username: { maxLength: 10, message: '用户名最多10个字符' }
      })
      expect(validate('username', '12345678901')).toBe(false)
      expect(validate('username', '1234567890')).toBe(true)
    })

    it('should validate with custom validator', () => {
      const { validate } = useFormValidation({
        age: { 
          validator: (value) => {
            if (value < 18) return '年龄必须大于18'
            return true
          }
        }
      })
      expect(validate('age', 16)).toBe(false)
      expect(validate('age', 20)).toBe(true)
    })

    it('should validate array of rules', () => {
      const { validate } = useFormValidation({
        phone: [
          { required: true, message: '手机号不能为空' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }
        ]
      })
      expect(validate('phone', '')).toBe(false)
      expect(validate('phone', '123')).toBe(false)
      expect(validate('phone', '13812345678')).toBe(true)
    })
  })

  describe('validateAll', () => {
    it('should validate all fields', () => {
      const { validateAll } = useFormValidation({
        name: { required: true },
        email: { required: true, pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/ }
      })
      
      expect(validateAll({ name: '', email: 'test@example.com' })).toBe(false)
      expect(validateAll({ name: 'test', email: 'test@example.com' })).toBe(true)
    })
  })

  describe('clearErrors', () => {
    it('should clear specific field error', () => {
      const { validate, clearErrors, errors } = useFormValidation({
        name: { required: true }
      })
      validate('name', '')
      expect(errors.name).not.toBeNull()
      clearErrors('name')
      expect(errors.name).toBeNull()
    })

    it('should clear all errors when no field specified', () => {
      const { validateAll, clearErrors, errors } = useFormValidation({
        name: { required: true },
        email: { required: true }
      })
      validateAll({ name: '', email: '' })
      clearErrors()
      expect(errors.name).toBeNull()
      expect(errors.email).toBeNull()
    })
  })

  describe('touch and hasError', () => {
    it('should track touched state', () => {
      const { validate, touch, hasError } = useFormValidation({
        name: { required: true }
      })
      
      expect(hasError('name')).toBe(false)
      validate('name', '')
      expect(hasError('name')).toBe(false)
      touch('name')
      expect(hasError('name')).toBe(true)
    })
  })

  describe('getError', () => {
    it('should return error only after touch', () => {
      const { validate, getError } = useFormValidation({
        name: { required: true, message: '姓名不能为空' }
      })
      
      validate('name', '')
      expect(getError('name')).toBeNull()
      
      const { touch } = useFormValidation({ name: { required: true } })
      touch('name')
      expect(getError('name')).not.toBeNull()
    })
  })
})
