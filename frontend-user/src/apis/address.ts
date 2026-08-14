import request from './request'

/** 收货地址 */
export interface AddressVO {
  id: number
  receiver: string
  phone: string
  detail: string
  lat: number | null
  lng: number | null
  isDefault: boolean
}

/** 地址表单（新增/编辑共用） */
export interface AddressForm {
  receiver: string
  phone: string
  detail: string
  lat?: number | null
  lng?: number | null
  isDefault?: boolean
}

/** 地址列表 */
export const listAddressesApi = () => {
  return request.get<AddressVO[]>('/user/address')
}

/** 单个地址 */
export const getAddressApi = (id: number) => {
  return request.get<AddressVO>(`/user/address/${id}`)
}

/** 新增地址 */
export const createAddressApi = (data: AddressForm) => {
  return request.post<AddressVO>('/user/address', data)
}

/** 更新地址 */
export const updateAddressApi = (id: number, data: AddressForm) => {
  return request.put<AddressVO>(`/user/address/${id}`, data)
}

/** 删除地址 */
export const deleteAddressApi = (id: number) => {
  return request.delete(`/user/address/${id}`)
}

/** 设为默认地址 */
export const setDefaultAddressApi = (id: number) => {
  return request.put(`/user/address/${id}/default`)
}

/** 手机号脱敏：138****8003 */
export const maskPhone = (phone: string) => {
  if (!phone || phone.length < 11) return phone || '-'
  return phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
}
