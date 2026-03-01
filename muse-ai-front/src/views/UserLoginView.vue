<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogin } from '@/api/userController'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loginForm = ref({
  userAccount: '',
  userPassword: '',
})

const loading = ref(false)

const handleLogin = async () => {
  if (!loginForm.value.userAccount || !loginForm.value.userPassword) {
    message.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    const res = await userLogin(loginForm.value)
    if (res.data.code === 0) {
      userStore.setUser(res.data.data)
      message.success('登录成功')
      router.push('/')
    } else {
      message.error(res.data.message || '登录失败')
    }
  } catch (error) {
    message.error('登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>MuseAI 登录</h1>
      </div>

      <a-form layout="vertical" :model="loginForm" class="login-form">
        <a-form-item label="账号">
          <a-input
            v-model:value="loginForm.userAccount"
            placeholder="请输入账号"
            size="large"
          />
        </a-form-item>

        <a-form-item label="密码">
          <a-input-password
            v-model:value="loginForm.userPassword"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            size="large"
            block
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </a-button>
        </a-form-item>

        <div class="register-link">
          还没有账号？
          <router-link to="/user/register">立即注册</router-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: calc(100vh - 64px - 70px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0;
}

.login-form {
  margin-top: 24px;
}

.register-link {
  text-align: center;
  color: #666;
}

.register-link a {
  color: #1890ff;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>
