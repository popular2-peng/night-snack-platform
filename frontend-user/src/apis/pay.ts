import request from './request'

/** 支付响应 */
export interface PayVO {
  orderNo: string
  channel: string
  amount: number
  status: string
  /** 支付二维码（仅支付宝返回） */
  qrCode: string | null
}

/** 发起支付（WECHAT_MOCK 直接成功；ALIPAY 返回沙箱二维码） */
export const payApi = (orderNo: string, channel: 'ALIPAY' | 'WECHAT_MOCK') => {
  return request.post<PayVO>('/user/pay', { orderNo, channel })
}
