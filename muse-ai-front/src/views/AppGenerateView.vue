<script setup lang="ts">
import { ref, onMounted, computed, nextTick, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal, Alert } from 'ant-design-vue'
import {
  SendOutlined,
  LoadingOutlined,
  CheckOutlined,
  CloseOutlined,
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
  EditOutlined,
  HighlightOutlined,
} from '@ant-design/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'
import { API_BASE_URL } from '@/request'
import { useUserStore } from '@/stores/user'
import { listMyApps, deployApp, updateAppName, downloadApp, getAppDetail } from '@/api/appController'
import { getHistory } from '@/api/historyController'
import type { AppVO, ToolExecutionRequest } from '@/api/typings.d'
import { useVisualEditor } from '@/composables/useVisualEditor'

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
const appInitPrompt = ref<string>('')
const appOwnerId = ref<string>('')
const prompt = ref<string>(route.query.prompt as string || '')
const codeGenType = ref<string>('multi-file')

// 聊天历史加载状态
const isLoadingHistory = ref(false)
const hasMoreHistory = ref(true)
const oldestHistoryId = ref<number>(0)
const historyTotalCount = ref<number>(0)

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
      id: appId.value,
      appName: newName,
    })
    if (res.data.code === 0) {
      appName.value = newName
      message.success('名称修改成功')
    }
  } catch (error) {
    // 错误已由全局拦截器处理
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

// 消息类型
type MessageType = 'user' | 'assistant' | 'tool_request' | 'tool_executed'

// 工具请求消息
interface ToolRequestMessage {
  type: 'tool_request'
  toolName: string
  toolArguments?: string
  toolId?: string
}

// 工具执行结果消息
interface ToolExecutedMessage {
  type: 'tool_executed'
  toolName: string
  toolOutput: string
  toolArguments?: string
  toolId?: string
  success?: boolean  // 工具执行是否成功
}

interface Message {
  id?: string
  role: MessageType
  content: MessageContent
  thinking?: string  // AI思考过程
  toolRequest?: ToolRequestMessage[]
  toolExecuted?: ToolExecutedMessage
}

// 消息 ID 计数器，用于生成唯一 ID
let messageIdCounter = 0

// 生成消息唯一 ID
const generateMessageId = (): string => {
  return `msg_${Date.now()}_${messageIdCounter++}`
}

const messages = ref<Message[]>([])

// 工具参数折叠状态
const collapsedToolArgs = ref<Set<string>>(new Set())

// 切换工具参数折叠状态
const toggleToolArgs = (index: number | string) => {
  const key = String(index)
  if (collapsedToolArgs.value.has(key)) {
    collapsedToolArgs.value.delete(key)
  } else {
    collapsedToolArgs.value.add(key)
  }
  // 触发响应式更新
  collapsedToolArgs.value = new Set(collapsedToolArgs.value)
}
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

  // 检测是否滚动到顶部，触发加载更多历史记录
  if (currentScrollTop === 0 && hasMoreHistory.value && !isLoadingHistory.value) {
    // 保存当前滚动位置，防止加载后跳转
    const oldScrollHeight = chatContainer.value.scrollHeight
    loadHistory(true).then(() => {
      // 加载完成后恢复滚动位置
      nextTick(() => {
        if (chatContainer.value) {
          const newScrollHeight = chatContainer.value.scrollHeight
          chatContainer.value.scrollTop = newScrollHeight - oldScrollHeight
        }
      })
    })
  }

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
const pageLoadError = ref(false)

// 判断当前用户是否是应用所有者
const isAppOwner = computed(() => {
  return userStore.isLogin && userStore.loginUser?.id === appOwnerId.value
})

// 判断是否应该显示预览
const shouldShowPreview = computed(() => {
  return messages.value.length > 0 && !isGenerating.value && previewStatus.value === 'ready'
})

// 判断是否显示"尚未生成完毕"提示
const showNotReadyMessage = computed(() => {
  return messages.value.length === 0 && !isGenerating.value && !isAppOwner.value
})

// iframe 预览
const iframeUrl = computed(() => {
  if (!appId.value) return ''
  if (codeGenType.value === 'multi-file') {
    return `${API_BASE_URL}/code/vue/${appId.value}/dist/index.html`
  } else {
    return `${API_BASE_URL}/code/html/${appId.value}/index.html`
  }
})

// iframe 加载错误处理
const handleIframeError = () => {
  pageLoadError.value = true
}

// iframe 加载成功处理
const handleIframeLoad = () => {
  pageLoadError.value = false
}

// 显示预览（代码生成完成后调用）
const showPreview = () => {
  if (shouldShowPreview.value) {
    refreshIframe()
  }
}

// iframe 刷新触发
const iframeKey = ref(0)
const iframeRef = ref<HTMLIFrameElement | null>(null)
const refreshIframe = () => {
  iframeKey.value++
  checkPreviewAvailability()
}

// 预览状态：'idle' - 未检查, 'checking' - 检查中, 'ready' - 可用, 'not_ready' - 不可用
const previewStatus = ref<'idle' | 'checking' | 'ready' | 'not_ready'>('idle')

// 检查预览是否可用
const checkPreviewAvailability = async () => {
  if (!iframeUrl.value) {
    previewStatus.value = 'not_ready'
    return
  }

  previewStatus.value = 'checking'
  try {
    // 发起请求检查预览是否可用
    const response = await fetch(iframeUrl.value, {
      method: 'GET',
      cache: 'no-store',
    })

    // 检查状态码，只有200-299才认为可用
    if (response.ok) {
      previewStatus.value = 'ready'
      pageLoadError.value = false
    } else {
      // 404、500等错误状态
      previewStatus.value = 'not_ready'
      console.log('预览不可用，状态码:', response.status)
    }
  } catch (error) {
    // 网络错误等
    previewStatus.value = 'not_ready'
    console.log('预览不可用:', error)
  }
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
  return app.id === appId.value
}

// 切换应用
const switchApp = (app: AppVO) => {
  if (!app.id) return
  appMenuOpen.value = false
  router.push({
    path: '/app/generate',
    query: { appId: app.id },
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
    }
  } catch (error) {
    // 错误已由全局拦截器处理
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
    }
  } catch (error) {
    console.error('部署异常:', error)
    // 错误已由全局拦截器处理
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
      const currentApp = appList.find((app: AppVO) => app.id === appId.value)
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

  // 如果有选中的元素，增强提示词
  let enhancedMessage = text
  if (visualEditor.selectedElement.value) {
    enhancedMessage = visualEditor.getEnhancedPrompt(text)
  }

  messages.value.push({
    role: 'user',
    content: text, // 显示原始用户输入
  })
  currentInput.value = ''

  // 发送增强后的消息
  await sendChatRequest(enhancedMessage)

  // 发送后清除选中状态并退出编辑模式
  if (visualEditor.isEditMode.value) {
    visualEditor.clearSelection()
    visualEditor.disableEditMode()
  }
}

// 发送聊天请求
const sendChatRequest = async (userMessage: string) => {
  if (!appId.value) {
    message.error('应用 ID 不存在')
    return
  }

  isGenerating.value = true
  pageLoadError.value = false // 重置页面加载错误状态
  previewStatus.value = 'idle' // 重置预览状态
  userScrolledUp = false // 重置滚动状态
  // 等待DOM更新后滚动到底部
  await nextTick()
  scrollToBottom()

  let currentResponse = ref('')
  let currentThinking = ref('')  // 思考过程累积
  let inThinking = false  // 是否在思考标签内
  let thinkingBuffer = ''  // 思考内容缓冲
  let hasToolExecuted = false  // 是否已经执行过工具（用于判断是否需要创建新气泡）
  let hasCreatedAIMessageAfterTool = false  // 在工具执行后是否已经创建过 AI 气泡（避免连续 TOOL_EXECUTED 创建多余气泡）

  messages.value.push({
    id: generateMessageId(),
    role: 'assistant',
    content: currentResponse,
  })

  // 获取当前AI消息的引用，用于更新思考内容
  const getCurrentAIMessage = () => {
    return messages.value[messages.value.length - 1]
  }

  // 开始新的AI回复气泡（仅当当前气泡有内容时才创建新气泡）
  const startNewAIMessage = () => {
    // 检查最后一个消息是否是空的 AI 气泡
    const lastMsg = messages.value[messages.value.length - 1]
    const isEmptyAIBubble = lastMsg && lastMsg.role === 'assistant' && !getMessageContent(lastMsg) && !lastMsg.thinking

    // 如果最后是空 AI 气泡，重用它的响应引用；否则创建新气泡
    if (isEmptyAIBubble) {
      currentResponse = lastMsg.content as Ref<string>
    } else {
      currentResponse = ref('')
      messages.value.push({
        id: generateMessageId(),
        role: 'assistant',
        content: currentResponse,
      })
    }
    hasToolExecuted = false  // 重置标志
    inThinking = false  // 重置思考状态
    thinkingBuffer = ''  // 重置思考缓冲
  }

  // 在工具执行后准备接收新的 AI 消息（延迟创建气泡）
  const startAIMessageAfterTool = () => {
    // 如果已经创建过预备气泡，直接重用
    if (hasCreatedAIMessageAfterTool) {
      return
    }

    // 只设置标志，延迟创建实际气泡
    // 不立即创建空气泡，等待 processTextContent 中有内容时再创建
    hasCreatedAIMessageAfterTool = true
    // 注意：不重置 hasToolExecuted，让 processTextContent 检测到并创建新气泡
    inThinking = false  // 重置思考状态
    thinkingBuffer = ''  // 重置思考缓冲

    // 注意：不在这里创建气泡，而是让 processTextContent 在有内容时调用 startNewAIMessage
  }

  // 处理文本内容，分离思考过程和普通内容
  const processTextContent = (text: string) => {
    // 如果在工具执行后收到TEXT，先确保有消息对象
    const thinkTag = String.raw`\x3Cthink\x3E`
    if (hasToolExecuted && !inThinking) {
      // 工具执行后，确保有消息对象来接收内容（无论是否包含思考标签）
      startNewAIMessage()
      hasCreatedAIMessageAfterTool = false
    }
    let result = ''
    let remaining = text
    const startTag = '<think>'
    const endTag = '</think>'

    while (remaining.length > 0) {
      if (inThinking) {
        // 查找思考结束标签
        const endTagIndex = remaining.indexOf(endTag)
        if (endTagIndex !== -1) {
          // 找到结束标签，结束思考模式
          thinkingBuffer += remaining.substring(0, endTagIndex)
          inThinking = false
          // 更新消息的思考内容
          const msg = getCurrentAIMessage()
          if (msg) {
            msg.thinking = thinkingBuffer.trim()
            msg._thinkingCollapsed = false  // 默认展开
          }
          thinkingBuffer = ''
          remaining = remaining.substring(endTagIndex + endTag.length)  // 跳过 </think>
        } else {
          // 整个剩余内容都是思考内容
          thinkingBuffer += remaining
          // 实时更新思考内容
          const msg = getCurrentAIMessage()
          if (msg) {
            msg.thinking = thinkingBuffer.trim()
            msg._thinkingCollapsed = false
          }
          remaining = ''
        }
      } else {
        // 查找思考开始标签
        const startTagIndex = remaining.indexOf(startTag)
        if (startTagIndex !== -1) {
          // 找到开始标签，将前面的内容作为普通内容
          result += remaining.substring(0, startTagIndex)
          inThinking = true
          remaining = remaining.substring(startTagIndex + startTag.length)  // 跳过 <think>
        } else {
          // 没有思考标签，全部作为普通内容
          result += remaining
          remaining = ''
        }
      }
    }

    return result
  }

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
      // HTTP 错误
      let errorMsg = `请求失败: ${response.status}`
      try {
        const errorData = await response.json()
        if (errorData.message) {
          errorMsg = errorData.message
        }
      } catch (e) {
        // 忽略 JSON 解析错误
      }
      throw new Error(errorMsg)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    // 先读取第一块数据，检查是否是错误响应
    const { done: firstDone, value: firstValue } = await reader.read()
    if (firstValue) {
      buffer += decoder.decode(firstValue, { stream: true })
    }

    // 检查第一行是否是业务错误响应
    const firstLine = buffer.split('\n')[0].trim()
    if (firstLine) {
      let jsonStr = firstLine
      if (jsonStr.startsWith('data:')) {
        jsonStr = jsonStr.slice(5).trim()
      }
      try {
        const parsed = JSON.parse(jsonStr)
        // 检查是否是业务错误响应
        if (parsed.code !== undefined && parsed.code !== 0) {
          message.error(parsed.message || '操作失败')
          isGenerating.value = false
          messages.value.pop() // 移除刚才添加的 assistant 消息
          return
        }
        // 处理标准消息格式
        if (parsed.jsonViewType !== undefined) {
          switch (parsed.jsonViewType) {
            case 'TEXT':
              if (parsed.v !== undefined) {
                const processedContent = processTextContent(parsed.v)
                currentResponse.value += processedContent
                smartScroll()
              }
              break
            case 'FINISH':
              isGenerating.value = false
              break
            case 'TOOL_REQUEST':
              if (parsed.toolName) {
                // 将工具请求附加到当前 AI 消息
                const currentMsg = getCurrentAIMessage()
                if (currentMsg && currentMsg.role === 'assistant') {
                  if (!currentMsg.toolRequest) {
                    currentMsg.toolRequest = []
                  }
                  const msgId = currentMsg.id
                  const toolReqIndex = currentMsg.toolRequest.length
                  currentMsg.toolRequest.push({
                    type: 'tool_request',
                    toolName: parsed.toolName,
                    toolArguments: parsed.v,
                    toolId: parsed.toolId,
                  })
                  // 如果有参数，默认收起
                  if (parsed.v) {
                    collapsedToolArgs.value.add(String(msgId + '-req-' + toolReqIndex))
                    collapsedToolArgs.value = new Set(collapsedToolArgs.value)
                  }
                }
                smartScroll()
              }
              break
            case 'TOOL_EXECUTED':
              if (parsed.toolName) {
                hasToolExecuted = true  // 标记已执行工具
                // 处理 success 字段：支持布尔值和字符串
                let success = false
                if (parsed.success === true || parsed.success === 'true') {
                  success = true
                } else if (parsed.success === false || parsed.success === 'false') {
                  success = false
                }
                const toolExecutedMsg: ToolExecutedMessage = {
                  type: 'tool_executed',
                  toolName: parsed.toolName,
                  toolOutput: parsed.toolOutput || parsed.toolResult || '',  // 支持 toolOutput 和 toolResult
                  toolArguments: parsed.v,
                  toolId: parsed.toolId,
                  success,  // 解析成功状态
                }
                const msgId = generateMessageId()
                messages.value.push({
                  id: msgId,
                  role: 'tool_executed',
                  content: '',
                  toolExecuted: toolExecutedMsg,
                })
                // 如果有参数，默认收起
                if (parsed.v) {
                  collapsedToolArgs.value.add(msgId)
                  collapsedToolArgs.value = new Set(collapsedToolArgs.value)
                }
                // 创建新的 assistant 消息，为下一个 TEXT 流做准备
                startAIMessageAfterTool()
                smartScroll()
              }
              break
          }
        } else if (parsed.v !== undefined) {
          // 兼容旧格式
          currentResponse.value += parsed.v
          smartScroll()
        }
      } catch {
        // 不是 JSON，当作普通文本处理
      }
    }

    // 如果没有更多数据，直接返回
    if (firstDone) {
      isGenerating.value = false
      return
    }

    // 继续处理剩余的流式响应
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

          // 处理标准消息格式
          if (parsed.jsonViewType !== undefined) {
            switch (parsed.jsonViewType) {
              case 'TEXT':
                // 文本消息，流式输出
                if (parsed.v !== undefined) {
                  const processedContent = processTextContent(parsed.v)
                  currentResponse.value += processedContent
                  smartScroll()
                }
                break
              case 'FINISH':
                // 消息结束
                isGenerating.value = false
                break
              case 'TOOL_REQUEST':
                // AI申请调用工具 - 附加到当前 AI 消息
                if (parsed.toolName) {
                  const currentMsg = getCurrentAIMessage()
                  if (currentMsg && currentMsg.role === 'assistant') {
                    if (!currentMsg.toolRequest) {
                      currentMsg.toolRequest = []
                    }
                    const msgId = currentMsg.id
                    const toolReqIndex = currentMsg.toolRequest.length
                    currentMsg.toolRequest.push({
                      type: 'tool_request',
                      toolName: parsed.toolName,
                      toolId: parsed.toolId,
                    })
                    // 如果有参数，默认收起
                    if (parsed.v && shouldCollapseArgs(parsed.v)) {
                      collapsedToolArgs.value.add(String(msgId + '-req-' + toolReqIndex))
                    }
                  }
                  smartScroll()
                }
                break
              case 'TOOL_EXECUTED':
                // 工具调用结果
                if (parsed.toolName) {
                  hasToolExecuted = true  // 标记已执行工具
                  // 处理 success 字段：支持布尔值和字符串
                  let success = false
                  if (parsed.success === true || parsed.success === 'true') {
                    success = true
                  } else if (parsed.success === false || parsed.success === 'false') {
                    success = false
                  }
                  const toolExecutedMsg: ToolExecutedMessage = {
                    type: 'tool_executed',
                    toolName: parsed.toolName,
                    toolOutput: parsed.toolOutput || parsed.toolResult || '',  // 支持 toolOutput 和 toolResult
                    toolArguments: parsed.v,
                    toolId: parsed.toolId,
                    success,  // 解析成功状态
                  }
                  // 添加工具执行结果消息
                  const msgId = generateMessageId()
                  messages.value.push({
                    id: msgId,
                    role: 'tool_executed',
                    content: '',
                    toolExecuted: toolExecutedMsg,
                  })
                  // 如果参数很长，默认折叠
                  if (parsed.v && shouldCollapseArgs(parsed.v)) {
                    collapsedToolArgs.value.add(msgId)
                    collapsedToolArgs.value = new Set(collapsedToolArgs.value)
                  }
                  // 创建新的 assistant 消息，为下一个 TEXT 流做准备
                  startAIMessageAfterTool()
                  smartScroll()
                }
                break
            }
          } else if (parsed.v !== undefined) {
            // 兼容旧格式
            currentResponse.value += parsed.v
            smartScroll()
          } else if (parsed.e === 'end') {
            // 兼容旧格式
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
        if (parsed.jsonViewType !== undefined) {
          switch (parsed.jsonViewType) {
            case 'TEXT':
              if (parsed.v !== undefined) {
                const processedContent = processTextContent(parsed.v)
                currentResponse.value += processedContent
              }
              break
            case 'FINISH':
              isGenerating.value = false
              break
            case 'TOOL_REQUEST':
              if (parsed.toolName) {
                // 将工具请求附加到当前 AI 消息
                const currentMsg = getCurrentAIMessage()
                if (currentMsg && currentMsg.role === 'assistant') {
                  if (!currentMsg.toolRequest) {
                    currentMsg.toolRequest = []
                  }
                  const msgId = currentMsg.id
                  const toolReqIndex = currentMsg.toolRequest.length
                  currentMsg.toolRequest.push({
                    type: 'tool_request',
                    toolName: parsed.toolName,
                    toolArguments: parsed.v,
                    toolId: parsed.toolId,
                  })
                  // 如果有参数，默认收起
                  if (parsed.v) {
                    collapsedToolArgs.value.add(String(msgId + '-req-' + toolReqIndex))
                    collapsedToolArgs.value = new Set(collapsedToolArgs.value)
                  }
                }
              }
              break
            case 'TOOL_EXECUTED':
              if (parsed.toolName) {
                hasToolExecuted = true  // 标记已执行工具
                // 处理 success 字段：支持布尔值和字符串
                let success = false
                if (parsed.success === true || parsed.success === 'true') {
                  success = true
                } else if (parsed.success === false || parsed.success === 'false') {
                  success = false
                }
                const toolExecutedMsg: ToolExecutedMessage = {
                  type: 'tool_executed',
                  toolName: parsed.toolName,
                  toolOutput: parsed.toolOutput || parsed.toolResult || '',  // 支持 toolOutput 和 toolResult
                  toolArguments: parsed.v,
                  toolId: parsed.toolId,
                  success,  // 解析成功状态
                }
                const msgId = generateMessageId()
                messages.value.push({
                  id: msgId,
                  role: 'tool_executed',
                  content: '',
                  toolExecuted: toolExecutedMsg,
                })
                // 如果有参数，默认收起
                if (parsed.v) {
                  collapsedToolArgs.value.add(msgId)
                  collapsedToolArgs.value = new Set(collapsedToolArgs.value)
                }
                // 创建新的 assistant 消息，为下一个 TEXT 流做准备
                startAIMessageAfterTool()
              }
              break
          }
        } else if (parsed.v !== undefined) {
          const processedContent = processTextContent(parsed.v)
          currentResponse.value += processedContent
        }
      } catch {
        // 非 JSON 数据，直接处理
        const processedContent = processTextContent(jsonStr)
        currentResponse.value += processedContent
      }
    }
  } catch (error: any) {
    console.error('生成失败', error)
    message.error(error.message || '生成失败，请重试')
    currentResponse.value += `\n[生成失败: ${error.message}]`
  } finally {
    // 如果还在思考标签内，需要结束并保存思考内容
    if (inThinking && thinkingBuffer) {
      const msg = getCurrentAIMessage()
      if (msg) {
        msg.thinking = thinkingBuffer.trim()
        msg._thinkingCollapsed = false
      }
    }

    isGenerating.value = false
    isCodeGenerated.value = true
    // 生成完成后尝试显示预览
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

// 提取思考过程 - 从历史消息中提取
const extractThinking = (content: string): { thinking: string; content: string } => {
  // 匹配 ...

  const startTag = '<think>'
  const endTag = '</think>'
  const startIdx = content.indexOf(startTag)
  const endIdx = content.indexOf(endTag)

  if (startIdx !== -1 && endIdx !== -1 && endIdx > startIdx) {
    const thinkingContent = content.substring(startIdx + startTag.length, endIdx)
    return {
      thinking: thinkingContent.trim(),
      content: (content.substring(0, startIdx) + content.substring(endIdx + endTag.length)).trim()
    }
  }
  return { thinking: '', content }
}

// 判断是否应该折叠参数（长度超过200字符）
const shouldCollapseArgs = (args: string): boolean => {
  if (!args) return false
  return args.length > 200
}

// 格式化工具参数为漂亮的 JSON 格式
const formatToolArguments = (toolArgs: string): string => {
  if (!toolArgs) return ''
  try {
    // 先解析 JSON 字符串
    const parsed = JSON.parse(toolArgs)
    // 再格式化为带缩进的 JSON，\n 会成为真正的换行符
    return JSON.stringify(parsed, null, 2)
  } catch {
    // 解析失败则返回原字符串
    return toolArgs
  }
}

// 格式化工具参数（截断版本，用于折叠时显示）
const formatToolArgumentsPreview = (toolArgs: string): string => {
  const formatted = formatToolArguments(toolArgs)
  return formatted.length > 150 ? formatted.substring(0, 150) + '...' : formatted
}

// 加载聊天历史记录
const loadHistory = async (loadMore = false) => {
  if (!appId.value || isLoadingHistory.value) return

  isLoadingHistory.value = true

  try {
    const res = await getHistory({
      historyQueryRequest: {
        appId: appId.value,
        pageSize: 10,
        lastId: loadMore && oldestHistoryId.value ? oldestHistoryId.value : undefined,
      },
    })

    if (res.data.code === 0 && res.data.data) {
      const historyList = res.data.data.list || []
      historyTotalCount.value = res.data.data.total || 0

      if (historyList.length > 0) {
        // 后端返回倒序数据（最新的在前），需要反转成正序（从旧到新）
        const reversedList = [...historyList].reverse()

        // 更新最老消息的ID（用于分页）- 反转后第一个元素是最早的
        if (reversedList.length > 0) {
          oldestHistoryId.value = reversedList[0].id || 0
        }

        // 将历史记录转换为消息格式
        const newMessages: Message[] = []

        reversedList.forEach((item) => {
          const messageType = item.messageType

          if (messageType === 'USER') {
            // 用户消息
            newMessages.push({
              id: String(item.id),
              role: 'user',
              content: item.message || '',
            })
          } else if (messageType === 'AI') {
            // AI消息 - 提取思考内容
            const rawContent = item.message || ''
            const { thinking, content } = extractThinking(rawContent)
            const aiMessage: Message = {
              id: String(item.id),
              role: 'assistant',
              content: content,
            }
            // 如果有思考内容，添加到消息中
            if (thinking) {
              aiMessage.thinking = thinking
              aiMessage._thinkingCollapsed = false  // 默认展开
            }

            // 解析 toolExecutionRequests，附加到 AI 消息的 toolRequest 字段
            if (item.toolExecutionRequests) {
              try {
                const toolRequests = JSON.parse(item.toolExecutionRequests) as ToolExecutionRequest[]
                if (Array.isArray(toolRequests) && toolRequests.length > 0) {
                  aiMessage.toolRequest = toolRequests
                    .filter(req => req.toolName)
                    .map((req, idx) => {
                      // 如果有参数，默认收起 - 使用消息 ID
                      if (req.arguments) {
                        const key = `${aiMessage.id}-req-${idx}`
                        collapsedToolArgs.value.add(key)
                      }
                      return {
                        type: 'tool_request' as const,
                        toolName: req.toolName || '',
                        toolArguments: req.arguments,
                        toolId: req.toolId,
                      }
                    })
                }
              } catch (e) {
                console.error('解析 toolExecutionRequests 失败', e)
              }
            }

            newMessages.push(aiMessage)
          } else if (messageType === 'TOOL_EXECUTED') {
            // 工具执行结果消息
            // message 字段包含 JSON: { toolName, toolOutput/toolResult, arguments, toolId, success }
            let toolName = '未知工具'
            let toolOutput = ''
            let toolArguments: string | undefined
            let toolId: string | undefined
            let success: boolean | undefined

            try {
              const parsed = JSON.parse(item.message || '{}')
              if (parsed.toolName) toolName = parsed.toolName
              if (parsed.toolOutput) toolOutput = parsed.toolOutput
              if (parsed.toolResult) toolOutput = parsed.toolResult  // 支持 toolResult 字段
              if (parsed.arguments) toolArguments = parsed.arguments
              if (parsed.toolId) toolId = parsed.toolId
              if (parsed.success !== undefined) success = parsed.success === true
            } catch {
              // 解析失败，使用默认值
            }

            // 如果有参数，默认收起 - 使用消息 ID
            if (toolArguments) {
              collapsedToolArgs.value.add(String(item.id))
            }

            newMessages.push({
              id: String(item.id),
              role: 'tool_executed' as const,
              content: '',
              toolExecuted: {
                type: 'tool_executed',
                toolName,
                toolOutput,
                toolArguments,
                toolId,
                success,  // 添加成功状态
              },
            })
          }
        })

        if (loadMore) {
          // 使用消息 ID 后，不再需要索引更新逻辑
          // 加载更多：将新消息插入到数组开头
          messages.value = [...newMessages, ...messages.value]
        } else {
          // 首次加载：直接设置消息列表
          messages.value = newMessages
          // 首次加载后滚动到底部
          await nextTick()
          scrollToBottom()
        }

        // 判断是否还有更多历史记录
        hasMoreHistory.value = historyList.length === 10 && messages.value.length < historyTotalCount.value
      } else {
        hasMoreHistory.value = false
      }

      return historyList.length > 0
    }
    return false
  } catch (error: any) {
    console.error('加载聊天历史失败', error)
    // 错误已由全局拦截器处理
    return false
  } finally {
    isLoadingHistory.value = false
  }
}

// 初始化应用数据
const initAppData = async (newAppId?: string) => {
  const targetAppId = newAppId || route.query.appId as string
  const isSwitchingApp = targetAppId && targetAppId !== appId.value

  if (isSwitchingApp) {
    appId.value = targetAppId
    messages.value = []
    hasMoreHistory.value = true
    oldestHistoryId.value = 0
    historyTotalCount.value = 0
    previewStatus.value = 'idle'  // 重置预览状态
    pageLoadError.value = false
  }

  // 加载应用列表
  await loadApps()

  // 获取当前应用详细信息
  const currentApp = myApps.value.find((app: AppVO) => String(app.id) === appId.value)
  if (currentApp) {
    appName.value = currentApp.appName || '未命名应用'
    appInitPrompt.value = currentApp.initPrompt || ''
    appOwnerId.value = currentApp.userId || ''
  } else {
    // 如果应用列表中没有，尝试单独获取
    try {
      const res = await getAppDetail({ id: appId.value })
      if (res.data.code === 0 && res.data.data) {
        const appDetail = res.data.data
        appName.value = appDetail.appName || '未命名应用'
        appInitPrompt.value = appDetail.initPrompt || ''
        appOwnerId.value = appDetail.userId || ''
      }
    } catch (error: any) {
      console.error('获取应用详情失败', error)
      // 错误已由全局拦截器处理
    }
  }

  // 加载聊天历史记录
  const hasHistory = await loadHistory()

  // 判断是否需要自动触发 AI 生成
  const isLoggedIn = userStore.isLogin
  const isOwner = isLoggedIn && userStore.loginUser?.id === appOwnerId.value
  const shouldAutoGenerate = !hasHistory && isOwner && appInitPrompt.value

  if (shouldAutoGenerate) {
    // 没有历史记录且是应用所有者，使用 initPrompt 触发生成
    messages.value.push({
      role: 'user',
      content: appInitPrompt.value,
    })
    await sendChatRequest(appInitPrompt.value)
  } else if (hasHistory) {
    // 有历史记录，检查预览是否可用
    await checkPreviewAvailability()
  }
}

// 监听路由变化
watch(() => route.query.appId, (newAppId) => {
  if (newAppId) {
    initAppData(newAppId as string)
  }
})

// 可视化编辑器
const visualEditor = useVisualEditor(iframeRef)

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
          <div v-else-if="msg.role === 'assistant'" class="ai-message">
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
              <!-- 思考过程 -->
              <div v-if="msg.thinking" class="thinking-section">
                <div class="thinking-label">思考</div>
                <div class="thinking-content markdown-content" v-html="renderMarkdown(msg.thinking)"></div>
              </div>
              <!-- 消息内容 -->
              <template v-if="getMessageContent(msg)">
                <div class="bubble-content markdown-content" v-html="renderMarkdown(getMessageContent(msg))"></div>
              </template>
              <div v-else-if="msg.thinking" class="bubble-content-spacer"></div>

              <!-- 工具调用请求 -->
              <div v-if="msg.toolRequest && msg.toolRequest.length > 0" class="ai-tool-requests">
                <div v-for="(tool, idx) in msg.toolRequest" :key="idx" class="tool-request-item-inline-compact">
                  <div class="tool-request-header-compact">
                    <span class="tool-request-name">🔧 {{ tool.toolName }}</span>
                    <span class="tool-badge">REQUEST</span>
                  </div>
                  <!-- 可折叠的参数 -->
                  <div v-if="tool.toolArguments" class="tool-request-arguments-compact">
                    <div class="tool-arguments-header">
                      <span class="tool-arguments-label">📋 参数</span>
                      <span
                        v-if="shouldCollapseArgs(tool.toolArguments)"
                        class="tool-arguments-toggle"
                        @click="toggleToolArgs(msg.id + '-req-' + idx)"
                      >
                        {{ collapsedToolArgs.has(String(msg.id + '-req-' + idx)) ? '展开' : '收起' }}
                      </span>
                    </div>
                    <div class="tool-arguments-value">
                      <!-- 折叠时显示截断版本 -->
                      <template v-if="shouldCollapseArgs(tool.toolArguments) && collapsedToolArgs.has(String(msg.id + '-req-' + idx))">
                        {{ formatToolArgumentsPreview(tool.toolArguments) }}
                      </template>
                      <!-- 展开时显示完整版本 -->
                      <template v-else>
                        {{ formatToolArguments(tool.toolArguments) }}
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 工具执行结果消息 -->
          <div v-else-if="msg.role === 'tool_executed'" :class="['tool-executed-message', { failed: msg.toolExecuted?.success === false }]">
            <div :class="['tool-executed-avatar', { failed: msg.toolExecuted?.success === false }]">
              <CheckOutlined v-if="msg.toolExecuted?.success !== false" />
              <CloseOutlined v-else />
            </div>
            <div :class="['tool-executed-bubble', { failed: msg.toolExecuted?.success === false }]">
              <div class="tool-executed-header">
                <span v-if="msg.toolExecuted?.success !== false" class="tool-executed-name">✅ {{ msg.toolExecuted?.toolName }}</span>
                <span v-else class="tool-executed-name failed">❌ {{ msg.toolExecuted?.toolName }}</span>
                <span v-if="msg.toolExecuted?.success !== false" class="tool-badge success">DONE</span>
                <span v-else class="tool-badge failed">FAILED</span>
              </div>
              <!-- 可折叠的参数 -->
              <div v-if="msg.toolExecuted?.toolArguments" class="tool-executed-arguments">
                <div class="tool-arguments-header">
                  <span class="tool-arguments-label">📋 参数</span>
                  <span
                    v-if="shouldCollapseArgs(msg.toolExecuted.toolArguments)"
                    class="tool-arguments-toggle"
                    @click="toggleToolArgs(msg.id)"
                  >
                    {{ collapsedToolArgs.has(String(msg.id)) ? '展开' : '收起' }}
                  </span>
                </div>
                <div class="tool-arguments-value">
                  <!-- 折叠时显示截断版本 -->
                  <template v-if="shouldCollapseArgs(msg.toolExecuted.toolArguments) && collapsedToolArgs.has(String(msg.id))">
                    {{ formatToolArgumentsPreview(msg.toolExecuted.toolArguments) }}
                  </template>
                  <!-- 展开时显示完整版本 -->
                  <template v-else>
                    {{ formatToolArguments(msg.toolExecuted.toolArguments) }}
                  </template>
                </div>
              </div>
              <div class="tool-executed-result">
                <div class="tool-result-title">📤 执行结果</div>
                <div class="tool-result-content" :class="{ empty: !msg.toolExecuted?.toolOutput }">
                  {{ msg.toolExecuted?.toolOutput || (msg.toolExecuted?.success === false ? '✗ 执行失败' : '✓ 执行成功') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <!-- 选中元素提示 -->
        <div v-if="visualEditor.selectedElement.value" class="selected-element-alert">
          <a-alert
            type="info"
            closable
            @close="visualEditor.clearSelection()"
          >
            <template #message>
              <div class="selected-element-content">
                <HighlightOutlined class="element-icon" />
                <span class="element-label">已选中元素:</span>
                <span class="element-desc">{{ visualEditor.elementDescription }}</span>
              </div>
            </template>
          </a-alert>
        </div>

        <div class="input-wrapper">
          <textarea
            v-model="currentInput"
            class="chat-input"
            :placeholder="visualEditor.isEditMode.value ? '点击右侧网页中的元素进行选择，然后输入修改需求...' : '继续描述你的需求，让 AI 完善代码...'"
            rows="2"
            :disabled="isGenerating"
            @keydown.enter.prevent="!isGenerating && currentInput.trim() && handleSend()"
          ></textarea>
          <!-- 编辑模式按钮 -->
          <button
            v-if="shouldShowPreview && !isGenerating"
            class="edit-mode-btn"
            :class="{ active: visualEditor.isEditMode.value }"
            @click="visualEditor.toggleEditMode"
            title="可视化编辑：点击右侧网页元素进行选择"
          >
            <EditOutlined />
          </button>
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
        <!-- 网页尚未生成完毕 -->
        <div v-else-if="showNotReadyMessage" class="preview-placeholder">
          <div class="placeholder-icon static-icon">
            📄
          </div>
          <p>网页尚未生成完毕</p>
        </div>
        <!-- 网页生成出错 -->
        <div v-else-if="pageLoadError" class="preview-error">
          <div class="error-icon">⚠️</div>
          <p class="error-text">网页生成出了点问题</p>
          <button class="retry-btn" @click="handleRefresh">
            <SyncOutlined />
            <span>重试</span>
          </button>
        </div>
        <!-- 预览检查中 -->
        <div v-else-if="previewStatus === 'checking'" class="preview-placeholder">
          <div class="placeholder-icon">
            <LoadingOutlined class="spinning" />
          </div>
          <p>正在检查应用状态...</p>
        </div>
        <!-- 应用尚未构建完毕 -->
        <div v-else-if="previewStatus === 'not_ready'" class="preview-placeholder">
          <div class="placeholder-icon static-icon">
            📄
          </div>
          <p>应用尚未构建完毕，请稍后重试</p>
          <button class="retry-btn" @click="checkPreviewAvailability">
            <SyncOutlined />
            <span>重新检查</span>
          </button>
        </div>
        <!-- iframe 预览 -->
        <iframe
          v-else-if="shouldShowPreview && iframeUrl"
          ref="iframeRef"
          :key="iframeKey"
          :src="iframeUrl"
          class="preview-iframe"
          :class="{ 'edit-mode': visualEditor.isEditMode.value }"
          frameborder="0"
          sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
          @error="handleIframeError"
          @load="handleIframeLoad"
        ></iframe>
        <!-- 首次进入未生成 -->
        <div v-else class="preview-placeholder">
          <div class="placeholder-icon">
            <LoadingOutlined class="spinning" />
          </div>
          <p>请先在左侧描述需求，AI 将为您生成代码</p>
        </div>
      </div>
    </div>
    </div>
  </div>

    <!-- 部署成功弹窗 -->
    <Modal
      v-model:open="showDeployModal"
      :footer="null"
      width="420"
      centered
      :wrap-style="{ background: 'transparent' }"
    >
      <div class="deploy-modal-content">
        <div class="deploy-success-icon-wrapper">
          <div class="deploy-success-icon">
            <CheckOutlined />
          </div>
        </div>
        <h3 class="deploy-success-title">部署成功！</h3>
        <p class="deploy-success-desc">您的应用已成功部署，可通过以下链接访问</p>

        <div class="deploy-url-box">
          <div class="deploy-url-label">🔗 应用链接</div>
          <div class="deploy-url-input">
            <span class="deploy-url-text">{{ deployedUrl }}</span>
            <button class="copy-url-btn" @click="copyDeployUrl">
              <CopyOutlined />
              <span>复制</span>
            </button>
          </div>
        </div>

        <div class="deploy-modal-actions">
          <button class="deploy-action-btn secondary" @click="showDeployModal = false">
            稍后访问
          </button>
          <button class="deploy-action-btn primary" @click="openDeployUrl">
            <GlobalOutlined />
            立即访问
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
  max-width: 65%;
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

/* ===== 思考过程 ===== */
.thinking-section {
  margin: 0 12px 0;
  padding: 0;
  position: relative;
}

/* 空内容时的占位符 */
.bubble-content-spacer {
  height: 0;
}

.thinking-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.2);
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.thinking-content {
  padding-left: 12px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  border-left: 2px solid rgba(255, 255, 255, 0.06);
  padding-top: 4px;
  padding-bottom: 4px;
}

/* 覆盖 markdown 内容样式 */
.thinking-content :deep(*) {
  color: #666 !important;
}

.thinking-content :deep(p) {
  margin: 0 0 8px 0;
}

.thinking-content :deep(p:last-child) {
  margin-bottom: 0;
}

.thinking-content :deep(h1),
.thinking-content :deep(h2),
.thinking-content :deep(h3),
.thinking-content :deep(h4),
.thinking-content :deep(h5),
.thinking-content :deep(h6) {
  color: #777 !important;
  font-size: 13px;
  font-weight: 500;
  margin: 10px 0 6px 0;
}

.thinking-content :deep(code) {
  background: rgba(255, 255, 255, 0.05);
  color: #888 !important;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

.thinking-content :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  padding: 10px;
  margin: 8px 0;
}

.thinking-content :deep(pre code) {
  background: transparent;
  padding: 0;
}

/* ===== 单独的工具请求消息 ===== */
.tool-request-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.tool-request-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.15) 0%, rgba(0, 210, 106, 0.05) 100%);
  border: 2px solid rgba(0, 210, 106, 0.5);
  color: #00d26a;
  flex-shrink: 0;
  font-size: 16px;
}

.tool-request-bubble {
  flex: 1;
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.08) 0%, rgba(26, 26, 26, 1) 100%);
  border: 1px solid rgba(0, 210, 106, 0.3);
  border-left: 3px solid #00d26a;
  border-radius: 12px;
  overflow: hidden;
  max-width: 400px;
}

.tool-request-bubble-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border-bottom: 1px solid rgba(0, 210, 106, 0.2);
  background: rgba(0, 210, 106, 0.1);
}

.tool-request-bubble-title {
  font-size: 13px;
  font-weight: 600;
  color: #00d26a;
}

.tool-request-item-inline {
  padding: 10px 14px;
}

.tool-request-name {
  font-size: 14px;
  color: #e0e0e0;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 500;
}

/* ===== 工具请求列表 ===== */
.tool-requests-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.tool-requests-title {
  font-size: 11px;
  color: #00d26a;
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.tool-requests-title .title-icon {
  font-size: 12px;
}

.tool-request-item {
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.08) 0%, rgba(0, 210, 106, 0.03) 100%);
  border: 1px solid rgba(0, 210, 106, 0.3);
  border-left: 3px solid #00d26a;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;
}

.tool-request-item:last-child {
  margin-bottom: 0;
}

.tool-request-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-request-header .tool-icon {
  color: #00d26a;
  font-size: 14px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.tool-request-header .tool-name {
  font-size: 14px;
  color: #00d26a;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 600;
  flex: 1;
}

.tool-badge {
  font-size: 9px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(0, 210, 106, 0.2);
  color: #00d26a;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.tool-badge.success {
  background: rgba(0, 210, 106, 0.2);
  color: #00d26a;
}

/* ===== AI 消息内的工具请求 ===== */
.ai-tool-requests {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 210, 106, 0.2);
}

.tool-request-item-inline-compact {
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.08) 0%, rgba(0, 210, 106, 0.03) 100%);
  border: 1px solid rgba(0, 210, 106, 0.25);
  border-left: 3px solid #00d26a;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}

.tool-request-item-inline-compact:last-child {
  margin-bottom: 0;
}

.tool-request-header-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tool-request-header-compact .tool-request-name {
  font-size: 13px;
  color: #00d26a;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-weight: 600;
}

.tool-request-arguments-compact {
  margin-top: 8px;
  padding: 8px 10px;
  background: rgba(0, 0, 0, 0.25);
  border-radius: 6px;
  font-size: 12px;
}

.tool-arguments {
  margin-top: 10px;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 6px;
  font-size: 12px;
  color: #b0b0b0;
}

.tool-arguments-label {
  color: #00d26a;
  font-weight: 600;
  margin-right: 6px;
}

.tool-arguments-value {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  color: #e0e0e0;
  word-break: break-all;
  line-height: 1.5;
}

/* ===== 工具执行结果消息 ===== */
.tool-executed-message {
  position: relative;
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.tool-executed-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.2) 0%, rgba(0, 210, 106, 0.1) 100%);
  border: 2px solid #00d26a;
  color: #00d26a;
  flex-shrink: 0;
  font-size: 16px;
}

.tool-executed-bubble {
  flex: 1;
  background: linear-gradient(135deg, rgba(0, 210, 106, 0.05) 0%, rgba(26, 26, 26, 1) 100%);
  border: 1px solid rgba(0, 210, 106, 0.3);
  border-left: 3px solid #00d26a;
  border-radius: 12px;
  overflow: hidden;
}

.tool-executed-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 210, 106, 0.2);
  background: rgba(0, 210, 106, 0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tool-executed-name {
  font-size: 14px;
  font-weight: 600;
  color: #00d26a;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.tool-executed-arguments {
  padding: 0;
  font-size: 12px;
  color: #b0b0b0;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(0, 210, 106, 0.15);
}

.tool-arguments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
}

.tool-arguments-label {
  color: #00d26a;
  font-weight: 600;
}

.tool-arguments-toggle {
  font-size: 10px;
  color: #00d26a;
  padding: 3px 8px;
  background: rgba(0, 210, 106, 0.2);
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.tool-arguments-toggle:hover {
  background: rgba(0, 210, 106, 0.3);
}

.tool-executed-arguments .tool-arguments-value {
  padding: 0 16px 12px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  color: #e0e0e0;
  overflow-wrap: break-word;
  line-height: 1.5;
  white-space: pre-wrap;
}

.tool-result-title {
  font-size: 11px;
  color: #00d26a;
  padding: 10px 16px 6px;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

.tool-result-content {
  padding: 12px;
  font-size: 13px;
  color: #e0e0e0;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow-y: auto;
  background: rgba(0, 0, 0, 0.3);
  margin: 0 16px 12px;
  border-radius: 6px;
  border: 1px solid rgba(0, 210, 106, 0.2);
}

.tool-result-content:empty::before {
  content: '✓ 执行成功';
  color: #00d26a;
  font-weight: 500;
}

/* 滚动条样式 */
.tool-result-content::-webkit-scrollbar {
  width: 6px;
}

.tool-result-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.tool-result-content::-webkit-scrollbar-thumb {
  background: rgba(0, 210, 106, 0.3);
  border-radius: 3px;
}

.tool-result-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 210, 106, 0.5);
}

/* ===== 工具执行失败状态样式 ===== */
/* 失败状态的消息容器 */
.tool-executed-message.failed .tool-executed-avatar.failed {
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.15), rgba(26, 26, 26, 1));
  color: #ff4d4f;
}

/* 失败状态的气泡 */
.tool-executed-message.failed .tool-executed-bubble.failed {
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.05) 0%, rgba(26, 26, 26, 1) 100%);
  border-color: rgba(255, 77, 79, 0.3);
}

/* 失败状态的头部 */
.tool-executed-message.failed .tool-executed-header {
  border-bottom-color: rgba(255, 77, 79, 0.2);
  background: rgba(255, 77, 79, 0.1);
}

/* 失败状态的名称 */
.tool-executed-message.failed .tool-executed-name.failed {
  color: #ff4d4f;
}

/* 失败状态的徽章 */
.tool-executed-message.failed .tool-badge.failed {
  background: rgba(255, 77, 79, 0.2);
  color: #ff4d4f;
  border-color: rgba(255, 77, 79, 0.4);
}

/* 失败状态的结果区域 */
.tool-executed-message.failed .tool-result-title {
  color: #ff4d4f;
}

.tool-executed-message.failed .tool-result-content {
  border-color: rgba(255, 77, 79, 0.3);
  background: rgba(255, 77, 79, 0.05);
}

/* 失败状态的结果区域滚动条 */
.tool-executed-message.failed .tool-result-content::-webkit-scrollbar-thumb {
  background: rgba(255, 77, 79, 0.3);
}

.tool-executed-message.failed .tool-result-content::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 77, 79, 0.5);
}

/* 失败状态的参数标签和展开按钮 */
.tool-executed-message.failed .tool-arguments-label {
  color: #ff4d4f;
}

.tool-executed-message.failed .tool-arguments-toggle {
  color: #ff4d4f;
  background: rgba(255, 77, 79, 0.2);
}

.tool-executed-message.failed .tool-arguments-toggle:hover {
  background: rgba(255, 77, 79, 0.3);
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

.placeholder-icon.static-icon {
  font-size: 64px;
  opacity: 0.8;
}

/* ===== 网页生成出错提示 ===== */
.preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--bg-card);
  padding: 40px;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.error-text {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--accent-green);
  color: var(--bg-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.retry-btn:hover {
  opacity: 0.9;
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
/* 覆盖 ant-design modal 样式 */
:deep(.ant-modal-content) {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color);
  border-radius: 16px !important;
  overflow: hidden;
}

:deep(.ant-modal-header) {
  background: transparent !important;
  border-bottom: none !important;
  padding: 24px 24px 0 !important;
}

:deep(.ant-modal-body) {
  padding: 0 24px 24px !important;
}

:deep(.ant-modal-close) {
  color: var(--text-muted);
}

:deep(.ant-modal-close:hover) {
  color: var(--text-primary);
}

.deploy-modal-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

/* 成功图标外层动画容器 */
.deploy-success-icon-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.deploy-success-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-green) 0%, #00b878 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32px;
  box-shadow: 0 8px 24px rgba(0, 210, 106, 0.3);
  animation: successPop 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes successPop {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 成功图标周围的波纹效果 */
.deploy-success-icon-wrapper::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--accent-green);
  opacity: 0.15;
  animation: ripple 1.5s ease-out infinite;
}

@keyframes ripple {
  0% {
    transform: translate(-50%, -50%) scale(0.8);
    opacity: 0.3;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

.deploy-success-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.deploy-success-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 20px 0;
  text-align: center;
}

.deploy-url-box {
  width: 100%;
  margin-bottom: 20px;
}

.deploy-url-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.deploy-url-input {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  transition: all 0.2s;
}

.deploy-url-input:hover {
  border-color: var(--accent-green);
}

.deploy-url-text {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
  word-break: break-all;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
}

.copy-url-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--accent-green);
  border: none;
  border-radius: 6px;
  color: var(--bg-primary);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.copy-url-btn:hover {
  background: #00e078;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 210, 106, 0.3);
}

.copy-url-btn:active {
  transform: translateY(0);
}

.deploy-modal-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.deploy-action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.deploy-action-btn.secondary {
  background: var(--bg-primary);
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
  background: #00e078;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 210, 106, 0.3);
}

.deploy-action-btn:active {
  transform: translateY(0);
}

/* ===== 可视化编辑相关样式 ===== */
.selected-element-alert {
  padding: 0 16px 8px;
}

.selected-element-alert :deep(.ant-alert) {
  padding: 10px 14px;
  border-radius: 8px;
  background: #1a1a1a;
  border: 1px solid #3a3a3a;
}

.selected-element-alert :deep(.ant-alert-info) {
  background: #1a1a1a;
  border-color: #3a3a3a;
}

.selected-element-alert :deep(.anticon-close) {
  color: #888;
}

.selected-element-alert :deep(.anticon-close:hover) {
  color: #fff;
}

.selected-element-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  flex-wrap: nowrap;
  overflow: hidden;
}

.element-icon {
  color: #00d26a;
  font-size: 14px;
  flex-shrink: 0;
}

.element-label {
  font-weight: 500;
  color: #fff;
  flex-shrink: 0;
  white-space: nowrap;
}

.element-desc {
  color: #888;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 12px;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.edit-mode-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  margin-right: 8px;
}

.edit-mode-btn:hover {
  border-color: #666;
  color: #fff;
  background: #2a2a2a;
}

.edit-mode-btn.active {
  border-color: #00d26a;
  background: #00d26a;
  color: #1a1a1a;
}

</style>
