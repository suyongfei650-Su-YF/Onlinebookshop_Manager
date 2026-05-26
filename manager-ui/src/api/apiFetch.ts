import { apiBaseCandidates, ensureApiBase, noteApiBaseFailure, pinApiBase, resetApiBaseCache } from './resolveApiBase'

export type ApiResult<T> = { success: boolean; message?: string; data?: T }

function looksLikeHtml(text: string): boolean {
  const t = text.trimStart().toLowerCase()
  return t.startsWith('<!doctype html') || t.startsWith('<html')
}

function parseBody<T>(text: string, res: Response): ApiResult<T> {
  if (!text) {
    return res.ok ? { success: true } : { success: false, message: `请求失败 (HTTP ${res.status})` }
  }
  try {
    const parsed = JSON.parse(text) as ApiResult<T> & { error?: string; status?: number }
    if (typeof parsed.success === 'boolean') return parsed
    if (parsed.error && typeof parsed.status === 'number') {
      return { success: false, message: parsed.error || `请求失败 (HTTP ${parsed.status})` }
    }
  } catch {
    /* fall through */
  }
  return { success: false, message: text.slice(0, 200) || `HTTP ${res.status}` }
}

function messageForProxyError(status: number): string | null {
  if (status === 502 || status === 503 || status === 504) {
    return (
      '后端未在 8080 响应（HTTP ' +
      status +
      '）。请先在 IDEA 运行「启动后端 SpringBoot」或 Tomcat，' +
      '看到控制台出现 Started 后再刷新；可运行 scripts\\检查端口.bat 确认 8080 已监听。'
    )
  }
  return null
}

async function fetchOnce<T>(base: string, path: string, options: RequestInit): Promise<ApiResult<T>> {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const url = `${base}${normalizedPath}`
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  const res = await fetch(url, { ...options, headers, credentials: 'include' })
  if (res.status > 0 && res.status < 502) {
    pinApiBase(base)
  }
  const proxyErr = messageForProxyError(res.status)
  if (proxyErr) {
    return { success: false, message: proxyErr }
  }
  const text = await res.text()
  if (looksLikeHtml(text)) {
    return {
      success: false,
      message:
        res.status === 404
          ? `接口不存在 (${url})，请检查 Tomcat Application context 与 .env 中 VITE_API_BASE`
          : `服务器返回了页面而非 JSON (${url})`,
    }
  }
  const body = parseBody<T>(text, res)
  if (res.status === 404) {
    return { success: false, message: body.message || `接口 404: ${url}` }
  }
  return body
}

/**
 * 依次尝试各 API 前缀；成功后固定 cachedBase，避免 Cookie 路径错乱。
 * 连接失败时会自动重新探测一次后端。
 */
export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<ApiResult<T>> {
  try {
    return await apiFetchInner<T>(path, options)
  } catch (e) {
    return { success: false, message: e instanceof Error ? e.message : String(e) }
  }
}

async function apiFetchInner<T>(path: string, options: RequestInit = {}): Promise<ApiResult<T>> {
  const attempt = async () => {
    await ensureApiBase()
    const bases = apiBaseCandidates()
    let last: ApiResult<T> = { success: false, message: '请求失败' }
    for (const base of bases) {
      try {
        const body = await fetchOnce<T>(base, path, options)
        if (body.success) {
          return body
        }
        last = body
        const retryable =
          body.message?.includes('404') ||
          body.message?.includes('非 JSON') ||
          body.message?.includes('HTML') ||
          body.message?.includes('接口不存在')
        if (retryable) continue
        if (path.startsWith('/admin/') && body.message?.includes('未登录')) {
          return body
        }
        return body
      } catch (e) {
        last = { success: false, message: String(e) }
      }
    }
    noteApiBaseFailure(last.message)
    return last
  }

  let result = await attempt()
  const msg = result.message || ''
  if (
    !result.success &&
    (msg.includes('Failed to fetch') || msg.includes('NetworkError') || msg.includes('fetch'))
  ) {
    resetApiBaseCache()
    result = await attempt()
  }
  return result
}
