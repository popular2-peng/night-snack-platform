import request from './request'

// 首页统计卡片
export function getDashboardStats() {
  return request.get('/admin/dashboard/stats')
}

// 订单状态分布
export function getOrderStatusDistribution() {
  return request.get('/admin/dashboard/order-stats')
}

// 最新订单
export function getRecentOrders(limit = 10) {
  return request.get('/admin/dashboard/recent-orders', { params: { limit } })
}

// ---- 商家入驻审核 ----
export function listApplications(params: { page: number; size: number }) {
  return request.get('/admin/merchant-application/pending', { params })
}

export function approveApplication(id: number) {
  return request.put(`/admin/merchant-application/${id}/approve`)
}

export function rejectApplication(id: number, reason: string) {
  return request.put(`/admin/merchant-application/${id}/reject`, { reason })
}

export function getApplicationDetail(id: number) {
  return request.get(`/admin/merchant-application/${id}/detail`)
}

export function approveApplicationV2(id: number, data: { initialStatus?: string; deliveryRadius?: number; reviewNote?: string }) {
  return request.post(`/admin/application/${id}/approve`, data)
}

export function rejectApplicationV2(id: number, rejectReason: string) {
  return request.post(`/admin/application/${id}/reject`, { rejectReason })
}

// ---- 用户管理 ----
export function listUsers(params: { page: number; size: number; keyword?: string; status?: string; dateStart?: string; dateEnd?: string }) {
  return request.get('/admin/users', { params })
}

export function getUserDetail(id: number) {
  return request.get(`/admin/users/${id}/detail`)
}

export function banUser(id: number, data: { reason: string; durationDays?: number | null }) {
  return request.put(`/admin/users/${id}/ban`, data)
}

export function unbanUser(id: number) {
  return request.put(`/admin/users/${id}/unban`)
}

export function resetPassword(id: number) {
  return request.put(`/admin/users/${id}/reset-password`)
}

export function getUserLogs(id: number) {
  return request.get(`/admin/users/${id}/logs`)
}

// 旧版兼容
export function disableUser(id: number) {
  return request.put(`/admin/users/${id}/disable`)
}

export function enableUser(id: number) {
  return request.put(`/admin/users/${id}/enable`)
}

// ---- 商家管理 ----
export function listMerchants(params: { page: number; size: number; keyword?: string; status?: string }) {
  return request.get('/admin/merchants', { params })
}

export function getMerchantStats() {
  return request.get('/admin/merchant/stats')
}

export function getMerchantDetail(id: number) {
  return request.get(`/admin/merchant/${id}/detail`)
}

export function pauseMerchant(id: number) {
  return request.put(`/admin/merchant/${id}/pause`)
}

export function resumeMerchant(id: number) {
  return request.put(`/admin/merchant/${id}/resume`)
}

export function banMerchant(id: number, reason: string) {
  return request.put(`/admin/merchant/${id}/ban`, { reason })
}

export function unbanMerchant(id: number) {
  return request.put(`/admin/merchant/${id}/unban`)
}

export function deleteMerchant(id: number) {
  return request.delete(`/admin/merchant/${id}`)
}

export function getMerchantLogs(id: number) {
  return request.get(`/admin/merchant/${id}/logs`)
}

export function suspendMerchant(id: number) {
  return request.put(`/admin/merchants/${id}/suspend`)
}

export function activateMerchant(id: number) {
  return request.put(`/admin/merchants/${id}/activate`)
}
