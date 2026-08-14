import request from './request'

/** 商家端菜品分类 */
export interface MerchantCategory {
  id: number
  merchantId: number
  name: string
  sort: number
}

/** 品类列表（当前登录商家，JWT 解析） */
export const listCategoriesApi = () => {
  return request.get<MerchantCategory[]>('/merchant/category')
}

/** 新增品类 */
export const createCategoryApi = (name: string, sort = 0) => {
  return request.post('/merchant/category', null, { params: { name, sort } })
}

/** 更新品类 */
export const updateCategoryApi = (id: number, name: string, sort?: number) => {
  return request.put(`/merchant/category/${id}`, null, { params: { name, sort } })
}

/** 删除品类 */
export const deleteCategoryApi = (id: number) => {
  return request.delete(`/merchant/category/${id}`)
}
