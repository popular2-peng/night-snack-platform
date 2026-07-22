import request from './request'

// 首页统计
export function getDashboardStats() {
  return request.get('/admin/dashboard')
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

// ---- 用户管理 ----
export function listUsers(params: { page: number; size: number; keyword?: string }) {
  return request.get('/admin/users', { params })
}

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

export function suspendMerchant(id: number) {
  return request.put(`/admin/merchants/${id}/suspend`)
}

export function activateMerchant(id: number) {
  return request.put(`/admin/merchants/${id}/activate`)
}
