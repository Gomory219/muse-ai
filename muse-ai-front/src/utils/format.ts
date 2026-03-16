/**
 * 格式化工具函数
 */

// 需要转换为字符串的 ID 字段名
const ID_FIELDS = ['id', 'userId', 'appId', 'ownerId']

/**
 * 递归转换对象中的 ID 字段为字符串，防止 JavaScript 大整数精度丢失
 */
export function convertIdsToString(data: any): any {
  if (data === null || data === undefined) {
    return data
  }

  // 处理数组
  if (Array.isArray(data)) {
    return data.map(convertIdsToString)
  }

  // 处理对象
  if (typeof data === 'object') {
    const result: any = {}
    for (const key in data) {
      if (Object.prototype.hasOwnProperty.call(data, key)) {
        // 如果是 ID 字段且是数字，转换为字符串
        if (ID_FIELDS.includes(key) && typeof data[key] === 'number') {
          result[key] = String(data[key])
        } else {
          result[key] = convertIdsToString(data[key])
        }
      }
    }
    return result
  }

  return data
}

/**
 * 格式化时间
 */
export function formatTime(timeStr?: string): string {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * 格式化时间为相对时间
 */
export function formatRelativeTime(timeStr?: string): string {
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

/**
 * 截断文本
 */
export function truncateText(text: string, maxLength: number): string {
  if (!text) return 'null'
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

/**
 * 获取代码类型标签
 */
export function getCodeTypeLabel(type?: string): string {
  if (!type) return '-'
  const t = type.toUpperCase()
  if (t === 'HTML') return '单文件'
  if (t === 'MULTI_FILE') return '多文件'
  if (t === 'VUE') return 'VUE工程'
  // 兼容旧值
  if (t.includes('SINGLE') || t.includes('ONE')) return '单文件'
  if (t.includes('MULTI')) return '多文件'
  if (t.includes('VUE')) return 'VUE工程'
  return '-'
}

/**
 * 获取代码类型样式类
 */
export function getCodeTypeClass(type?: string): string {
  if (!type) return ''
  const t = type.toUpperCase()
  if (t === 'HTML') return 'type-single'
  if (t === 'MULTI_FILE') return 'type-multi'
  if (t === 'VUE') return 'type-vue'
  // 兼容旧值
  if (t.includes('SINGLE') || t.includes('ONE')) return 'type-single'
  if (t.includes('MULTI')) return 'type-multi'
  if (t.includes('VUE')) return 'type-vue'
  return ''
}

/**
 * 根据代码类型获取预览 URL 路径
 * @param type 代码类型 (HTML | MULTI_FILE | VUE)
 * @param appId 应用 ID
 * @param apiBaseUrl API 基础 URL
 * @returns 完整的预览 URL
 */
export function getCodeTypeUrl(type?: string, appId?: string, apiBaseUrl: string = ''): string {
  if (!type || !appId) return ''
  const t = type.toUpperCase()
  if (t === 'HTML') {
    return `${apiBaseUrl}/code/html/${appId}/index.html`
  }
  if (t === 'MULTI_FILE') {
    return `${apiBaseUrl}/code/multi-file/${appId}/index.html`
  }
  if (t === 'VUE') {
    return `${apiBaseUrl}/code/vue/${appId}/dist/index.html`
  }
  // 兼容旧值
  if (t.includes('VUE')) {
    return `${apiBaseUrl}/code/vue/${appId}/dist/index.html`
  }
  if (t.includes('MULTI')) {
    return `${apiBaseUrl}/code/multi-file/${appId}/index.html`
  }
  // 默认返回单文件路径
  return `${apiBaseUrl}/code/html/${appId}/index.html`
}
