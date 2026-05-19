import { userRequest } from './http'

export type UserProfile = {
  id: number
  nickname: string
  username?: string | null
  email?: string | null
  phone?: string | null
  avatarUrl?: string | null
  roleTag?: string | null
  status?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export function userRegister(body: { account: string; nickname: string; password: string; captcha: string }) {
  return userRequest<UserProfile>('/user/register', { method: 'POST', body: JSON.stringify(body) })
}

export function userLogin(body: { account: string; password: string; captcha: string }) {
  return userRequest<UserProfile>('/user/login', { method: 'POST', body: JSON.stringify(body) })
}

export function userLogout() {
  return userRequest<null>('/user/logout', { method: 'POST' })
}

export function userMe() {
  return userRequest<UserProfile>('/user/me', { method: 'GET' })
}

export function userUpdateMe(body: {
  nickname?: string | null
  username?: string | null
  email?: string | null
  phone?: string | null
  avatarUrl?: string | null
}) {
  return userRequest<UserProfile>('/user/me', { method: 'POST', body: JSON.stringify(body) })
}

