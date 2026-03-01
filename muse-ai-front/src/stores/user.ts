import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

const USER_KEY = 'museai-user'

// 从 localStorage 获取用户信息
function getStoredUser(): API.LoginUserVO | null {
  const stored = localStorage.getItem(USER_KEY)
  if (stored) {
    try {
      return JSON.parse(stored)
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
