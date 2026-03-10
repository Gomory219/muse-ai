<script setup lang="ts">
import { computed, ref } from 'vue'
import { PAGE_SIZE_OPTIONS } from '@/config/constants'

interface Props {
  current?: number
  pageSize?: number
  total?: number
  totalPages?: number
  pageSizeOptions?: readonly number[]
}

const props = withDefaults(defineProps<Props>(), {
  current: 1,
  pageSize: 10,
  total: 0,
  totalPages: 0,
  pageSizeOptions: () => PAGE_SIZE_OPTIONS,
})

const emit = defineEmits<{
  (e: 'page-change', page: number): void
  (e: 'page-size-change', size: number): void
  (e: 'jump-to-page', page: number): void
}>()

// 计算总页数（如果未提供）
const computedTotalPages = computed(() => {
  if (props.totalPages > 0) return props.totalPages
  return Math.ceil(props.total / props.pageSize) || 1
})

// 显示的页码列表
const displayedPages = computed(() => {
  const current = props.current
  const total = computedTotalPages.value
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
  emit('page-change', page)
}

// 每页条数变化
const handlePageSizeChange = (size: number) => {
  emit('page-size-change', size)
}

// 跳转到指定页
const jumpPageInput = ref('')
const handleJumpToPage = () => {
  const page = parseInt(jumpPageInput.value)
  if (isNaN(page)) return
  if (page < 1 || page > computedTotalPages.value) return
  jumpPageInput.value = ''
  emit('jump-to-page', page)
}

// 处理 select 的 change 事件
const handleSelectChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  if (target) {
    handlePageSizeChange(Number(target.value))
  }
}
</script>

<template>
  <div class="pagination">
    <div class="pagination-info">
      <span class="pagination-text">// {{ total }} entries</span>
    </div>
    <div class="pagination-controls">
      <!-- 每页条数选择 -->
      <div class="page-size-selector">
        <span class="page-size-label">每页</span>
        <div class="page-size-dropdown" @click.stop>
          <select
            :value="pageSize"
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
          :disabled="current === 1"
          @click="handlePageChange(1)"
          title="首页"
        >
          &laquo;
        </button>
        <button
          class="page-btn"
          :disabled="current === 1"
          @click="handlePageChange(current - 1)"
        >
          &lt; prev
        </button>

        <!-- 页码列表 -->
        <div class="page-numbers">
          <template v-for="page in displayedPages" :key="page">
            <span v-if="page === '...'" class="page-ellipsis">...</span>
            <button
              v-else
              :class="['page-number-btn', { active: page === current }]"
              @click="handlePageChange(page as number)"
            >
              {{ page }}
            </button>
          </template>
        </div>

        <button
          class="page-btn"
          :disabled="current >= computedTotalPages"
          @click="handlePageChange(current + 1)"
        >
          next &gt;
        </button>
        <button
          class="page-btn"
          :disabled="current >= computedTotalPages"
          @click="handlePageChange(computedTotalPages)"
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
          :max="computedTotalPages"
          @keydown.enter="handleJumpToPage"
        />
        <button class="jump-btn" @click="handleJumpToPage">GO</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
  flex-wrap: wrap;
  gap: 12px;
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

/* 响应式 */
@media (max-width: 768px) {
  .pagination {
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
