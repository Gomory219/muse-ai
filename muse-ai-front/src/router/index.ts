import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '@/layouts/BasicLayout.vue'

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
      ],
    },
  ],
})

export default router
