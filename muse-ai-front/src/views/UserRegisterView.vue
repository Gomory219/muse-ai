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
  <div class="register-page">
    <!-- 背景效果 -->
    <div class="grid-background"></div>
    <div class="dot-matrix"></div>

    <div class="register-container">
      <!-- Logo -->
      <div class="logo-section">
        <img src="/favicon-static.svg" alt="Muse AI" class="brand-logo" />
      </div>

      <!-- 标题 -->
      <h1 class="register-title">
        <span class="title-prefix">Muse</span>
        <span class="title-suffix">AI</span>
      </h1>

      <p class="register-subtitle">
        <span class="code-bracket">&lt;</span>
        用户注册
        <span class="code-bracket">/&gt;</span>
      </p>

      <!-- 终端风格注册框 -->
      <div class="terminal-window">
        <div class="terminal-header">
          <div class="terminal-dots">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
          </div>
          <span class="terminal-title">register.ts</span>
        </div>

        <div class="terminal-body">
          <div class="form-prompt">// 输入账号</div>
          <div class="input-line">
            <span class="prompt-symbol">$</span>
            <span class="input-label">user_account:</span>
            <input
              v-model="registerForm.userAccount"
              class="terminal-input"
              placeholder="'输入账号...'"
            />
          </div>

          <div class="form-prompt">// 输入密码</div>
          <div class="input-line">
            <span class="prompt-symbol">$</span>
            <span class="input-label">user_password:</span>
            <input
              v-model="registerForm.userPassword"
              type="password"
              class="terminal-input"
              placeholder="'********'"
            />
          </div>

          <div class="form-prompt">// 确认密码</div>
          <div class="input-line">
            <span class="prompt-symbol">$</span>
            <span class="input-label">confirm_password:</span>
            <input
              v-model="registerForm.confirmPassword"
              type="password"
              class="terminal-input"
              placeholder="'********'"
              @keydown.enter="handleRegister"
            />
          </div>

          <div class="terminal-footer">
            <button
              class="execute-btn"
              :class="{ loading }"
              :disabled="loading"
              @click="handleRegister"
            >
              <span v-if="!loading" class="btn-text">EXECUTE →</span>
              <span v-else class="loading-text">
                <span class="spinner"></span>
                REGISTERING...
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- 登录链接 -->
      <div class="login-link">
        <span class="link-prompt">// 已有账号?</span>
        <router-link to="/user/login" class="link-text">login()</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* CSS 变量 */
.register-page {
  --bg-primary: #0a0a0a;
  --bg-secondary: #111111;
  --bg-card: #1a1a1a;
  --text-primary: #ffffff;
  --text-secondary: #888888;
  --text-muted: #444444;
  --accent-green: #00d26a;
  --accent-green-dim: rgba(0, 210, 106, 0.1);
  --border-color: #2a2a2a;

  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 背景效果 */
.grid-background {
  position: absolute;
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
  position: absolute;
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

/* 主容器 */
.register-container {
  position: relative;
  z-index: 1;
  max-width: 500px;
  width: 100%;
  padding: 40px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Logo */
.logo-section {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  border: 2px solid var(--border-color);
  border-radius: 16px;
  margin-bottom: 32px;
}

.brand-logo {
  width: 80px;
  height: 80px;
}

/* 标题 */
.register-title {
  font-size: clamp(36px, 6vw, 48px);
  font-weight: 300;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  letter-spacing: -0.02em;
  margin: 0 0 16px 0;
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

.register-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  margin: 0 0 40px 0;
}

.code-bracket {
  color: var(--accent-green);
  margin: 0 4px;
}

/* 终端窗口 */
.terminal-window {
  width: 100%;
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

/* 终端内容 */
.terminal-body {
  padding: 24px;
}

.form-prompt {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.input-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.prompt-symbol {
  color: var(--accent-green);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 14px;
  font-weight: 600;
}

.input-label {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  color: var(--text-secondary);
}

.terminal-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  color: var(--text-primary);
}

.terminal-input::placeholder {
  color: var(--text-muted);
}

/* 终端底部 */
.terminal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.execute-btn {
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

.execute-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.execute-btn:not(:disabled) {
  border-color: var(--accent-green);
  color: var(--accent-green);
  background: var(--accent-green-dim);
  cursor: pointer;
}

.execute-btn:not(:disabled):hover {
  background: var(--accent-green);
  color: var(--bg-primary);
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

/* 登录链接 */
.login-link {
  margin-top: 32px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.link-prompt {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 12px;
  color: var(--text-muted);
}

.link-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 12px;
  color: var(--accent-green);
  text-decoration: none;
  transition: opacity 0.2s ease;
}

.link-text:hover {
  opacity: 0.8;
}

/* 响应式 */
@media (max-width: 768px) {
  .register-title {
    font-size: 32px;
  }

  .terminal-body {
    padding: 20px;
  }
}
</style>
