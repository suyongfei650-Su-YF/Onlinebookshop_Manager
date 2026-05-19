import { adminRequest } from './http'

export type AdminLoginBody = {
  username: string
  password: string
  captcha: string
  rememberMe?: boolean
}

export type AdminInfo = { id: number; username: string; displayName: string }
export type AdminProfile = { id: number; username: string; displayName: string; avatarUrl?: string }

export type AdminRegisterBody = {
  username: string
  password: string
  displayName: string
  email: string
  captcha: string
}

export type AdminForgotBody = {
  username: string
  email: string
  newPassword: string
  captcha: string
}

/** 是否已有服务端 Session（供路由守卫使用，无需登录即可调用） */
export function adminSession() {
  return adminRequest<{ loggedIn: boolean }>('/admin/session', { method: 'GET' })
}

export function adminLogin(body: AdminLoginBody) {
  return adminRequest<AdminInfo>('/admin/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminRegister(body: AdminRegisterBody) {
  return adminRequest<{ id: number; username: string }>('/admin/register', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminForgotPassword(body: AdminForgotBody) {
  return adminRequest<null>('/admin/forgot-password', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminLogout() {
  return adminRequest('/admin/logout', { method: 'POST' })
}

export function adminMe() {
  return adminRequest<AdminProfile>('/admin/me', { method: 'GET' })
}

export function adminUpdateMe(body: { avatarUrl: string }) {
  return adminRequest<AdminProfile>('/admin/me', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminChangePassword(body: { oldPassword: string; newPassword: string }) {
  return adminRequest<null>('/admin/change-password', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
