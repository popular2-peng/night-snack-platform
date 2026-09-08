import request from './request'

/** 订单明细项（快照） */
export interface MerchantOrderItemVO {
  productId: number
  specId: number
  nameSnapshot: string
  priceSnapshot: number
  quantity: number
  imageUrl?: string
}

/** 订单列表项（含明细与地址快照，可直接渲染卡片） */
export interface MerchantOrderListVO {
  id: number
  orderNo: string
  merchantName: string
  status: string
  totalAmount: number
  payChannel: string
  createdAt: string
  items: MerchantOrderItemVO[]
  addressSnapshot: string
}

/** 订单详情 */
export interface MerchantOrderDetailVO {
  id: number
  orderNo: string
  merchantName: string
  status: string
  totalAmount: number
  addressSnapshot: string
  payChannel: string
  payTime: string
  createdAt: string
  items: MerchantOrderItemVO[]
  statusLogs: { fromStatus: string | null; toStatus: string; operator: string; reason: string; createdAt: string }[]
}

/** 店铺订单列表（支持状态筛选与下单日期范围，今日订单 = dateStart=dateEnd=今天） */
export const getMerchantOrdersApi = (params: {
  page: number
  size: number
  status?: string
  dateStart?: string
  dateEnd?: string
}) => {
  return request.get<{ total: number; list: MerchantOrderListVO[] }>('/merchant/order', { params })
}

/** 订单状态分布统计（tab 角标：状态码 → 数量；可选日期范围，今日角标 = 传今天） */
export const getMerchantOrderStatsApi = (params?: { dateStart?: string; dateEnd?: string }) => {
  return request.get<Record<string, number>>('/merchant/order/stats', { params })
}

/** 订单详情 */
export const getMerchantOrderDetailApi = (id: number) => {
  return request.get<MerchantOrderDetailVO>(`/merchant/order/${id}`)
}

/** 接单：PAID → ACCEPTED */
export const acceptOrderApi = (id: number) => {
  return request.put(`/merchant/order/${id}/accept`)
}

/** 开始制作：ACCEPTED → COOKING */
export const startCookingApi = (id: number) => {
  return request.put(`/merchant/order/${id}/cooking`)
}

/** 完成出餐：COOKING → COMPLETED */
export const completeOrderApi = (id: number) => {
  return request.put(`/merchant/order/${id}/complete`)
}

/** 拒单：PAID → CANCELLED（自动退款） */
export const rejectOrderApi = (id: number, reason: string) => {
  return request.put(`/merchant/order/${id}/reject`, null, { params: { reason } })
}

/** 处理退款申请（用户超1分钟发起）：approve 同意退款 / false 拒绝并回滚订单 */
export const refundDecisionApi = (id: number, approve: boolean) => {
  return request.put(`/merchant/order/${id}/refund-decision`, null, { params: { approve } })
}
