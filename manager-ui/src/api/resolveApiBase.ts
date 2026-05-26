/** 开发环境：探测并固定后端 API 前缀，保证 Session Cookie 与请求路径一致 */

const DEFAULT_BASE = '/api'

const ALL_CANDIDATES = Array.from(
  new Set(
    [
      import.meta.env.VITE_API_BASE as string | undefined,
      DEFAULT_BASE,
      '/Onlinebookshop_Manager/api',
      '/Onlinebookshop_Manager_war/api',
    ]
      .filter((v): v is string => !!v && v.trim().length > 0)
      .map((v) => v.replace(/\/$/, '')),
  ),
)

let cachedBase: string | null = null
let probePromise: Promise<string> | null = null

export function resetApiBaseCache() {
  cachedBase = null
  probePromise = null
}

/** 验证码/登录成功后固定 API 前缀，避免 JSESSIONID 与请求路径不一致 */
export function pinApiBase(base: string) {
  const normalized = base.replace(/\/$/, '')
  if (normalized && cachedBase !== normalized) {
    cachedBase = normalized
    if (import.meta.env.DEV) {
      console.info('[api] 固定后端前缀:', normalized)
    }
  }
}

export function apiBaseCandidates(): string[] {
  if (cachedBase) {
    return [cachedBase, ...ALL_CANDIDATES.filter((b) => b !== cachedBase)]
  }
  return [...ALL_CANDIDATES]
}

function looksLikeHtml(text: string): boolean {
  const t = text.trimStart().toLowerCase()
  return t.startsWith('<!doctype html') || t.startsWith('<html')
}

async function probeBase(base: string): Promise<boolean> {
  try {
    const res = await fetch(`${base}/portal/categories`, {
      method: 'GET',
      credentials: 'include',
      cache: 'no-store',
    })
    const text = await res.text()
    if (!res.ok || !text || looksLikeHtml(text)) return false
    const json = JSON.parse(text) as { success?: boolean }
    return json.success !== false
  } catch {
    return false
  }
}

async function runProbe(): Promise<string> {
  for (const base of ALL_CANDIDATES) {
    if (await probeBase(base)) {
      cachedBase = base
      if (import.meta.env.DEV) {
        console.info('[api] 使用后端前缀:', base)
      }
      return base
    }
  }
  const fallback = (import.meta.env.VITE_API_BASE || DEFAULT_BASE).replace(/\/$/, '')
  if (import.meta.env.DEV) {
    console.warn('[api] 未探测到可用后端，暂用:', fallback, '（请确认 8080 已启动）')
  }
  return fallback
}

export async function ensureApiBase(): Promise<string> {
  if (cachedBase) return cachedBase
  if (!probePromise) {
    probePromise = runProbe().finally(() => {
      probePromise = null
    })
  }
  return probePromise
}

/** 某次请求明确失败且像是路径错误时，可强制重新探测 */
export function noteApiBaseFailure(message?: string) {
  const m = message || ''
  if (
    m.includes('非 JSON') ||
    m.includes('HTML') ||
    m.includes('接口不存在') ||
    m.includes('404') ||
    m.includes('Failed to fetch') ||
    m.includes('NetworkError')
  ) {
    resetApiBaseCache()
  }
}
