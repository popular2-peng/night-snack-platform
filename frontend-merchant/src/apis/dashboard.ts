import request from './request'

/** 商家工作台首页统计 */
export interface MerchantDashboardVO {
  shopName: string
  shopStatus: string
  todayOrders: number
  todayRevenue: number
  pendingAccept: number
  onSaleProducts: number
  orderStatusStats: Record<string, number>
  updatedAt: string
}

/** 获取当前登录商家的工作台统计 */
export const getMerchantDashboardApi = () => {
  return request.get<MerchantDashboardVO>('/merchant/dashboard')
}
