<script setup lang="ts">
import { health } from '@/api/healthController.ts'

health().then((res) => {
  console.log(res)
})

</script>

<template>
  <router-view/>
</template>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* Ant Design Modal.confirm 浅色主题覆盖 */
.ant-modal-confirm .ant-modal-content {
  background: #ffffff !important;
  border-radius: 12px !important;
  border: none !important;
}

.ant-modal-confirm .ant-modal-header {
  background: transparent !important;
  border-bottom: none !important;
}

.ant-modal-confirm .ant-modal-body {
  background: #ffffff !important;
}

.ant-modal-confirm .ant-modal-title {
  color: #1a1a1a !important;
}

.ant-modal-confirm .ant-modal-confirm-content {
  color: #666 !important;
}

.ant-modal-confirm .ant-modal-confirm-btns {
  margin-top: 20px !important;
}

.ant-modal-confirm .ant-btn {
  border-radius: 8px !important;
  border: 1px solid #e0e0e0 !important;
  background: #ffffff !important;
  color: #666 !important;
}

.ant-modal-confirm .ant-btn:hover {
  border-color: var(--accent-green) !important;
  color: var(--accent-green) !important;
}

.ant-modal-confirm .ant-btn-primary {
  background: var(--accent-green) !important;
  border-color: var(--accent-green) !important;
  color: #ffffff !important;
}

.ant-modal-confirm .ant-btn-primary:hover {
  background: #00c260 !important;
  border-color: #00c260 !important;
}

.ant-modal-confirm .ant-modal-close {
  color: #999 !important;
}

.ant-modal-confirm .ant-modal-close:hover {
  color: #1a1a1a !important;
}
</style>
