import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const USER_KEY = 'museai-user'

// 需要转换为字符串的 ID 字段名
const ID_FIELDS = ['id', 'userId']

/**
 * 递归转换对象中的 ID 字段为字符串
 */
function convertIdsToString(data: any): any {
  if (data === null || data === undefined) {
    return data
  }
  if (Array.isArray(data)) {
    return data.map(convertIdsToString)
  }
  if (typeof data === 'object') {
    const result: any = {}
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
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

// 从 localStorage 获取用户信息
function getStoredUser(): API.LoginUserVO | null {
  const stored = localStorage.getItem(USER_KEY)
  if (stored) {
    try {
      const parsed = JSON.parse(stored)
      // 转换 ID 字段为字符串，确保与类型定义一致
      return convertIdsToString(parsed)
    } catch {
      return null
    }
  }
  return null
}

// 保存用户信息到 localStorage
function saveUser(user: API.LoginUserVO | null) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export const useUserStore = defineStore('user', () => {
  // 初始化时从 localStorage 读取
  const loginUser = ref<API.LoginUserVO | null>(getStoredUser())

  const isLogin = computed(() => !!loginUser.value)

  function setUser(user: API.LoginUserVO | null) {
    loginUser.value = user
    saveUser(user)
  }

  function logout() {
    loginUser.value = null
    saveUser(null)
  }

  // 从 localStorage 重新同步用户信息
  function syncFromStorage() {
    loginUser.value = getStoredUser()
  }

  return { loginUser, isLogin, setUser, logout, syncFromStorage }
})
