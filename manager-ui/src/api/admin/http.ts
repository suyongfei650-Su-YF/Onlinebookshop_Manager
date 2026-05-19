/** 管理员端 API 前缀（与 Tomcat 部署的 WAR 上下文名一致，可在 .env 中覆盖） */
const API_BASE = import.meta.env.VITE_API_BASE || '/Onlinebookshop_Manager/api'

export function adminApiBase(): string {
  return API_BASE.replace(/\/$/, '')
}

/** 兼容不同启动方式（spring-boot:run / 外置 Tomcat）的验证码地址候选 */
export function adminCaptchaCandidates(): string[] {
  const ts = Date.now()
  const candidates = [
    `${adminApiBase()}/admin/captcha?t=${ts}`,
    `/Onlinebookshop_Manager/api/admin/captcha?t=${ts}`,
    `/Onlinebookshop_Manager_war/api/admin/captcha?t=${ts}`,
    `/api/admin/captcha?t=${ts}`,
  ]
  return Array.from(new Set(candidates))
}

/** 主动拉取验证码，避免 <img> 直接加载时难以诊断失败原因 */
export async function fetchAdminCaptchaObjectUrl(): Promise<{ objectUrl: string; sourceUrl: string }> {
  const candidates = adminCaptchaCandidates()
  const failures: string[] = []
  for (const url of candidates) {
    try {
      const res = await fetch(url, { method: 'GET', credentials: 'include', cache: 'no-store' })
      if (!res.ok) {
        failures.push(`${url} -> HTTP ${res.status}`)
        continue
      }
      const blob = await res.blob()
      if (!blob || blob.size === 0) {
        failures.push(`${url} -> empty body`)
        continue
      }
      return { objectUrl: URL.createObjectURL(blob), sourceUrl: url }
    } catch (e) {
      failures.push(`${url} -> ${String(e)}`)
    }
  }
  // 某些部署下 fetch 可能因 CORS 读取限制失败，但 <img src> 仍可直接显示图片
  // 这里回退到直接 URL，避免验证码区域空白。
  if (candidates.length > 0) {
    return { objectUrl: candidates[0], sourceUrl: candidates[0] }
  }
  throw new Error(failures.join(' | ') || 'captcha load failed')
}

/** 图形验证码图片地址（需携带 Cookie，与接口同域；加时间戳防缓存） */
export function adminCaptchaUrl(): string {
  return adminCaptchaCandidates()[0]
}

export type ApiResult<T> = { success: boolean; message?: string; data?: T }

export function adminApiBaseCandidates(): string[] {
  return Array.from(
    new Set([adminApiBase(), '/Onlinebookshop_Manager_war/api', '/Onlinebookshop_Manager/api'].map((v) => v.replace(/\/$/, ''))),
  )
}

export async function adminRequest<T>(path: string, options: RequestInit = {}): Promise<ApiResult<T>> {
  const bases = adminApiBaseCandidates()
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  let lastResult: ApiResult<T> | null = null
  for (const base of bases) {
    const url = `${base}${normalizedPath}`
    try {
      const res = await fetch(url, {
        ...options,
        headers,
        credentials: 'include',
      })
      const text = await res.text()
      const trimmed = text.trimStart()
      // 命中前端 dev server 回退页（index.html）时，继续尝试下一个 API 前缀
      if (trimmed.startsWith('<!doctype html') || trimmed.startsWith('<html')) {
        lastResult = { success: false, message: `Non-API HTML response from ${url}` }
        continue
      }
      if (!text) {
        if (res.status === 404) continue
        return { success: res.ok }
      }
      let parsed: ApiResult<T>
      try {
        parsed = JSON.parse(text) as ApiResult<T>
      } catch {
        parsed = { success: false, message: text || `HTTP ${res.status}` }
      }
      // 404 可能是上下文路径不匹配，尝试下一个 API 前缀
      if (res.status === 404) {
        lastResult = parsed
        continue
      }
      return parsed
    } catch (e) {
      lastResult = { success: false, message: String(e) }
    }
  }
  return lastResult || { success: false, message: 'request failed' }
}
