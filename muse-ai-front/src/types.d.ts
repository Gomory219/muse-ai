/**
 * 类型覆盖文件
 * 将 API 类型中的 ID 字段覆盖为字符串类型，防止大整数精度丢失
 * 此文件不会被自动生成的 API 代码覆盖
 */

declare namespace API {
  // 覆盖 AppVO 类型
  type AppVO = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    user?: UserVO
    createTime?: string
    updateTime?: string
  }

  // 覆盖 UserVO 类型
  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
    createTime?: string
  }

  // 覆盖 LoginUserVO 类型
  type LoginUserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
    createTime?: string
    updateTime?: string
  }

  // 覆盖 User 类型
  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  // 覆盖 History 类型
  type History = {
    id?: string
    message?: string
    messageType?: 'USER' | 'AI'
    appId?: string
    userId?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  // 覆盖请求类型中的 ID 字段
  type AppChatRequest = {
    userMessage?: string
    appId?: string
  }

  type AppNameUpdateRequest = {
    id?: string
    appName?: string
  }

  type AppPinRequest = {
    appId?: string
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    appName?: string
    userId?: string
    codeGenType?: 'HTML' | 'MULTI_FILE'
    minPriority?: number
    maxPriority?: number
  }

  type AppUpdateRequest = {
    id?: string
    appName?: string
    cover?: string
    priority?: number
  }

  type DeleteRequest = {
    id?: string
  }

  type HistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    appId?: string
    lastId?: number
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
  }

  type UserUpdateRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
  }

  // 覆盖参数类型中的 ID 字段
  type deleteAppParams = {
    id: string
  }

  type downloadAppParams = {
    id: string
  }

  type getAppDetailByAdminParams = {
    id: string
  }

  type getAppDetailParams = {
    id: string
  }

  type getParams = {
    id: string
  }

  type getVOParams = {
    id: string
  }
}
