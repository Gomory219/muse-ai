import axios from 'axios'
import { message } from 'ant-design-vue'

// API 基础地址
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:7777/api'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      handleUnauthorized(response)
    }
    return response
  },
  function (error) {
    // 未登录（error response）
    if (error.response?.data?.code === 40100) {
      handleUnauthorized(error.response)
    }
    // HTTP 401 状态码
    if (error.response?.status === 401) {
      handleUnauthorized(error.response)
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
