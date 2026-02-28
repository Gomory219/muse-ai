<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const selectedKeys = ref<string[]>([route.path])

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [path]
  },
)
</script>

<template>
  <a-layout class="layout">
    <!-- Header -->
    <a-layout-header class="header">
      <div class="header-content">
        <!-- Logo + 项目名称 -->
        <div class="logo-section">
          <img src="/favicon.svg" alt="logo" class="logo" />
          <span class="app-name">MuseAI</span>
        </div>

        <!-- 导航栏 -->
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          class="nav-menu"
          :style="{ background: 'transparent', border: 'none' }"
        >
          <a-menu-item key="/" @click="router.push('/')"> 首页 </a-menu-item>
          <a-menu-item key="/about" @click="router.push('/about')"> 关于 </a-menu-item>
        </a-menu>

        <!-- 登录按钮 -->
        <a-button type="primary" class="login-btn">登录</a-button>
      </div>
    </a-layout-header>

    <!-- Body -->
    <a-layout-content class="content">
      <router-view />
    </a-layout-content>

    <!-- Footer -->
    <a-layout-footer class="footer"> MuseAI &copy; 2025 </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0;
  line-height: 64px;
  height: 64px;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  width: 32px;
  height: 32px;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
  color: #1890ff;
}

.nav-menu {
  flex: 1;
  margin: 0 48px;
}

.content {
  flex: 1;
  background: #f5f5f5;
}

.footer {
  text-align: center;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
