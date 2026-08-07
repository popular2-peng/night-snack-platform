import request from './request'

export const loginApi = (data: { username: string; password: string }) => {
  return request.post('/user/login', data)
}
