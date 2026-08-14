import request from './request'

/** 商品规格 */
export interface ProductSpecVO {
  id?: number
  productId?: number
  name: string
  priceDelta: number
  stock: number
}

/** 商品 */
export interface MerchantProductVO {
  id: number
  name: string
  price: number
  imageUrl: string | null
  status: string
  description: string | null
  categoryId: number
  categoryName: string | null
  specs: ProductSpecVO[]
}

/** 商品新增/编辑请求体 */
export interface ProductFormDTO {
  name: string
  categoryId: number
  price: number
  imageUrl?: string
  description?: string
}

/** 商品列表（分页 + 品类/名称/状态筛选） */
export const listProductsApi = (params: {
  merchantId: number
  categoryId?: number
  page: number
  size: number
  keyword?: string
  status?: string
}) => {
  return request.get<{ total: number; list: MerchantProductVO[] }>('/merchant/product/list', { params })
}

/** 商品详情（含规格） */
export const getProductApi = (id: number) => {
  return request.get<MerchantProductVO>(`/merchant/product/${id}`)
}

/** 新增商品（默认上架） */
export const createProductApi = (data: ProductFormDTO) => {
  return request.post<MerchantProductVO>('/merchant/product', data)
}

/** 更新商品 */
export const updateProductApi = (id: number, data: ProductFormDTO) => {
  return request.put<MerchantProductVO>(`/merchant/product/${id}`, data)
}

/** 上架/下架 */
export const updateProductStatusApi = (id: number, status: 'ON' | 'OFF') => {
  return request.put(`/merchant/product/${id}/status`, null, { params: { status } })
}

/** 删除商品（仅停售可删） */
export const deleteProductApi = (id: number) => {
  return request.delete(`/merchant/product/${id}`)
}

/** 添加规格 */
export const addSpecApi = (productId: number, data: { name: string; priceDelta: number; stock: number }) => {
  return request.post(`/merchant/product/${productId}/spec`, data)
}

/** 删除规格 */
export const removeSpecApi = (specId: number) => {
  return request.delete(`/merchant/product/spec/${specId}`)
}
