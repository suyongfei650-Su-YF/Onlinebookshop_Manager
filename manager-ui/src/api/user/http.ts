export type ApiResult<T> = { success: boolean; message?: string; data?: T }

/** 用户端 API 前缀（与后端部署上下文保持一致；可用 .env 覆盖） */
const API_BASE = import.meta.env.VITE_API_BASE || '/Onlinebookshop_Manager/api'

function userApiBaseCandidates(): string[] {
  const base = API_BASE.replace(/\/$/, '')
  return Array.from(new Set([base, '/Onlinebookshop_Manager_war/api', '/Onlinebookshop_Manager/api'].map((v) => v.replace(/\/$/, ''))))
}

function parseUserApiBody<T>(text: string, res: Response): ApiResult<T> {
  if (!text) {
    return res.ok ? { success: true } : { success: false, message: `请求失败 (HTTP ${res.status})` }
  }
  let parsed: unknown
  try {
    parsed = JSON.parse(text)
  } catch {
    return { success: false, message: text.slice(0, 200) || `HTTP ${res.status}` }
  }
  if (parsed && typeof parsed === 'object') {
    const o = parsed as Record<string, unknown>
    if (typeof o.success === 'boolean') {
      return parsed as ApiResult<T>
    }
    const message =
      (typeof o.message === 'string' && o.message) ||
      (typeof o.error === 'string' && o.error) ||
      `请求失败 (HTTP ${res.status})`
    return { success: false, message, data: parsed as T }
  }
  return { success: false, message: `请求失败 (HTTP ${res.status})` }
}

export async function userRequest<T>(path: string, options: RequestInit = {}): Promise<ApiResult<T>> {
  const bases = userApiBaseCandidates()
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  let last: ApiResult<T> | null = null
  for (const base of bases) {
    const url = `${base}${normalizedPath}`
    try {
      const res = await fetch(url, { ...options, headers, credentials: 'include' })
      const text = await res.text()
      const trimmed = text.trimStart()
      if (trimmed.startsWith('<!doctype html') || trimmed.startsWith('<html')) {
        last = {
          success: false,
          message: res.status === 404 ? '接口不存在，请重新编译并部署后端' : `服务器返回了非 JSON 页面 (HTTP ${res.status})`,
        }
        continue
      }
      const body = parseUserApiBody<T>(text, res)
      if (body.success) {
        return body
      }
      last = body
      if (res.status === 401 || res.status === 403) {
        return body
      }
    } catch (e) {
      last = { success: false, message: String(e) }
    }
  }
  return last || { success: false, message: '无法连接服务器，请确认后端已启动' }
}

