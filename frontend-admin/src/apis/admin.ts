import request from './request'

// 首页统计卡片
export const getDashboardStats = () => {
  return request.get('/admin/dashboard/stats')
}

// 订单状态分布
export const getOrderStatusDistribution = () => {
  return request.get('/admin/dashboard/order-stats')
}

// 最新订单
export const getRecentOrders = (limit = 10) => {
  return request.get('/admin/dashboard/recent-orders', { params: { limit } })
}

// ---- 商家入驻审核 ----
export const listApplications = (params: { page: number; size: number }) => {
  return request.get('/admin/merchant-application/pending', { params })
}

export const approveApplication = (id: number) => {
  return request.put(`/admin/merchant-application/${id}/approve`)
}

export const rejectApplication = (id: number, reason: string) => {
  return request.put(`/admin/merchant-application/${id}/reject`, { reason })
}

export const getApplicationDetail = (id: number) => {
  return request.get(`/admin/merchant-application/${id}/detail`)
}

export const approveApplicationV2 = (id: number, data: { initialStatus?: string; deliveryRadius?: number; reviewNote?: string }) => {
  return request.post(`/admin/application/${id}/approve`, data)
}

export const rejectApplicationV2 = (id: number, rejectReason: string) => {
  return request.post(`/admin/application/${id}/reject`, { rejectReason })
}

// ---- 用户管理 ----
export const listUsers = (params: { page: number; size: number; keyword?: string; status?: string; dateStart?: string; dateEnd?: string }) => {
  return request.get('/admin/users', { params })
}

export const getUserDetail = (id: number) => {
  return request.get(`/admin/users/${id}/detail`)
}

export const banUser = (id: number, data: { reason: string; durationDays?: number | null }) => {
  return request.put(`/admin/users/${id}/ban`, data)
}

export const unbanUser = (id: number) => {
  return request.put(`/admin/users/${id}/unban`)
}

export const resetPassword = (id: number) => {
  return request.put(`/admin/users/${id}/reset-password`)
}

export const getUserLogs = (id: number) => {
  return request.get(`/admin/users/${id}/logs`)
}

// 旧版兼容
export const disableUser = (id: number) => {
  return request.put(`/admin/users/${id}/disable`)
}

export const enableUser = (id: number) => {
  return request.put(`/admin/users/${id}/enable`)
}

// ---- 商家管理 ----
export const listMerchants = (params: { page: number; size: number; keyword?: string; status?: string }) => {
  return request.get('/admin/merchants', { params })
}

export const getMerchantStats = () => {
  return request.get('/admin/merchant/stats')
}

export const getMerchantDetail = (id: number) => {
  return request.get(`/admin/merchant/${id}/detail`)
}

export const pauseMerchant = (id: number) => {
  return request.put(`/admin/merchant/${id}/pause`)
}

export const resumeMerchant = (id: number) => {
  return request.put(`/admin/merchant/${id}/resume`)
}

export const banMerchant = (id: number, reason: string) => {
  return request.put(`/admin/merchant/${id}/ban`, { reason })
}

export const unbanMerchant = (id: number) => {
  return request.put(`/admin/merchant/${id}/unban`)
}

export const deleteMerchant = (id: number) => {
  return request.delete(`/admin/merchant/${id}`)
}

export const getMerchantLogs = (id: number) => {
  return request.get(`/admin/merchant/${id}/logs`)
}

export const suspendMerchant = (id: number) => {
  return request.put(`/admin/merchants/${id}/suspend`)
}

export const activateMerchant = (id: number) => {
  return request.put(`/admin/merchants/${id}/activate`)
}

// ---- 订单管理 ----
export const listAdminOrders = (params: { page: number; size: number; keyword?: string; status?: string; dateStart?: string; dateEnd?: string }) => {
  return request.get('/admin/orders', { params })
}

export const getAdminOrderDetail = (id: number) => {
  return request.get(`/admin/orders/${id}`)
}

// ---- 品类管理 ----
export const listCategories = (params: { page: number; size: number; keyword?: string; status?: string }) => {
  return request.get('/admin/category', { params })
}

export const listAllEnabledCategories = () => {
  return request.get('/admin/category/all')
}

export const getCategoryStats = () => {
  return request.get('/admin/category/stats')
}

export const getCategoryDetail = (id: number) => {
  return request.get(`/admin/category/${id}`)
}

export const createCategory = (data: { name: string; sortOrder?: number; icon?: string; iconType?: string; status?: string; remark?: string }) => {
  return request.post('/admin/category', data)
}

export const updateCategory = (id: number, data: { name?: string; sortOrder?: number; icon?: string; iconType?: string; status?: string; remark?: string }) => {
  return request.put(`/admin/category/${id}`, data)
}

export const enableCategory = (id: number) => {
  return request.put(`/admin/category/${id}/enable`)
}

export const disableCategory = (id: number) => {
  return request.put(`/admin/category/${id}/disable`)
}

export const deleteCategory = (id: number) => {
  return request.delete(`/admin/category/${id}`)
}

export const reorderCategories = (items: { id: number; sortOrder: number }[]) => {
  return request.post('/admin/category/reorder', items)
}
