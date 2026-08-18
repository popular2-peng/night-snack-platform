import request from './request'

/** 店铺信息（商家端） */
export interface MerchantShopVO {
  id: number
  name: string
  phone: string
  address: string
  lat: number | null
  lng: number | null
  status: string
  businessHoursStart: string | null
  businessHoursEnd: string | null
  deliveryRadius: number
  distance?: number
  logoUrl: string | null
  description: string | null
  notice: string | null
}

/** 更新店铺信息请求体（全字段可选，仅更新非空字段） */
export interface MerchantShopUpdateDTO {
  name?: string
  logoUrl?: string
  address?: string
  businessHoursStart?: string
  businessHoursEnd?: string
  deliveryRadius?: number
  description?: string
  notice?: string
}

/** 获取当前登录商家的店铺信息 */
export const getShopApi = () => {
  return request.get<MerchantShopVO>('/merchant/shop')
}

/** 更新店铺信息（部分更新） */
export const updateShopApi = (data: MerchantShopUpdateDTO) => {
  return request.put<MerchantShopVO>('/merchant/shop', data)
}

/** 更新营业状态（OPEN 营业中 / CLOSED 打烊） */
export const updateShopStatusApi = (status: 'OPEN' | 'CLOSED') => {
  return request.put('/merchant/shop/status', null, { params: { status } })
}
