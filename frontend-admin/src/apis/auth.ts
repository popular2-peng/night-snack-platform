import request from './request'

/** 管理员登录（管理端专用接口 /api/admin/login，用户名 + 密码） */
export const loginApi = (data: { username: string; password: string }) => {
  return request.post('/admin/login', data)
}
