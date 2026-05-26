/** 管理员端 API */
import { apiBaseCandidates, resetApiBaseCache } from '../resolveApiBase'

export function adminApiBaseCandidates(): string[] {
  return apiBaseCandidates()
}
import { fetchCaptchaObjectUrl } from '../fetchCaptcha'
import { apiFetch, type ApiResult } from '../apiFetch'

export { resetApiBaseCache }
export type { ApiResult }

export function adminApiBase(): string {
  return apiBaseCandidates()[0]
}

export function adminCaptchaUrl(): string {
  const base = apiBaseCandidates()[0]
  return `${base}/admin/captcha?t=${Date.now()}`
}

export function fetchAdminCaptchaObjectUrl() {
  return fetchCaptchaObjectUrl('admin')
}

export const adminRequest = apiFetch
