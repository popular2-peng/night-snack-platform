import request from './request'

// ============ 类型定义 ============

/** 平台品类（分类导航） */
export interface CategoryVO {
  id: number
  name: string
  icon: string
  iconType: string
  sortOrder: number
  status: string
}

/** 商家（附近列表） */
export interface MerchantVO {
  id: number
  name: string
  phone: string
  address: string
  lat: number
  lng: number
  avatarUrl: string
  status: string
  businessHoursStart: string
  businessHoursEnd: string
  deliveryRadius: number
  distance: number
  logoUrl: string
  description: string
  notice: string
  avgRating: number | null
}

/** 商品 */
export interface ProductVO {
  id: number
  name: string
  price: number
  imageUrl: string
  status: string
  description: string
  categoryId: number
  categoryName: string
}

/** 分页响应 */
export interface PageVO<T> {
  total: number
  list: T[]
}

/** 首页商家（含在售商品预览） */
export interface HomeMerchant extends MerchantVO {
  products: ProductVO[]
}

// ============ 接口 ============

/** 平台启用品类（分类导航，匿名公开） */
export const listCategoriesApi = () => request.get<CategoryVO[]>('/user/categories')

/** 附近商家列表（按距离排序） */
export const listMerchantsApi = (params: {
  lat: number
  lng: number
  page: number
  size: number
}) => request.get<PageVO<MerchantVO>>('/user/merchants', { params })

/** 商家在售商品列表 */
export const listProductsApi = (
  merchantId: number,
  params: { page?: number; size?: number } = {},
) => request.get<PageVO<ProductVO>>(`/user/merchants/${merchantId}/products`, { params })
