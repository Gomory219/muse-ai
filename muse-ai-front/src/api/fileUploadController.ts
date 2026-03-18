// @ts-ignore
/* eslint-disable */
import request from '@/request'

/**
 * 上传文件
 * @param file 要上传的文件
 * @param options 额外选项
 */
export async function uploadFile(file: File, options?: { [key: string]: any }) {
  const formData = new FormData()
  formData.append('file', file)

  return request<API.BaseResponseString>('/upload', {
    method: 'POST',
    // 不设置 Content-Type，让浏览器自动设置为 multipart/form-data
    data: formData,
    ...(options || {}),
  })
}
