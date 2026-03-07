declare namespace API {
  type AppAddRequest = {
    initPrompt?: string
  }

  type AppChatRequest = {
    userMessage?: string
    appId?: number
  }

  type AppDeployRequest = {
    appId?: string
  }

  type AppNameUpdateRequest = {
    id?: number
    appName?: string
  }

  type AppPinRequest = {
    appId?: number
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    appName?: string
    userId?: number
    codeGenType?: 'HTML' | 'MULTI_FILE'
    minPriority?: number
    maxPriority?: number
  }

  type AppUpdateRequest = {
    id?: number
    appName?: string
    cover?: string
    priority?: number
  }

  type AppVO = {
    id?: number
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: number
    user?: UserVO
    createTime?: string
    updateTime?: string
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePageResultAppVO = {
    code?: number
    data?: PageResultAppVO
    message?: string
  }

  type BaseResponsePageResultUserVO = {
    code?: number
    data?: PageResultUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type deleteAppParams = {
    id: number
  }

  type DeleteRequest = {
    id?: number
  }

  type downloadAppParams = {
    id: number
  }

  type getAppDetailByAdminParams = {
    id: number
  }

  type getAppDetailParams = {
    id: number
  }

  type getParams = {
    id: number
  }

  type getVOParams = {
    id: number
  }

  type listAppsByAdminParams = {
    appQueryRequest: AppQueryRequest
  }

  type listFeaturedAppsParams = {
    appQueryRequest: AppQueryRequest
  }

  type listMyAppsParams = {
    appQueryRequest: AppQueryRequest
  }

  type listParams = {
    userQueryRequest: UserQueryRequest
  }

  type LoginUserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
    createTime?: string
    updateTime?: string
  }

  type PageResultAppVO = {
    pageNum?: number
    pageSize?: number
    total?: number
    list?: AppVO[]
  }

  type PageResultUserVO = {
    pageNum?: number
    pageSize?: number
    total?: number
    list?: UserVO[]
  }

  type pinAppParams = {
    appPinRequest: AppPinRequest
  }

  type saveParams = {
    userAddRequest: UserAddRequest
  }

  type User = {
    id?: number
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

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: number
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    confirmPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
  }

  type UserVO = {
    id?: number
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: 'user' | 'admin'
    createTime?: string
  }
}
