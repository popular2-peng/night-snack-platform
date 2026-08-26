import request from './request'

/** 购物车条目 */
export interface CartItem {
  /** 购物车条目键（更新/删除接口用）：有规格=`{specId}`，无规格=`p{productId}` */
  cartKey: string
  /** 规格 ID（无规格商品为 null） */
  specId: number | null
  productId: number
  productName: string
  /** 规格名称（无规格商品为 null） */
  specName: string | null
  /** 单价（有规格=基础价+加价，无规格=基础价） */
  price: number
  quantity: number
  imageUrl: string
}

/** 购物车（含商品列表与总金额） */
export interface CartVO {
  merchantId: number
  merchantName: string
  items: CartItem[]
  totalPrice: number
}

/**
 * 加入购物车（productId 必填、specId 选填）
 * @param specId 有规格商品传规格 ID；无规格商品传 null
 */
export const addToCartApi = (merchantId: number, productId: number, specId: number | null, quantity: number) =>
  request.post<CartVO>(`/user/cart?merchantId=${merchantId}`, { productId, specId, quantity })
