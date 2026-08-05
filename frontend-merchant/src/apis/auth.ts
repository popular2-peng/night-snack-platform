import request from './request'

/** 商家登录（跨表登录，role 校验为 MERCHANT） */
export function loginApi(data: { username: string; password: string }) {
  return request.post('/user/login', data)
}

/** 商家注册（提交入驻申请） */
export function registerApi(data: {
  username: string
  password: string
  phone: string
  shopName: string
  categoryId: number
  reason?: string
  description?: string
}) {
  return request.post('/merchant/register', data)
}

/** 平台启用品类（注册页下拉，公开接口） */
export function getCategoryList() {
  return request.get('/merchant/category/all')
}
