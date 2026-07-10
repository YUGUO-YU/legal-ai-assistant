import type { AxiosRequestConfig } from 'axios'
import type { ApiResponse } from './index'

declare module '@vue/runtime-core' {
  interface ComponentCustomProperties {
    $request: any
    $api: {
      get<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
      post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
      put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
      delete<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>>
    }
  }
}
