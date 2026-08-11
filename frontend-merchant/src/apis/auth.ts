import request from './request'

/** 商家登录（商家端统一接口 /api/merchant/login，手机号 + 密码） */
export const loginApi = (data: { phone: string; password: string }) => {
  return request.post('/merchant/login', data)
}

/** 商家注册（提交入驻申请） */
export const registerApi = (data: {
  username: string
  password: string
  phone: string
  ownerName: string
  shopName: string
  categoryId: number
  province: string
  city: string
  district: string
  address: string
  logoUrl?: string
  description?: string
}) => {
  return request.post('/merchant/register', data)
}

/** 平台启用品类（注册页下拉，公开接口） */
export const getCategoryList = () => {
  return request.get('/merchant/category/all')
}
