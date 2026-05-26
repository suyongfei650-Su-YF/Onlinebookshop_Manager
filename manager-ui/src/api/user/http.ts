import { fetchCaptchaObjectUrl } from '../fetchCaptcha'
import { apiFetch, type ApiResult } from '../apiFetch'

export type { ApiResult }

export const userRequest = apiFetch

export function fetchUserCaptchaObjectUrl() {
  return fetchCaptchaObjectUrl('user')
}
