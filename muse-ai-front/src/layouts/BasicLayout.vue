<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Dropdown, Menu, MenuItem } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { userLogout, update } from '@/api/userController'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const selectedKeys = ref<string[]>([route.path])

// 编辑个人信息弹窗
const editModalVisible = ref(false)
const editModalLoading = ref(false)
const editForm = ref({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
  },
)

const handleLogout = async () => {
  try {
    await userLogout()
    userStore.logout()
    message.success('退出登录成功')
  } catch (error) {
    userStore.logout()
  }
}

const handleEditProfile = () => {
  // 填充当前用户信息
  if (userStore.loginUser) {
    editForm.value = {
      userName: userStore.loginUser.userName || '',
      userAvatar: userStore.loginUser.userAvatar || '',
      userProfile: userStore.loginUser.userProfile || '',
    }
    editModalVisible.value = true
  }
}

const handleSaveProfile = async () => {
  if (!editForm.value.userName) {
    message.warning('用户名不能为空')
    return
  }

  editModalLoading.value = true
  try {
    const res = await update({
      id: userStore.loginUser!.id!,
      userName: editForm.value.userName,
      userAvatar: editForm.value.userAvatar,
      userProfile: editForm.value.userProfile,
    })
    if (res.data.code === 0) {
      // 更新 store 中的用户信息
      userStore.setUser({
        ...userStore.loginUser!,
        userName: editForm.value.userName,
        userAvatar: editForm.value.userAvatar,
        userProfile: editForm.value.userProfile,
      })
      message.success('修改成功')
      editModalVisible.value = false
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    editModalLoading.value = false
  }
}
</script>

<template>
  <a-layout class="layout">
    <!-- Header -->
    <a-layout-header class="header">
      <div class="header-content">
        <!-- Logo + 项目名称 -->
        <div class="logo-section">
          <img src="/favicon.svg" alt="logo" class="logo" />
          <span class="app-name">MuseAI 零代码生成平台</span>
        </div>

        <!-- 导航栏 -->
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          class="nav-menu"
          :style="{ background: 'transparent', border: 'none' }"
        >
          <a-menu-item key="/" @click="router.push('/')"> 首页 </a-menu-item>
          <a-menu-item
            v-if="userStore.loginUser?.userRole === 'admin'"
            key="/user/manage"
            @click="router.push('/user/manage')"
          >
            用户管理
          </a-menu-item>
        </a-menu>

        <!-- 登录按钮 / 用户信息 -->
        <template v-if="!userStore.isLogin">
          <a-button type="primary" class="login-btn" @click="router.push('/user/login')">登录</a-button>
        </template>
        <template v-else>
          <a-dropdown>
            <div class="user-info">
              <a-avatar :src="userStore.loginUser?.userAvatar" :size="40">
                {{ userStore.loginUser?.userName?.[0] || 'U' }}
              </a-avatar>
              <span class="user-name">{{ userStore.loginUser?.userName || '用户' }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="edit" @click="handleEditProfile">
                  <template #icon><UserOutlined /></template>
                  编辑信息
                </a-menu-item>
                <a-menu-item key="logout" @click="handleLogout">
                  <template #icon><LogoutOutlined /></template>
                  注销
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
      </div>
    </a-layout-header>

    <!-- Body -->
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>

    <!-- Footer -->
    <a-layout-footer class="footer"> MuseAI &copy; 2025 </a-layout-footer>
  </a-layout>

  <!-- 编辑个人信息弹窗 -->
  <a-modal
    v-model:open="editModalVisible"
    title="编辑个人信息"
    :confirm-loading="editModalLoading"
    @ok="handleSaveProfile"
    width="450px"
  >
    <a-form layout="vertical" :model="editForm" style="margin-top: 24px">
      <a-form-item label="账号">
        <a-input :value="userStore.loginUser?.userAccount" disabled />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="editForm.userName" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="头像URL">
        <a-input v-model:value="editForm.userAvatar" placeholder="请输入头像URL" />
      </a-form-item>
      <a-form-item label="个人简介">
        <a-textarea
          v-model:value="editForm.userProfile"
          placeholder="请输入个人简介"
          :rows="3"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0;
  line-height: 64px;
  height: 64px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 32px;
  height: 32px;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.nav-menu {
  flex: 1;
  margin: 0 48px;
}

.content {
  flex: 1;
  background: #f5f5f5;
}

.footer {
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f0f2f5;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  line-height: 1;
}
</style>
