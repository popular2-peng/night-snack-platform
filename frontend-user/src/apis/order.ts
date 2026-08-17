import request from './request'

/** 订单明细项（下单时快照） */
export interface UserOrderItem {
  productId: number
  specId: number
  nameSnapshot: string
  priceSnapshot: number
  quantity: number
  imageUrl: string | null
}

/** 订单列表项 */
export interface UserOrder {
  id: number
  orderNo: string
  merchantName: string
  status: string
  totalAmount: number
  payChannel: string
  createdAt: string
  items: UserOrderItem[]
  addressSnapshot: string | null
}

/** 我的订单列表（状态筛选 + 关键词搜索，排除已删除订单） */
export const getOrdersApi = (params: {
  page: number
  size: number
  status?: string
  keyword?: string
}) => {
  return request.get<{ total: number; list: UserOrder[] }>('/user/order', { params })
}

/** 删除订单（软删：user_deleted=1，对用户不再显示） */
export const deleteOrderApi = (id: number) => {
  return request.post(`/user/order/${id}/delete`)
}
