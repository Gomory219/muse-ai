<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Dropdown, Menu, MenuItem } from 'ant-design-vue'
import { LogoutOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { userLogout, update } from '@/api/userController'
import { uploadFile } from '@/api/fileUploadController'

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

// 头像上传相关
const avatarUploading = ref(false)
const avatarInputRef = ref<HTMLInputElement | null>(null)

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

// 头像上传处理
const handleAvatarClick = () => {
  avatarInputRef.value?.click()
}

const handleAvatarChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    message.warning('请选择图片文件')
    return
  }

  // 验证文件大小（2MB）
  if (file.size > 2 * 1024 * 1024) {
    message.warning('图片大小不能超过2MB')
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadFile(file)
    if (res.data.code === 0 && res.data.data) {
      editForm.value.userAvatar = res.data.data
      message.success('头像上传成功')
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (error) {
    message.error('上传失败')
  } finally {
    avatarUploading.value = false
    // 清空 input 以便可以重复选择同一文件
    target.value = ''
  }
}

// 获取默认头像
const getDefaultAvatar = (userName?: string) => {
  if (!userName) return '/default-avatar.png'
  const firstChar = userName.charAt(0).toUpperCase()
  return firstChar
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
    width="480px"
  >
    <div class="edit-modal">
      <!-- 弹窗头部 -->
      <div class="modal-header">
        <span class="modal-prompt">$</span>
        <span class="modal-title">edit_profile</span>
        <button class="modal-close" @click="editModalVisible = false">×</button>
      </div>

      <!-- 弹窗内容 -->
      <div class="modal-form">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="handleAvatarClick">
            <img
              v-if="editForm.userAvatar"
              :src="editForm.userAvatar"
              class="avatar-image"
              alt="头像"
            />
            <div v-else class="avatar-placeholder">
              <span class="avatar-text">{{ getDefaultAvatar(editForm.userName) }}</span>
            </div>
            <div class="avatar-overlay">
              <span v-if="!avatarUploading">点击上传</span>
              <span v-else>上传中...</span>
            </div>
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/*"
            class="avatar-input"
            @change="handleAvatarChange"
          />
        </div>

        <div class="form-group">
          <label class="form-label">账号 <span class="form-hint">// read_only</span></label>
          <div class="form-input-readonly">{{ userStore.loginUser?.userAccount }}</div>
        </div>
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            v-model="editForm.userName"
            class="form-input"
            placeholder="输入用户名..."
            autofocus
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
        <button class="modal-btn modal-btn-confirm" @click="handleSaveProfile" :disabled="editModalLoading">
          <span v-if="!editModalLoading">保存修改</span>
          <span v-else>
            保存中...
            <span class="btn-spinner"></span>
          </span>
        </button>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg-primary);
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
  background: var(--bg-primary);
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
  background: var(--bg-secondary);
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
  color: var(--accent-green);
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
  background: var(--bg-secondary);
}

.nav-item.active .nav-text {
  color: var(--accent-green);
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
  background: var(--accent-green);
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
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: rgba(0, 210, 106, 0.05);
}

.btn-bracket {
  color: var(--text-muted);
  font-size: 11px;
}

.login-btn:hover .btn-bracket {
  color: var(--accent-green);
}

/* ===== 用户区域 ===== */
.user-card {
  display: flex;
  align-items: center;
  gap: 0;
  cursor: pointer;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 24px 6px 6px 24px;
  padding: 4px 4px 4px 4px;
  transition: all 0.2s ease;
  height: 44px;
}

.user-card:hover {
  border-color: var(--accent-green);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-green);
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
  color: var(--bg-primary);
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
  color: var(--accent-green);
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
  background: var(--bg-primary);
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
  background: #ffffff;
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.edit-modal :deep(.ant-modal-body) {
  padding: 0;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border-bottom: none;
  background: transparent;
  position: relative;
  margin-bottom: 4px;
}

.modal-prompt {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 18px;
  color: #00d26a;
  font-weight: 600;
}

.modal-title {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 16px;
  color: #1a1a1a;
  font-weight: 600;
}

.modal-close {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  background: #f5f5f5;
  border: none;
  color: #666;
  font-size: 22px;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  line-height: 1;
}

.modal-close:hover {
  background: #e8e8e8;
  color: #1a1a1a;
}

.modal-form {
  padding: 24px 24px 8px;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.avatar-wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 210, 106, 0.3);
  transform: scale(1.02);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #00d26a 0%, #00a856 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-text {
  font-size: 32px;
  font-weight: 600;
  color: #ffffff;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay span {
  font-size: 12px;
  color: #ffffff;
  font-weight: 500;
}

.avatar-input {
  display: none;
}

.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-hint {
  color: #999;
  font-size: 12px;
  font-weight: 400;
  margin-left: 6px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  background: #f8f9fa;
  border: 1.5px solid #e8e8e8;
  border-radius: 10px;
  color: #1a1a1a;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #00d26a;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(0, 210, 106, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #aaa;
}

.form-textarea {
  resize: none;
  line-height: 1.5;
}

.form-input-readonly {
  padding: 12px 14px;
  background: #f0f0f0;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  color: #666;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px 24px;
  border-top: none;
  background: transparent;
  justify-content: flex-end;
}

.modal-btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  color: #666;
}

.modal-btn:hover:not(:disabled) {
  color: #1a1a1a;
}

.modal-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.modal-btn-cancel {
  background: #f5f5f5;
}

.modal-btn-cancel:hover:not(:disabled) {
  background: #e8e8e8;
}

.modal-btn-confirm {
  background: #00d26a;
  color: #ffffff;
  min-width: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn-confirm:hover:not(:disabled) {
  background: #00c05f;
  box-shadow: 0 4px 12px rgba(0, 210, 106, 0.3);
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
