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

/** 订单详情 */
export interface OrderDetail extends UserOrder {
  userId: number
  merchantId: number
  payTime: string | null
  addressSnapshot: string | null
  statusLogs: { fromStatus: string | null; toStatus: string; operator: string; reason: string; createdAt: string }[]
}

/** 订单详情 */
export const getOrderDetailApi = (id: number) => {
  return request.get<OrderDetail>(`/user/order/${id}`)
}

/** 取消订单（仅待支付可取消） */
export const cancelOrderApi = (id: number) => {
  return request.post(`/user/order/${id}/cancel`)
}

/** 申请退款（支付1分钟内即时退款；超过1分钟待商家确认） */
export const applyRefundApi = (id: number) => {
  return request.post<string>(`/user/order/${id}/refund`)
}

/** 订单明细项（下单请求：productId 必填、specId 选填——有规格商品必传，无规格商品不传） */
export interface OrderItemDTO {
  productId: number
  specId?: number | null
  quantity: number
}

/** 下单响应（新创建的订单） */
export interface OrderResult {
  id: number
  orderNo: string
  status: string
  totalAmount: number
  /** 下单时间（ISO-8601，用于未支付自动取消倒计时） */
  createdAt: string
}

/** 下单（提交订单） */
export const createOrderApi = (data: {
  merchantId: number
  addressId: number
  items: OrderItemDTO[]
  payChannel: 'ALIPAY' | 'WECHAT_MOCK'
  clientToken: string
}) => {
  return request.post<OrderResult>('/user/order', data)
}
