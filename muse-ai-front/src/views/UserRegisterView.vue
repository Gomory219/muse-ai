<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { register } from '@/api/userController'

const router = useRouter()

const registerForm = ref({
  userAccount: '',
  userPassword: '',
  confirmPassword: '',
})

const loading = ref(false)

const handleRegister = async () => {
  if (!registerForm.value.userAccount || !registerForm.value.userPassword) {
    message.warning('请输入账号和密码')
    return
  }

  if (registerForm.value.userPassword !== registerForm.value.confirmPassword) {
    message.warning('两次密码输入不一致')
    return
  }

  loading.value = true
  try {
    const res = await register(registerForm.value)
    // register API 直接返回用户ID (number)
    if (typeof res.data === 'number' && res.data > 0) {
      message.success('注册成功，请登录')
      router.push('/user/login')
    } else {
      message.error('注册失败')
    }
  } catch (error: any) {
    // 显示后端返回的错误信息
    const errorMsg = error?.response?.data?.message || error?.message || '注册失败，请稍后重试'
    message.error(errorMsg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1>MuseAI 注册</h1>
      </div>

      <a-form layout="vertical" :model="registerForm" class="register-form">
        <a-form-item label="账号">
          <a-input
            v-model:value="registerForm.userAccount"
            placeholder="请输入账号"
            size="large"
          />
        </a-form-item>

        <a-form-item label="密码">
          <a-input-password
            v-model:value="registerForm.userPassword"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item label="确认密码">
          <a-input-password
            v-model:value="registerForm.confirmPassword"
            placeholder="请再次输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            size="large"
            block
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </a-button>
        </a-form-item>

        <div class="login-link">
          已有账号？
          <router-link to="/user/login">立即登录</router-link>
        </div>
      </a-form>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: calc(100vh - 64px - 70px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.register-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 32px;
}

.register-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0;
}

.register-form {
  margin-top: 24px;
}

.login-link {
  text-align: center;
  color: #666;
}

.login-link a {
  color: #1890ff;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
