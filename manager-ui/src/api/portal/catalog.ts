import { userRequest } from '../user/http'

export type PortalBook = {
  id: number
  categoryId: number | null
  title: string
  author: string
  isbn: string
  price: number
  stock: number
  status: 'ON_SHELF' | 'OFF_SHELF'
  coverUrl?: string | null
  description?: string | null
}

export type PortalBookDetail = PortalBook & {
  categoryName?: string | null
}

export type PortalCategory = {
  id: number
  parentId: number | null
  name: string
  nameEn?: string | null
  sortWeight?: number
  bookCount: number
}

const PLACEHOLDER =
  'https://placehold.co/240x360/2C2420/D4AF37?text=Book&font=source-sans-pro'

/** 解析图书封面 URL（支持相对路径 /book-covers/...） */
export function resolveBookCover(url?: string | null): string {
  if (!url?.trim()) return PLACEHOLDER
  const u = url.trim()
  if (u.startsWith('http://') || u.startsWith('https://') || u.startsWith('data:')) return u
  const apiBase = (import.meta.env.VITE_API_BASE || '/Onlinebookshop_Manager/api').replace(/\/$/, '')
  const ctx = apiBase.replace(/\/api$/, '')
  return `${ctx}${u.startsWith('/') ? u : `/${u}`}`
}

export function portalCategories() {
  return userRequest<PortalCategory[]>('/portal/categories', { method: 'GET' })
}

export function portalBooksList(params: {
  page: number
  size: number
  keyword?: string
  categoryId?: number | null
  priceMin?: number | null
  priceMax?: number | null
  sort?: 'default' | 'price_asc' | 'price_desc' | 'title'
}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page))
  q.set('size', String(params.size))
  if (params.keyword?.trim()) q.set('keyword', params.keyword.trim())
  if (params.categoryId != null) q.set('categoryId', String(params.categoryId))
  if (params.priceMin != null) q.set('priceMin', String(params.priceMin))
  if (params.priceMax != null) q.set('priceMax', String(params.priceMax))
  if (params.sort) q.set('sort', params.sort)
  return userRequest<{ list: PortalBook[]; total: number; page: number; size: number }>(
    `/portal/books?${q.toString()}`,
    { method: 'GET' },
  )
}

export function portalBookDetail(id: number) {
  return userRequest<PortalBookDetail>(`/portal/books/${id}`, { method: 'GET' })
}

export function formatPrice(price: number | string | null | undefined): string {
  const n = Number(price)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

/** 列表/卡片用简介摘要 */
export function bookDescriptionExcerpt(text?: string | null, maxLen = 72): string {
  if (!text?.trim()) return ''
  const t = text.trim().replace(/\s+/g, ' ')
  return t.length <= maxLen ? t : `${t.slice(0, maxLen)}…`
}
