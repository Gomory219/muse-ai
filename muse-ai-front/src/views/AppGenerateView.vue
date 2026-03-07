<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SendOutlined,
  LoadingOutlined,
  CheckOutlined,
  CopyOutlined,
  PlusOutlined,
  DownloadOutlined,
  RocketOutlined,
  SyncOutlined,
  GlobalOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  CaretDownOutlined,
} from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { API_BASE_URL } from '@/request'
import { useUserStore } from '@/stores/user'
import { listMyApps, deployApp, updateAppName, downloadApp } from '@/api/appController'
import type { AppVO } from '@/api/typings.d'

// 初始化 markdown-it
const md = new MarkdownIt({
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
  html: true,
  linkify: true,
  typographer: true,
})

// 渲染 markdown 内容
const renderMarkdown = (content: string) => {
  if (!content) return ''
  return md.render(content)
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 应用信息
const appId = ref<string>(route.query.appId as string)
const appName = ref<string>('未命名应用')
const prompt = ref<string>(route.query.prompt as string || '')
const codeGenType = ref<string>('multi-file')

// 菜单相关
const appMenuOpen = ref(false)
let closeMenuTimer: number | null = null

// 打开菜单
const openMenu = () => {
  if (closeMenuTimer) {
    clearTimeout(closeMenuTimer)
    closeMenuTimer = null
  }
  appMenuOpen.value = true
}

// 延迟关闭菜单
const closeMenu = () => {
  closeMenuTimer = setTimeout(() => {
    appMenuOpen.value = false
  }, 100) as unknown as number
}

// 应用信息下拉菜单
const appInfoMenuOpen = ref(false)
let closeAppInfoTimer: number | null = null

const openAppInfoMenu = () => {
  if (closeAppInfoTimer) {
    clearTimeout(closeAppInfoTimer)
    closeAppInfoTimer = null
  }
  appInfoMenuOpen.value = true
}

const closeAppInfoMenu = () => {
  closeAppInfoTimer = setTimeout(() => {
    appInfoMenuOpen.value = false
  }, 100) as unknown as number
}

// 编辑应用名称
const isEditingName = ref(false)
const editingAppName = ref('')
const appNameInput = ref<HTMLInputElement>()

const startEditName = () => {
  editingAppName.value = appName.value
  isEditingName.value = true
  nextTick(() => {
    appNameInput.value?.focus()
    appNameInput.value?.select()
  })
}

const cancelEditName = () => {
  isEditingName.value = false
  editingAppName.value = ''
}

const saveAppName = async () => {
  const newName = editingAppName.value.trim()
  if (!newName || !appId.value) {
    cancelEditName()
    return
  }

  // 只有新名字和原来不一样时才修改
  if (newName === appName.value) {
    isEditingName.value = false
    return
  }

  try {
    const res = await updateAppName({
      id: Number(appId.value),
      appName: newName,
    })
    if (res.data.code === 0) {
      appName.value = newName
      message.success('名称修改成功')
    } else {
      message.error(res.data.message || '修改失败')
    }
  } catch (error) {
    message.error('修改失败')
  } finally {
    isEditingName.value = false
  }
}

// 左侧代码面板显示状态
const isCodePanelVisible = ref(true)

// 我的应用列表
const myApps = ref<AppVO[]>([])
const loadingApps = ref(false)

// 聊天相关
type MessageContent = string | Ref<string>

interface Message {
  role: 'user' | 'assistant'
  content: MessageContent
}

const messages = ref<Message[]>([])
const currentInput = ref('')
const isGenerating = ref(false)
const chatContainer = ref<HTMLElement>()

// 滚动相关：判断是否在底部
const isNearBottom = () => {
  if (!chatContainer.value) return true
  const container = chatContainer.value
  const threshold = 150 // 距离底部150px内算作在底部
  return container.scrollHeight - container.scrollTop - container.clientHeight < threshold
}

const scrollToBottom = (smooth = true) => {
  if (!chatContainer.value) return
  // 直接设置 scrollTop 确保完全滚动到底部
  chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

// 记录用户是否主动向上滚动离开底部
let userScrolledUp = false
let lastScrollTop = 0

const onScroll = () => {
  if (!chatContainer.value) return
  const currentScrollTop = chatContainer.value.scrollTop

  // 只有当用户主动向上滚动（scrollTop 减小）且不在底部时，才标记为已向上滚动
  // 如果是内容增长导致的相对位置变化，scrollTop 会保持不变或增加
  if (currentScrollTop < lastScrollTop && !isNearBottom()) {
    userScrolledUp = true
  } else if (isNearBottom()) {
    // 如果用户滚回底部附近，重置状态
    userScrolledUp = false
  }

  lastScrollTop = currentScrollTop
}

// 智能滚动：只有在用户没有向上滚动时才自动滚动
const smartScroll = () => {
  if (!userScrolledUp) {
    // 使用 nextTick 确保 DOM 更新后再滚动
    nextTick(() => {
      if (chatContainer.value) {
        chatContainer.value.scrollTop = chatContainer.value.scrollHeight
      }
    })
  }
}

// 代码生成状态
const isCodeGenerated = ref(false)
const showPreviewIframe = ref(false)

// iframe 预览
const iframeUrl = computed(() => {
  if (!appId.value || !showPreviewIframe.value) return ''
  if (codeGenType.value === 'multi-file') {
    return `${API_BASE_URL}/code/multi-file/${appId.value}/index.html`
  } else {
    return `${API_BASE_URL}/code/html/${appId.value}/index.html`
  }
})

// 显示预览（代码生成完成后调用）
const showPreview = () => {
  showPreviewIframe.value = true
  refreshIframe()
}

// iframe 刷新触发
const iframeKey = ref(0)
const refreshIframe = () => {
  iframeKey.value++
}

// 应用分组（按时间）
const groupedApps = computed(() => {
  const groups: Record<string, AppVO[]> = {
    今天: [],
    昨天: [],
    '一周前': [],
    '一个月前': [],
    更早: [],
  }

  const now = new Date()
  const oneDay = 24 * 60 * 60 * 1000
  const oneWeek = 7 * oneDay
  const oneMonth = 30 * oneDay

  myApps.value.forEach((app) => {
    if (!app.createTime) return
    const createTime = new Date(app.createTime)
    const diff = now.getTime() - createTime.getTime()

    if (diff < oneDay) {
      groups['今天'].push(app)
    } else if (diff < 2 * oneDay) {
      groups['昨天'].push(app)
    } else if (diff < oneWeek) {
      groups['一周前'].push(app)
    } else if (diff < oneMonth) {
      groups['一个月前'].push(app)
    } else {
      groups['更早'].push(app)
    }
  })

  return groups
})

// 是否当前应用
const isCurrentApp = (app: AppVO) => {
  return String(app.id) === appId.value
}

// 切换应用
const switchApp = (app: AppVO) => {
  if (!app.id) return
  appMenuOpen.value = false
  router.push({
    path: '/app/generate',
    query: { appId: String(app.id) },
  })
}

// 新建应用
const createNew = () => {
  appMenuOpen.value = false
  router.push('/')
}

// 刷新页面
const handleRefresh = () => {
  refreshIframe()
  message.success('已刷新')
}

// 在窗口打开
const handleOpenInWindow = () => {
  if (iframeUrl.value) {
    window.open(iframeUrl.value, '_blank')
  }
}

// 全屏浏览（切换左侧代码面板显示）
const handleFullscreen = () => {
  isCodePanelVisible.value = !isCodePanelVisible.value
}

// 下载代码
const isDownloading = ref(false)
const handleDownload = async () => {
  if (!appId.value) {
    message.error('应用 ID 不存在')
    return
  }

  isDownloading.value = true
  try {
    const res = await downloadApp({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      // 使用返回的下载URL进行下载
      const downloadUrl = res.data.data
      // 如果是完整URL则直接使用，否则拼接 /api/download/
      const finalUrl = downloadUrl.startsWith('http')
        ? downloadUrl
        : `${API_BASE_URL}/download/${downloadUrl}`

      // 使用隐藏的 a 标签进行下载，不打开新窗口
      const link = document.createElement('a')
      link.href = finalUrl
      link.download = '' // 让浏览器使用服务器返回的文件名
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } else {
      message.error(res.data.message || '获取下载链接失败')
    }
  } catch (error) {
    message.error('下载失败')
  } finally {
    isDownloading.value = false
  }
}

// 部署
const isDeploying = ref(false)
const deployedUrl = ref('')
const showDeployModal = ref(false)

const handleDeploy = async () => {
  if (!appId.value) return

  isDeploying.value = true
  try {
    const res = await deployApp({ appId: appId.value })
    console.log('部署返回数据:', res.data)
    if (res.data.code === 0) {
      deployedUrl.value = res.data.data || ''
      console.log('部署URL:', deployedUrl.value)
      showDeployModal.value = true
    } else {
      console.error('部署失败:', res.data.message)
      message.error(res.data.message || '部署失败')
    }
  } catch (error) {
    console.error('部署异常:', error)
    message.error('部署失败')
  } finally {
    isDeploying.value = false
  }
}

const copyDeployUrl = () => {
  navigator.clipboard.writeText(deployedUrl.value).then(() => {
    message.success('已复制到剪贴板')
  })
}

const openDeployUrl = () => {
  if (deployedUrl.value) {
    window.open(deployedUrl.value, '_blank')
  }
}

// 加载应用列表
const loadApps = async () => {
  if (!userStore.isLogin) return

  loadingApps.value = true
  try {
    const res = await listMyApps({
      appQueryRequest: {
        pageNum: 1,
        pageSize: 50,
      },
    })
    if (res.data.code === 0 && res.data.data) {
      const appList = res.data.data.list || []
      myApps.value = appList
      // 查找当前应用名称
      const currentApp = appList.find((app: AppVO) => String(app.id) === appId.value)
      if (currentApp?.appName) {
        appName.value = currentApp.appName
      }
    }
  } catch (error) {
    console.error('加载应用列表失败', error)
  } finally {
    loadingApps.value = false
  }
}

// 发送消息
const handleSend = async () => {
  const text = currentInput.value.trim()
  if (!text || isGenerating.value) return

  messages.value.push({
    role: 'user',
    content: text,
  })
  currentInput.value = ''

  await sendChatRequest(text)
}

// 发送聊天请求
const sendChatRequest = async (userMessage: string) => {
  if (!appId.value) {
    message.error('应用 ID 不存在')
    return
  }

  isGenerating.value = true
  userScrolledUp = false // 重置滚动状态
  // 等待DOM更新后滚动到底部
  await nextTick()
  scrollToBottom()

  const currentResponse = ref('')

  messages.value.push({
    role: 'assistant',
    content: currentResponse,
  })

  try {
    const response = await fetch(`${API_BASE_URL}/app/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        appId: appId.value,
        userMessage,
      }),
    })

    if (!response.ok) {
      throw new Error(`请求失败: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      buffer += chunk

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.trim()) continue

        let jsonStr = line.trim()
        if (jsonStr.startsWith('data:')) {
          jsonStr = jsonStr.slice(5).trim()
        }

        try {
          const parsed = JSON.parse(jsonStr)

          if (parsed.v !== undefined) {
            currentResponse.value += parsed.v
            smartScroll()
          } else if (parsed.e === 'end') {
            isGenerating.value = false
            break
          }
        } catch (e) {
          currentResponse.value += jsonStr
        }
      }
    }

    if (buffer.trim()) {
      let jsonStr = buffer.trim()
      if (jsonStr.startsWith('data:')) {
        jsonStr = jsonStr.slice(5).trim()
      }
      try {
        const parsed = JSON.parse(jsonStr)
        if (parsed.v !== undefined) {
          currentResponse.value += parsed.v
        }
      } catch {
        currentResponse.value += jsonStr
      }
    }
  } catch (error: any) {
    console.error('生成失败', error)
    message.error(error.message || '生成失败，请重试')
    currentResponse.value += `\n[生成失败: ${error.message}]`
  } finally {
    isGenerating.value = false
    isCodeGenerated.value = true
    // 生成完成后显示预览
    showPreview()
  }
}

const copyCode = (content: string) => {
  navigator.clipboard.writeText(content).then(() => {
    message.success('已复制到剪贴板')
  })
}

const getMessageContent = (msg: any) => {
  if (typeof msg.content === 'string') {
    return msg.content
  }
  return msg.content?.value || ''
}

// 初始化应用数据
const initAppData = async (newAppId?: string) => {
  const targetAppId = newAppId || route.query.appId as string
  const isSwitchingApp = targetAppId && targetAppId !== appId.value

  // 判断是否是首次进入（不带 prompt）- 这种情况应该直接显示预览
  const isExistingApp = !prompt.value && targetAppId

  if (isSwitchingApp) {
    appId.value = targetAppId
    messages.value = []
    // 切换应用时：直接显示预览（不重置状态）
    showPreviewIframe.value = true
    isCodeGenerated.value = true
  } else if (isExistingApp) {
    // 首次进入已有应用，直接显示预览
    showPreviewIframe.value = true
    isCodeGenerated.value = true
  }

  await loadApps()

  // 查找当前应用名称
  const currentApp = myApps.value.find((app: AppVO) => String(app.id) === appId.value)
  if (currentApp?.appName) {
    appName.value = currentApp.appName
  }

  if (prompt.value && messages.value.length === 0) {
    // 有 prompt 说明是新生成的应用，需要生成代码
    messages.value.push({
      role: 'user',
      content: prompt.value,
    })
    await sendChatRequest(prompt.value)
  }
}

// 监听路由变化
watch(() => route.query.appId, (newAppId) => {
  if (newAppId) {
    initAppData(newAppId as string)
  }
})

onMounted(async () => {
  await initAppData()
})

const handleCodeGenTypeChange = (type: string) => {
  codeGenType.value = type
  refreshIframe()
}
</script>

<template>
  <div class="generate-page">
    <!-- 新 Header -->
    <header class="gen-header">
      <!-- 左侧：Logo + 应用名称 -->
      <div class="header-left">
        <div class="logo-section">
          <div
            class="logo-dropdown"
            @mouseenter="openMenu"
            @mouseleave="closeMenu"
          >
            <img src="/favicon-static.svg" alt="logo" class="logo" @click="router.push('/')" />
          </div>
          <!-- 应用名称：可编辑 -->
          <div class="app-name-wrapper">
            <template v-if="!isEditingName">
              <span class="app-name" @click="startEditName">{{ appName }}</span>
            </template>
            <template v-else>
              <input
                ref="appNameInput"
                v-model="editingAppName"
                class="app-name-input"
                @blur="saveAppName"
                @keydown.enter="saveAppName"
                @keydown.esc="cancelEditName"
              />
            </template>
          </div>
          <!-- 应用信息按钮 -->
          <div class="app-info-dropdown">
            <div
              class="info-btn"
              @mouseenter="openAppInfoMenu"
              @mouseleave="closeAppInfoMenu"
            >
              <CaretDownOutlined />
            </div>
            <div
              v-show="appInfoMenuOpen"
              class="app-info-menu"
              @mouseenter="openAppInfoMenu"
              @mouseleave="closeAppInfoMenu"
            >
              <div class="info-title">应用信息</div>
              <div class="info-row">
                <span class="info-label">应用名称</span>
                <span class="info-value">{{ appName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">应用 ID</span>
                <span class="info-value">{{ appId || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">代码类型</span>
                <span class="info-value">{{ codeGenType === 'multi-file' ? '多文件' : '单文件' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">初始需求</span>
                <span class="info-value">{{ prompt || '-' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 下拉菜单 -->
          <div
            v-show="appMenuOpen"
            class="app-menu"
            @mouseenter="openMenu"
            @mouseleave="closeMenu"
          >
              <!-- 新建任务 -->
              <div class="menu-item new-task" @click="createNew">
                <PlusOutlined />
                <span>新建任务</span>
              </div>

              <div class="menu-divider"></div>

              <!-- 我的应用列表 -->
              <div class="menu-section">
                <div class="menu-title">我的应用</div>
                <div v-if="loadingApps" class="menu-loading">
                  <LoadingOutlined />
                </div>
                <template v-else>
                  <template v-for="(apps, groupName) in groupedApps" :key="groupName">
                    <div v-if="apps.length > 0" class="app-group">
                      <div class="group-name">{{ groupName }}</div>
                      <div
                        v-for="app in apps"
                        :key="app.id"
                        :class="['menu-item app-item', { active: isCurrentApp(app) }]"
                        @click="switchApp(app)"
                      >
                        {{ app.appName || '未命名应用' }}
                      </div>
                    </div>
                  </template>
                  <div v-if="myApps.length === 0" class="menu-empty">暂无应用</div>
                </template>
              </div>

              <!-- 用户信息 -->
              <div class="menu-footer">
                <div class="user-info-card">
                  <!-- 第一行：头像 + 昵称 -->
                  <div class="user-row user-name-row">
                    <a-avatar :src="userStore.loginUser?.userAvatar" :size="32">
                      {{ userStore.loginUser?.userName?.[0] || 'U' }}
                    </a-avatar>
                    <span class="user-nickname">{{ userStore.loginUser?.userName || '用户' }}</span>
                  </div>
                  <!-- 第二行：对话进度条 -->
                  <div class="progress-group">
                    <div class="progress-header">
                      <span class="progress-label">对话次数</span>
                      <span class="progress-text">1/100</span>
                    </div>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 1%"></div>
                    </div>
                  </div>
                  <!-- 第三行：作品进度条 -->
                  <div class="progress-group">
                    <div class="progress-header">
                      <span class="progress-label">作品数量</span>
                      <span class="progress-text">4/10</span>
                    </div>
                    <div class="progress-bar">
                      <div class="progress-fill" style="width: 40%"></div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="header-actions">
          <button class="action-btn" @click="handleRefresh">
            <SyncOutlined />
            <span>刷新页面</span>
          </button>
          <button class="action-btn" @click="handleOpenInWindow">
            <GlobalOutlined />
            <span>在窗口打开</span>
          </button>
          <button class="action-btn" @click="handleFullscreen">
            <MenuFoldOutlined v-if="isCodePanelVisible" />
            <MenuUnfoldOutlined v-else />
            <span>{{ isCodePanelVisible ? '全屏预览' : '收起全屏' }}</span>
          </button>
        </div>
      </div>

      <!-- 右侧：下载 + 部署 -->
      <div class="header-right">
        <button class="header-btn secondary" :disabled="isDownloading" @click="handleDownload">
          <LoadingOutlined v-if="isDownloading" class="spinning" />
          <DownloadOutlined v-else />
          <span>{{ isDownloading ? '获取链接...' : '下载代码' }}</span>
        </button>
        <button class="header-btn primary" :disabled="isDeploying" @click="handleDeploy">
          <RocketOutlined v-if="!isDeploying" />
          <LoadingOutlined v-else class="spinning" />
          <span>{{ isDeploying ? '部署中...' : '部署' }}</span>
        </button>
      </div>
    </header>

    <!-- 主内容区域：左右布局 -->
    <div class="main-content">
      <!-- 左侧：代码生成区域 -->
      <div :class="['code-panel', { collapsed: !isCodePanelVisible }]">
      <div class="panel-header">
        <div class="header-left">
          <h2 class="panel-title">
            <LoadingOutlined v-if="isGenerating" class="spinning" />
            <CheckOutlined v-else class="success-icon" />
            代码生成
          </h2>
          <span v-if="isGenerating" class="status-text generating">生成中...</span>
          <span v-else class="status-text done">生成完成</span>
        </div>
      </div>

      <div ref="chatContainer" class="chat-messages" @scroll="onScroll">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message', msg.role]"
        >
          <!-- 用户消息 -->
          <div v-if="msg.role === 'user'" class="user-message">
            <div class="message-avatar user-avatar">
              <img
                v-if="userStore.loginUser?.userAvatar"
                :src="userStore.loginUser.userAvatar"
                alt="用户"
              />
              <UserOutlined v-else />
            </div>
            <div class="message-bubble user-bubble">
              <div class="bubble-content">{{ getMessageContent(msg) }}</div>
            </div>
          </div>

          <!-- AI 消息 -->
          <div v-else class="ai-message">
            <div class="message-avatar ai-avatar">
              <img src="/favicon-static.svg" alt="AI" />
            </div>
            <div class="message-bubble ai-bubble">
              <div class="bubble-header">
                <span class="ai-name">Muse AI</span>
                <button
                  v-if="getMessageContent(msg)"
                  class="copy-btn"
                  @click="copyCode(getMessageContent(msg))"
                >
                  <CopyOutlined />
                </button>
              </div>
              <div class="bubble-content markdown-content" v-html="renderMarkdown(getMessageContent(msg))"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            v-model="currentInput"
            class="chat-input"
            placeholder="继续描述你的需求，让 AI 完善代码..."
            rows="2"
            :disabled="isGenerating"
            @keydown.enter.prevent="!isGenerating && currentInput.trim() && handleSend()"
          ></textarea>
          <button
            class="send-btn"
            :class="{ active: currentInput.trim() && !isGenerating }"
            :disabled="!currentInput.trim() || isGenerating"
            @click="handleSend"
          >
            <SendOutlined v-if="!isGenerating" />
            <LoadingOutlined v-else class="spinning" />
          </button>
        </div>
      </div>
    </div>

    <!-- 右侧：预览区域 -->
    <div class="preview-panel">
      <div class="preview-content">
        <!-- AI 生成中提示 -->
        <div v-if="isGenerating" class="preview-generating">
          <div class="generating-animation">
            <div class="code-lines">
              <div class="line" style="width: 60%"></div>
              <div class="line" style="width: 80%"></div>
              <div class="line" style="width: 45%"></div>
              <div class="line" style="width: 70%"></div>
              <div class="line" style="width: 55%"></div>
            </div>
          </div>
          <p class="generating-text">AI 正在生成代码，请稍候...</p>
          <p class="generating-subtext">代码生成完成后将自动预览</p>
        </div>
        <!-- 首次进入未生成 -->
        <div v-else-if="!iframeUrl && !isCodeGenerated" class="preview-placeholder">
          <div class="placeholder-icon">
            <LoadingOutlined class="spinning" />
          </div>
          <p>请先在左侧描述需求，AI 将为您生成代码</p>
        </div>
        <!-- iframe 预览 -->
        <iframe
          v-else
          :key="iframeKey"
          :src="iframeUrl"
          class="preview-iframe"
          frameborder="0"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
        ></iframe>
      </div>
    </div>
    </div>
  </div>

    <!-- 部署成功弹窗 -->
    <Modal
      v-model:open="showDeployModal"
      title="部署成功"
      :footer="null"
      width="400"
      centered
    >
      <div class="deploy-modal-content">
        <div class="deploy-success-icon">
          <CheckOutlined />
        </div>
        <p class="deploy-success-text">部署成功！</p>
        <div class="deploy-url-box">
          <span class="deploy-url">{{ deployedUrl }}</span>
          <button class="copy-url-btn" @click="copyDeployUrl">
            <CopyOutlined />
          </button>
        </div>
        <div class="deploy-modal-actions">
          <button class="deploy-action-btn secondary" @click="showDeployModal = false">
            关闭
          </button>
          <button class="deploy-action-btn primary" @click="openDeployUrl">
            <GlobalOutlined />
            访问应用
          </button>
        </div>
      </div>
    </Modal>
</template>

<style scoped>
/* ===== CSS 变量 ===== */
.generate-page {
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

  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-primary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

/* 内容区域：左右布局 */
.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ===== Header ===== */
.gen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* Logo 下拉区域 */
.logo-dropdown {
  position: relative;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-dropdown {
  cursor: pointer;
  padding: 6px;
  border-radius: 8px;
  transition: background 0.2s;
}

.logo-dropdown:hover {
  background: var(--accent-green-dim);
}

.logo {
  width: 40px;
  height: 40px;
}

.app-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

/* 应用名称包装器：支持编辑 */
.app-name-wrapper {
  display: flex;
  align-items: center;
}

.app-name-wrapper .app-name {
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: all 0.2s;
}

.app-name-wrapper .app-name:hover {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.app-name-input {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  background: var(--bg-primary);
  border: 1px solid var(--accent-green);
  border-radius: 4px;
  padding: 2px 8px;
  outline: none;
  min-width: 150px;
}

/* 应用信息下拉 */
.app-info-dropdown {
  position: relative;
  margin-left: 4px;
}

.info-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.info-btn:hover {
  background: var(--accent-green-dim);
  color: var(--accent-green);
}

.app-info-menu {
  position: fixed;
  top: 56px;
  left: 200px;
  width: 280px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.5);
  padding: 12px 16px;
  z-index: 1001;
}

.info-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-color);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 8px 0;
}

.info-label {
  font-size: 12px;
  color: var(--text-muted);
  min-width: 60px;
}

.info-value {
  font-size: 13px;
  color: var(--text-secondary);
  text-align: right;
  word-break: break-all;
  max-width: 180px;
}

/* ===== 下拉菜单 ===== */
.app-menu {
  position: fixed;
  top: 56px;
  left: 0;
  bottom: 0;
  width: 280px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  box-shadow: 4px 0 32px rgba(0, 0, 0, 0.5);
  padding: 8px 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.menu-item:hover {
  background: var(--accent-green-dim);
  color: var(--text-primary);
}

.menu-item.new-task {
  color: var(--accent-green);
}

.menu-divider {
  height: 1px;
  background: var(--border-color);
  margin: 8px 0;
}

.menu-section {
  flex: 1;
  overflow-y: auto;
}

.menu-title {
  padding: 8px 16px;
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
}

.app-group {
  margin-bottom: 8px;
}

.group-name {
  padding: 8px 16px 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.menu-item.app-item {
  padding: 8px 16px 8px 24px;
  font-size: 14px;
}

.menu-item.app-item.active {
  background: var(--accent-green-dim);
  color: var(--accent-green);
}

.menu-loading {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
}

.menu-empty {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
}

/* ===== 用户信息卡片 ===== */
.menu-footer {
  border-top: 1px solid var(--border-color);
  padding: 12px 16px;
  flex-shrink: 0;
}

.user-info-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-secondary);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.user-row {
  display: flex;
  align-items: center;
}

.user-name-row {
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color);
}

.user-nickname {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.progress-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: 11px;
  color: var(--text-muted);
}

.progress-bar {
  height: 4px;
  background: var(--bg-primary);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00a854, var(--accent-green));
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 11px;
  color: var(--text-muted);
}

/* ===== 操作按钮 ===== */
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
  transition: all 0.2s;
}

.action-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

/* ===== Header 按钮 ===== */
.header-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  cursor: pointer;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.header-btn.secondary {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.header-btn.secondary:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

.header-btn.primary {
  background: var(--accent-green);
  color: var(--bg-primary);
  border: 1px solid var(--accent-green);
}

.header-btn.primary:hover:not(:disabled) {
  background: var(--bg-primary);
  color: var(--accent-green);
}

.header-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== 左侧代码面板 ===== */
.code-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--border-color);
  min-width: 400px;
  max-width: 600px;
  transition: all 0.3s ease;
  background: var(--bg-card);
}

/* 代码面板折叠状态 */
.code-panel.collapsed {
  min-width: 0;
  max-width: 0;
  overflow: hidden;
  border-right: none;
}

/* ===== 右侧预览面板 ===== */
.preview-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  flex-shrink: 0;
}

.preview-panel .panel-header {
  background: #f5f5f5;
  border-bottom: 1px solid #e5e5e5;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-panel .panel-title {
  color: #333;
}

.status-text {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-text.generating {
  color: var(--accent-green);
}

.status-text.done {
  color: #52c41a;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: var(--accent-green-dim);
  color: var(--accent-green);
}

.preview-panel .icon-btn {
  color: #666;
}

.preview-panel .icon-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: #333;
}

.type-btn {
  padding: 6px 12px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}

.type-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.type-btn.active {
  background: var(--accent-green);
  border-color: var(--accent-green);
  color: var(--bg-primary);
}

/* ===== 聊天消息区域 ===== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  background: var(--bg-primary);
}

.message {
  margin-bottom: 20px;
}

/* ===== 用户消息 ===== */
.user-message {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.user-message .message-avatar {
  order: 2;
}

.user-message .message-bubble {
  order: 1;
}

/* ===== AI 消息 ===== */
.ai-message {
  display: flex;
  gap: 12px;
}

/* ===== 头像 ===== */
.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-avatar {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.ai-avatar img {
  width: 24px;
  height: 24px;
}

/* ===== 消息气泡 ===== */
.message-bubble {
  max-width: calc(100% - 48px);
  border-radius: 12px;
  overflow: hidden;
}

.user-bubble {
  background: var(--accent-green);
}

.ai-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.bubble-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 8px;
  border-bottom: 1px solid var(--border-color);
}

.ai-bubble .bubble-header {
  border-bottom: none;
  padding: 12px 14px 8px;
}

.ai-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--accent-green);
}

.bubble-content {
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.user-bubble .bubble-content {
  color: var(--bg-primary);
  font-weight: 500;
}

/* ===== 复制按钮 ===== */
.copy-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.copy-btn:hover {
  background: var(--accent-green-dim);
  color: var(--accent-green);
}

/* ===== Markdown 内容样式 ===== */
.markdown-content {
  color: var(--text-primary);
}

.markdown-content :deep(p) {
  margin: 0 0 12px 0;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin: 16px 0 8px 0;
  font-weight: 600;
  color: var(--text-primary);
}

.markdown-content :deep(h1) { font-size: 18px; }
.markdown-content :deep(h2) { font-size: 16px; }
.markdown-content :deep(h3) { font-size: 14px; }

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(code) {
  background: var(--bg-primary);
  color: var(--accent-green);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 13px;
}

.markdown-content :deep(pre) {
  background: var(--bg-primary);
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  margin: 12px 0;
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
  font-size: 13px;
}

.markdown-content :deep(.hljs) {
  background: transparent;
  padding: 0;
}

.markdown-content :deep(a) {
  color: var(--accent-green);
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid var(--accent-green);
  padding-left: 12px;
  margin: 12px 0;
  color: var(--text-secondary);
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: var(--bg-secondary);
  font-weight: 600;
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 8px 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 16px 0;
}

/* ===== 输入区域 ===== */
.input-area {
  padding: 16px;
  background: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  background: var(--bg-card);
  border-radius: 12px;
  padding: 12px;
  border: 1px solid var(--border-color);
}

.input-wrapper:focus-within {
  border-color: var(--accent-green);
}

.chat-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

.chat-input::placeholder {
  color: var(--text-muted);
}

.chat-input:disabled {
  opacity: 0.6;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-muted);
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  flex-shrink: 0;
}

.send-btn.active {
  border-color: var(--accent-green);
  background: var(--accent-green-dim);
  color: var(--accent-green);
  cursor: pointer;
}

.send-btn.active:hover {
  background: var(--accent-green);
  color: var(--bg-primary);
}

/* ===== 预览区域 ===== */
.preview-content {
  flex: 1;
  position: relative;
  background: #f0f0f0;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.preview-placeholder .large {
  font-size: 32px;
  margin-bottom: 16px;
}

.placeholder-icon {
  font-size: 48px;
  color: var(--accent-green);
  margin-bottom: 16px;
}

/* ===== AI 生成中提示 ===== */
.preview-generating {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, var(--bg-card) 0%, var(--bg-secondary) 100%);
  padding: 40px;
}

.generating-animation {
  width: 200px;
  height: 120px;
  background: var(--bg-primary);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
}

.generating-animation::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 24px;
  background: var(--bg-card);
  border-radius: 12px 12px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.generating-animation::after {
  content: '';
  position: absolute;
  top: 8px;
  left: 12px;
  width: 8px;
  height: 8px;
  background: #ff5f57;
  border-radius: 50%;
  box-shadow: 16px 0 0 #ffbd2e, 32px 0 0 #28c940;
}

.code-lines {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 20px;
}

.code-lines .line {
  height: 6px;
  background: var(--accent-green);
  border-radius: 3px;
  animation: typing 1.5s infinite ease-in-out;
  opacity: 0;
}

.code-lines .line:nth-child(1) { animation-delay: 0s; }
.code-lines .line:nth-child(2) { animation-delay: 0.2s; }
.code-lines .line:nth-child(3) { animation-delay: 0.4s; }
.code-lines .line:nth-child(4) { animation-delay: 0.6s; }
.code-lines .line:nth-child(5) { animation-delay: 0.8s; }

@keyframes typing {
  0% {
    opacity: 0;
    transform: translateX(-10px);
  }
  50% {
    opacity: 1;
    transform: translateX(0);
  }
  100% {
    opacity: 0;
    transform: translateX(10px);
  }
}

.generating-text {
  margin-top: 32px;
  font-size: 18px;
  font-weight: 500;
  color: var(--accent-green);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.generating-subtext {
  margin-top: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.preview-iframe {
  width: 100%;
  height: 100%;
  background: #fff;
}

/* ===== 滚动条 ===== */
.chat-messages::-webkit-scrollbar,
.message-text::-webkit-scrollbar,
.menu-section::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.chat-messages::-webkit-scrollbar-track,
.message-text::-webkit-scrollbar-track,
.menu-section::-webkit-scrollbar-track {
  background: var(--bg-primary);
}

.chat-messages::-webkit-scrollbar-thumb,
.message-text::-webkit-scrollbar-thumb,
.menu-section::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.message-text::-webkit-scrollbar-thumb:hover,
.menu-section::-webkit-scrollbar-thumb:hover {
  background: var(--accent-green);
}

/* ===== 动画 ===== */
.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.success-icon {
  color: var(--accent-green);
}

/* ===== 部署成功弹窗 ===== */
.deploy-modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.deploy-success-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--accent-green-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-green);
  font-size: 24px;
  margin-bottom: 12px;
}

.deploy-success-text {
  font-size: 15px;
  color: var(--text-primary);
  margin: 0 0 16px 0;
}

.deploy-url-box {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  margin-bottom: 16px;
}

.deploy-url {
  flex: 1;
  font-size: 12px;
  color: var(--text-secondary);
  word-break: break-all;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.copy-url-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.copy-url-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

.deploy-modal-actions {
  display: flex;
  gap: 10px;
  width: 100%;
}

.deploy-action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.deploy-action-btn.secondary {
  background: var(--bg-card);
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.deploy-action-btn.secondary:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.deploy-action-btn.primary {
  background: var(--accent-green);
  color: var(--bg-primary);
}

.deploy-action-btn.primary:hover {
  opacity: 0.9;
}
</style>
