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
</style>
