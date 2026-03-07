import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'
import { getLoginUser } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: BasicLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/HomeView.vue'),
        },
        {
          path: 'about',
          name: 'about',
          component: () => import('@/views/AboutView.vue'),
        },
        {
          path: 'user/login',
          name: 'login',
          component: () => import('@/views/UserLoginView.vue'),
        },
        {
          path: 'user/register',
          name: 'register',
          component: () => import('@/views/UserRegisterView.vue'),
        },
        {
          path: 'user/manage',
          name: 'userManage',
          component: () => import('@/views/UserManageView.vue'),
        },
        {
          path: 'app/generate',
          name: 'appGenerate',
          component: () => import('@/views/AppGenerateView.vue'),
        },
        {
          path: 'app/manage',
          name: 'appManage',
          component: () => import('@/views/AppManageView.vue'),
        },
      ],
    },
  ],
})

// 全局前置守卫：获取当前登录用户信息
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 每次路由切换时，尝试获取当前登录用户信息
  try {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      // 登录成功，更新用户信息
      userStore.setUser(res.data.data)
    } else {
      // 未登录，清空用户信息
      userStore.logout()
    }
  } catch (error) {
    // 请求失败，保持当前状态或清空
    // userStore.logout()
  }

  next()
})

export default router
