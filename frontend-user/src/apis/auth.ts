import request from './request'

/** 用户登录（跨表登录，role 校验为 USER） */
export function loginApi(data: { username: string; password: string }) {
  return request.post('/user/login', data)
}

/** 用户注册 */
export function registerApi(data: { username: string; password: string; phone: string }) {
  return request.post('/user/register', data)
}
