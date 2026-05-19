import { adminApiBaseCandidates, adminRequest, type ApiResult } from './http'

export type AdminBook = {
  id: number
  categoryId: number | null
  categoryName?: string | null
  title: string
  author?: string | null
  isbn?: string | null
  price: number
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF' | string
  coverUrl?: string | null
  description?: string | null
  createdAt?: string | null
}

export type AdminBookForm = {
  categoryId: number | null
  title: string
  author: string
  isbn: string
  price: number
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
  coverUrl: string
  description: string
}

export function resolveAdminBookCover(url?: string | null): string {
  const placeholder = 'https://placehold.co/120x168/2C2420/D4AF37?text=Book'
  if (!url?.trim()) return placeholder
  const u = url.trim()
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:')) return u
  const apiBase = (import.meta.env.VITE_API_BASE || '/Onlinebookshop_Manager/api').replace(/\/$/, '')
  const ctx = apiBase.replace(/\/api$/, '')
  return `${ctx}${u.startsWith('/') ? u : `/${u}`}`
}

export function adminBooksList(params: {
  page: number
  size: number
  keyword?: string
  status?: string
  categoryId?: number
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page))
  q.set('size', String(params.size))
  if (params.keyword?.trim()) q.set('keyword', params.keyword.trim())
  if (params.status?.trim()) q.set('status', params.status.trim())
  if (params.categoryId != null) q.set('categoryId', String(params.categoryId))
  return adminRequest<{ list: AdminBook[]; total: number; page: number; size: number }>(`/admin/books?${q.toString()}`, { method: 'GET' })
}

export function adminBookCreate(body: Partial<AdminBook>) {
  return adminRequest<AdminBook>('/admin/books', { method: 'POST', body: JSON.stringify(body) })
}

export function adminBookUpdate(id: number, body: Partial<AdminBook>) {
  return adminRequest<AdminBook>(`/admin/books/${id}`, { method: 'PUT', body: JSON.stringify(body) })
}

export function adminBookToggleStatus(id: number) {
  return adminRequest<AdminBook>(`/admin/books/${id}/toggle-status`, { method: 'POST' })
}

export function adminBookDelete(id: number) {
  return adminRequest<null>(`/admin/books/${id}`, { method: 'DELETE' })
}

/** 上传图书封面，返回 /book-covers/xxx.jpg */
export async function adminBookCoverUpload(file: File): Promise<ApiResult<{ coverUrl: string }>> {
  const formData = new FormData()
  formData.append('file', file)
  const bases = adminApiBaseCandidates()
  const path = '/admin/books/cover-upload'
  let last: ApiResult<{ coverUrl: string }> = { success: false, message: 'upload failed' }
  for (const base of bases) {
    try {
      const res = await fetch(`${base}${path}`, {
        method: 'POST',
        credentials: 'include',
        body: formData,
      })
      const text = await res.text()
      if (!text.trim()) {
        last = { success: res.ok }
        continue
      }
      const parsed = JSON.parse(text) as ApiResult<{ coverUrl: string }>
      if (res.status === 404) {
        last = parsed
        continue
      }
      return parsed
    } catch (e) {
      last = { success: false, message: String(e) }
    }
  }
  return last
}
