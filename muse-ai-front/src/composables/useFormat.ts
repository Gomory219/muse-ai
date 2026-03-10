/**
 * 格式化相关的 composable
 */
import { formatTime, formatRelativeTime, truncateText, getCodeTypeLabel, getCodeTypeClass } from '@/utils/format'

export function useFormat() {
  return {
    formatTime,
    formatRelativeTime,
    truncateText,
    getCodeTypeLabel,
    getCodeTypeClass,
  }
}
