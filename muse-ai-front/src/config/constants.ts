/**
 * 应用常量定义
 */

/**
 * 分页每页条数选项
 */
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100] as const

/**
 * 角色选项
 */
export const ROLE_OPTIONS = [
  { label: '全部角色', value: '' },
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
] as const

/**
 * 代码生成类型选项
 */
export const CODE_GEN_TYPE_OPTIONS = [
  { label: '全部类型', value: '' },
  { label: '单文件', value: 'single' },
  { label: '多文件', value: 'multi' },
] as const

/**
 * 用户角色类型
 */
export type UserRole = 'user' | 'admin'

/**
 * 代码生成类型
 */
export type CodeGenType = '' | 'single' | 'multi' | 'HTML' | 'MULTI_FILE'
