<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
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
    const res = await list({
      userQueryRequest: { pageNum: 1, pageSize: 100 },
    })
    if (res.data.code === 0) {
      dataSource.value = res.data.data || []
    } else {
      message.error(res.data.message || '获取用户列表失败')
    }
  } catch (error) {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
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
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (error) {
    message.error('操作失败')
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
        const res = await deleteUsingPost({ id: String(user.id) })
        if (res.data.code === 0) {
          message.success('删除成功')
          fetchUsers()
        } else {
          message.error(res.data.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div class="user-manage-container">
    <a-card title="用户管理" class="user-card">
      <template #extra>
        <a-button type="primary" @click="openAddModal">添加用户</a-button>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }"
        :scroll="{ x: 1100 }"
        row-key="id"
        size="large"
        class="user-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'id'">
            <a-tooltip :title="String(record.id)">
              <span class="id-cell">{{ record.id }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'userAvatar'">
            <a-avatar v-if="record.userAvatar" :src="record.userAvatar" :size="36" />
            <span v-else class="empty-text">-</span>
          </template>
          <template v-if="column.key === 'userRole'">
            <a-tag :color="record.userRole === 'admin' ? 'red' : 'processing'">
              {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'userProfile'">
            <a-tooltip :title="record.userProfile">
              <span class="profile-cell">{{ record.userProfile || '-' }}</span>
            </a-tooltip>
          </template>
          <template v-if="column.key === 'action'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openDetailModal(record)">
                详情
              </a-button>
              <a-button type="link" size="small" @click="openEditModal(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" danger @click="handleDelete(record)">
                删除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加/编辑用户弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleSave"
      width="500px"
    >
      <a-form layout="vertical" :model="userForm" style="margin-top: 24px">
        <a-form-item label="账号">
          <a-input
            v-model:value="userForm.userAccount"
            placeholder="请输入账号"
            :disabled="!!editingUser?.id"
          />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="userForm.userName" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="头像URL">
          <a-input v-model:value="userForm.userAvatar" placeholder="请输入头像URL" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="userForm.userRole">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea
            v-model:value="userForm.userProfile"
            placeholder="请输入用户简介"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 用户详情弹窗 -->
    <a-modal v-model:open="detailVisible" title="用户详情" :footer="null" width="500px">
      <a-descriptions v-if="detailUser" :column="1" bordered>
        <a-descriptions-item label="ID">
          {{ detailUser.id }}
        </a-descriptions-item>
        <a-descriptions-item label="账号">
          {{ detailUser.userAccount }}
        </a-descriptions-item>
        <a-descriptions-item label="用户名">
          {{ detailUser.userName }}
        </a-descriptions-item>
        <a-descriptions-item label="头像">
          <div v-if="detailUser.userAvatar" class="avatar-preview">
            <a-avatar :src="detailUser.userAvatar" :size="64" />
          </div>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="角色">
          <a-tag :color="detailUser.userRole === 'admin' ? 'red' : 'blue'">
            {{ detailUser.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="简介">
          {{ detailUser.userProfile || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ detailUser.createTime }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<style scoped>
.user-manage-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.user-card {
  background: #fff;
}

.avatar-preview {
  display: flex;
  justify-content: center;
}

.empty-text {
  color: #999;
}

.id-cell {
  display: inline-block;
  max-width: 70px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.profile-cell {
  display: inline-block;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

.profile-cell {
  display: inline-block;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-table :deep(.ant-table) {
  font-size: 15px;
}

.user-table :deep(.ant-table-thead > tr > th) {
  font-weight: 600;
  background: #fafafa;
  font-size: 15px;
  padding: 16px 12px;
}

.user-table :deep(.ant-table-tbody > tr > td) {
  padding: 16px 12px;
}

.user-table :deep(.ant-btn-link) {
  padding: 0 4px;
  font-size: 15px;
}
</style>
