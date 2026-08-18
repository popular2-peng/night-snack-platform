import request from './request'

/** 购物车条目（规格维度） */
export interface CartItem {
  specId: number
  name: string
  price: number
  quantity: number
  imageUrl: string
}

/** 购物车（含商品列表与总金额） */
export interface CartVO {
  merchantId: number
  merchantName: string
  items: CartItem[]
  totalAmount: number
}

/** 加入购物车（跨店会提示清空其他店铺商品，由后端返回提示码） */
export const addToCartApi = (merchantId: number, specId: number, quantity: number) =>
  request.post<CartVO>(`/user/cart?merchantId=${merchantId}`, { specId, quantity })
