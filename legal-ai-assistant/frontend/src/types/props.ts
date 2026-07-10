export interface TableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  align?: 'left' | 'center' | 'right'
  fixed?: 'left' | 'right' | true
  sortable?: boolean
  formatter?: (row: any, column: any, cellValue: any) => string
  slots?: {
    default?: string
    header?: string
  }
}

export interface FilterConfig {
  prop: string
  label: string
  type: 'input' | 'select' | 'date' | 'daterange' | 'cascader' | 'radio'
  options?: { label: string; value: any }[]
  placeholder?: string
  clearable?: boolean
  defaultValue?: any
}

export interface FormConfig {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'select' | 'radio' | 'checkbox' | 'switch' | 'date' | 'upload'
  rules?: any[]
  options?: { label: string; value: any }[]
  placeholder?: string
  clearable?: boolean
  disabled?: boolean
  props?: Record<string, any>
}
