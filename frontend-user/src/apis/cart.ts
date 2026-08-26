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

/** 查询购物车 */
export const getCartApi = () => request.get<CartVO>('/user/cart')

/** 更新购物车商品数量（cartKey：有规格={specId}，无规格=p{productId}） */
export const updateCartQtyApi = (cartKey: string, quantity: number) =>
  request.put('/user/cart', { cartKey, quantity })

/** 移除购物车商品 */
export const removeCartItemApi = (cartKey: string) =>
  request.delete(`/user/cart/${cartKey}`)

/** 清空购物车 */
export const clearCartApi = () => request.delete('/user/cart')
