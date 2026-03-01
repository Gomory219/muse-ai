import axios from 'axios'
import { message } from 'ant-design-vue'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: 'http://localhost:7777/api',
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
      handleUnauthorized()
    }
    return response
  },
  function (error) {
    // 未登录（error response）
    if (error.response?.data?.code === 40100) {
      handleUnauthorized()
    }
    return Promise.reject(error)
  },
)

// 处理未登录
function handleUnauthorized() {
  // 清空存储中的用户信息
  localStorage.removeItem('museai-user')
  // 如果不在登录页，跳转并刷新页面以同步状态
  if (!window.location.pathname.includes('/user/login')) {
    message.warning('请先登录')
    const loginUrl = `/user/login?redirect=${window.location.href}`
    window.location.href = loginUrl
  }
}

export default myAxios
