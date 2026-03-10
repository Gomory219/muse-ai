<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { StarFilled, StarOutlined } from '@ant-design/icons-vue'
import type { PropType } from 'vue'

export interface AppCardProps {
  id: string
  appName?: string
  cover?: string
  initPrompt?: string
  createTime?: string
  priority?: number
  user?: {
    userName?: string
  }
  userId?: string
}

const props = defineProps({
  app: {
    type: Object as PropType<AppCardProps>,
    required: true,
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits<{
  (e: 'click', app: AppCardProps): void
  (e: 'delete', app: AppCardProps): void
  (e: 'edit', app: AppCardProps): void
}>()

const router = useRouter()

const isFeatured = computed(() => {
  return props.app.priority !== undefined && props.app.priority > 0
})

const handleClick = () => {
  emit('click', props.app)
}
</script>

<template>
  <div
    class="app-card"
    :class="{ loading: isLoading, featured: isFeatured }"
    @click="handleClick"
  >
    <div class="app-cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
      <div v-else class="default-cover">{{ app.appName?.[0] || 'A' }}</div>
      <div class="app-overlay">
        <span class="overlay-text">view_source</span>
      </div>
      <div v-if="isFeatured" class="featured-badge">
        <StarFilled />
      </div>
    </div>
    <div class="app-info">
      <h3 class="app-name" :title="app.appName">
        {{ app.appName || '未命名应用' }}
      </h3>
      <p class="app-time">
        {{ app.createTime ? new Date(app.createTime).toLocaleDateString('zh-CN') : '' }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.app-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.app-card:hover {
  border-color: var(--accent-green);
  transform: translateY(-4px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.app-card.featured {
  border-color: var(--accent-gold);
}

.app-cover {
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--bg-secondary);
  position: relative;
}

.app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.default-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 300;
  color: var(--accent-green);
  background: var(--bg-secondary);
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.overlay-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
  font-size: 13px;
  color: var(--accent-green);
  letter-spacing: 0.1em;
}

.featured-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: var(--accent-gold-dim);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-gold);
  font-size: 14px;
}

.app-info {
  padding: 16px;
  background: var(--bg-card);
}

.app-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-time {
  font-size: 12px;
  color: var(--text-muted);
  margin: 0;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
}

/* 骨架屏样式 */
.app-card.loading {
  pointer-events: none;
}

.app-card.loading .app-cover {
  background: linear-gradient(
    90deg,
    var(--bg-secondary) 25%,
    var(--border-color) 50%,
    var(--bg-secondary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.app-card.loading .app-name,
.app-card.loading .app-time {
  background: linear-gradient(
    90deg,
    var(--bg-secondary) 25%,
    var(--border-color) 50%,
    var(--bg-secondary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  color: transparent;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
