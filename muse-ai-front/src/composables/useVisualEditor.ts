/**
 * 可视化编辑 composable
 * 用于处理 iframe 通信、元素选中等功能
 */
import { ref, computed, onMounted, onUnmounted, watch, nextTick, type Ref } from 'vue'
import { message } from 'ant-design-vue'

// 选中的元素信息接口
export interface SelectedElement {
  tag: string
  id?: string
  classes?: string
  text?: string
  selector?: string  // 完整的 CSS 选择器路径
  pagePath?: string // 页面路径
}

// iframe 消息类型
type IframeMessage =
  | { type: 'MUSE_ELEMENT_HOVER'; element: SelectedElement }
  | { type: 'MUSE_ELEMENT_SELECT'; element: SelectedElement }
  | { type: 'MUSE_ELEMENT_DESELECT' }
  | { type: 'MUSE_EDITOR_MODE'; enabled: boolean }
  | { type: 'MUSE_CLEAR_SELECTION' }

const EDITOR_INJECT_SCRIPT = `
(function() {
  'use strict';

  // 防止重复注入
  if (window.__muse_editor_injected__) {
    try {
      window.parent.postMessage({ type: 'MUSE_EDITOR_READY' }, '*');
    } catch(e) {
      console.error('[Muse Editor] 发送 READY 失败:', e);
    }
    return;
  }

  window.__muse_editor_injected__ = true;

  var hoveredElement = null;
  var selectedElement = null;
  var originalStyles = new WeakMap();

  // 生成 CSS 选择器路径
  function generateSelector(el) {
    if (!el || el === document.body || el === document.documentElement) {
      return '';
    }

    var path = [];
    var current = el;

    while (current && current !== document.body && path.length < 10) {
      var tag = current.tagName.toLowerCase();
      var selector = tag;

      // 如果有 id，直接使用 id
      if (current.id) {
        selector = tag + '#' + current.id;
        path.unshift(selector);
        break; // id 是唯一的，可以停止
      }

      // 如果有 class，添加 class
      if (current.className && typeof current.className === 'string' && current.className.trim()) {
        var classes = current.className.trim().split(/\s+/).slice(0, 2).join('.');
        if (classes) {
          selector = tag + '.' + classes;
        }
      }

      // 计算同级元素中的位置
      var siblings = current.parentNode ? Array.from(current.parentNode.children).filter(function(child) {
        return child.tagName === current.tagName;
      }) : [];

      if (siblings.length > 1) {
        var index = siblings.indexOf(current) + 1;
        selector += ':nth-child(' + index + ')';
      }

      path.unshift(selector);
      current = current.parentElement;
    }

    return path.join(' > ');
  }

  // 获取元素信息
  function getElementInfo(el) {
    if (!el) return null;

    var info = {
      tag: el.tagName.toLowerCase(),
      selector: generateSelector(el),
      pagePath: window.location.hash || '/'  // 只保留 hash 部分（如 #/about）
    };

    if (el.id) info.id = el.id;
    if (el.className && typeof el.className === 'string') {
      info.classes = el.className;
    }
    if (el.textContent) {
      info.text = el.textContent.slice(0, 100);
    }

    return info;
  }

  // 保存原始样式
  function saveOriginalStyle(el) {
    if (!originalStyles.has(el)) {
      var computed = window.getComputedStyle(el);
      originalStyles.set(el, {
        outline: computed.outline,
        cursor: computed.cursor
      });
    }
  }

  // 恢复原始样式
  function restoreOriginalStyle(el) {
    var styles = originalStyles.get(el);
    if (styles) {
      el.style.outline = styles.outline;
      el.style.cursor = styles.cursor;
    }
  }

  // 设置悬浮样式
  function setHoverStyle(el) {
    saveOriginalStyle(el);
    el.style.setProperty('outline', '2px dashed #1677ff', 'important');
    el.style.setProperty('cursor', 'crosshair', 'important');
  }

  // 设置选中样式
  function setSelectedStyle(el) {
    saveOriginalStyle(el);
    el.style.setProperty('outline', '3px solid #1677ff', 'important');
    el.style.setProperty('cursor', 'pointer', 'important');
  }

  // 清除悬浮状态
  function clearHover() {
    if (hoveredElement && hoveredElement !== selectedElement) {
      restoreOriginalStyle(hoveredElement);
    }
    hoveredElement = null;
  }

  // 处理鼠标移入
  function handleMouseOver(e) {
    if (!window.__muse_editor_enabled__) return;

    var target = e.target;
    if (target === hoveredElement) return;

    clearHover();

    if (target.tagName !== 'IFRAME' && target !== document.body && target !== document.documentElement) {
      hoveredElement = target;
      if (hoveredElement !== selectedElement) {
        setHoverStyle(hoveredElement);
      }
      try {
        window.parent.postMessage({
          type: 'MUSE_ELEMENT_HOVER',
          element: getElementInfo(hoveredElement)
        }, '*');
      } catch(e) {
        console.error('[Muse Editor] 发送悬浮消息失败:', e);
      }
    }
  }

  // 处理鼠标移出
  function handleMouseOut(e) {
    if (!window.__muse_editor_enabled__) return;
    var relatedTarget = e.relatedTarget;
    if (!relatedTarget || !hoveredElement || !hoveredElement.contains(relatedTarget)) {
      clearHover();
    }
  }

  // 处理点击
  function handleClick(e) {
    if (!window.__muse_editor_enabled__) return;

    var target = e.target;
    if (target.tagName === 'IFRAME' || target === document.body || target === document.documentElement) return;

    e.preventDefault();
    e.stopPropagation();

    if (selectedElement && selectedElement !== target) {
      restoreOriginalStyle(selectedElement);
    }

    selectedElement = target;
    setSelectedStyle(selectedElement);

    try {
      window.parent.postMessage({
        type: 'MUSE_ELEMENT_SELECT',
        element: getElementInfo(selectedElement)
      }, '*');
      console.log('[Muse Editor] ✓ 元素已选中:', getElementInfo(selectedElement));
    } catch(e) {
      console.error('[Muse Editor] 发送选中消息失败:', e);
    }
  }

  // 启用编辑模式
  window.__muse_enable_editor__ = function() {
    console.log('[Muse Editor] ✓ 启用编辑模式');
    window.__muse_editor_enabled__ = true;
    document.body.style.setProperty('cursor', 'crosshair', 'important');

    document.addEventListener('mouseover', handleMouseOver, { capture: true, passive: true });
    document.addEventListener('mouseout', handleMouseOut, { capture: true, passive: true });
    document.addEventListener('click', handleClick, { capture: true });

    try {
      window.parent.postMessage({ type: 'MUSE_EDITOR_MODE', enabled: true }, '*');
    } catch(e) {
      console.error('[Muse Editor] 发送编辑模式消息失败:', e);
    }
  };

  // 禁用编辑模式
  window.__muse_disable_editor__ = function() {
    console.log('[Muse Editor] ✓ 禁用编辑模式');
    window.__muse_editor_enabled__ = false;
    document.body.style.cursor = '';

    // 强制清除所有元素的编辑器样式
    var allElements = document.querySelectorAll('*');
    for (var i = 0; i < allElements.length; i++) {
      var el = allElements[i];
      el.style.removeProperty('outline');
      el.style.removeProperty('cursor');
    }

    document.removeEventListener('mouseover', handleMouseOver, { capture: true });
    document.removeEventListener('mouseout', handleMouseOut, { capture: true });
    document.removeEventListener('click', handleClick, { capture: true });

    clearHover();
    if (selectedElement) {
      restoreOriginalStyle(selectedElement);
      selectedElement = null;
    }

    try {
      window.parent.postMessage({ type: 'MUSE_EDITOR_MODE', enabled: false }, '*');
    } catch(e) {
      console.error('[Muse Editor] 发送编辑模式消息失败:', e);
    }
  };

  // 清除选中状态
  window.__muse_clear_selection__ = function() {
    console.log('[Muse Editor] ✓ 清除选中');
    if (selectedElement) {
      restoreOriginalStyle(selectedElement);
      selectedElement = null;
    }
    clearHover();

    try {
      window.parent.postMessage({ type: 'MUSE_ELEMENT_DESELECT' }, '*');
    } catch(e) {
      console.error('[Muse Editor] 发送取消选中消息失败:', e);
    }
  };

  // 监听来自父窗口的消息
  window.addEventListener('message', function(e) {
    var data = e.data;
    if (!data || typeof data !== 'object') return;

    switch (data.type) {
      case 'MUSE_EDITOR_MODE':
        if (data.enabled) {
          window.__muse_enable_editor__();
        } else {
          window.__muse_disable_editor__();
        }
        break;
      case 'MUSE_ENABLE_EDITOR':
        window.__muse_enable_editor__();
        break;
      case 'MUSE_DISABLE_EDITOR':
        window.__muse_disable_editor__();
        break;
      case 'MUSE_CLEAR_SELECTION':
        window.__muse_clear_selection__();
        break;
      case 'MUSE_PING':
        try {
          window.parent.postMessage({ type: 'MUSE_PONG' }, '*');
        } catch(e) {
          console.error('[Muse Editor] 发送 PONG 失败:', e);
        }
        break;
    }
  });

  // 发送 READY 消息的函数
  function sendReady() {
    console.log('[Muse Editor] 尝试发送 MUSE_EDITOR_READY 消息到父窗口');
    console.log('[Muse Editor] 父窗口 URL:', window.location.href);
    try {
      window.parent.postMessage({ type: 'MUSE_EDITOR_READY' }, '*');
      console.log('[Muse Editor] ✓ MUSE_EDITOR_READY 消息已发送');
    } catch(e) {
      console.error('[Muse Editor] ✗ 发送 MUSE_EDITOR_READY 失败:', e);
    }
  }

  console.log('[Muse Editor] 脚本初始化完成，准备发送 READY 消息');

  // 立即发送
  sendReady();

  // 延迟再次发送，确保父窗口监听器已注册
  setTimeout(sendReady, 50);
  setTimeout(sendReady, 200);
  setTimeout(sendReady, 500);

  console.log('[Muse Editor] ===== 脚本初始化完成 =====');
})();
`

export function useVisualEditor(iframeRef: Ref<HTMLIFrameElement | null>) {
  // 编辑模式状态
  const isEditMode = ref(false)
  // 选中的元素
  const selectedElement = ref<SelectedElement | null>(null)
  // 悬浮的元素
  const hoveredElement = ref<SelectedElement | null>(null)
  // iframe 是否就绪
  const isIframeReady = ref(false)

  // 脚本注入超时定时器
  let injectTimeoutTimer: number | null = null
  // 防止重复注入的标记
  let isLoading = false

  // 生成选中元素的描述文本
  const elementDescription = computed(() => {
    if (!selectedElement.value) return ''
    const el = selectedElement.value

    // 使用 CSS 选择器作为主要描述
    let desc = el.selector || `<${el.tag}>`

    // 如果有文本内容，附加显示
    if (el.text) {
      const textPreview = el.text.length > 30 ? el.text.slice(0, 30) + '...' : el.text
      desc += ` 「${textPreview}」`
    }

    return desc
  })

  // 生成发送给 AI 的提示词增强
  const getEnhancedPrompt = (userPrompt: string): string => {
    if (!selectedElement.value) return userPrompt

    const el = selectedElement.value

    // 构建位置描述
    let positionInfo = ''
    positionInfo += `\n页面路径: ${el.pagePath || '/'}\n`
    positionInfo += `标签: ${el.tag}\n`
    positionInfo += `选择器: ${el.selector || el.tag}\n`

    return `${userPrompt}\n\n以下是用户对于想要修改元素的位置描述：${positionInfo}`
  }

  // 向 iframe 发送消息
  const postMessageToIframe = (message: IframeMessage) => {
    const iframe = iframeRef.value
    if (!iframe || !iframe.contentWindow) {
      console.warn('[useVisualEditor] iframe 未就绪')
      return
    }
    try {
      iframe.contentWindow.postMessage(message, '*')
    } catch (e) {
      console.error('[useVisualEditor] 发送消息到 iframe 失败:', e)
    }
  }

  // 启用编辑模式
  const enableEditMode = () => {
    // 立即更新状态，让按钮有反馈
    isEditMode.value = true
    selectedElement.value = null

    // 如果 iframe 未就绪，先尝试注入脚本
    if (!isIframeReady.value) {
      attemptInject()

      // 等待 1 秒后发送消息
      setTimeout(() => {
        if (isIframeReady.value) {
          postMessageToIframe({ type: 'MUSE_EDITOR_MODE', enabled: true })
        } else {
          message.warning('编辑器初始化失败，请刷新页面重试')
        }
      }, 1000)
      return
    }

    postMessageToIframe({ type: 'MUSE_EDITOR_MODE', enabled: true })
  }

  // 禁用编辑模式
  const disableEditMode = () => {
    isEditMode.value = false
    selectedElement.value = null
    postMessageToIframe({ type: 'MUSE_EDITOR_MODE', enabled: false })
  }

  // 切换编辑模式
  const toggleEditMode = () => {
    if (isEditMode.value) {
      disableEditMode()
    } else {
      enableEditMode()
    }
    console.log('[useVisualEditor] 切换后 isEditMode:', isEditMode.value)
  }

  // 清除选中元素
  const clearSelection = () => {
    selectedElement.value = null
    postMessageToIframe({ type: 'MUSE_CLEAR_SELECTION' })
  }

  // 处理来自 iframe 的消息
  const handleIframeMessage = (event: MessageEvent) => {
    const data = event.data

    // 过滤非对象类型的消息
    if (!data || typeof data !== 'object') {
      return
    }

    // 过滤没有 type 字段的消息
    if (!data.type || typeof data.type !== 'string') {
      return
    }

    // 只处理我们定义的消息类型
    const validTypes = [
      'MUSE_EDITOR_READY',
      'MUSE_ELEMENT_HOVER',
      'MUSE_ELEMENT_SELECT',
      'MUSE_ELEMENT_DESELECT',
      'MUSE_EDITOR_MODE',
      'MUSE_ENABLE_EDITOR_CHECK',
      'MUSE_PONG',
    ]

    if (!validTypes.includes(data.type)) {
      return
    }

    switch (data.type) {
      case 'MUSE_PONG':
        // 收到 PONG 说明 iframe 内部的脚本正在运行，但可能没收到 READY
        // 再次尝试触发 READY
        isIframeReady.value = true
        if (injectTimeoutTimer) {
          clearTimeout(injectTimeoutTimer)
          injectTimeoutTimer = null
        }
        break
      case 'MUSE_EDITOR_READY':
      case 'MUSE_ENABLE_EDITOR_CHECK':
        isIframeReady.value = true
        console.log('[useVisualEditor] ✓ iframe 编辑器已就绪')
        // 清除超时定时器
        if (injectTimeoutTimer) {
          clearTimeout(injectTimeoutTimer)
          injectTimeoutTimer = null
        }
        break

      case 'MUSE_ELEMENT_HOVER':
        hoveredElement.value = data.element
        break

      case 'MUSE_ELEMENT_SELECT':
        selectedElement.value = data.element
        console.log('[useVisualEditor] ✓ 选中元素:', data.element)
        break

      case 'MUSE_ELEMENT_DESELECT':
        selectedElement.value = null
        break

      case 'MUSE_EDITOR_MODE':
        isEditMode.value = data.enabled
        console.log('[useVisualEditor] 编辑模式状态:', data.enabled)
        break
    }
  }

  // 处理 iframe 加载完成
  const handleIframeLoad = () => {
    if (isLoading) {
      console.log('[useVisualEditor] 正在注入中，跳过重复调用')
      return
    }

    console.log('[useVisualEditor] iframe 加载完成，开始注入脚本')
    isLoading = true
    isIframeReady.value = false

    // 清除之前的超时定时器
    if (injectTimeoutTimer) {
      clearTimeout(injectTimeoutTimer)
      injectTimeoutTimer = null
    }

    // 尝试多次注入，因为 iframe 可能需要更多时间来完全加载
    let retryCount = 0
    const maxRetries = 5
    const retryDelay = 500
    let timeoutRetryCount = 0
    const maxTimeoutRetries = 2  // 最多超时重试 2 次

    const tryInject = () => {
      // 如果是超时后的重试，重置 retryCount
      if (timeoutRetryCount > 0) {
        retryCount = 0
      }

      retryCount++
      const success = attemptInject()

      if (!success && retryCount < maxRetries) {
        setTimeout(tryInject, retryDelay)
      } else if (success) {
        // 设置超时检查，如果 3 秒后还没收到 READY 消息，重试
        injectTimeoutTimer = setTimeout(() => {
          if (!isIframeReady.value) {
            timeoutRetryCount++
            if (timeoutRetryCount <= maxTimeoutRetries) {
              console.warn(`[useVisualEditor] 脚本注入超时，重试 (${timeoutRetryCount}/${maxTimeoutRetries})...`)
              tryInject()
            } else {
              console.error('[useVisualEditor] 脚本注入超时，已达最大重试次数，停止重试')
              isLoading = false
            }
          } else {
            isLoading = false
          }
        }, 3000) as unknown as number
      } else if (retryCount >= maxRetries) {
        console.error('[useVisualEditor] 脚本注入失败，已达最大重试次数')
        isLoading = false
      }
    }

    // 首次尝试延迟
    setTimeout(tryInject, 500)
  }

  // 尝试注入脚本，返回是否成功
  const attemptInject = (): boolean => {
    const iframe = iframeRef.value
    if (!iframe || !iframe.contentWindow) {
      console.warn('[useVisualEditor] iframe 或 contentWindow 不存在')
      return false
    }

    try {
      const doc = iframe.contentDocument || iframe.contentWindow?.document
      if (!doc) {
        console.warn('[useVisualEditor] 无法访问 iframe document (跨域限制)')
        console.log('[useVisualEditor] 尝试通过 postMessage 触发 iframe 内部脚本')

        // 由于跨域无法直接注入，尝试发送启动消息
        // 假设 iframe 内部可能已经有编辑器脚本，或者需要手动加载
        iframe.contentWindow.postMessage({ type: 'MUSE_START_EDITOR' }, '*')

        // 也尝试发送脚本内容供 eval 执行（作为后备）
        iframe.contentWindow.postMessage({
          type: 'MUSE_INJECT_SCRIPT',
          script: EDITOR_INJECT_SCRIPT
        }, '*')

        return false
      }

      // 检查是否已经注入过
      if (doc.getElementById('muse-visual-editor-inject')) {
        iframe.contentWindow.postMessage({ type: 'MUSE_ENABLE_EDITOR_CHECK' }, '*')
        return true
      }

      // 创建 script 元素注入
      const script = doc.createElement('script')
      script.id = 'muse-visual-editor-inject'
      script.text = EDITOR_INJECT_SCRIPT
      doc.head.appendChild(script)

      console.log('[useVisualEditor] ✓ script 元素已添加到 iframe')

      // 多次发送 PING 测试 iframe 通信
      const pingIntervals = [100, 300, 600, 1000, 2000]
      pingIntervals.forEach(delay => {
        setTimeout(() => {
          try {
            iframe.contentWindow.postMessage({ type: 'MUSE_PING' }, '*')
          } catch (e) {
            console.error('[useVisualEditor] 发送 PING 失败:', e)
          }
        }, delay)
      })

      return true
    } catch (e) {
      console.error('[useVisualEditor] 注入脚本异常:', e)
      return false
    }
  }

  // 注入编辑器脚本到 iframe（保留原方法供外部调用）
  const injectEditorScript = () => {
    attemptInject()
  }

  // 监听 iframe ref 变化
  watch(iframeRef, (newIframe, oldIframe) => {
    // 移除旧 iframe 的 load 事件监听
    if (oldIframe) {
      oldIframe.removeEventListener('load', handleIframeLoad)
    }
    // 添加新 iframe 的 load 事件监听
    if (newIframe) {
      newIframe.addEventListener('load', handleIframeLoad)
      // 延迟尝试注入，如果 iframe 已加载完成会立即成功
      nextTick(() => {
        handleIframeLoad()
      })
    }
  })

  // 组件挂载时添加消息监听
  onMounted(() => {
    console.log('[useVisualEditor] 注册消息监听器')
    window.addEventListener('message', handleIframeMessage)

    // 如果 iframe 已经存在，添加 load 事件监听并尝试注入
    if (iframeRef.value) {
      iframeRef.value.addEventListener('load', handleIframeLoad)
      nextTick(() => {
        handleIframeLoad()
      })
    }
  })

  // 组件卸载时移除消息监听
  onUnmounted(() => {
    console.log('[useVisualEditor] 移除消息监听器')
    window.removeEventListener('message', handleIframeMessage)

    // 移除 iframe 的 load 事件监听
    if (iframeRef.value) {
      iframeRef.value.removeEventListener('load', handleIframeLoad)
    }

    // 清除超时定时器
    if (injectTimeoutTimer) {
      clearTimeout(injectTimeoutTimer)
      injectTimeoutTimer = null
    }
  })

  return {
    // 状态
    isEditMode,
    selectedElement,
    hoveredElement,
    isIframeReady,
    elementDescription,

    // 方法
    enableEditMode,
    disableEditMode,
    toggleEditMode,
    clearSelection,
    getEnhancedPrompt,
    handleIframeLoad,

    // 注入方法（用于手动重新注入）
    injectEditorScript,
  }
}
