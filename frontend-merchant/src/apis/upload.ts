import request from './request'

/** OSS 上传签名响应 */
export interface UploadSignVO {
  accessKeyId: string
  policy: string
  signature: string
  host: string
  dir: string
  expire: number
}

/**
 * 获取 OSS 上传签名（匿名可用：入驻注册页未登录直传店铺 LOGO）
 *
 * @param bizType 业务类型：AVATAR / SHOP_LOGO / PRODUCT_IMAGE
 * @param fileName 原始文件名（用于扩展名校验，如 xx.jpg）
 */
export const uploadSignApi = (bizType: string, fileName: string) => {
  return request.get<UploadSignVO>('/upload/sign', { params: { bizType, fileName } })
}

/** OSS 签名直传：FormData 提交到签名 Host（不经过后端） */
export const uploadFileToOss = (sign: UploadSignVO, file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    // 上传 key = 签名目录 + 随机文件名（policy 限定以 dir 开头）
    const ext = file.name.split('.').pop()?.toLowerCase() || 'jpg'
    const randomName = Date.now().toString(36) + Math.random().toString(36).slice(2, 8)
    const objectKey = sign.dir + randomName + '.' + ext
    const objectUrl = sign.host + '/' + objectKey

    const form = new FormData()
    form.append('key', objectKey)
    form.append('policy', sign.policy)
    form.append('OSSAccessKeyId', sign.accessKeyId)
    form.append('signature', sign.signature)
    form.append('file', file)

    /** 用 Image 探测验证对象是否可访问（img 加载不受 CORS 读取限制） */
    const probeByImage = () => {
      const img = new Image()
      img.onload = () => resolve(objectUrl)
      img.onerror = () => reject(new Error('OSS 直传失败，请检查网络后重试'))
      img.src = objectUrl
    }

    const xhr = new XMLHttpRequest()
    xhr.open('POST', sign.host)
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        // 直传成功：对象 URL = host + '/' + key
        resolve(objectUrl)
      } else {
        reject(new Error(`OSS 直传失败: HTTP ${xhr.status}`))
      }
    }
    xhr.onerror = () => {
      // 网络错误时可能是 bucket 未配置 CORS 导致响应被浏览器拦截（图片实际已上传成功，OSS 返回 204）。
      // 用 Image 探测确认：对象可访问则视为上传成功，避免误报。
      probeByImage()
    }
    xhr.send(form)
  })
}
