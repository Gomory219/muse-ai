<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  list,
  save,
  update,
  deleteUsingPost,
  getVo,
} from '@/api/userController'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount', width: 140 },
  { title: '用户名', dataIndex: 'userName', key: 'userName', width: 120 },
  { title: '头像', dataIndex: 'userAvatar', key: 'userAvatar', width: 90, align: 'center' },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100, align: 'center' },
  { title: '简介', dataIndex: 'userProfile', key: 'userProfile', width: 200 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 220, align: 'center' },
]

const dataSource = ref<API.UserVO[]>([])
const loading = ref(false)

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
  userName: '',
  userRole: '',
})

// 角色下拉框状态
const roleDropdownOpen = ref(false)

// 角色选项
const roleOptions = [
  { label: '全部角色', value: '' },
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'admin' },
]

// 切换角色下拉框
const toggleRoleDropdown = () => {
  roleDropdownOpen.value = !roleDropdownOpen.value
}

// 选择角色
const selectRole = (value: string) => {
  searchForm.value.userRole = value
  roleDropdownOpen.value = false
}

// 获取当前选中的角色标签
const selectedRoleLabel = () => {
  const option = roleOptions.find(opt => opt.value === searchForm.value.userRole)
  return option ? option.label : '全部角色'
}

// 点击外部关闭下拉框的处理器
const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.custom-dropdown')) {
    roleDropdownOpen.value = false
  }
}

// 添加/编辑用户弹窗
const modalVisible = ref(false)
const modalTitle = computed(() => (editingUser.value?.id ? '编辑用户' : '添加用户'))
const editingUser = ref<API.UserVO | null>(null)
const modalLoading = ref(false)

const userForm = ref<API.UserVO>({
  id: undefined,
  userAccount: '',
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: 'user',
  createTime: '',
})

// 详情弹窗
const detailVisible = ref(false)
const detailUser = ref<API.UserVO | null>(null)

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const queryParams: API.UserQueryRequest = {
      pageNum: pagination.value.current,
      pageSize: pagination.value.pageSize,
    }

    // 添加搜索条件
    if (searchForm.value.userName) {
      queryParams.userName = searchForm.value.userName
    }
    if (searchForm.value.userRole) {
      queryParams.userRole = searchForm.value.userRole as 'user' | 'admin'
    }

    const res = await list({
      userQueryRequest: queryParams,
    })
    if (res.data.code === 0 && res.data.data) {
      dataSource.value = res.data.data.list || []
      pagination.value.total = res.data.data.total || 0
    }
  } catch (error) {
    // 错误已由全局拦截器处理
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.current = 1
  fetchUsers()
}

// 重置搜索
const handleReset = () => {
  searchForm.value = {
    userName: '',
    userRole: '',
  }
  pagination.value.current = 1
  fetchUsers()
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

// 分页变化
const handlePageChange = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  pagination.value.current = page
  fetchUsers()
}

// 每页条数变化
const handlePageSizeChange = (size: number) => {
  pagination.value.pageSize = size
  pagination.value.current = 1
  fetchUsers()
}

// 处理 select 的 change 事件
const handleSelectChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  if (target) {
    handlePageSizeChange(Number(target.value))
  }
}

// 跳转到指定页
const handleJumpToPage = () => {
  const page = parseInt(jumpPageInput.value)
  if (isNaN(page)) return
  if (page < 1 || page > totalPages.value) {
    message.warning(`请输入 1 到 ${totalPages.value} 之间的页码`)
    return
  }
  jumpPageInput.value = ''
  handlePageChange(page)
}

// 打开添加用户弹窗
const openAddModal = () => {
  editingUser.value = null
  userForm.value = {
    id: undefined,
    userAccount: '',
    userName: '',
    userAvatar: '',
    userProfile: '',
    userRole: 'user',
    createTime: '',
  }
  modalVisible.value = true
}

// 打开编辑用户弹窗
const openEditModal = async (user: API.UserVO) => {
  // 先使用表格数据填充表单
  editingUser.value = { ...user }
  userForm.value = { ...user }
  modalVisible.value = true

  // 尝试获取更详细的用户信息
  try {
    const res = await getVo({ id: user.id! })
    if (res.data.code === 0 && res.data.data) {
      editingUser.value = res.data.data
      userForm.value = { ...res.data.data }
    }
  } catch (error) {
    // 忽略错误，使用表格数据
    console.log('获取详情失败，使用表格数据', error)
  }
}

// 打开详情弹窗
const openDetailModal = async (user: API.UserVO) => {
  // 直接使用表格中的数据显示详情
  detailUser.value = user
  detailVisible.value = true

  // 尝试获取更详细的用户信息
  try {
    const res = await getVo({ id: user.id! })
    if (res.data.code === 0 && res.data.data) {
      detailUser.value = res.data.data
    }
  } catch (error) {
    // 忽略错误，使用表格数据
    console.log('获取详情失败，使用表格数据', error)
  }
}

// 保存用户（添加或编辑）
const handleSave = async () => {
  if (!userForm.value.userAccount || !userForm.value.userName) {
    message.warning('账号和用户名不能为空')
    return
  }

  modalLoading.value = true
  try {
    let res
    if (editingUser.value?.id) {
      // 编辑
      res = await update({
        id: editingUser.value.id,
        userName: userForm.value.userName,
        userAvatar: userForm.value.userAvatar,
        userProfile: userForm.value.userProfile,
        userRole: userForm.value.userRole,
      })
    } else {
      // 添加
      res = await save({
        userAddRequest: {
          userAccount: userForm.value.userAccount,
          userName: userForm.value.userName,
          userAvatar: userForm.value.userAvatar,
          userProfile: userForm.value.userProfile,
          userRole: userForm.value.userRole,
        },
      })
    }

    if (res.data.code === 0) {
      message.success(editingUser.value?.id ? '修改成功' : '添加成功')
      modalVisible.value = false
      fetchUsers()
    }
  } catch (error) {
    // 错误已由全局拦截器处理
  } finally {
    modalLoading.value = false
  }
}

// 删除用户
const handleDelete = (user: API.UserVO) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户 "${user.userName}" 吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await deleteUsingPost({ id: user.id })
        if (res.data.code === 0) {
          message.success('删除成功')
          fetchUsers()
        }
      } catch (error) {
        // 错误已由全局拦截器处理
      }
    },
  })
}

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

onMounted(() => {
  fetchUsers()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div class="user-manage-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">
        <span class="title-prompt">$</span>
        <span class="title-text">user_manage</span>
      </h1>
    </div>

    <!-- 搜索筛选面板 -->
    <div class="filter-panel">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">用户名</label>
          <input
            v-model="searchForm.userName"
            type="text"
            class="filter-input"
            placeholder="搜索用户名..."
            @keydown.enter="handleSearch"
          />
        </div>
        <div class="filter-item">
          <label class="filter-label">角色</label>
          <div class="custom-dropdown" :class="{ open: roleDropdownOpen }">
            <div class="dropdown-trigger" @click="toggleRoleDropdown">
              <span>{{ selectedRoleLabel() }}</span>
              <span class="dropdown-arrow" :class="{ rotate: roleDropdownOpen }"></span>
            </div>
            <div class="dropdown-menu" v-show="roleDropdownOpen">
              <div
                v-for="(opt, index) in roleOptions"
                :key="opt.value"
                class="dropdown-item"
                :style="{ animationDelay: `${index * 0.05}s` }"
                @click="selectRole(opt.value)"
              >
                {{ opt.label }}
              </div>
            </div>
          </div>
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
        <button class="btn-primary" @click="openAddModal">
          <span class="btn-bracket">&lt;</span>
          <span>add_user</span>
          <span class="btn-bracket">/&gt;</span>
        </button>
      </div>

      <div class="table-container" :class="{ loading: loading }">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-id">ID</th>
              <th class="col-account">账号</th>
              <th class="col-name">用户名</th>
              <th class="col-avatar">头像</th>
              <th class="col-role">角色</th>
              <th class="col-profile">简介</th>
              <th class="col-time">创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody v-if="!loading">
            <tr v-for="user in dataSource" :key="user.id" class="table-row">
              <td class="col-id">
                <span class="cell-mono">{{ user.id }}</span>
              </td>
              <td class="col-account">
                <span class="cell-mono">{{ user.userAccount }}</span>
              </td>
              <td class="col-name">{{ user.userName }}</td>
              <td class="col-avatar">
                <div v-if="user.userAvatar" class="avatar-small">
                  <img :src="user.userAvatar" :alt="user.userName" />
                </div>
                <span v-else class="empty-value">null</span>
              </td>
              <td class="col-role">
                <span :class="['role-tag', user.userRole]">
                  {{ user.userRole === 'admin' ? 'admin' : 'user' }}
                </span>
              </td>
              <td class="col-profile">
                <span class="profile-text">{{ user.userProfile || 'null' }}</span>
              </td>
              <td class="col-time">
                <span class="cell-mono cell-time">{{ formatTime(user.createTime) }}</span>
              </td>
              <td class="col-action">
                <div class="action-buttons">
                  <button class="action-btn" @click="openDetailModal(user)">view</button>
                  <button class="action-btn" @click="openEditModal(user)">edit</button>
                  <button class="action-btn action-danger" @click="handleDelete(user)">del</button>
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
                @change="handleSelectChange"
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

    <!-- 添加/编辑用户弹窗 -->
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
          <span class="modal-title">{{ editingUser?.id ? 'update_user' : 'create_user' }}</span>
        </div>

        <div class="modal-form">
          <div class="form-group">
            <label class="form-label">账号</label>
            <input
              v-model="userForm.userAccount"
              class="form-input"
              placeholder="输入账号..."
              :disabled="!!editingUser?.id"
            />
          </div>
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input
              v-model="userForm.userName"
              class="form-input"
              placeholder="输入用户名..."
            />
          </div>
          <div class="form-group">
            <label class="form-label">头像URL</label>
            <input
              v-model="userForm.userAvatar"
              class="form-input"
              placeholder="输入头像URL..."
            />
          </div>
          <div class="form-group">
            <label class="form-label">角色</label>
            <div class="role-selector">
              <div
                :class="['role-option', { active: userForm.userRole === 'user' }]"
                @click="userForm.userRole = 'user'"
              >
                <span>user</span>
              </div>
              <div
                :class="['role-option', { active: userForm.userRole === 'admin' }]"
                @click="userForm.userRole = 'admin'"
              >
                <span>admin</span>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">简介</label>
            <textarea
              v-model="userForm.userProfile"
              class="form-textarea"
              placeholder="输入用户简介..."
              rows="3"
            ></textarea>
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

    <!-- 用户详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      title="null"
      :footer="null"
      :closable="false"
      width="420px"
    >
      <div class="detail-modal">
        <div class="modal-header">
          <span class="modal-prompt">$</span>
          <span class="modal-title">user_detail</span>
          <button class="modal-close" @click="detailVisible = false">×</button>
        </div>

        <div class="detail-content" v-if="detailUser">
          <div class="detail-item">
            <span class="detail-key">id</span>
            <span class="detail-value">{{ detailUser.id }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">account</span>
            <span class="detail-value detail-mono">{{ detailUser.userAccount }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">username</span>
            <span class="detail-value">{{ detailUser.userName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">avatar</span>
            <div class="detail-value">
              <div v-if="detailUser.userAvatar" class="avatar-large">
                <img :src="detailUser.userAvatar" :alt="detailUser.userName" />
              </div>
              <span v-else class="empty-value">null</span>
            </div>
          </div>
          <div class="detail-item">
            <span class="detail-key">role</span>
            <span class="detail-value">
              <span :class="['role-tag', detailUser.userRole]">
                {{ detailUser.userRole === 'admin' ? 'admin' : 'user' }}
              </span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-key">profile</span>
            <span class="detail-value">{{ detailUser.userProfile || 'null' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-key">created_at</span>
            <span class="detail-value detail-mono">{{ detailUser.createTime }}</span>
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
.user-manage-container {
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
  margin-bottom: 24px;
  flex-shrink: 0;
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
  gap: 16px;
  flex-shrink: 0;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: var(--accent-green-dim);
  border: 1px solid var(--accent-green);
  border-radius: 6px;
  color: var(--accent-green);
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  background: var(--accent-green);
  color: var(--bg-primary);
}

.btn-bracket {
  color: var(--text-muted);
  font-size: 11px;
}

.btn-primary:hover .btn-bracket {
  color: var(--bg-primary);
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

.profile-text {
  max-width: 200px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-secondary);
}

.empty-value {
  color: var(--text-muted);
  font-style: italic;
}

/* 头像 */
.avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-green);
}

.avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-large {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--accent-green);
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 角色标签 */
.role-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.role-tag.admin {
  background: rgba(255, 71, 87, 0.15);
  color: var(--danger-color);
}

.role-tag.user {
  background: var(--accent-green-dim);
  color: var(--accent-green);
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
  background: #ffffff;
  border: none;
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.edit-modal :deep(.ant-modal-body) {
  padding: 0;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  position: relative;
}

.modal-prompt {
  font-size: 16px;
  color: var(--accent-green);
  font-weight: 600;
}

.modal-title {
  font-size: 15px;
  color: #1a1a1a;
}

.modal-close {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: #f5f5f5;
  border: none;
  color: #666;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: var(--accent-green);
  color: white;
}

.modal-form {
  padding: 24px;
  background: #ffffff;
}

.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 10px;
  font-weight: 500;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  color: #1a1a1a;
  font-family: inherit;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  border-color: var(--accent-green);
  box-shadow: 0 0 0 3px rgba(0, 210, 106, 0.1);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #aaa;
}

.form-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f8f9fa;
}

.form-textarea {
  resize: none;
  line-height: 1.5;
}

/* 角色选择器 */
.role-selector {
  display: flex;
  gap: 10px;
}

.role-option {
  flex: 1;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #666;
}

.role-option:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.role-option.active {
  background: rgba(0, 210, 106, 0.1);
  border-color: var(--accent-green);
  color: var(--accent-green);
  font-weight: 500;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  justify-content: flex-end;
}

.modal-btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e0e0e0;
  background: #ffffff;
  color: #666;
  font-family: inherit;
}

.modal-btn-cancel:hover {
  border-color: #ccc;
  color: #1a1a1a;
}

.modal-btn-confirm {
  border-color: var(--accent-green);
  color: white;
  background: var(--accent-green);
  min-width: 100px;
}

.modal-btn-confirm:hover {
  background: #00c260;
  border-color: #00c260;
  box-shadow: 0 4px 12px rgba(0, 210, 106, 0.3);
}

.modal-btn-close {
  border-color: #e0e0e0;
  color: #666;
}

.modal-btn-close:hover {
  border-color: var(--accent-green);
  color: var(--accent-green);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 详情弹窗 ===== */
.detail-modal :deep(.ant-modal-content) {
  background: #ffffff;
  border: none;
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.detail-modal :deep(.ant-modal-body) {
  padding: 0;
}

.detail-content {
  padding: 20px;
  background: #ffffff;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-key {
  width: 100px;
  font-size: 12px;
  color: #999;
  text-transform: uppercase;
}

.detail-value {
  flex: 1;
  font-size: 13px;
  color: #1a1a1a;
}

.detail-mono {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  color: #666;
  background: #f8f9fa;
  padding: 4px 8px;
  border-radius: 6px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .user-manage-container {
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

  .filter-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
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

  .table-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
