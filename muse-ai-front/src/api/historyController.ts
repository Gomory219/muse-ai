// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /history */
export async function getHistory(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getHistoryParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageResultHistory>('/history', {
    method: 'GET',
    params: {
      ...params,
      historyQueryRequest: undefined,
      ...params['historyQueryRequest'],
    },
    ...(options || {}),
  })
}
