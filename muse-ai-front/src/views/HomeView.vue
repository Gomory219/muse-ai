<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SendOutlined, RightOutlined, FireOutlined, AppstoreOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { listMyApps, listFeaturedApps, chatToGenApp, createApp } from '@/api/appController'

const router = useRouter()
const userStore = useUserStore()

// 聊天输入
const promptInput = ref('')
const isGenerating = ref(false)

// 提示词建议
const promptSuggestions = [
  { text: '博客网站', icon: '📝' },
  { text: '企业官网', icon: '🏢' },
  { text: '个人简历', icon: '👤' },
  { text: '在线商城', icon: '🛒' },
  { text: '后台管理系统', icon: '⚙️' },
]

// 我的作品
const myApps = ref<API.AppVO[]>([])
const myAppsLoading = ref(false)

// 精选案例
const featuredApps = ref<AppVO[]>([])
const featuredAppsLoading = ref(false)

// 发送按钮状态
const canSend = computed(() => promptInput.value.trim().length > 0)

// 点击提示词建议
const handleClickSuggestion = (text: string) => {
  promptInput.value = text
}

// 发送消息生成应用
const handleSend = async () => {
  const trimmedPrompt = promptInput.value.trim()
  if (!trimmedPrompt) {
    message.warning('请输入提示词')
    return
  }

  if (!userStore.isLogin) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  isGenerating.value = true
  try {
    // 先创建应用
    const createRes = await createApp({ initPrompt: trimmedPrompt })
    if (createRes.data.code !== 0 || !createRes.data.data) {
      message.error(createRes.data.message || '创建应用失败')
      return
    }

    const appId = String(createRes.data.data)

    // 跳转到生成页面
    router.push({
      path: '/app/generate',
      query: { appId, prompt: trimmedPrompt },
    })
  } catch (error: any) {
    if (error?.response?.status === 401) {
      message.warning('请先登录')
      router.push('/user/login')
    } else {
      message.error('生成失败，请重试')
    }
  } finally {
    isGenerating.value = false
  }
}

// 加载我的作品
const loadMyApps = async () => {
  if (!userStore.isLogin) return

  myAppsLoading.value = true
  try {
    const res = await listMyApps({
      appQueryRequest: {
        pageNum: 1,
        pageSize: 8,
      },
    })
    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.list || []
    }
  } catch (error) {
    console.error('加载我的作品失败', error)
  } finally {
    myAppsLoading.value = false
  }
}

// 加载精选案例
const loadFeaturedApps = async () => {
  featuredAppsLoading.value = true
  try {
    const res = await listFeaturedApps({
      appQueryRequest: {
        pageNum: 1,
        pageSize: 8,
      },
    })
    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.list || []
    }
  } catch (error) {
    console.error('加载精选案例失败', error)
  } finally {
    featuredAppsLoading.value = false
  }
}

// 格式化时间
const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 点击应用卡片
const handleAppClick = (app: API.AppVO) => {
  if (!app.id) return
  // 跳转到应用生成页面
  router.push({
    path: '/app/generate',
    query: {
      appId: String(app.id),
    },
  })
}

onMounted(() => {
  loadFeaturedApps()
  loadMyApps()
})

// 监听登录状态变化
const unwatch = userStore.$subscribe(() => {
  loadMyApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 背景网格 -->
    <div class="grid-background"></div>
    <div class="dot-matrix"></div>

    <!-- Hero 区域 -->
    <div class="hero-section">
      <div class="hero-content">
        <!-- Logo 和标题 -->
        <div class="brand-header">
          <div class="logo-container">
            <img src="/favicon.svg" alt="Muse AI" class="brand-logo" />
          </div>
          <h1 class="hero-title">
            <span class="title-prefix">Muse</span>
            <span class="title-suffix">AI</span>
          </h1>
        </div>

        <p class="hero-subtitle">
          <span class="code-bracket">&lt;</span>
          与 AI 对话轻松创建应用和网站
          <span class="code-bracket">/&gt;</span>
        </p>

        <!-- 聊天框 - 终端风格 -->
        <div class="chat-container">
          <div class="terminal-window">
            <div class="terminal-header">
              <div class="terminal-dots">
                <span class="dot red"></span>
                <span class="dot yellow"></span>
                <span class="dot green"></span>
              </div>
              <span class="terminal-title">muse-input.ts</span>
            </div>
            <div class="terminal-body">
              <div class="terminal-prompt">
                <span class="prompt-symbol">$</span>
                <textarea
                  v-model="promptInput"
                  class="chat-input"
                  placeholder="描述你想创建的应用..."
                  rows="3"
                  @keydown.enter.prevent="handleSend"
                ></textarea>
              </div>
            </div>
            <div class="terminal-footer">
              <button
                class="send-btn"
                :class="{ active: canSend, loading: isGenerating }"
                :disabled="!canSend || isGenerating"
                @click="handleSend"
              >
                <span v-if="!isGenerating" class="btn-text">EXECUTE →</span>
                <span v-else class="loading-text">
                  <span class="spinner"></span>
                  GENERATING...
                </span>
              </button>
            </div>
          </div>

          <!-- 提示词建议 -->
          <div class="prompt-suggestions">
            <span class="suggestions-label">// quick_start:</span>
            <button
              v-for="suggestion in promptSuggestions"
              :key="suggestion.text"
              class="suggestion-btn"
              @click="handleClickSuggestion(suggestion.text)"
            >
              <span class="suggestion-keyword">{{ suggestion.text }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 我的作品栏 -->
    <div v-if="userStore.isLogin" class="section-container my-works-section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-line">│</span>
          <span>我的作品</span>
          <span class="title-count">{{ myApps.length > 0 ? `(${myApps.length})` : '' }}</span>
        </h2>
        <button class="view-all-btn" @click="router.push('/my/apps')">
          <span>全部</span>
          <RightOutlined />
        </button>
      </div>
      <div v-if="myAppsLoading" class="loading-skeleton">
        <div v-for="i in 4" :key="i" class="app-card skeleton">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>
      <div v-else-if="myApps.length > 0" class="app-grid">
        <div
          v-for="app in myApps"
          :key="app.id"
          class="app-card"
          @click="handleAppClick(app)"
        >
          <div class="app-cover">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="default-cover">{{ app.appName?.[0] || 'A' }}</div>
            <div class="app-overlay">
              <span class="overlay-text">view_source</span>
            </div>
          </div>
          <div class="app-info">
            <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
            <p class="app-time">{{ formatTime(app.createTime) }}</p>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">⚡</div>
        <p>暂无作品</p>
        <span class="empty-hint">开始创建你的第一个应用</span>
      </div>
    </div>

    <!-- 精选案例 -->
    <div class="section-container featured-section">
      <div class="section-header">
        <h2 class="section-title">
          <span class="title-line accent">│</span>
          <span>精选案例</span>
        </h2>
        <button class="view-all-btn" @click="router.push('/featured')">
          <span>全部</span>
          <RightOutlined />
        </button>
      </div>
      <div v-if="featuredAppsLoading" class="loading-skeleton">
        <div v-for="i in 4" :key="i" class="app-card skeleton">
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>
      <div v-else-if="featuredApps.length > 0" class="app-grid">
        <div
          v-for="app in featuredApps"
          :key="app.id"
          class="app-card"
          @click="handleAppClick(app)"
        >
          <div class="app-cover">
            <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
            <div v-else class="default-cover">{{ app.appName?.[0] || 'A' }}</div>
            <div class="app-overlay">
              <span class="overlay-text">view_source</span>
            </div>
          </div>
          <div class="app-info">
            <h3 class="app-name">{{ app.appName || '未命名应用' }}</h3>
            <p class="app-time">{{ formatTime(app.createTime) }}</p>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <div class="empty-icon">∅</div>
        <p>暂无精选案例</p>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* ===== CSS 变量 ===== */
.home-page {
  --bg-primary: #0a0a0a;
  --bg-secondary: #111111;
  --bg-card: #1a1a1a;
  --text-primary: #ffffff;
  --text-secondary: #888888;
  --text-muted: #444444;
  --accent-green: #00d26a;
  --accent-green-dim: rgba(0, 210, 106, 0.1);
  --border-color: #2a2a2a;
  --border-hover: #00d26a;

  min-height: 100vh;
  background: var(--bg-primary);
  position: relative;
  overflow-x: hidden;
}

/* ===== 背景效果 ===== */
.grid-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 50px 50px;
  pointer-events: none;
  z-index: 0;
}

.dot-matrix {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: radial-gradient(circle, rgba(0, 210, 106, 0.15) 1px, transparent 1px);
  background-size: 30px 30px;
  pointer-events: none;
  z-index: 0;
  opacity: 0.5;
}

/* ===== Hero 区域 ===== */
.hero-section {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  position: relative;
  z-index: 1;
}

.hero-content {
  max-width: 800px;
  width: 100%;
}

/* 品牌 Header */
.brand-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  margin-bottom: 40px;
}

.logo-container {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 16px;
  position: relative;
  overflow: hidden;
}

.logo-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 210, 106, 0.2), transparent);
  animation: shimmer-border 3s infinite;
}

@keyframes shimmer-border {
  0% { left: -100%; }
  100% { left: 100%; }
}

.brand-logo {
  width: 80px;
  height: 80px;
  position: relative;
  z-index: 1;
}

.hero-title {
  font-size: clamp(48px, 8vw, 72px);
  font-weight: 300;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  letter-spacing: -0.02em;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.title-prefix {
  font-weight: 600;
  color: var(--text-primary);
}

.title-suffix {
  font-weight: 300;
  color: var(--accent-green);
}

.cursor {
  display: inline-block;
  width: 4px;
  height: 1em;
  background: var(--accent-green);
  margin-left: 4px;
}

@keyframes blink {
  50% { opacity: 0; }
}

.hero-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  margin: 0 0 48px;
  text-align: center;
}

.code-bracket {
  color: var(--accent-green);
  margin: 0 4px;
}

/* ===== 终端风格聊天框 ===== */
.chat-container {
  max-width: 680px;
  margin: 0 auto;
}

.terminal-window {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
}

.terminal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.terminal-dots {
  display: flex;
  gap: 8px;
}

.terminal-dots .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.terminal-dots .dot.red { background: #ff5f57; }
.terminal-dots .dot.yellow { background: #febc2e; }
.terminal-dots .dot.green { background: #28c840; }

.terminal-title {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}

.terminal-body {
  padding: 16px;
}

.terminal-prompt {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.prompt-symbol {
  color: var(--accent-green);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 16px;
  font-weight: 600;
  user-select: none;
  padding-top: 4px;
}

.chat-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  color: var(--text-primary);
}

.chat-input::placeholder {
  color: var(--text-muted);
}

.terminal-footer {
  padding: 12px 16px;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
}

.send-btn {
  padding: 10px 24px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-muted);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  font-weight: 500;
  cursor: not-allowed;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.send-btn.active {
  border-color: var(--accent-green);
  color: var(--accent-green);
  cursor: pointer;
  background: var(--accent-green-dim);
}

.send-btn.active:hover {
  background: var(--accent-green);
  color: var(--bg-primary);
}

.send-btn.loading {
  cursor: wait;
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--text-muted);
  border-top-color: var(--accent-green);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 提示词建议 ===== */
.prompt-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
  justify-content: center;
  align-items: center;
}

.suggestions-label {
  color: var(--text-muted);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
}

.suggestion-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

.suggestion-keyword {
  color: var(--text-primary);
}

/* ===== Section 容器 ===== */
.section-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px;
  position: relative;
  z-index: 1;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40px;
}

.section-title {
  font-size: 24px;
  font-weight: 500;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
}

.title-line {
  color: var(--text-muted);
  font-size: 20px;
}

.title-line.accent {
  color: var(--accent-green);
}

.title-count {
  color: var(--text-muted);
  font-size: 14px;
  font-weight: 400;
}

.view-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.view-all-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

/* ===== 应用网格 ===== */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.app-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.app-card:hover {
  border-color: var(--accent-green);
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.app-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--bg-secondary);
  position: relative;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 300;
  color: var(--accent-green);
  background: var(--bg-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  color: var(--accent-green);
  letter-spacing: 0.1em;
}

.app-info {
  padding: 16px;
  background: var(--bg-card);
}

.app-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-time {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

/* ===== 骨架屏 ===== */
.loading-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.app-card.skeleton {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  min-height: 220px;
}

.skeleton-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--border-color) 50%, var(--bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info {
  padding: 16px;
}

.skeleton-line {
  height: 14px;
  background: var(--bg-secondary);
  border-radius: 4px;
  margin-bottom: 8px;
}

.skeleton-line.short {
  width: 60%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 空状态 ===== */
.empty-state {
  text-align: center;
  padding: 80px 24px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: var(--text-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-muted);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

/* ===== 我的作品栏 ===== */
.my-works-section {
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

/* ===== 精选案例区域 ===== */
.featured-section {
  background: var(--bg-primary);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }

  .app-grid {
    grid-template-columns: 1fr;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .view-all-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
