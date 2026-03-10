/**
 * 分页相关的 composable
 */
import { ref, computed } from 'vue'

export interface PaginationOptions {
  currentPage?: number
  pageSize?: number
  total?: number
}

export function usePagination(options: PaginationOptions = {}) {
  const pagination = ref({
    current: options.currentPage ?? 1,
    pageSize: options.pageSize ?? 10,
    total: options.total ?? 0,
  })

  // 总页数
  const totalPages = computed(() => {
    return Math.ceil(pagination.value.total / pagination.value.pageSize) || 1
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

  // 设置当前页
  const setCurrentPage = (page: number) => {
    if (page >= 1 && page <= totalPages.value) {
      pagination.value.current = page
    }
  }

  // 设置每页条数
  const setPageSize = (size: number) => {
    pagination.value.pageSize = size
    pagination.value.current = 1
  }

  // 设置总数
  const setTotal = (total: number) => {
    pagination.value.total = total
  }

  // 重置分页
  const reset = () => {
    pagination.value.current = 1
    pagination.value.total = 0
  }

  return {
    pagination,
    totalPages,
    displayedPages,
    setCurrentPage,
    setPageSize,
    setTotal,
    reset,
  }
}
