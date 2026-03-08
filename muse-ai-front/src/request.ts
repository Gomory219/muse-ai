import axios from 'axios'
import { message } from 'ant-design-vue'

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:7777/api'

// 需要转换为字符串的 ID 字段名
const ID_FIELDS = ['id', 'userId', 'appId', 'ownerId']

/**
 * 递归转换对象中的 ID 字段为字符串，防止 JavaScript 大整数精度丢失
 */
function convertIdsToString(data: any): any {
  if (data === null || data === undefined) {
    return data
  }

  // 处理数组
  if (Array.isArray(data)) {
    return data.map(convertIdsToString)
  }

  // 处理对象
  if (typeof data === 'object') {
    const result: any = {}
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        // 如果是 ID 字段且是数字，转换为字符串
        if (ID_FIELDS.includes(key) && typeof data[key] === 'number') {
          result[key] = String(data[key])
        } else {
          result[key] = convertIdsToString(data[key])
        }
      }
    }
    return result
  }

  return data
}

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    console.log('[request.ts] 发送请求:', config.method?.toUpperCase(), config.url)
    return config
  },
  function (error) {
    console.log('[request.ts] 请求拦截器错误:', error)
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    console.log('[request.ts] 成功响应拦截器, data:', response.data)
    const { data } = response
    // 转换 ID 字段为字符串，防止大整数精度丢失
    if (data && typeof data === 'object') {
      const convertedData = convertIdsToString(data)
      response.data = convertedData
    }
    // 处理业务错误码
    if (data.code !== 0) {
      console.log('[request.ts] 业务错误:', data)
      // 未登录
      if (data.code === 40100) {
        handleUnauthorized(response)
      } else {
        // 其他业务错误，统一显示错误消息并 reject
        console.log('[request.ts] 显示错误消息:', data.message || '操作失败')
        message.error(data.message || '操作失败')
      }
      // 业务错误也抛出异常，让调用方能感知到失败
      return Promise.reject({ response, businessError: true })
    }
    return response
  },
  function (error) {
    console.log('[request.ts] 错误拦截器, error:', error)
    console.log('[request.ts] error.response?.data:', error.response?.data)
    // 处理 HTTP 错误响应
    const errorData = error.response?.data
    if (errorData) {
      console.log('[request.ts] errorData.code:', errorData.code)
      if (errorData.code === 40100) {
        handleUnauthorized(error.response)
      } else {
        console.log('[request.ts] 显示 HTTP 错误消息:', errorData.message)
        message.error(errorData.message || '请求失败')
      }
    } else if (error.response?.status === 401) {
      handleUnauthorized(error.response)
    } else {
      message.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

// 处理未登录
function handleUnauthorized(response?: any) {
  // 如果是获取当前用户信息的请求，由调用方处理，不在此处跳转
  const isGetLoginUserRequest = response?.request?.responseURL?.includes('user/get/login')

  if (!isGetLoginUserRequest) {
    // 清空存储中的用户信息
    localStorage.removeItem('museai-user')
    // 如果不在登录页，跳转并刷新页面以同步状态
    if (!window.location.pathname.includes('/user/login')) {
      message.warning('请先登录')
      const loginUrl = `/user/login?redirect=${window.location.href}`
      window.location.href = loginUrl
    }
  }
}

export default myAxios
