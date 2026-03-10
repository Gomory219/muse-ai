/**
 * API 相关类型定义
 * 扩展自动生成的 API 类型
 */

declare namespace API {
  /**
   * 分页请求基类
   */
  export interface PageRequest {
    pageNum?: number
    pageSize?: number
  }

  /**
   * 分页响应
   */
  export interface PageResponse<T> {
    list: T[]
    total: number
    pageNum: number
    pageSize: number
    totalPages: number
  }

  /**
   * 基础响应
   */
  export interface BaseResponse<T = any> {
    code: number
    data: T
    message: string
  }
}
