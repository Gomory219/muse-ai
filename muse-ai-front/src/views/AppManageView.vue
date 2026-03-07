<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { StarFilled, StarOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { listAppsByAdmin, deleteAppByAdmin, updateAppByAdmin, pinApp } from '@/api/appController'

const dataSource = ref<API.AppVO[]>([])
const loading = ref(false)

// 点击外部关闭下拉框的处理器
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.custom-dropdown')) {
    codeTypeDropdownOpen.value = false
  }
}

// 编辑应用弹窗
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingApp = ref<API.AppVO | null>(null)

const appForm = ref({
  id: undefined as number | undefined,
  appName: '',
})

// 详情弹窗
const detailVisible = ref(false)
const detailApp = ref<API.AppVO | null>(null)

// 分页
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

// 每页条数选项
const pageSizeOptions = [10, 20, 50, 100]

// 跳转页码输入
const jumpPageInput = ref('')

// 搜索条件
const searchForm = ref({
  appName: '',
  userId: '',
  codeGenType: '',
  onlyFeatured: false,
})

// 代码类型下拉框状态
const codeTypeDropdownOpen = ref(false)

// 代码类型选项
const codeGenTypeOptions = [
  { label: '全部类型', value: '' },
  { label: '单文件', value: 'single' },
  { label: '多文件', value: 'multi' },
]

// 切换代码类型下拉框
const toggleCodeTypeDropdown = () => {
  codeTypeDropdownOpen.value = !codeTypeDropdownOpen.value
}

// 选择代码类型
const selectCodeType = (value: string) => {
  searchForm.value.codeGenType = value
  codeTypeDropdownOpen.value = false
}

// 获取当前选中的代码类型标签
const selectedCodeTypeLabel = () => {
  const option = codeGenTypeOptions.find(opt => opt.value === searchForm.value.codeGenType)
  return option ? option.label : '全部类型'
}

// 获取应用列表
const fetchApps = async () => {
  loading.value = true
  try {
    const queryParams: API.AppQueryRequest = {
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
    }

    // 添加搜索条件
    if (searchForm.value.appName) {
      queryParams.appName = searchForm.value.appName
    }
    if (searchForm.value.userId) {
      queryParams.userId = Number(searchForm.value.userId)
    }
    if (searchForm.value.codeGenType) {
      // 转换为后端期望的值
      queryParams.codeGenType = searchForm.value.codeGenType === 'single' ? 'HTML' : 'MULTI_FILE'
    }
    // 只看精选：priority > 0 的应用
    if (searchForm.value.onlyFeatured) {
      queryParams.minPriority = 1
    }

    const res = await listAppsByAdmin({
      appQueryRequest: queryParams,
    })
    if (res.data.code === 0 && res.data.data) {
      dataSource.value = res.data.data.list || []
      pagination.value.total = res.data.data.total || 0
    } else {
      message.error(res.data.message || '获取应用列表失败')
    }
  } catch (error) {
    message.error('获取应用列表失败')
  } finally {
    loading.value = false
  }
}

// 切换精选状态
const toggleFeatured = async (app: API.AppVO) => {
  if (!app.id) return

  const newFeaturedStatus = !isFeatured(app)
  const oldPriority = app.priority

  // 先乐观更新本地状态
  app.priority = newFeaturedStatus ? 1 : 0

  try {
    const res = await pinApp({
      appId: app.id,
      appPinRequest: {
        appId: app.id,
      },
    })
    if (res.data.code === 0) {
      message.success(newFeaturedStatus ? '已设为精选' : '已取消精选')
    } else {
      // 失败则回滚状态
      app.priority = oldPriority
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    // 失败则回滚状态
    app.priority = oldPriority
    message.error('操作失败')
  }
}

// 判断是否精选
const isFeatured = (app: API.AppVO) => {
  return app.priority !== undefined && app.priority > 0
}

// 打开编辑弹窗
const openEditModal = (app: API.AppVO) => {
  editingApp.value = { ...app }
  appForm.value = {
    id: app.id,
    appName: app.appName || '',
  }
  modalVisible.value = true
}

// 打开详情弹窗
const openDetailModal = (app: API.AppVO) => {
  detailApp.value = app
  detailVisible.value = true
}

// 保存应用（编辑名称）
const handleSave = async () => {
  if (!appForm.value.appName) {
    message.warning('应用名称不能为空')
    return
  }
  if (!appForm.value.id) {
    message.error('应用 ID 不存在')
    return
  }

  modalLoading.value = true
  try {
    const res = await updateAppByAdmin({
      id: appForm.value.id,
      appName: appForm.value.appName,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      modalVisible.value = false
      fetchApps()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

// 删除应用
const handleDelete = (app: API.AppVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除应用 "${app.appName || '未命名应用'}" 吗？此操作不可恢复。`,
    okText: '确定',
    okButtonProps: { danger: true },
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteAppByAdmin({ id: app.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          fetchApps()
        } else {
          message.error(res.data.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

// 搜索
const handleSearch = () => {
  pagination.value.current = 1
  fetchApps()
}

// 重置搜索
const handleReset = () => {
  searchForm.value = {
    appName: '',
    userId: '',
    codeGenType: '',
    onlyFeatured: false,
  }
  pagination.value.current = 1
  fetchApps()
}

// 分页变化
const handlePageChange = (page: number) => {
  pagination.value.current = page
  fetchApps()
}

// 每页条数变化
const handlePageSizeChange = (size: number) => {
  pagination.value.pageSize = size
  pagination.value.current = 1
  fetchApps()
}

// 跳转到指定页
const handleJumpToPage = () => {
  const page = parseInt(jumpPageInput.value)
  if (isNaN(page)) return
  const total = Math.ceil(pagination.value.total / pagination.value.pageSize)
  if (page < 1 || page > total) {
    message.warning(`请输入 1 到 ${total} 之间的页码`)
    return
  }
  jumpPageInput.value = ''
  handlePageChange(page)
}

// 计算总页数
const totalPages = computed(() => {
  return Math.ceil(pagination.value.total / pagination.value.pageSize)
})

// 显示的页码列表
const displayedPages = computed(() => {
  const current = pagination.value.current
  const total = totalPages.value
  const pages: (number | string)[] = []

  if (total <= 7) {
    // 总页数小于等于7，显示全部页码
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    // 总页数大于7，显示部分页码
    pages.push(1)

    if (current <= 4) {
      // 当前页在前面
      for (let i = 2; i <= 5; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      // 当前页在后面
      pages.push('...')
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      // 当前页在中间
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) {
        pages.push(i)
      }
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
})

// 格式化时间
const formatTime = (timeStr?: string) => {
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

// 截断文本
const truncateText = (text: string, maxLength: number) => {
  if (!text) return 'null'
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

// 获取代码类型标签
const getCodeTypeLabel = (type?: string) => {
  if (!type) return '-'
  const t = type.toUpperCase()
  if (t === 'HTML') return '单文件'
  if (t === 'MULTI_FILE') return '多文件'
  // 兼容旧值
  if (t.includes('SINGLE') || t.includes('ONE')) return '单文件'
  if (t.includes('MULTI')) return '多文件'
  return '-'
}

// 获取代码类型样式类
const getCodeTypeClass = (type?: string) => {
  if (!type) return ''
  const t = type.toUpperCase()
  if (t === 'HTML') return 'type-single'
  if (t === 'MULTI_FILE') return 'type-multi'
  // 兼容旧值
  if (t.includes('SINGLE') || t.includes('ONE')) return 'type-single'
  if (t.includes('MULTI')) return 'type-multi'
  return ''
}

onMounted(() => {
  fetchApps()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="app-manage-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <span class="title-prompt">$</span>
        <span class="title-text">app_manage</span>
      </h1>
    </div>

    <!-- 搜索筛选面板 -->
    <div class="filter-panel">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">应用名称</label>
          <input
            v-model="searchForm.appName"
            type="text"
            class="filter-input"
            placeholder="搜索应用名称..."
            @keydown.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">创建者ID</label>
          <input
            v-model="searchForm.userId"
            type="number"
            class="filter-input no-spinner"
            placeholder="输入用户ID..."
            @keydown.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">代码类型</label>
          <div class="custom-dropdown" :class="{ open: codeTypeDropdownOpen }">
            <div class="dropdown-trigger" @click="toggleCodeTypeDropdown">
              <span>{{ selectedCodeTypeLabel() }}</span>
              <span class="dropdown-arrow" :class="{ rotate: codeTypeDropdownOpen }"></span>
            </div>
            <div class="dropdown-menu" v-show="codeTypeDropdownOpen">
              <div
                v-for="(opt, index) in codeGenTypeOptions"
                :key="opt.value"
                class="dropdown-item"
                :style="{ animationDelay: `${index * 0.05}s` }"
                @click="selectCodeType(opt.value)"
              >
                {{ opt.label }}
              </div>
            </div>
          </div>
        </div>
        <div class="filter-item filter-checkbox">
          <label class="featured-toggle" :class="{ active: searchForm.onlyFeatured }">
            <input v-model="searchForm.onlyFeatured" type="checkbox" />
            <span class="toggle-slider"></span>
            <span class="toggle-label">只看精选</span>
          </label>
        </div>
        <div class="filter-actions">
          <button class="filter-btn filter-btn-primary" @click="handleSearch">
            <SearchOutlined />
            <span>搜索</span>
          </button>
          <button class="filter-btn filter-btn-secondary" @click="handleReset">
            <span>重置</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-wrapper">
      <div class="table-header">
        <div class="table-info">
          <span class="table-count">{{ pagination.total }}</span>
          <span class="table-label">records</span>
        </div>
      </div>

      <div class="table-container" :class="{ loading: loading }">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-featured">精选</th>
              <th class="col-id">ID</th>
              <th class="col-name">应用名称</th>
              <th class="col-user">创建者</th>
              <th class="col-type">代码类型</th>
              <th class="col-prompt">初始需求</th>
              <th class="col-time">创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody v-if="!loading">
            <tr v-for="app in dataSource" :key="app.id" class="table-row">
              <td class="col-featured">
                <button
                  class="featured-btn"
                  :class="{ active: isFeatured(app) }"
                  @click="toggleFeatured(app)"
                  :title="isFeatured(app) ? '取消精选' : '设为精选'"
                >
                  <StarFilled v-if="isFeatured(app)" />
                  <StarOutlined v-else />
                </button>
              </td>
              <td class="col-id">
                <span class="cell-mono">{{ app.id }}</span>
              </td>
              <td class="col-name">
                <span class="app-name">
                  <span v-if="isFeatured(app)" class="featured-badge">精选</span>
                  {{ app.appName || '未命名应用' }}
                </span>
              </td>
              <td class="col-user">
                <span class="cell-mono">{{ app.user?.userName || app.userId || '-' }}</span>
              </td>
              <td class="col-type">
                <span class="code-type-badge" :class="getCodeTypeClass(app.codeGenType)">
                  {{ getCodeTypeLabel(app.codeGenType) }}
                </span>
              </td>
              <td class="col-prompt">
                <span class="prompt-text" :title="app.initPrompt">
                  {{ truncateText(app.initPrompt || '', 40) }}
                </span>
              </td>
              <td class="col-time">
                <span class="cell-mono cell-time">{{ formatTime(app.createTime) }}</span>
              </td>
              <td class="col-action">
                <div class="action-buttons">
                  <button class="action-btn" @click="openDetailModal(app)">view</button>
                  <button class="action-btn" @click="openEditModal(app)">edit</button>
                  <button class="action-btn action-danger" @click="handleDelete(app)">del</button>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr v-for="i in 5" :key="i" class="skeleton-row">
              <td v-for="j in 8" :key="j" class="skeleton-cell">
                <div class="skeleton-line"></div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="table-footer">
        <div class="pagination-info">
          <span class="pagination-text">// {{ pagination.total }} entries</span>
        </div>
        <div class="pagination-controls">
          <!-- 每页条数选择 -->
          <div class="page-size-selector">
            <span class="page-size-label">每页</span>
            <div class="page-size-dropdown" @click.stop>
              <select
                :value="pagination.pageSize"
                @change="handlePageSizeChange(Number($event.target.value))"
                class="page-size-select"
              >
                <option v-for="size in pageSizeOptions" :key="size" :value="size">
                  {{ size }}
                </option>
              </select>
            </div>
            <span class="page-size-label">条</span>
          </div>

          <!-- 分页按钮 -->
          <div class="pagination-buttons">
            <button
              class="page-btn"
              :disabled="pagination.current === 1"
              @click="handlePageChange(1)"
              title="首页"
            >
              &laquo;
            </button>
            <button
              class="page-btn"
              :disabled="pagination.current === 1"
              @click="handlePageChange(pagination.current - 1)"
            >
              &lt; prev
            </button>

            <!-- 页码列表 -->
            <div class="page-numbers">
              <template v-for="page in displayedPages" :key="page">
                <span v-if="page === '...'" class="page-ellipsis">...</span>
                <button
                  v-else
                  :class="['page-number-btn', { active: page === pagination.current }]"
                  @click="handlePageChange(page as number)"
                >
                  {{ page }}
                </button>
              </template>
            </div>

            <button
              class="page-btn"
              :disabled="pagination.current >= totalPages"
              @click="handlePageChange(pagination.current + 1)"
            >
              next &gt;
            </button>
            <button
              class="page-btn"
              :disabled="pagination.current >= totalPages"
              @click="handlePageChange(totalPages)"
              title="末页"
            >
              &raquo;
            </button>
          </div>

          <!-- 跳转输入 -->
          <div class="pagination-jump">
            <span class="jump-label">跳至</span>
            <input
              v-model="jumpPageInput"
              type="number"
              class="jump-input"
              placeholder="页码"
              min="1"
              :max="totalPages"
              @keydown.enter="handleJumpToPage"
            />
            <button class="jump-btn" @click="handleJumpToPage">GO</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑应用弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      title="null"
      :footer="null"
      :closable="false"
      width="420px"
    >
      <div class="edit-modal">
        <div class="modal-header">
          <span class="modal-prompt">$</span>
          <span class="modal-title">update_app</span>
        </div>

        <div class="modal-form">
          <div class="form-group">
            <label class="form-label">应用ID // read_only</label>
            <div class="form-input-readonly">{{ editingApp?.id }}</div>
          </div>
          <div class="form-group">
            <label class="form-label">应用名称</label>
            <input
              v-model="appForm.appName"
              class="form-input"
              placeholder="输入应用名称..."
            />
          </div>
        </div>

        <div class="modal-footer">
          <button class="modal-btn modal-btn-cancel" @click="modalVisible = false">
            <span>取消</span>
          </button>
          <button class="modal-btn modal-btn-confirm" @click="handleSave">
            <span v-if="!modalLoading">保存</span>
            <span v-else class="btn-spinner"></span>
          </button>
        </div>
      </div>
    </a-modal>

    <!-- 应用详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="null"
      :footer="null"
      :closable="false"
      width="500px"
    >
      <div class="detail-modal">
        <div class="modal-header">
          <span class="modal-prompt">$</span>
          <span class="modal-title">app_detail</span>
          <button class="modal-close" @click="detailVisible = false">×</button>
        </div>

        <div class="detail-content" v-if="detailApp">
          <div class="detail-item">
            <span class="detail-key">id</span>
            <span class="detail-value detail-mono">{{ detailApp.id }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">app_name</span>
            <span class="detail-value">
              <span v-if="isFeatured(detailApp)" class="featured-badge-small">精选</span>
              {{ detailApp.appName || '未命名应用' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-key">creator</span>
            <span class="detail-value">{{ detailApp.user?.userName || detailApp.userId || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">code_type</span>
            <span class="detail-value">
              <span class="code-type-badge" :class="getCodeTypeClass(detailApp.codeGenType)">
                {{ getCodeTypeLabel(detailApp.codeGenType) }}
              </span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-key">priority</span>
            <span class="detail-value detail-mono">{{ detailApp.priority ?? 0 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">init_prompt</span>
            <span class="detail-value prompt-full">{{ detailApp.initPrompt || 'null' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">created_at</span>
            <span class="detail-value detail-mono">{{ detailApp.createTime }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">updated_at</span>
            <span class="detail-value detail-mono">{{ detailApp.updateTime || 'null' }}</span>
          </div>
        </div>

        <div class="modal-footer">
          <button class="modal-btn modal-btn-close" @click="detailVisible = false">
            <span>关闭</span>
          </button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
/* ===== CSS 变量 ===== */
.app-manage-container {
  --bg-primary: #0a0a0a;
  --bg-secondary: #111111;
  --bg-card: #1a1a1a;
  --text-primary: #ffffff;
  --text-secondary: #888888;
  --text-muted: #444444;
  --accent-green: #00d26a;
  --accent-green-dim: rgba(0, 210, 106, 0.1);
  --accent-gold: #ffd700;
  --accent-gold-dim: rgba(255, 215, 0, 0.15);
  --border-color: #2a2a2a;
  --border-hover: #00d26a;
  --danger-color: #ff4757;

  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 40px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  box-sizing: border-box;
  overflow: hidden;
}

/* ===== 页面头部 ===== */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 500;
  margin: 0;
}

.title-prompt {
  color: var(--accent-green);
  font-weight: 600;
}

.title-text {
  color: var(--text-primary);
}

/* ===== 筛选面板 ===== */
.filter-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 24px;
  padding: 16px 20px;
  flex-shrink: 0;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px 32px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-item.filter-checkbox {
  justify-content: center;
  padding-bottom: 4px;
}

.filter-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.filter-input,
.filter-select {
  min-width: 160px;
  padding: 8px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-family: inherit;
  font-size: 13px;
  outline: none;
  transition: all 0.2s ease;
}

/* 移除数字输入框的上下箭头 */
.filter-input.no-spinner::-webkit-inner-spin-button,
.filter-input.no-spinner::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.filter-input.no-spinner {
  -moz-appearance: textfield;
}

.filter-input:focus,
.filter-select:focus {
  border-color: var(--accent-green);
}

.filter-input::placeholder {
  color: var(--text-muted);
}

/* ===== 自定义下拉框 ===== */
.custom-dropdown {
  position: relative;
  min-width: 160px;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.dropdown-trigger:hover {
  border-color: var(--accent-green);
}

.custom-dropdown.open .dropdown-trigger {
  border-color: var(--accent-green);
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.dropdown-arrow {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid var(--text-muted);
  transition: all 0.3s ease;
}

.dropdown-arrow.rotate {
  transform: rotate(180deg);
  border-top-color: var(--accent-green);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 0 0 6px 6px;
  border-top: none;
  margin-top: -1px;
  z-index: 100;
  overflow: hidden;
}

.dropdown-item {
  padding: 10px 12px;
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  opacity: 0;
  animation: slideDown 0.3s ease forwards;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dropdown-item:hover {
  background: var(--accent-green-dim);
  color: var(--accent-green);
}

/* 精选切换开关 */
.featured-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.featured-toggle input[type="checkbox"] {
  display: none;
}

.toggle-slider {
  position: relative;
  width: 40px;
  height: 20px;
  background: var(--border-color);
  border-radius: 10px;
  transition: all 0.3s ease;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  background: var(--text-secondary);
  border-radius: 50%;
  transition: all 0.3s ease;
}

.featured-toggle.active .toggle-slider {
  background: var(--accent-gold);
}

.featured-toggle.active .toggle-slider::before {
  transform: translateX(20px);
  background: #fff;
}

.toggle-label {
  color: var(--text-secondary);
  font-size: 13px;
  transition: color 0.2s ease;
}

.featured-toggle.active .toggle-label {
  color: var(--accent-gold);
  font-weight: 500;
}

.filter-actions {
  display: flex;
  gap: 10px;
  padding-bottom: 4px;
  margin-left: auto;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
}

.filter-btn-primary {
  background: var(--accent-green-dim);
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.filter-btn-primary:hover {
  background: var(--accent-green);
  color: var(--bg-primary);
}

.filter-btn-secondary {
  background: transparent;
  color: var(--text-secondary);
}

.filter-btn-secondary:hover {
  border-color: var(--text-muted);
  color: var(--text-primary);
}

/* ===== 表格容器 ===== */
.table-wrapper {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  flex-shrink: 0;
}

.table-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-count {
  font-size: 18px;
  font-weight: 600;
  color: var(--accent-green);
}

.table-label {
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
}

/* ===== 数据表格 ===== */
.table-container {
  overflow-x: auto;
  overflow-y: auto;
  flex: 1;
  min-height: 0;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  background: var(--bg-secondary);
}

.data-table th {
  padding: 14px 16px;
  text-align: left;
  font-weight: 500;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
  white-space: nowrap;
}

.data-table th:first-child {
  padding-left: 20px;
}

.data-table th:last-child {
  padding-right: 20px;
}

.data-table tbody tr {
  border-bottom: 1px solid var(--border-color);
  transition: background 0.2s ease;
}

.data-table tbody tr:hover {
  background: var(--bg-secondary);
}

.data-table tbody tr:last-child {
  border-bottom: none;
}

.data-table td {
  padding: 14px 16px;
  color: var(--text-primary);
}

.data-table td:first-child {
  padding-left: 20px;
}

.data-table td:last-child {
  padding-right: 20px;
}

/* 单元格样式 */
.cell-mono {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  color: var(--text-secondary);
  font-size: 12px;
}

.cell-time {
  color: var(--text-muted);
}

.col-featured {
  width: 60px;
  text-align: center;
}

.featured-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;
  font-size: 16px;
}

.featured-btn:hover {
  color: var(--accent-gold);
  background: var(--accent-gold-dim);
}

.featured-btn.active {
  color: var(--accent-gold);
}

.app-name {
  color: var(--accent-green);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.featured-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  background: var(--accent-gold-dim);
  color: var(--accent-gold);
  font-size: 11px;
  border-radius: 4px;
  font-weight: 500;
}

.featured-badge-small {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  background: var(--accent-gold-dim);
  color: var(--accent-gold);
  font-size: 11px;
  border-radius: 4px;
  font-weight: 500;
}

.code-type-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.code-type-badge.type-single {
  background: rgba(0, 210, 106, 0.1);
  color: var(--accent-green);
}

.code-type-badge.type-multi {
  background: rgba(255, 215, 0, 0.1);
  color: var(--accent-gold);
}

.prompt-text {
  max-width: 250px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}

.prompt-full {
  display: block;
  max-width: 400px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 6px;
}

.action-btn {
  padding: 4px 10px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.action-danger:hover {
  border-color: var(--danger-color);
  color: var(--danger-color);
}

/* ===== 骨架屏 ===== */
.skeleton-row td {
  padding: 14px 16px;
}

.skeleton-cell {
  padding: 0;
}

.skeleton-line {
  height: 14px;
  background: linear-gradient(90deg, var(--bg-secondary) 25%, var(--border-color) 50%, var(--bg-secondary) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  width: 80%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.table-container.loading {
  min-height: 200px;
}

/* ===== 表格底部 ===== */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
  flex-wrap: wrap;
  gap: 12px;
  flex-shrink: 0;
}

.pagination-text {
  font-size: 12px;
  color: var(--text-muted);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 每页条数选择器 */
.page-size-selector {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-size-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.page-size-dropdown {
  position: relative;
}

.page-size-select {
  padding: 4px 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: all 0.2s ease;
}

.page-size-select:hover {
  border-color: var(--accent-green);
}

.page-size-select:focus {
  border-color: var(--accent-green);
}

/* 分页按钮组 */
.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  padding: 4px 10px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 32px;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 页码列表 */
.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-number-btn {
  padding: 4px 8px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 32px;
}

.page-number-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.page-number-btn.active {
  background: var(--accent-green);
  border-color: var(--accent-green);
  color: var(--bg-primary);
}

.page-ellipsis {
  padding: 4px 8px;
  color: var(--text-muted);
  font-size: 12px;
  min-width: 32px;
  text-align: center;
}

/* 跳转输入 */
.pagination-jump {
  display: flex;
  align-items: center;
  gap: 6px;
}

.jump-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.jump-input {
  width: 50px;
  padding: 4px 8px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-primary);
  font-size: 12px;
  font-family: inherit;
  text-align: center;
  outline: none;
  transition: all 0.2s ease;
}

.jump-input:focus {
  border-color: var(--accent-green);
}

.jump-input::-webkit-inner-spin-button,
.jump-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.jump-input {
  -moz-appearance: textfield;
}

.jump-btn {
  padding: 4px 10px;
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  color: var(--text-secondary);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.jump-btn:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
}

/* ===== 编辑弹窗 ===== */
.edit-modal :deep(.ant-modal-content) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
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
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  position: relative;
}

.modal-prompt {
  font-size: 14px;
  color: var(--accent-green);
  font-weight: 600;
}

.modal-title {
  font-size: 14px;
  color: var(--text-primary);
}

.modal-close {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: var(--border-color);
  color: var(--text-primary);
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
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-primary);
  font-family: inherit;
  font-size: 13px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: var(--accent-green);
}

.form-input::placeholder {
  color: var(--text-muted);
}

.form-input-readonly {
  padding: 10px 14px;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--text-muted);
  font-family: inherit;
  font-size: 13px;
}

.modal-footer {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
  justify-content: flex-end;
}

.modal-btn {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--text-secondary);
  font-family: inherit;
}

.modal-btn-cancel:hover {
  border-color: var(--text-muted);
  color: var(--text-primary);
}

.modal-btn-confirm {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
  min-width: 80px;
}

.modal-btn-confirm:hover {
  background: var(--accent-green);
  color: var(--bg-primary);
}

.modal-btn-close {
  border-color: var(--border-color);
  color: var(--text-secondary);
}

.modal-btn-close:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(10, 10, 10, 0.3);
  border-top-color: #0a0a0a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 详情弹窗 ===== */
.detail-modal :deep(.ant-modal-content) {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
}

.detail-modal :deep(.ant-modal-body) {
  padding: 0;
}

.detail-content {
  padding: 20px;
  max-height: 60vh;
  overflow-y: auto;
}

.detail-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-key {
  width: 100px;
  font-size: 12px;
  color: var(--text-muted);
  text-transform: uppercase;
  flex-shrink: 0;
}

.detail-value {
  flex: 1;
  font-size: 13px;
  color: var(--text-primary);
}

.detail-mono {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  color: var(--text-secondary);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .app-manage-container {
    padding: 20px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .data-table {
    font-size: 12px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-item,
  .filter-input,
  .filter-select {
    min-width: 100%;
  }

  .table-footer {
    flex-direction: column;
    align-items: center;
  }

  .pagination-controls {
    flex-wrap: wrap;
    justify-content: center;
  }

  .pagination-buttons {
    flex-wrap: wrap;
    justify-content: center;
  }

  .page-numbers {
    display: none;
  }
}
</style>
