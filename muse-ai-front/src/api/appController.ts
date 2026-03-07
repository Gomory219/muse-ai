// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /app/admin/delete */
export async function deleteAppByAdmin(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/app/admin/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/admin/get */
export async function getAppDetailByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppDetailByAdminParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppVO>('/app/admin/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/admin/list */
export async function listAppsByAdmin(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listAppsByAdminParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultAppVO>('/app/admin/list', {
    method: 'GET',
    params: {
      ...params,
      appQueryRequest: undefined,
      ...params['appQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/admin/pin */
export async function pinApp(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.pinAppParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/app/admin/pin', {
    method: 'POST',
    params: {
      ...params,
      appPinRequest: undefined,
      ...params['appPinRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/admin/update */
export async function updateAppByAdmin(
  body: API.AppUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/app/admin/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/chat */
export async function chatToGenApp(body: API.AppChatRequest, options?: { [key: string]: any }) {
  return request<string[]>('/app/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/create */
export async function createApp(body: API.AppAddRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseLong>('/app/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/delete */
export async function deleteApp(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteAppParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/app/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/deploy */
export async function deployApp(body: API.AppDeployRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseString>('/app/deploy', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/download */
export async function downloadApp(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.downloadAppParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/app/download', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/get */
export async function getAppDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getAppDetailParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseAppVO>('/app/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/list/featured */
export async function listFeaturedApps(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listFeaturedAppsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultAppVO>('/app/list/featured', {
    method: 'GET',
    params: {
      ...params,
      appQueryRequest: undefined,
      ...params['appQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /app/list/my */
export async function listMyApps(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listMyAppsParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultAppVO>('/app/list/my', {
    method: 'GET',
    params: {
      ...params,
      appQueryRequest: undefined,
      ...params['appQueryRequest'],
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /app/update/name */
export async function updateAppName(
  body: API.AppNameUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/app/update/name', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
