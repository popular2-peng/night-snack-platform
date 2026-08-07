import request from './request'

/** 用户登录（手机号 + 密码，role 校验为 USER） */
export const loginApi = (data: { phone: string; password: string }) => {
  return request.post('/user/login', data)
}

/** 用户注册 */
export const registerApi = (data: { username: string; password: string; phone: string }) => {
  return request.post('/user/register', data)
}
