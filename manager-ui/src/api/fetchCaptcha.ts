import { ensureApiBase, pinApiBase } from './resolveApiBase'

export type CaptchaScope = 'admin' | 'user'

/** 仅从当前 API 前缀拉取验证码，保证与后续 login 请求共用同一 Session */
export async function fetchCaptchaObjectUrl(
  scope: CaptchaScope,
): Promise<{ objectUrl: string; sourceUrl: string }> {
  const base = (await ensureApiBase()).replace(/\/$/, '')
  const url = `${base}/${scope}/captcha?t=${Date.now()}`
  const res = await fetch(url, { method: 'GET', credentials: 'include', cache: 'no-store' })
  if (!res.ok) {
    throw new Error(`验证码加载失败 HTTP ${res.status}（${url}）`)
  }
  const blob = await res.blob()
  if (!blob?.size) {
    throw new Error('验证码图片为空')
  }
  pinApiBase(base)
  return { objectUrl: URL.createObjectURL(blob), sourceUrl: url }
}
