<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Dropdown, Menu, MenuItem } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { userLogout, update } from '@/api/userController'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const selectedKeys = ref<string[]>([route.path])

// 是否显示 footer（生成页面隐藏）
const showFooter = computed(() => !route.path.startsWith('/app/generate'))

// 是否显示 header（生成页面隐藏）
const showHeader = computed(() => !route.path.startsWith('/app/generate'))

// 是否全屏（生成页面全屏）
const isFullscreen = computed(() => route.path.startsWith('/app/generate'))

// 是否是登录/注册页面（不需要滚动条）
const isAuthPage = computed(() =>
  route.path === '/user/login' || route.path === '/user/register'
)

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
  <a-layout :class="['layout', { fullscreen: isFullscreen, 'auth-page': isAuthPage }]">
    <!-- Header -->
    <a-layout-header v-if="showHeader" class="header">
      <div class="header-border-top"></div>
      <div class="header-content">
        <!-- Logo + 项目名称 -->
        <div class="logo-section" @click="router.push('/')">
          <img src="/favicon-static.svg" alt="logo" class="logo" />
          <span class="app-name">
            <span class="app-name-muse">Muse</span>
            <span class="app-name-ai">AI</span>
          </span>
        </div>

        <!-- 导航栏 -->
        <div class="nav-menu">
          <div
            :class="['nav-item', { active: selectedKeys.includes('/') }]"
            @click="router.push('/')"
          >
            <span class="nav-text">首页</span>
            <span class="nav-indicator"></span>
          </div>
          <div
            v-if="userStore.loginUser?.userRole === 'admin'"
            :class="['nav-item', { active: selectedKeys.includes('/user/manage') }]"
            @click="router.push('/user/manage')"
          >
            <span class="nav-text">用户管理</span>
            <span class="nav-indicator"></span>
          </div>
          <div
            v-if="userStore.loginUser?.userRole === 'admin'"
            :class="['nav-item', { active: selectedKeys.includes('/app/manage') }]"
            @click="router.push('/app/manage')"
          >
            <span class="nav-text">应用管理</span>
            <span class="nav-indicator"></span>
          </div>
          <a
            href="https://github.com/Gomory219"
            target="_blank"
            class="nav-item external-link"
          >
            <span class="nav-text">更多作品</span>
            <span class="nav-indicator"></span>
          </a>
        </div>

        <!-- 登录按钮 / 用户信息 -->
        <template v-if="!userStore.isLogin">
          <button class="login-btn" @click="router.push('/user/login')">
            <span class="btn-bracket">&lt;</span>
            <span>登录</span>
            <span class="btn-bracket">/&gt;</span>
          </button>
        </template>
        <template v-else>
          <a-dropdown>
            <div class="user-card">
              <div class="user-avatar">
                <img v-if="userStore.loginUser?.userAvatar" :src="userStore.loginUser.userAvatar" alt="avatar" />
                <span v-else class="avatar-placeholder">{{ userStore.loginUser?.userName?.[0] || 'U' }}</span>
              </div>
              <div class="user-divider"></div>
              <div class="user-info">
                <span class="user-prompt">$</span>
                <span class="user-name">{{ userStore.loginUser?.userName || 'user' }}</span>
              </div>
            </div>
            <template #overlay>
              <div class="dropdown-overlay">
                <div class="dropdown-item" @click="handleEditProfile">
                  <UserOutlined />
                  <span>编辑信息</span>
                </div>
                <div class="dropdown-item" @click="handleLogout">
                  <LogoutOutlined />
                  <span>注销</span>
                </div>
              </div>
            </template>
          </a-dropdown>
        </template>
      </div>
      <div class="header-border-bottom"></div>
    </a-layout-header>

    <!-- Body -->
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>

    <!-- Footer -->
    <a-layout-footer v-if="showFooter" class="footer">// muse_ai_platform v1.0.0</a-layout-footer>
  </a-layout>

  <!-- 编辑个人信息弹窗 -->
  <a-modal
    v-model:open="editModalVisible"
    title="null"
    :footer="null"
    :closable="false"
    width="420px"
  >
    <div class="edit-modal">
      <!-- 弹窗头部 -->
      <div class="modal-header">
        <span class="modal-prompt">$</span>
        <span class="modal-title">edit_profile</span>
      </div>

      <!-- 弹窗内容 -->
      <div class="modal-form">
        <div class="form-group">
          <label class="form-label">账号 // read_only</label>
          <div class="form-input-readonly">{{ userStore.loginUser?.userAccount }}</div>
        </div>
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            v-model="editForm.userName"
            class="form-input"
            placeholder="输入用户名..."
          />
        </div>
        <div class="form-group">
          <label class="form-label">头像URL</label>
          <input
            v-model="editForm.userAvatar"
            class="form-input"
            placeholder="输入头像URL..."
          />
        </div>
        <div class="form-group">
          <label class="form-label">个人简介</label>
          <textarea
            v-model="editForm.userProfile"
            class="form-textarea"
            placeholder="输入个人简介..."
            rows="3"
          ></textarea>
        </div>
      </div>

      <!-- 弹窗底部 -->
      <div class="modal-footer">
        <button class="modal-btn modal-btn-cancel" @click="editModalVisible = false">
          <span>取消</span>
        </button>
        <button class="modal-btn modal-btn-confirm" @click="handleSaveProfile">
          <span v-if="!editModalLoading">保存</span>
          <span v-else class="btn-spinner"></span>
        </button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
/* ===== CSS 变量 ===== */
.layout {
  --bg-header: #0a0a0a;
  --bg-header-secondary: #111111;
  --border-color: #2a2a2a;
  --border-accent: #00d26a;
  --text-primary: #ffffff;
  --text-secondary: #888888;
  --text-muted: #444444;

  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-header);
}

.layout.fullscreen {
  height: 100vh;
  min-height: 100vh;
}

/* 当有 footer 时，允许滚动（但管理页面除外） */
.layout:has(.footer):not(:has(.user-manage-container)):not(:has(.app-manage-container)) {
  min-height: 100vh;
  height: auto;
  overflow: visible;
}

/* 登录/注册页面：固定高度不滚动 */
.layout.auth-page {
  height: 100vh;
  overflow: hidden;
}

/* ===== Header ===== */
.header {
  background: var(--bg-header);
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-border-top {
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    var(--border-color) 20%,
    var(--border-color) 80%,
    transparent 100%
  );
}

.header-border-bottom {
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    var(--border-color) 20%,
    var(--border-color) 80%,
    transparent 100%
  );
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  overflow: hidden;
}

/* ===== Logo 区域 ===== */
.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.logo-section:hover {
  background: var(--bg-header-secondary);
}

.logo {
  width: 48px;
  height: 48px;
}

.app-name {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 18px;
  font-weight: 500;
  letter-spacing: -0.02em;
}

.app-name-muse {
  color: var(--text-primary);
  font-weight: 600;
}

.app-name-ai {
  color: var(--border-accent);
  font-weight: 400;
}

/* ===== 导航栏 ===== */
.nav-menu {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 60px;
}

.nav-item {
  position: relative;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: var(--bg-header-secondary);
}

.nav-item.active .nav-text {
  color: var(--border-accent);
}

.nav-item.active .nav-indicator {
  opacity: 1;
}

.nav-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 14px;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.nav-indicator {
  position: absolute;
  bottom: 6px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  background: var(--border-accent);
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s ease;
}

/* 外部链接样式 */
.nav-item.external-link {
  text-decoration: none;
}

/* ===== 登录按钮 ===== */
.login-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-btn:hover {
  border-color: var(--border-accent);
  color: var(--border-accent);
  background: rgba(0, 210, 106, 0.05);
}

.btn-bracket {
  color: var(--text-muted);
  font-size: 11px;
}

.login-btn:hover .btn-bracket {
  color: var(--border-accent);
}

/* ===== 用户区域 ===== */
.user-card {
  display: flex;
  align-items: center;
  gap: 0;
  cursor: pointer;
  background: var(--bg-header-secondary);
  border: 1px solid var(--border-color);
  border-radius: 24px 6px 6px 24px;
  padding: 4px 4px 4px 4px;
  transition: all 0.2s ease;
  height: 44px;
}

.user-card:hover {
  border-color: var(--border-accent);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--border-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 16px;
  font-weight: 600;
  color: var(--bg-header);
}

.user-divider {
  width: 1px;
  height: 20px;
  background: var(--border-color);
  margin: 0 4px 0 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px 0 8px;
}

.user-prompt {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 14px;
  color: var(--border-accent);
  font-weight: 600;
}

.user-name {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
  color: var(--text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== Dropdown 菜单 ===== */
.dropdown-overlay {
  background: #1a1a1a;
  border: 1px solid #2a2a2a;
  border-radius: 8px;
  padding: 6px;
  min-width: 150px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  color: #888;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.dropdown-item:hover {
  background: rgba(0, 210, 106, 0.1);
  color: #00d26a;
}

.dropdown-item .anticon {
  font-size: 14px;
  color: #444;
  transition: color 0.2s ease;
}

.dropdown-item:hover .anticon {
  color: #00d26a;
}

/* ===== Content ===== */
.content {
  flex: 1;
  background: transparent;
  overflow: hidden;
}

/* ===== Footer ===== */
.footer {
  text-align: center;
  background: var(--bg-header);
  border-top: 1px solid var(--border-color);
  color: var(--text-muted);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 12px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }

  .app-name {
    font-size: 16px;
  }

  .user-name {
    display: none;
  }
}

/* ===== 编辑弹窗 ===== */
.edit-modal :deep(.ant-modal-content) {
  background: #1a1a1a;
  border: 1px solid #2a2a2a;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

.edit-modal :deep(.ant-modal-body) {
  padding: 0;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  border-bottom: 1px solid #2a2a2a;
  background: #111111;
}

.modal-prompt {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 14px;
  color: #00d26a;
  font-weight: 600;
}

.modal-title {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 14px;
  color: #ffffff;
}

.modal-form {
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 14px;
  background: #0a0a0a;
  border: 1px solid #2a2a2a;
  border-radius: 6px;
  color: #ffffff;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #00d26a;
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #444;
}

.form-textarea {
  resize: none;
  line-height: 1.5;
}

.form-input-readonly {
  padding: 10px 14px;
  background: #0a0a0a;
  border: 1px solid #1a1a1a;
  border-radius: 6px;
  color: #444;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #2a2a2a;
  background: #111111;
  justify-content: flex-end;
}

.modal-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #2a2a2a;
  background: transparent;
  color: #888;
}

.modal-btn-cancel:hover {
  border-color: #444;
  color: #fff;
}

.modal-btn-confirm {
  border-color: #00d26a;
  color: #00d26a;
  background: rgba(0, 210, 106, 0.05);
  min-width: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn-confirm:hover {
  background: #00d26a;
  color: #0a0a0a;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(10, 10, 10, 0.3);
  border-top-color: #0a0a0a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

<style>
/* ===== 全局滚动条样式 ===== */
/* 滚动条整体 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

/* 滚动条轨道 */
::-webkit-scrollbar-track {
  background: #0a0a0a;
}

/* 滚动条滑块 */
::-webkit-scrollbar-thumb {
  background: #2a2a2a;
  border-radius: 4px;
}

/* 滚动条滑块悬停 */
::-webkit-scrollbar-thumb:hover {
  background: #00d26a;
}

/* Firefox 滚动条 */
* {
  scrollbar-width: thin;
  scrollbar-color: #2a2a2a #0a0a0a;
}
</style>
