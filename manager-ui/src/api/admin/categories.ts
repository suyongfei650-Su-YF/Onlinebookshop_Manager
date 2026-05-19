import { adminRequest } from './http'

export type AdminCategory = {
  id: number
  parentId: number | null
  name: string
  nameEn?: string | null
  code?: string | null
  sortWeight?: number
  visible?: number
  bookCount?: number
  createdAt?: string | null
}

export type AdminCategoryStats = {
  totalCategories: number
  visibleCount: number
  categorizedBooks: number
  uncategorizedBooks: number
}

export type AdminCategoryForm = {
  name: string
  nameEn: string
  code: string
  parentId: number | null
  sortWeight: number
  visible: number
}

export function adminCategoriesList(keyword?: string) {
  const q = keyword?.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
  return adminRequest<{ list: AdminCategory[]; stats: AdminCategoryStats }>(`/admin/categories${q}`, {
    method: 'GET',
  })
}

export function adminCategoryCreate(body: Partial<AdminCategoryForm>) {
  return adminRequest<AdminCategory>('/admin/categories', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function adminCategoryUpdate(id: number, body: Partial<AdminCategoryForm>) {
  return adminRequest<AdminCategory>(`/admin/categories/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export function adminCategoryToggleVisible(id: number) {
  return adminRequest<AdminCategory>(`/admin/categories/${id}/toggle-visible`, { method: 'POST' })
}

export function adminCategoryDelete(id: number) {
  return adminRequest<void>(`/admin/categories/${id}`, { method: 'DELETE' })
}
